package xyz.nifeather.morph.network.server;

import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.*;
import xiamomc.morph.network.commands.C2S.*;
import xiamomc.morph.network.commands.CommandRegistries;
import xiamomc.morph.network.commands.S2C.AbstractS2CCommand;
import xiamomc.morph.network.commands.S2C.S2CCurrentCommand;
import xiamomc.morph.network.commands.S2C.S2CReAuthCommand;
import xiamomc.morph.network.commands.S2C.S2CUnAuthCommand;
import xiamomc.morph.network.commands.S2C.query.QueryType;
import xiamomc.morph.network.commands.S2C.query.S2CQueryCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetModifyBoundingBoxCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetSelfViewingCommand;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPlugin;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.interfaces.IManageRequests;
import xyz.nifeather.morph.messages.EmoteStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.NetworkingHelper;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MorphClientHandler extends MorphPluginObject implements BasicClientHandler<Player>
{
    private final Bindable<Boolean> allowClient = new Bindable<>(false);
    private final Bindable<Boolean> logInComingPackets = new Bindable<>(false);
    private final Bindable<Boolean> logOutGoingPackets = new Bindable<>(false);
    private final Bindable<Boolean> forceClient = new Bindable<>(false);
    private final Bindable<Boolean> forceTargetVersion = new Bindable<>(false);

    private record MorphCustomPacketPayload(ResourceLocation channel, byte[] data) implements CustomPacketPayload
    {
        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return null;
        }
    }

    private void sendPacket(String channel, Player player, byte[] message)
    {
        if (channel == null || player == null || message == null)
            throw new IllegalArgumentException("Null channel/player/message");

        if (!player.isOnline() || !(player instanceof CraftPlayer craftPlayer)) return;

        if (logOutGoingPackets.get())
            logPacket(true, player, channel, message);

        try
        {
            var channelLocation = ResourceLocation.parse(channel);
            var packet = new ClientboundCustomPayloadPacket(new DiscardedPayload(channelLocation, Unpooled.wrappedBuffer(message)));

            craftPlayer.getHandle().connection.send(packet);
        }
        catch (Throwable t)
        {
            logger.error("Can't send packet to player: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * 服务端的接口版本
     */
    public final int targetApiVersion = Constants.PROTOCOL_VERSION;

    /**
     * 最低能接受的客户端接口版本
     */
    public final int minimumApiVersion = 1;

    @Resolved
    private MorphManager manager;

    private void logPacket(boolean isOutGoingPacket, Player player, String channel, byte[] data)
    {
        String msg = "<???>";
        var input = ByteStreams.newDataInput(data);

        try
        {
            msg = input.readUTF();
        }
        catch (Throwable t)
        {
            msg = new String(data, StandardCharsets.UTF_8);
            //logger.warn("Unable to convert byte data to string: " + t.getMessage());
        }

        this.logPacket(isOutGoingPacket, player, channel, msg, data.length);
    }

    private void logPacket(boolean isOutGoingPacket, Player player, String channel, String data, int size)
    {
        var arrow = isOutGoingPacket ? " -> " : " <- ";

        String builder = channel + arrow
                + player.getName()
                + " :: "
                + "'%s'".formatted(data)
                + " (≈ %s bytes)".formatted(size);

        logger.info(builder);
    }

    private final CommandRegistries registries = new CommandRegistries();
    private final Bindable<Boolean> modifyBoundingBoxes = new Bindable<>(false);
    private final Bindable<Boolean> useClientRenderer = new Bindable<>(false);

    @Initializer
    private void load(MorphPlugin plugin, MorphConfigManager configManager)
    {
        // Constants.initialize(true);

        registries.registerC2S(C2SCommandNames.Initial, a -> new C2SInitialCommand())
                .registerC2S(C2SCommandNames.Morph, C2SMorphCommand::new)
                .registerC2S(C2SCommandNames.Skill, a -> new C2SSkillCommand())
                .registerC2S(C2SCommandNames.Option, C2SOptionCommand::fromString)
                .registerC2S(C2SCommandNames.ToggleSelf, a -> new C2SToggleSelfCommand(C2SToggleSelfCommand.SelfViewMode.fromString(a)))
                .registerC2S(C2SCommandNames.Unmorph, a -> new C2SUnmorphCommand())
                .registerC2S(C2SCommandNames.Request, C2SRequestCommand::new)
                .registerC2S("animation", C2SAnimationCommand::new);

        var messenger = Bukkit.getMessenger();

        // 注册init频道处理
        messenger.registerIncomingPluginChannel(plugin, initializeChannel, (cN, player, data) ->
        {
            if (!allowClient.get() || this.getPlayerConnectionState(player).greaterThan(InitializeState.HANDSHAKE)) return;

            if (logInComingPackets.get())
                logPacket(false, player, initializeChannel, data);

            playerConnectionStates.put(player, InitializeState.HANDSHAKE);

            this.sendPacket(initializeChannel, player, "".getBytes(StandardCharsets.UTF_8));
        });

        // 注册api频道处理
        messenger.registerIncomingPluginChannel(plugin, versionChannel, (cN, player, data) ->
        {
            if (!allowClient.get()) return;

            var connectionState = this.getPlayerConnectionState(player);

            //初始化之前忽略API请求
            if (connectionState.worseThan(InitializeState.HANDSHAKE)) return;

            //尝试获取API版本
            int clientVersion = 1;

            try
            {
                var clientVersionStr = new String(data, StandardCharsets.UTF_8);

                if (logInComingPackets.get())
                    logPacket(false, player, versionChannel, clientVersionStr, data.length);

                clientVersion = Integer.parseInt(clientVersionStr);
            }
            catch (Throwable t)
            {
                logger.error("Failed to decode client version: " + t.getMessage());
            }

            var minimumApiVersion = this.minimumApiVersion;

            if (forceTargetVersion.get()) minimumApiVersion = targetApiVersion;

            //如果客户端版本低于最低能接受的版本或高于当前版本，拒绝初始化
            if (clientVersion < minimumApiVersion || clientVersion > Constants.PROTOCOL_VERSION)
            {
                unInitializePlayer(player);

                //player.sendMessage(MessageUtils.prefixes(player, MorphStrings.clientVersionMismatchString()));
                logger.info(player.getName() + " joined with incompatible client API version: " + clientVersion + " (This server requires " + targetApiVersion + ")");

                var msg = forceTargetVersion.get() ? MorphStrings.clientVersionMismatchKickString() : MorphStrings.clientVersionMismatchString();
                msg.withLocale(MessageUtils.getLocale(player))
                        .resolve("minimum_version", Component.text(minimumApiVersion))
                        .resolve("player_version", Component.text(clientVersion));

                if (forceTargetVersion.get())
                    player.kick(msg.toComponent());
                else
                    player.sendMessage(msg.toComponent());

                return;
            }

            logger.info(player.getName() + " joined with API version " + clientVersion);

            this.getPlayerOption(player, true).clientApiVersion = clientVersion;
            playerConnectionStates.put(player, InitializeState.API_CHECKED);

            var out = ByteStreams.newDataOutput();
            out.writeInt(targetApiVersion);
            this.sendPacket(versionChannel, player, out.toByteArray());
        });

        // 注册command频道处理
        messenger.registerIncomingPluginChannel(plugin, commandChannel, (cN, player, data) ->
        {
            if (!allowClient.get()) return;

            //在API检查完成之前忽略客户端的所有指令
            if (this.getPlayerConnectionState(player).worseThan(InitializeState.API_CHECKED))
            {
                return;
            }

            if (logInComingPackets.get())
                logPacket(false, player, commandChannel, data);

            var input = new String(data, StandardCharsets.UTF_8);
            var str = input.split(" ", 2);

            if (str.length < 1)
            {
                logger.warn("Incomplete server command: " + input);
                return;
            }

            var baseCommand = str[0];
            var c2sCommand = registries.createC2SCommand(baseCommand, str.length == 2 ? str[1] : "");

            if (c2sCommand != null)
            {
                c2sCommand.setOwner(player);
                c2sCommand.onCommand(this);
            }
            else
            {
                logger.warn("Unknown server command: " + baseCommand);
            }
        });

        // 注册outgoing频道
        messenger.registerOutgoingPluginChannel(plugin, initializeChannel);
        messenger.registerOutgoingPluginChannel(plugin, versionChannel);
        messenger.registerOutgoingPluginChannel(plugin, commandChannel);

        configManager.bind(allowClient, ConfigOption.ALLOW_CLIENT);
        //configManager.bind(forceClient, ConfigOption.FORCE_CLIENT);
        configManager.bind(forceTargetVersion, ConfigOption.FORCE_TARGET_VERSION);

        configManager.bind(logInComingPackets, ConfigOption.LOG_INCOMING_PACKETS);
        configManager.bind(logOutGoingPackets, ConfigOption.LOG_OUTGOING_PACKETS);

        configManager.bind(modifyBoundingBoxes, ConfigOption.MODIFY_BOUNDING_BOX);

        configManager.bind(useClientRenderer, ConfigOption.USE_CLIENT_RENDERER);

        modifyBoundingBoxes.onValueChanged((o, n) ->
        {
            var players = Bukkit.getOnlinePlayers();
            players.forEach(p -> sendCommand(p, new S2CSetModifyBoundingBoxCommand(n)));
        });

        forceTargetVersion.onValueChanged((o, n) -> scheduleReAuthPlayers());
        modifyBoundingBoxes.onValueChanged((o, n) -> scheduleReAuthPlayers());
        useClientRenderer.onValueChanged((o, n) -> scheduleReAuthPlayers());

        allowClient.onValueChanged((o, n) ->
        {
            var players = Bukkit.getOnlinePlayers();
            players.forEach(this::unInitializePlayer);

            if (n)
                this.sendReAuth(players);
            else
                this.sendUnAuth(players);
        });

        Bukkit.getOnlinePlayers().forEach(p -> playerStateMap.put(p, ConnectionState.JOINED));
    }

    private final AtomicBoolean scheduledReauthPlayers = new AtomicBoolean(false);

    //region wait until ready

    public void waitUntilConnected(Player player, Runnable r)
    {
        var bool = playerConnectionStates.getOrDefault(player, null);

        if (bool == null)
        {
            //logger.info("should remove for " + player.getName());
            return;
        }

        if (bool == InitializeState.DONE)
        {
            r.run();
        }
        else
        {
            //logger.info(player.getName() + " not ready! " + bool);
            this.addSchedule(() -> waitUntilConnected(player, r));
        }
    }

    @ApiStatus.Internal
    public void waitUntilReady(Player player, Runnable r)
    {
        var bool = playerStateMap.getOrDefault(player, null);

        if (bool == null)
        {
            //logger.info("should remove for " + player.getName());
            return;
        }

        if (bool == ConnectionState.JOINED)
        {
            r.run();
        }
        else
        {
            //logger.info(player.getName() + " not ready! " + bool);
            this.addSchedule(() -> waitUntilReady(player, r));
        }
    }

    private final Map<Player, ConnectionState> playerStateMap = new Object2ObjectOpenHashMap<>();

    public void markPlayerReady(Player player)
    {
        playerStateMap.put(player, ConnectionState.JOINED);
    }

    //endregion

    private final Map<UUID, PlayerOptions<Player>> playerOptionMap = new Object2ObjectOpenHashMap<>();

    private final Map<Player, InitializeState> playerConnectionStates = new Object2ObjectOpenHashMap<>();

    /**
     * 刷新某个玩家的客户端的伪装列表
     *
     * @param identifiers 伪装列表
     * @param player 目标玩家
     */
    public void refreshPlayerClientMorphs(List<String> identifiers, Player player)
    {
        if (!allowClient.get()) return;

        this.sendCommand(player, new S2CQueryCommand(QueryType.SET, identifiers.toArray(new String[]{})));
    }

    /**
     * 向某个玩家的客户端发送差异信息
     *
     * @param addits 添加
     * @param removal 删除
     * @param player 目标玩家
     */
    public void sendDiff(@Nullable List<String> addits, @Nullable List<String> removal, Player player)
    {
        if (!allowClient.get()) return;

        if (addits != null)
            this.sendCommand(player, new S2CQueryCommand(QueryType.ADD, addits.toArray(new String[]{})));

        if (removal != null)
            this.sendCommand(player, new S2CQueryCommand(QueryType.REMOVE, removal.toArray(new String[]{})));
    }

    /**
     * 更新某一玩家客户端的当前伪装
     *
     * @param player 目标玩家
     * @param str 伪装ID
     */
    public void updateCurrentIdentifier(Player player, String str)
    {
        if (!allowClient.get()) return;

        this.sendCommand(player, new S2CCurrentCommand(str));
    }

    //region Auth/UnAuth/ReAuth

    private void scheduleReAuthPlayers()
    {
        synchronized (scheduledReauthPlayers)
        {
            if (scheduledReauthPlayers.get()) return;
            scheduledReauthPlayers.set(true);
        }

        this.addSchedule(() ->
        {
            if (!scheduledReauthPlayers.get()) return;

            scheduledReauthPlayers.set(false);
            reAuthPlayers();
        });
    }

    private void reAuthPlayers()
    {
        var players = Bukkit.getOnlinePlayers();

        sendUnAuth(players);
        sendReAuth(players);
    }

    /**
     * 反初始化玩家
     *
     * @param player 目标玩家
     */
    public void unInitializePlayer(Player player)
    {
        this.sendCommand(player, new S2CUnAuthCommand(), true);

        synchronized (this)
        {
            playerOptionMap.remove(player.getUniqueId());
            playerStateMap.remove(player);
            playerConnectionStates.remove(player);
        }

        var playerConfig = manager.getPlayerMeta(player);
        var state = manager.getDisguiseStateFor(player);
        if (state != null) state.setServerSideSelfVisible(playerConfig.showDisguiseToSelf);
    }

    /**
     * 向列表中的玩家客户端发送reauth指令
     *
     * @param players 玩家列表
     */
    public void sendReAuth(Collection<? extends Player> players)
    {
        if (!allowClient.get()) return;

        players.forEach(p ->
        {
            playerStateMap.put(p, ConnectionState.JOINED);
            playerConnectionStates.put(p, InitializeState.NOT_CONNECTED);

            sendCommand(p, new S2CReAuthCommand(), true);
        });
    }

    /**
     * 向列表中的玩家客户端发送unauth指令
     *
     * @param players 玩家列表
     */
    public void sendUnAuth(Collection<? extends Player> players)
    {
        players.forEach(this::unInitializePlayer);
    }

    //endregion Auth/UnAuth

    private static final String nameSpace = MorphPlugin.getMorphNameSpace();

    public static final String initializeChannel = nameSpace + ":init";
    public static final String versionChannel = nameSpace + ":version";
    public static final String commandChannel = nameSpace + ":commands";

    //region Player Status/Properties/Option

    public boolean isFutureClientProtocol(Player player, int version)
    {
        return getPlayerVersion(player) >= version;
    }

    /**
     * 获取玩家的连接状态
     *
     * @param player 目标玩家
     * @return {@link InitializeState}, 客户端未连接或初始化被中断时返回 {@link InitializeState#NOT_CONNECTED}
     */
    public InitializeState getPlayerConnectionState(Player player)
    {
        return playerConnectionStates.getOrDefault(player, InitializeState.NOT_CONNECTED);
    }

    /**
     * 检查某个玩家是否使用客户端加入
     *
     * @param player 目标玩家
     * @return 玩家是否使用客户端加入
     * @apiNote 此API只能检查客户端是否已连接，检查初始化状态请使用 {@link MorphClientHandler#clientInitialized(Player)}
     */
    public boolean clientConnected(Player player)
    {
        return this.getPlayerConnectionState(player).greaterThan(InitializeState.NOT_CONNECTED);
    }

    /**
     * 检查某个玩家的客户端是否已初始化
     *
     * @param player 目标玩家
     * @return 此玩家的客户端是否已初始化
     */
    public boolean clientInitialized(Player player)
    {
        return playerConnectionStates.getOrDefault(player, null) == InitializeState.DONE;
    }

    private final PlayerOptions<Player> nilRecord = new PlayerOptions<Player>(null);

    public PlayerOptions<Player> getPlayerOption(Player player, boolean createIfNull)
    {
        var option = this.getPlayerOption(player);

        if (option != null) return option;
        else if (!createIfNull) return null;

        option = new PlayerOptions<>(player);

        playerOptionMap.put(player.getUniqueId(), option);

        return option;
    }

    /**
     * 获取某一玩家的客户端选项
     * @param player 目标玩家
     * @return 此玩家的客户端选项
     */
    @Nullable
    public PlayerOptions<Player> getPlayerOption(Player player)
    {
        var uuid = player.getUniqueId();
        return playerOptionMap.getOrDefault(uuid, null);
    }

    @Override
    public int getPlayerVersion(Player player)
    {
        return this.playerOptionMap.getOrDefault(player.getUniqueId(), nilRecord).clientApiVersion;
    }

    @Override
    public InitializeState getInitializeState(Player player)
    {
        return playerConnectionStates.getOrDefault(player, InitializeState.NOT_CONNECTED);
    }

    @Override
    public boolean isPlayerInitialized(Player player)
    {
        return getInitializeState(player) == InitializeState.DONE;
    }

    @Override
    public boolean isPlayerConnected(Player player)
    {
        return getInitializeState(player).greaterThan(InitializeState.PENDING);
    }

    @Override
    public List<Player> getConnectedPlayers()
    {
        return new ObjectArrayList<>(playerConnectionStates.keySet());
    }

    //endregion Player Status/Option

    @Override
    public void disconnect(Player player)
    {
        unInitializePlayer(player);
    }

    private boolean sendCommand(Player player, AbstractS2CCommand<?> command, boolean forceSend)
    {
        var cmd = command.buildCommand();
        if (cmd == null || cmd.isEmpty() || cmd.isBlank()) return false;

        if ((!allowClient.get() || !this.clientConnected(player)) && !forceSend) return false;

        this.sendPacket(commandChannel, player, cmd.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    @Override
    public boolean sendCommand(Player player, AbstractS2CCommand<?> basicS2CCommand)
    {
        return this.sendCommand(player, basicS2CCommand, false);
    }

    //region C2S(Serverbound) commands

    @Override
    public void onInitialCommand(C2SInitialCommand c2SInitialCommand)
    {
        Player player = c2SInitialCommand.getOwner();

        if (this.clientInitialized(player)) return;

        if (playerStateMap.getOrDefault(player, null) != ConnectionState.JOINED)
            playerStateMap.put(player, ConnectionState.CONNECTING);

        this.waitUntilReady(player, () ->
        {
            //再检查一遍玩家有没有初始化完成
            if (clientInitialized(player))
                return;

            var config = manager.getPlayerMeta(player);
            var list = config.getUnlockedDisguiseIdentifiers();
            refreshPlayerClientMorphs(list, player);

            var state = manager.getDisguiseStateFor(player);

            if (state != null)
                manager.refreshClientState(state);

            sendCommand(player, new S2CSetSelfViewingCommand(config.showDisguiseToSelf));
            sendCommand(player, new S2CSetModifyBoundingBoxCommand(modifyBoundingBoxes.get()));

            if (player.hasPermission(CommonPermissions.DISGUISE_REVEALING))
                sendCommand(player, manager.genMapCommand());

            //TODO: 独立客户端渲染器
            if (state != null && state.getDisguiseWrapper().getBackend().dependsClientRenderer())
            {
                sendCommand(player, manager.genRenderSyncCommand());

                var disguises = manager.getActiveDisguises();
                for (DisguiseState bindingState : disguises)
                {
                    var bindingPlayer = bindingState.getPlayer();

                    var packet = networkingHelper.prepareMeta(bindingPlayer)
                            .forDisguiseState(bindingState)
                            .build();

                    this.sendCommand(player, packet);
                }
            }

            playerConnectionStates.put(player, InitializeState.DONE);
        });
    }

    @Override
    public void onMorphCommand(C2SMorphCommand c2SMorphCommand)
    {
        Player player = c2SMorphCommand.getOwner();
        var id = c2SMorphCommand.getArgumentAt(0, "");

        if (id.isEmpty() || id.isBlank())
            manager.tryQuickDisguise(player);
        else if (manager.canMorph(player))
            manager.morph(player, player, id, player.getTargetEntity(5));
    }

    @Override
    public void onOptionCommand(C2SOptionCommand c2SOptionCommand)
    {
        var option = c2SOptionCommand.getOption();
        Player player = c2SOptionCommand.getOwner();

        switch (option)
        {
            case CLIENTVIEW ->
            {
                var val = Boolean.parseBoolean(c2SOptionCommand.getValue());
                this.getPlayerOption(player, true).setClientSideSelfView(val);

                var state = manager.getDisguiseStateFor(player);
                if (state != null) state.setServerSideSelfVisible(!val);
            }

            case HUD ->
            {
                var val = Boolean.parseBoolean(c2SOptionCommand.getValue());
                this.getPlayerOption(player, true).displayDisguiseOnHUD = val;

                if (!val) player.sendActionBar(Component.empty());
            }
        }
    }

    @Override
    public void onSkillCommand(C2SSkillCommand c2SSkillCommand)
    {
        manager.executeDisguiseSkill(c2SSkillCommand.getOwner());
    }

    @Override
    public void onToggleSelfCommand(C2SToggleSelfCommand c2SToggleSelfCommand)
    {
        Player player = c2SToggleSelfCommand.getOwner();

        var playerOption = this.getPlayerOption(player, true);
        var playerConfig = manager.getPlayerMeta(player);

        switch (c2SToggleSelfCommand.getSelfViewMode())
        {
            case ON ->
            {
                if (playerConfig.showDisguiseToSelf) return;
                manager.setSelfDisguiseVisible(player, true, true, false, false);
            }

            case OFF ->
            {
                if (!playerConfig.showDisguiseToSelf) return;
                manager.setSelfDisguiseVisible(player, false, true, false, false);
            }

            case CLIENT_ON ->
            {
                playerOption.setClientSideSelfView(true);

                var state = manager.getDisguiseStateFor(player);

                if (state != null)
                    state.setServerSideSelfVisible(false);
            }

            case CLIENT_OFF ->
            {
                playerOption.setClientSideSelfView(false);

                var state = manager.getDisguiseStateFor(player);

                if (state != null)
                    state.setServerSideSelfVisible(true);
            }
        }
    }

    @Override
    public void onUnmorphCommand(C2SUnmorphCommand c2SUnmorphCommand)
    {
        manager.unMorph(c2SUnmorphCommand.getOwner());
    }

    @Resolved
    private IManageRequests requestManager;

    @Resolved
    private NetworkingHelper networkingHelper;

    @Override
    public void onRequestCommand(C2SRequestCommand c2SRequestCommand)
    {
        Player player = c2SRequestCommand.getOwner();
        var target = c2SRequestCommand.targetRequestName;
        var deceison = c2SRequestCommand.decision;

        if (target.equalsIgnoreCase("unknown") || deceison == C2SRequestCommand.Decision.UNKNOWN)
        {
            logger.warn("Received an invalid request response");
            return;
        }

        var targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null) return;

        if (deceison == C2SRequestCommand.Decision.ACCEPT)
            requestManager.acceptRequest(player, targetPlayer);
        else
            requestManager.denyRequest(player, targetPlayer);
    }

    @Override
    public void onAnimationCommand(C2SAnimationCommand c2SAnimationCommand)
    {
        var player = (Player) c2SAnimationCommand.getOwner();
        var state = manager.getDisguiseStateFor(player);
        if (state == null) return;

        var animationProvider = state.getProvider().getAnimationProvider();
        var disguiseID = state.getDisguiseIdentifier();
        var animationID = c2SAnimationCommand.getAnimationId();
        var sequencePair =  animationProvider.getAnimationSetFor(disguiseID).sequenceOf(animationID);

        if (!state.tryScheduleSequence(animationID, sequencePair.left(), sequencePair.right()))
            player.sendMessage(MessageUtils.prefixes(player, EmoteStrings.notAvailable()));
    }

    //endregion C2S(Serverbound) commands
}
