package xyz.nifeather.morph.network.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
import xyz.nifeather.morph.network.server.handlers.CommandPacketHandler;
import xyz.nifeather.morph.network.server.handlers.ICommandPacketHandler;
import xyz.nifeather.morph.network.server.handlers.LegacyCommandPacketHandler;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MorphClientHandler extends MorphPluginObject implements BasicClientHandler<Player>
{
    private final Bindable<Boolean> allowClient = new Bindable<>(false);
    private final Bindable<Boolean> logInComingPackets = new Bindable<>(false);
    private final Bindable<Boolean> logOutGoingPackets = new Bindable<>(false);
    private final Bindable<Boolean> forceClient = new Bindable<>(false);
    private final Bindable<Boolean> forceTargetVersion = new Bindable<>(false);

    public boolean allowClient()
    {
        return allowClient.get();
    }

    public boolean logInComingPackets()
    {
        return logInComingPackets.get();
    }

    private void sendPacket(String channel, Player player, String message, boolean legacy)
    {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        if (!legacy)
            buffer.writeUtf(message);
        else
            buffer.writeBytes(message.getBytes(StandardCharsets.UTF_8));

        if (logOutGoingPackets.get())
            logPacket(true, player, channel, message, buffer.readableBytes());

        this.sendPacketRaw(channel, player, buffer, false);
    }

    private void sendPacket(String channel, Player player, int integer)
    {
        var buffer = new FriendlyByteBuf(Unpooled.buffer()).writeInt(integer);

        if (logOutGoingPackets.get())
            logPacket(true, player, channel, "" + integer, buffer.array().length);

        this.sendPacketRaw(channel, player, buffer, false);
    }

    private void sendPacketRaw(String channel, Player player, ByteBuf buffer)
    {
        sendPacketRaw(channel, player, buffer, true);
    }

    private void sendPacketRaw(String channel, Player player, ByteBuf buffer, boolean logData)
    {
        if (channel == null || player == null || buffer == null)
            throw new IllegalArgumentException("Null channel/player/message");

        if (!player.isOnline() || !(player instanceof CraftPlayer craftPlayer)) return;

        if (logData && logOutGoingPackets.get())
            logPacket(true, player, channel, buffer.array());

        try
        {
            var channelLocation = ResourceLocation.parse(channel);
            var packet = new ClientboundCustomPayloadPacket(new DiscardedPayload(channelLocation, buffer));

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
        var input = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

        try
        {
            msg = input.readUtf();
        }
        catch (Throwable t)
        {
            msg = "<base64> " + Base64.getEncoder().encodeToString(data);
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

    @ApiStatus.Internal
    public CommandRegistries getRegistries()
    {
        return this.registries;
    }

    private final Bindable<Boolean> modifyBoundingBoxes = new Bindable<>(false);
    private final Bindable<Boolean> useClientRenderer = new Bindable<>(false);
    private final Bindable<Boolean> debugOutput = new Bindable<>(false);

    private static final String newProtocolIdentify = "1_21_3_packetbuf";

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

        // 注册incoming频道
        messenger.registerIncomingPluginChannel(plugin, MessageChannel.initializeChannel, this::handleInitializeMessage);

        messenger.registerIncomingPluginChannel(plugin, MessageChannel.versionChannel, this::handleVersionMessage);
        messenger.registerIncomingPluginChannel(plugin, MessageChannel.commandChannel, this::handleCommandMessage);

        // Legacy incoming channels
        // todo: Remove legacy packetbuf support along with 1.22 update
        messenger.registerIncomingPluginChannel(plugin, MessageChannel.versionChannelLegacy, this::handleVersionMessageLegacy);
        messenger.registerIncomingPluginChannel(plugin, MessageChannel.commandChannelLegacy, this::handleCommandMessageLegacy);

        // 注册outgoing频道
        messenger.registerOutgoingPluginChannel(plugin, MessageChannel.initializeChannel);
        messenger.registerOutgoingPluginChannel(plugin, MessageChannel.versionChannel);
        messenger.registerOutgoingPluginChannel(plugin, MessageChannel.commandChannel);

        configManager.bind(allowClient, ConfigOption.ALLOW_CLIENT);
        //configManager.bind(forceClient, ConfigOption.FORCE_CLIENT);
        configManager.bind(forceTargetVersion, ConfigOption.FORCE_TARGET_VERSION);

        configManager.bind(logInComingPackets, ConfigOption.LOG_INCOMING_PACKETS);
        configManager.bind(logOutGoingPackets, ConfigOption.LOG_OUTGOING_PACKETS);

        configManager.bind(modifyBoundingBoxes, ConfigOption.MODIFY_BOUNDING_BOX);

        configManager.bind(useClientRenderer, ConfigOption.USE_CLIENT_RENDERER);

        configManager.bind(debugOutput, ConfigOption.DEBUG_OUTPUT);

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

        Bukkit.getOnlinePlayers().forEach(p ->
        {
            var session = this.getOrCreateSession(p);
            session.connectionState = ConnectionState.JOINED;
        });
    }

    private final AtomicBoolean scheduledReauthPlayers = new AtomicBoolean(false);

    public void handleInitializeMessage(@NotNull String cN, @NotNull Player player, byte @NotNull [] data)
    {
        if (!allowClient.get() || this.getPlayerConnectionState(player).greaterThan(InitializeState.HANDSHAKE)) return;

        if (logInComingPackets.get())
            logPacket(false, player, MessageChannel.initializeChannel, data);

        boolean isLegacyBuf = false;

        try
        {
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

            var utfData = Arrays.stream(buffer.readUtf().split(" ")).toList();

            // 似乎有点多余，因为使用新版序列化方法的客户端总是会发送这个id
            if (utfData.stream().noneMatch(s -> s.equals(newProtocolIdentify)))
            {
                isLegacyBuf = true;
                logger.info("'%s' is using a legacy client.".formatted(player.getName()));
                //rejectPlayer(player);
                //return;
            }

            buffer.clear();
        }
        catch (Throwable t)
        {
            isLegacyBuf = true;
            logger.info("'%s' is using a legacy client.".formatted(player.getName()));

            if (debugOutput.get())
            {
                logger.info("Unable to decode packet. Is '%s' using a legacy client? %s".formatted(player.getName(), t.getMessage()));
                t.printStackTrace();
            }

            //rejectPlayer(player);
            //return;
        }

        var session = getOrCreateSession(player);
        session.initializeState = InitializeState.HANDSHAKE;
        session.isLegacyPacketBuf = isLegacyBuf;

        this.sendPacket(MessageChannel.initializeChannel, player, newProtocolIdentify, isLegacyBuf);
    }

    private void handleVersionInput(ICommandPacketHandler commandPacketHandler, String sourceChannel, Player player, byte[] data)
    {
        if (!allowClient.get()) return;

        if (logInComingPackets.get())
            logPacket(false, player, sourceChannel, data);

        var result = commandPacketHandler.handleVersionData(player, data);
        if (!result.success())
        {
            logger.info("Packet decode failed for player %s, Rejecting...".formatted(player.getName()));
            rejectPlayer(player);
            return;
        }

        int clientVersion = result.result();

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

        var session = this.getOrCreateSession(player);
        session.initializeState = InitializeState.API_CHECKED;

        if (!session.isLegacyPacketBuf)
            this.sendPacket(MessageChannel.versionChannel, player, targetApiVersion);
        else
            this.sendPacket(MessageChannel.versionChannelLegacy, player, targetApiVersion);
    }

    private void handleCommandInput(ICommandPacketHandler commandPacketHandler, String sourceChannel, Player player, byte[] data)
    {
        if (!allowClient.get()) return;

        if (logInComingPackets.get())
            logPacket(false, player, sourceChannel, data);

        var session = getSession(player);
        if (session == null || session.initializeState.worseThan(InitializeState.API_CHECKED)) return;

        var result = commandPacketHandler.handleCommandData(player, data);
        if (!result.success())
        {
            logger.info("Packet decode failed for player %s, Rejecting...".formatted(player.getName()));
            rejectPlayer(player);
            return;
        }

        String input = result.result();

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
            logger.warn("Unknown server command '%s', rejecting...".formatted(baseCommand));
            rejectPlayer(player);
        }
    }

    private void handleCommandMessageLegacy(@NotNull String cN, @NotNull Player player, @NotNull byte[] data)
    {
        this.handleCommandInput(LegacyCommandPacketHandler.INSTANCE, cN, player, data);
    }

    private void handleVersionMessageLegacy(@NotNull String cN, @NotNull Player player, @NotNull byte[] data)
    {
        this.handleVersionInput(LegacyCommandPacketHandler.INSTANCE, cN, player, data);
    }

    public void handleVersionMessage(@NotNull String cN, @NotNull Player player, byte @NotNull [] data)
    {
        this.handleVersionInput(CommandPacketHandler.INSTANCE, cN, player, data);
    }

    public void handleCommandMessage(@NotNull String cN, @NotNull Player player, byte @NotNull [] data)
    {
        this.handleCommandInput(CommandPacketHandler.INSTANCE, cN, player, data);
    }

    private final Map<Player, PlayerSession> playerSessionMap = new ConcurrentHashMap<>();

    private PlayerSession createSession(Player player, boolean isUsingLegacyBuf)
    {
        var uuid = player.getUniqueId();

        var cached = playerSessionMap.getOrDefault(player, null);

        if (cached != null)
            return cached;

        var instance = PlayerSession.SessionBuilder
                .builder(player)
                .isLegacy(isUsingLegacyBuf)
                .build();

        playerSessionMap.put(player, instance);
        return instance;
    }

    @Nullable
    public PlayerSession getSession(Player player)
    {
        return playerSessionMap.getOrDefault(player, null);
    }

    @NotNull
    public PlayerSession getOrCreateSession(Player player)
    {
        return createSession(player, false);
    }

    //region wait until ready

    @ApiStatus.Internal
    public void waitUntilReady(Player player, Runnable r)
    {
        var session = getOrCreateSession(player);

        if (session.connectionState == ConnectionState.JOINED)
        {
            r.run();
        }
        else
        {
            //logger.info(player.getName() + " not ready! " + bool);
            this.addSchedule(() -> waitUntilReady(player, r));
        }
    }

    public void markPlayerReady(Player player)
    {
        var session = getOrCreateSession(player);
        session.connectionState = ConnectionState.JOINED;
    }

    //endregion

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

    public void rejectPlayer(Player player)
    {
        logger.info("Rejecting player " + player.getName());
        player.sendMessage(MessageUtils.prefixes(player, MorphStrings.unsupportedClientBehavior()));

        this.unInitializePlayer(player);
    }

    /**
     * 反初始化玩家
     *
     * @param player 目标玩家
     */
    public void unInitializePlayer(Player player)
    {
        this.sendCommand(player, new S2CUnAuthCommand(), true);

        this.playerSessionMap.remove(player);

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
            var session = this.getSession(p);

            if (session == null) return;

            session.connectionState = ConnectionState.JOINED;
            session.initializeState = InitializeState.NOT_CONNECTED;

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
        var session = this.getSession(player);
        if (session == null) return InitializeState.NOT_CONNECTED;

        return session.initializeState;
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
        var session = this.getSession(player);
        if (session == null) return false;

        return session.initializeState == InitializeState.DONE;
    }

    private final PlayerOptions<Player> nilRecord = new PlayerOptions<Player>(null);

    @Nullable
    @Contract("_, false -> null; _, true -> !null")
    public PlayerOptions<Player> getPlayerOption(Player player, boolean createIfNull)
    {
        var session = getSession(player);

        if (session != null)
            return session.options;
        else if (!createIfNull)
            return null;

        return createSession(player, false).options;
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

        var session = getSession(player);
        if (session == null) return null;

        return session.options;
    }

    @Override
    public int getPlayerVersion(Player player)
    {
        var option = getPlayerOption(player);

        return option == null ? -1 : option.clientApiVersion;
    }

    @Override
    public InitializeState getInitializeState(Player player)
    {
        var session = getSession(player);

        return session == null ? InitializeState.NOT_CONNECTED : session.initializeState;
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
        return playerSessionMap.keySet().stream().toList();
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

        var session = this.getSession(player);
        var isLegacy = session == null || session.isLegacyPacketBuf;

        if (!isLegacy)
            this.sendPacket(MessageChannel.commandChannel, player, cmd, false);
        else
            this.sendPacket(MessageChannel.commandChannelLegacy, player, cmd, true);

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

        var session = getOrCreateSession(player);
        if (session.connectionState != ConnectionState.JOINED)
            session.connectionState = ConnectionState.CONNECTING;

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

        session.initializeState = InitializeState.DONE;
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
