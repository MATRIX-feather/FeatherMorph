package xyz.nifeather.morph;

import ca.spottedleaf.moonrise.common.util.TickThread;
import it.unimi.dsi.fastutil.objects.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.backends.DisguiseBackend;
import xyz.nifeather.morph.backends.DisguiseWrapper;
import xyz.nifeather.morph.backends.WrapperProperties;
import xyz.nifeather.morph.backends.fallback.NilBackend;
import xyz.nifeather.morph.backends.server.ServerBackend;
import xyz.nifeather.morph.events.api.gameplay.*;
import xyz.nifeather.morph.misc.*;
import xyz.nifeather.morph.misc.playerList.PlayerListHandler;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.events.api.lifecycle.ManagerFinishedInitializeEvent;
import xyz.nifeather.morph.interfaces.IManagePlayerData;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.HintStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapAddCommand;
import xiamomc.morph.network.commands.S2C.clientrender.S2CRenderMapSyncCommand;
import xiamomc.morph.network.commands.S2C.map.S2CMapCommand;
import xiamomc.morph.network.commands.S2C.map.S2CMapRemoveCommand;
import xiamomc.morph.network.commands.S2C.map.S2CPartialMapCommand;
import xiamomc.morph.network.commands.S2C.set.*;
import xyz.nifeather.morph.network.multiInstance.MultiInstanceService;
import xyz.nifeather.morph.network.multiInstance.protocol.Operation;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.providers.disguise.DisguiseProvider;
import xyz.nifeather.morph.providers.disguise.FallbackDisguiseProvider;
import xyz.nifeather.morph.providers.disguise.PlayerDisguiseProvider;
import xyz.nifeather.morph.providers.disguise.VanillaDisguiseProvider;
import xyz.nifeather.morph.skills.MorphSkillHandler;
import xyz.nifeather.morph.skills.SkillCooldownInfo;
import xyz.nifeather.morph.skills.SkillType;
import xyz.nifeather.morph.storage.offlinestore.OfflineDisguiseState;
import xyz.nifeather.morph.storage.offlinestore.OfflineStateStore;
import xyz.nifeather.morph.storage.playerdata.PlayerDataStoreNew;
import xyz.nifeather.morph.storage.playerdata.PlayerMeta;
import xyz.nifeather.morph.utilities.DisguiseUtils;
import xyz.nifeather.morph.utilities.PermissionUtils;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Bindables.BindableList;

import java.io.InvalidObjectException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MorphManager extends MorphPluginObject implements IManagePlayerData
{
    private final List<DisguiseState> activeDisguises = ObjectLists.synchronize(new ObjectArrayList<>());

    private final PlayerDataStoreNew data = new PlayerDataStoreNew();

    private final OfflineStateStore offlineStorage = new OfflineStateStore();

    @Resolved
    private MorphSkillHandler skillHandler;

    @Resolved
    private MorphConfigManager config;

    @Resolved
    private NetworkingHelper networkingHelper;

    @Resolved
    private MultiInstanceService multiInstanceService;

    @Resolved
    private DisguiseProperties disguiseProperties;

    public static final DisguiseProvider fallbackProvider = new FallbackDisguiseProvider();

    public static final String disguiseFallbackName = "@default";

    public static final String forcedDisguiseNoneId = "@none";

    //region Backends

    private final NilBackend nilBackend = new NilBackend();

    @NotNull
    private DisguiseBackend<?, ?> defaultBackend = nilBackend;

    @NotNull
    public DisguiseBackend<?, ?> getDefaultBackend()
    {
        return defaultBackend;
    }

    private final Map<String, DisguiseBackend<?, ?>> backends = new Object2ObjectArrayMap<>();

    public boolean registerBackend(DisguiseBackend<?, ?> backend)
    {
        var id = backend.getIdentifier();

        if (backends.containsKey(id))
            return false;

        backends.put(id, backend);

        return true;
    }

    @Nullable
    public DisguiseBackend<?, ?> getBackend(String id)
    {
        return backends.getOrDefault(id, null);
    }

    @Nullable
    public <I, W extends DisguiseWrapper<I>, T extends DisguiseBackend<I, W>> T getBackend(String id, Class<T> exceptedClass)
    {
        var backend = this.getBackend(id);

        if (exceptedClass.isInstance(backend))
            return (T) backend;

        return null;
    }

    /**
     * @return A list that contains all backends registered to this MorphManager instance
     */
    public Collection<DisguiseBackend<?, ?>> listManagedBackends()
    {
        return backends.values();
    }

    public boolean switchBackend(DisguiseBackend<?, ?> backend)
    {
        if (!backends.containsKey(backend.getIdentifier()))
        {
            logger.error("Trying to switch to a backend that is not registered");
            return false;
        }

        try
        {
            defaultBackend = backend;

            activeDisguises.forEach(state ->
            {
                state.getDisguiseWrapper().getBackend().unDisguise(state.getPlayer());

                //TODO: 更改默认后端时刷新伪装
                //var newWrapper = backend.cloneWrapperFrom(state.getDisguiseWrapper());
                //state.updateDisguise(
                //        state.getDisguiseIdentifier(), state.skillLookupIdentifier(),
                //        newWrapper, false, state.getDisguisedItems()
                //);

                // 等待1tick让客户端处理一些网络事务
                this.addSchedule(() ->
                {
                    if (!state.getDisguiseWrapper().disposed())
                        backend.disguise(state.getPlayer(), state.getDisguiseWrapper());
                });
            });
        }
        catch (Throwable t)
        {
            logger.error("Error occurred switching backend: " + t.getMessage());
            t.printStackTrace();

            return false;
        }

        return true;
    }

    private void tryBackends()
    {
        try
        {
            var serverBackend = new ServerBackend();

            registerBackend(serverBackend);
            switchBackend(serverBackend);
        }
        catch (NoClassDefFoundError e)
        {
            logger.error("Unable to initialize ServerBackend as our disguise backend, maybe ProtocolLib is not installed on the server.");
            logger.error("Using NilBackend, displaying disguises at the server side will not be supported this run.");
        }
        catch (Throwable t)
        {
            logger.error("Unable to initialize ServerBackend as our disguise backend: " + t.getMessage());
            logger.error("Using NilBackend, displaying disguises at the server side will not be supported this run.");
            logger.error("Please consider reporting this issue to our GitHub: https://github.com/MATRIX-feather/FeatherMorph/issues");

            t.printStackTrace();
        }
    }

    //endregion Backends

    @Deprecated(forRemoval = true)
    public Material getActionItem()
    {
        return Material.AIR;
    }

    @Resolved
    private MorphClientHandler clientHandler;

    @Initializer
    private void load()
    {
        this.addSchedule(this::update);

        registerBackend(nilBackend);
        tryBackends();

        logger.info("Default backend: %s".formatted(defaultBackend));

        bannedDisguises = config.getBindableList(String.class, ConfigOption.BANNED_DISGUISES);
        disabledWorlds = config.getBindableList(String.class, ConfigOption.DISGUISE_DISABLED_WORLDS);

        config.bind(allowHeadMorph, ConfigOption.ALLOW_HEAD_MORPH);
        config.bind(allowAcquireMorph, ConfigOption.ALLOW_ACQUIRE_MORPHS);
        config.bind(useClientRenderer, ConfigOption.USE_CLIENT_RENDERER);
        config.bind(hideDisguisedPlayers, ConfigOption.HIDE_DISGUISED_PLAYERS_IN_TAB);

        registerProviders(ObjectList.of(
                new VanillaDisguiseProvider(),
                new PlayerDisguiseProvider(),
                //new ItemDisplayProvider(),
                //new LocalDisguiseProvider(),
                fallbackProvider
        ));

        Bukkit.getPluginManager().callEvent(new ManagerFinishedInitializeEvent(this));
    }

    public boolean disguiseDisabledInWorld(World world)
    {
        return disguiseDisabledInWorld(world.getName());
    }

    public boolean disguiseDisabledInWorld(String worldName)
    {
        return disabledWorlds.contains(worldName);
    }

    private void update()
    {
        this.addSchedule(this::update);

        var states = this.getActiveDisguises();

        states.forEach(state ->
        {
            var p = state.tryGetPlayer();

            if (p == null) return;

            if (!TickThread.isTickThreadFor(NmsRecord.ofPlayer(p)))
                this.scheduleOn(p, () -> this.updateDisguiseSingle(state));
            else
                this.updateDisguiseSingle(state);
        });
    }

    private void updateDisguiseSingle(DisguiseState state)
    {
        var player = state.getPlayer();

        //logger.info("Run at " + plugin.getCurrentTick() + " -> " + i);

        //跳过离线玩家
        if (!player.isOnline() || state.disposed()) return;

        boolean stateSuccess = false;
        boolean providerSuccess = false;

        try
        {
            providerSuccess = state.getProvider().updateDisguise(player, state);
            stateSuccess = state.selfUpdate();
        }
        catch (Throwable t)
        {
            logger.error("Error occurred updating disguise! " + t.getMessage());
            t.printStackTrace();
        }

        if (!providerSuccess || !stateSuccess)
        {
            player.sendMessage(MessageUtils.prefixes(player, MorphStrings.errorWhileUpdatingDisguise()));

            unMorph(nilCommandSource, player, true, true);
        }
    }

    //region 玩家伪装相关

    /**
     * 使某个玩家执行伪装的主动技能
     * @param player 目标玩家
     */
    public void executeDisguiseSkill(Player player)
    {
        skillHandler.executeDisguiseSkill(player);
    }

    /**
     * 获取所有已伪装的玩家
     * @return 玩家列表
     * @apiNote 列表中的玩家可能已经离线
     */
    public List<DisguiseState> getActiveDisguises()
    {
        return new ObjectArrayList<>(activeDisguises);
    }

    private final Map<UUID, Long> uuidMoprhTimeMap = new ConcurrentHashMap<>();

    /**
     * 检查某个玩家是否可以伪装
     * @param player 玩家
     * @return 是否可以伪装
     */
    public boolean canMorph(Player player)
    {
        return this.canMorph(player.getUniqueId());
    }

    /**
     * 检查某个玩家是否可以伪装
     * @param uuid 玩家UUID
     * @return 是否可以伪装
     */
    public boolean canMorph(UUID uuid)
    {
        var val = uuidMoprhTimeMap.get(uuid);

        return val == null || plugin.getCurrentTick() - val >= 4;
    }

    /**
     * 更新某个玩家的上次伪装操作事件
     * @param player 要更新的玩家
     */
    public void updateLastPlayerMorphOperationTime(Player player)
    {
        uuidMoprhTimeMap.put(player.getUniqueId(), plugin.getCurrentTick());
    }

    private BindableList<String> bannedDisguises;
    private BindableList<String> disabledWorlds;

    /**
     * 内部轮子，检查某个伪装是否被禁用建议使用 {@link MorphManager#disguiseDisabled(String)}
     */
    @ApiStatus.Internal
    public BindableList<String> getBannedDisguises()
    {
        return bannedDisguises;
    }

    //region 伪装提供器

    private static final List<DisguiseProvider> providers = new ObjectArrayList<>();

    public static List<DisguiseProvider> getProviders()
    {
        return new ObjectArrayList<>(providers);
    }

    /**
     * 从ID获取DisguiseProvider
     * @param id 目标ID
     * @return 一个DisguiseProvider，若没找到或id是null则返回null
     */
    @NotNull
    public static DisguiseProvider getProvider(String id)
    {
        if (id == null) return null;

        id += ":";
        var splitedId = id.split(":", 2);

        return providers.stream().filter(p -> p.getNameSpace().equals(splitedId[0])).findFirst().orElse(fallbackProvider);
    }

    /**
     * 注册一个DisguiseProvider
     * @param provider 目标Provider
     * @return 操作是否成功
     */
    public boolean registerProvider(DisguiseProvider provider)
    {
        //logger.info("Registering disguise provider: " + provider.getNameSpace());

        if (provider.getNameSpace().contains(":"))
        {
            logger.error("Can't register disguise provider: Illegal character found in namespace: ':'");
            return false;
        }

        if (providers.stream().anyMatch(p -> p.getNameSpace().equals(provider.getNameSpace())))
        {
            logger.error("Can't register disguise provider: Another provider instance already registered as " + provider.getNameSpace() + " !");
            return false;
        }

        providers.add(provider);
        return true;
    }

    /**
     * 注册一批DisguiseProvider
     * @param providers Provider列表
     * @return 所有操作是否成功
     */
    public boolean registerProviders(List<DisguiseProvider> providers)
    {
        AtomicBoolean success = new AtomicBoolean(false);

        providers.forEach(p -> success.set(registerProvider(p) || success.get()));

        return success.get();
    }

    //endregion

    private final Bindable<Boolean> allowHeadMorph = new Bindable<>(true);

    private final Bindable<Boolean> allowAcquireMorph = new Bindable<>(true);
    private final Bindable<Boolean> useClientRenderer = new Bindable<>(false);

    private final Bindable<Boolean> hideDisguisedPlayers = new Bindable<>(false);

    private final Map<UUID, PlayerTextures> uuidPlayerTexturesMap = new ConcurrentHashMap<>();

    /**
     * 尝试调用快速伪装
     *
     * @param player 发起玩家
     * @return 操作是否成功
     */
    public boolean tryQuickDisguise(Player player)
    {
        var state = this.getDisguiseStateFor(player);
        var mainHandItem = player.getEquipment().getItemInMainHand();
        var mainHandItemType = mainHandItem.getType();

        // 检查是否可以通过头颅伪装
        if (DisguiseUtils.validForHeadMorph(mainHandItemType))
        {
            if (!allowHeadMorph.get())
            {
                //player.sendMessage(MessageUtils.prefixes(player, MorphStrings.headDisguiseDisabledString()));

                return true;
            }

            if (!player.hasPermission(CommonPermissions.HEAD_MORPH))
            {
                player.sendMessage(MessageUtils.prefixes(player, CommandStrings.noPermissionMessage()));

                return true;
            }

            if (!canMorph(player))
            {
                player.sendMessage(MessageUtils.prefixes(player, MorphStrings.disguiseCoolingDownString()));

                return true;
            }

            var targetEntity = player.getTargetEntity(5);

            switch (mainHandItemType)
            {
                case PIGLIN_HEAD -> morphOrUnMorph(player, EntityType.PIGLIN.getKey().asString(), targetEntity);
                case DRAGON_HEAD -> morphOrUnMorph(player, EntityType.ENDER_DRAGON.getKey().asString(), targetEntity);
                case ZOMBIE_HEAD -> morphOrUnMorph(player, EntityType.ZOMBIE.getKey().asString(), targetEntity);
                case SKELETON_SKULL -> morphOrUnMorph(player, EntityType.SKELETON.getKey().asString(), targetEntity);
                case WITHER_SKELETON_SKULL -> morphOrUnMorph(player, EntityType.WITHER_SKELETON.getKey().asString(), targetEntity);
                case PLAYER_HEAD ->
                {
                    var profile = ((SkullMeta) mainHandItem.getItemMeta()).getPlayerProfile();

                    //忽略没有profile的玩家伪装
                    if (profile == null)
                    {
                        player.sendMessage(MessageUtils.prefixes(player, MorphStrings.invalidSkinString()));
                        return true;
                    }

                    var name = profile.getName();
                    var profileTexture = profile.getTextures();
                    var playerUniqueId = player.getUniqueId();

                    //如果玩家有伪装，并且伪装的材质和Profile中的一样，那么取消伪装
                    if (state != null)
                    {
                        var disguise = state.getDisguiseWrapper();

                        if (disguise.isPlayerDisguise()
                                && disguise.getDisguiseName().equals(name)
                                && profileTexture.equals(uuidPlayerTexturesMap.get(playerUniqueId)))
                        {
                            unMorph(player);
                            return true;
                        }
                    }

                    //否则，更新或应用伪装
                    if (morph(player, player, DisguiseTypes.PLAYER.toId(profile.getName()), targetEntity))
                        uuidPlayerTexturesMap.put(playerUniqueId, profileTexture);
                }
            }

            updateLastPlayerMorphOperationTime(player);
        }
        else
        {
            var targetedEntity = player.getTargetEntity(5);

            if (targetedEntity != null)
            {
                String targetKey;

                if (targetedEntity instanceof Player targetPlayer)
                {
                    var playerState = this.getDisguiseStateFor(targetPlayer);

                    //目标实体是玩家：玩家伪装ID > 玩家名
                    targetKey = playerState != null
                            ? playerState.getDisguiseIdentifier()
                            : DisguiseTypes.PLAYER.toId(targetPlayer.getName());
                }
                else
                {
                    //否则：伪装ID > 生物类型
                    var theirState = this.getDisguiseStateFor(targetedEntity);
                    targetKey = theirState != null
                            ? theirState.getDisguiseIdentifier()
                            : targetedEntity.getType().getKey().asString();
                }

                morph(player, player, targetKey, targetedEntity);

                return true;
            }
        }

        return false;
    }

    /**
     * 通过给定的key来决定要伪装还是取消伪装。
     * 如果伪装ID和给定的ID一致，则取消伪装，反之进行伪装。
     *
     * @param player 目标玩家
     * @param key 伪装ID
     * @param targetEntity 目标实体（如果有）
     */
    public void morphOrUnMorph(Player player, String key, @Nullable Entity targetEntity)
    {
        var state = this.getDisguiseStateFor(player);

        if (state != null && state.getDisguiseIdentifier().equals(key))
            unMorph(player);
        else
            morph(player, player, key, targetEntity);
    }

    /**
     * 伪装某一玩家
     *
     * @param source 伪装发起方
     * @param player 目标玩家
     * @param key 伪装ID
     * @param targetEntity 目标实体（如果有）
     * @return 操作是否成功
     */
    public boolean morph(CommandSender source, Player player,
                         String key, @Nullable Entity targetEntity)
    {
        var parameters = MorphParameters.create(player, key)
                .setSource(source)
                .setTargetedEntity(targetEntity);

        return this.doDisguise(parameters);
    }

    /**
     * 伪装某一玩家
     *
     * @param source 伪装发起方
     * @param player 目标玩家
     * @param key 伪装ID
     * @param targetEntity 目标实体（如果有）
     * @param forceExecute 是否强制执行
     * @return 操作是否成功
     */
    public boolean morph(CommandSender source, Player player,
                         String key, @Nullable Entity targetEntity,
                         boolean forceExecute)
    {
        var parameters = MorphParameters.create(player, key)
                .setForceExecute(forceExecute)
                .setSource(source)
                .setTargetedEntity(targetEntity);

        return this.doDisguise(parameters);
    }

    public boolean morph(MorphParameters parameters)
    {
        return doDisguise(parameters);
    }

    private boolean doDisguise(MorphParameters parameters)
    {
        var source = parameters.commandSource == null ? parameters.targetPlayer : parameters.commandSource;

        try
        {
            var meta = preDisguise(parameters);
            if (meta == null)
                return false;

            // 更新上次操作时间
            updateLastPlayerMorphOperationTime(parameters.targetPlayer);

            var validateResult = validateDisguise(meta);
            switch (validateResult)
            {
                case VALIDATE_NO_ISSUE -> {}

                case VALIDATE_NO_PROVIDER ->
                {
                    logger.error("Unable to find any provider that matches the identifier '%s'".formatted(parameters.targetDisguiseIdentifier()));
                    source.sendMessage(MessageUtils.prefixes(source, MorphStrings.disguiseBannedOrNotSupportedString()));
                    return false;
                }

                case VALIDATE_PROVIDER_FAIL ->
                {
                    source.sendMessage(MessageUtils.prefixes(source, MorphStrings.invalidIdentityString()));
                    return false;
                }

                // In case I forgot something...
                default -> throw new InvalidObjectException("Invalid validate result: " + validateResult);
            }

            var buildResult = buildDisguise(parameters, meta);
            if (!buildResult.success())
                return false;

            var playerMeta = getPlayerMeta(parameters.targetPlayer);
            this.postBuildDisguise(buildResult, parameters, playerMeta);

            if (!applyDisguise(parameters, buildResult.state(), meta, playerMeta))
            {
                return false;
            }

            this.afterDisguise(buildResult, parameters, playerMeta);

            return true;
        }
        catch (Throwable t)
        {
            logger.error("Unable to disguise player: " + t.getMessage());
            t.printStackTrace();

            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.errorWhileDisguising()));

            unMorph(parameters.targetPlayer);

            return false;
        }
    }

    /**
     * 准备一些伪装需要的东西，包括权限检查
     * @param parameters MorphParameters
     * @return 一个DisguiseMeta，如果构建失败则返回Null
     */
    @Nullable
    private DisguiseMeta preDisguise(MorphParameters parameters)
    {
        // 确保source不为null
        var source = parameters.commandSource == null ? nilCommandSource : parameters.commandSource;
        var player = parameters.targetPlayer;
        var disguiseIdentifier = parameters.targetDisguiseIdentifier();

        // 检查玩家是否拥有此伪装的权限
        if (!parameters.bypassPermission)
        {
            // 1.玩家是否可以通过指令或客户端伪装
            // 2.玩家是否可以通过此ID伪装；若没有设置，则默认为允许
            var childNode = CommonPermissions.MORPH + ".as." + disguiseIdentifier.replace(":", ".");
            var hasPerm = player.hasPermission(CommonPermissions.MORPH)
                    && PermissionUtils.hasPermission(player, childNode, true);

            if (!hasPerm)
            {
                source.sendMessage(MessageUtils.prefixes(source, CommandStrings.noPermissionMessage()));

                return null;
            }
        }

        // 如果ID不包含命名空间，则为其加上 "minecraft:" 前缀
        if (!disguiseIdentifier.contains(":"))
        {
            disguiseIdentifier = DisguiseTypes.VANILLA.toId(disguiseIdentifier);
            parameters.setDisguiseIdentifier(disguiseIdentifier);
        }

        // 检查是否禁用
        if (disguiseDisabled(disguiseIdentifier))
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.disguiseBannedOrNotSupportedString()));
            return null;
        }

        if (disguiseDisabledInWorld(player.getWorld()))
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.disguiseDisabledInWorldString()));
            return null;
        }

        // 调用早期事件
        var earlyEventPassed = new PlayerMorphEarlyEvent(player, null, disguiseIdentifier, parameters.forceExecute).callEvent();
        if (!parameters.forceExecute && !earlyEventPassed)
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.operationCancelledString()));
            return null;
        }

        // 检查是否拥有此伪装
        DisguiseMeta info = null;

        if (!parameters.bypassAvailableCheck)
        {
            String finalKey = disguiseIdentifier;
            info = getAvaliableDisguisesFor(player).stream()
                    .filter(i -> i.getIdentifier().equals(finalKey)).findFirst().orElse(null);
        }
        else if (!disguiseIdentifier.equals("minecraft:player")) // 禁止不带参数的玩家伪装
        {
            info = new DisguiseMeta(disguiseIdentifier, DisguiseTypes.fromId(disguiseIdentifier));
        }

        if (info == null)
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.morphNotOwnedString()));
            return null;
        }

        return info;
    }

    public static final int VALIDATE_NO_ISSUE = 0;
    public static final int VALIDATE_NO_PROVIDER = 1;
    public static final int VALIDATE_PROVIDER_FAIL = 2;

    public int validateDisguise(DisguiseMeta meta)
    {
        var disguiseIdentifier = meta.getIdentifier();

        // 查找provider
        var strippedKey = disguiseIdentifier.split(":", 2);

        var provider = getProvider(strippedKey[0]);

        if (provider == MorphManager.fallbackProvider) // 如果没找到provider
            return VALIDATE_NO_PROVIDER;
        else if (!provider.isValid(disguiseIdentifier)) // 如果provider不认识这个ID
            return VALIDATE_PROVIDER_FAIL;

        return VALIDATE_NO_ISSUE;
    }

    /**
     * 构建一个最小的
     * @param parameters A {@link MorphParameters}
     * @param disguiseMeta A {@link DisguiseMeta}
     * @return {@link DisguiseBuildResult} ，如果不能进行下一步则返回null
     */
    @NotNull
    private DisguiseBuildResult buildDisguise(MorphParameters parameters, DisguiseMeta disguiseMeta)
    {
        // 确保source不为null
        var source = parameters.commandSource == null ? nilCommandSource : parameters.commandSource;
        var player = parameters.targetPlayer;
        var disguiseIdentifier = parameters.targetDisguiseIdentifier();
        var targetEntity = parameters.targetedEntity;

        DisguiseState outComingState = null;

        // 执行伪装操作
        try
        {
            var provider = getProvider(disguiseIdentifier);

            // 从Provider获取此伪装的Wrapper
            var result = provider.makeWrapper(player, disguiseMeta, targetEntity);

            // 如果provider未能构建DisguiseWrapper
            if (!result.success())
            {
                if (!result.failSilent())
                {
                    source.sendMessage(MessageUtils.prefixes(source, MorphStrings.errorWhileDisguising()));
                    logger.error("Unable to get disguise for player with provider {}", provider);
                }

                return DisguiseBuildResult.FAILED;
            }

            var wrapper = result.wrapperInstance();
            assert wrapper != null;

            // 向Wrapper写入伪装ID
            wrapper.writeProperty(WrapperProperties.DISGUISE_ID, disguiseIdentifier);

            // 获取此伪装将用来显示的目标装备
            EntityEquipment equipment = null;
            var theirState = getDisguiseStateFor(targetEntity);
            if (targetEntity != null && provider.canCloneEquipment(disguiseMeta, targetEntity, theirState))
            {
                if (theirState != null)
                {
                    equipment = theirState.showingDisguisedItems()
                            ? theirState.getDisguisedItems()
                            : ((LivingEntity) targetEntity).getEquipment();

                }
                else
                {
                    equipment = ((LivingEntity) targetEntity).getEquipment();
                }
            }

            // 技能
            var rawIdentifierHasSkill = skillHandler.hasSkill(disguiseIdentifier) || skillHandler.hasSpeficSkill(disguiseIdentifier, SkillType.NONE);
            var targetSkillID = rawIdentifierHasSkill ? disguiseIdentifier : provider.getNameSpace() + ":" + MorphManager.disguiseFallbackName;

            var playerMorphConfig = getPlayerMeta(player);
            outComingState = new DisguiseState(player, disguiseIdentifier, targetSkillID,
                    wrapper, provider, equipment,
                    clientHandler.getPlayerOption(player, true), playerMorphConfig);

            if (result.isCopy())
                outComingState.setSessionData(DATAKEY_SKIP_PROPERTIES, true);

            return DisguiseBuildResult.of(outComingState, provider, disguiseMeta, targetEntity);
        }
        catch (IllegalArgumentException iae)
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.parseErrorString()
                    .resolve("id", disguiseIdentifier)));

            logger.error("Unable to parse key " + disguiseIdentifier + ": " + iae.getMessage());
            iae.printStackTrace();

            unMorph(player);

            return DisguiseBuildResult.FAILED;
        }
        catch (Throwable t)
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.errorWhileDisguising()));

            logger.error("Error while disguising: %s".formatted(t.getMessage()));
            t.printStackTrace();

            unMorph(player);
            return DisguiseBuildResult.FAILED;
        }
    }

    public static final String DATAKEY_SKIP_PROPERTIES = "skip_properties_init";

    private void postBuildDisguise(DisguiseBuildResult result,
                                   MorphParameters parameters,
                                   PlayerMeta playerOptions)
    {
        if (!result.success())
            throw new IllegalArgumentException("Passing a failed result to postDisguise() !");

        // 确保source不为null
        var player = parameters.targetPlayer;
        var targetEntity = parameters.targetedEntity;
        var provider = result.provider();
        var state = result.state();
        var wrapper = state.getDisguiseWrapper();

        if (!state.getSessionDataOr(DATAKEY_SKIP_PROPERTIES, Boolean.class, false))
        {
            // 同步伪装属性
            var propertyHandler = state.disguisePropertyHandler();
            var properties = disguiseProperties.get(state.getEntityType());
            propertyHandler.initProperties(properties);
            propertyHandler.updateFromPropertiesInput(parameters.properties);

            propertyHandler.getAll().forEach((property, value) ->
            {
                wrapper.writeProperty((SingleProperty<Object>) property, value);
            });
        }

        // 初始化nbt
        var initialNbtCompound = provider.getInitialNbtCompound(state, targetEntity, false);

        if (initialNbtCompound != null)
            state.getDisguiseWrapper().mergeCompound(initialNbtCompound);

        // 设定显示名称
        if (targetEntity != null && targetEntity.customName() != null)
        {
            var name = targetEntity.customName();

            state.entityCustomName = name;
            state.setCustomDisplayName(name);
        }
        else
        {
            var disguiseID = parameters.targetDisguiseIdentifier();
            var playerDisplay = provider.getDisplayName(disguiseID, MessageUtils.getLocale(player));
            var serverDisplay = provider.getDisplayName(disguiseID, config.get(String.class, ConfigOption.LANGUAGE_CODE));

            state.setPlayerDisplay(playerDisplay);
            state.setServerDisplay(serverDisplay);
        }

        provider.postConstructDisguise(state, targetEntity);
        wrapper.onPostConstructDisguise(state, targetEntity);

        SkillCooldownInfo cdInfo;

        //获取与技能对应的CDInfo
        cdInfo = skillHandler.getCooldownInfo(player.getUniqueId(), state.skillLookupIdentifier());
        state.setCooldownInfo(cdInfo, false);

        state.setSkillCooldown(Math.max(40, cdInfo.getCooldown()), false);
        cdInfo.setLastInvoke(plugin.getCurrentTick());

        // 切换CD
        skillHandler.switchCooldown(player.getUniqueId(), cdInfo);
    }

    private boolean applyDisguise(MorphParameters parameters,
                                  DisguiseState state,
                                  DisguiseMeta meta,
                                  PlayerMeta playerOptions)
    {
        var player = parameters.targetPlayer;
        var provider = getProvider(parameters.targetDisguiseIdentifier());
        var wrapper = state.getDisguiseWrapper();
        var source = parameters.commandSource == null ? parameters.targetPlayer : parameters.commandSource;

        // 玩家是否已有活跃的DisguiseState?
        var currentState = getDisguiseStateFor(player);

        // 重置上个State的伪装
        if (currentState != null)
        {
            var stateProvider = currentState.getProvider();

            // 检查上个State的后端是否允许我们在不取消伪装的情况下重置State
            if (stateProvider.allowSwitchingWithoutUndisguise(provider, meta))
                currentState.reset(false);
            else
                currentState.reset();

            activeDisguises.remove(currentState);
        }

        // 在初始化服务端伪装状态后，交由后端来为玩家套上伪装
        var backendSuccess = wrapper.getBackend().disguise(player, wrapper);
        if (!backendSuccess)
        {
            logger.warn("Backend '%s' failed to disguise the player...".formatted(wrapper.getBackend().getIdentifier()));
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.errorWhileDisguising()));

            // Reset last anyway
            if (currentState != null)
                currentState.reset();

            return false;
        }

        this.activeDisguises.add(state);

        // 确保玩家可以根据设置看到自己的伪装
        state.setServerSideSelfVisible(playerOptions.showDisguiseToSelf && !this.clientViewAvailable(player));

        // Network below!

        // 向管理员发送map消息
        networkingHelper.sendCommandToRevealablePlayers(genPartialMapCommand(state));

        // 向客户端更新当前伪装ID
        // 因为下面postConstruct有初始化技能的操作，根据协议标准中current会重置客户端伪装状态的规定，因此在这里更新
        clientHandler.updateCurrentIdentifier(player, state.getDisguiseIdentifier());

        // Skill
        state.getSkill().applyToClient(state);

        // Cooldown
        state.applyCooldownToClient();

        // 如果此伪装可以同步给客户端，那么初始化客户端状态
        if (provider.validForClient(state))
        {
            clientHandler.sendCommand(player, new S2CSetSNbtCommand(state.getCulledNbtString()));

            clientHandler.sendCommand(player, new S2CSetSelfViewIdentifierCommand(provider.getSelfViewIdentifier(state)));
            provider.getInitialSyncCommands(state).forEach(s -> clientHandler.sendCommand(player, s));

            // 设置Profile
            if (state.haveProfile())
                clientHandler.sendCommand(player, new S2CSetProfileCommand(state.getProfileNbtString()));
        }

        // 设置可用动作
        var availableAnimations = provider.getAnimationProvider()
                .getAnimationSetFor(state.getDisguiseIdentifier())
                .getAvailableAnimationsForClient();

        clientHandler.sendCommand(player, new S2CSetAvailableAnimationsCommand(availableAnimations));

        // 调用事件
        new PlayerMorphEvent(player, state).callEvent();

        return true;
    }

    /**
     *
     * @param result
     * @param parameters
     * @param playerOptions
     */
    private void afterDisguise(DisguiseBuildResult result,
                               MorphParameters parameters,
                               PlayerMeta playerOptions)
    {
        // 确保source不为null
        var source = parameters.commandSource == null ? nilCommandSource : parameters.commandSource;
        var player = parameters.targetPlayer;
        var disguiseMeta = result.meta();

        // 消息源是否为玩家自己
        var isDirect = source == player;

        // 返回消息
        var playerLocale = MessageUtils.getLocale(source);

        var morphSuccessMessage = (isDirect ? MorphStrings.morphSuccessString() : CommandStrings.morphedSomeoneString())
                .withLocale(playerLocale)
                .resolve("who", player.getName())
                .resolve("what", disguiseMeta.asComponent(playerLocale));

        source.sendMessage(MessageUtils.prefixes(source, morphSuccessMessage));

        // 显示粒子
        double cX, cY, cZ;

        var wrapper = result.state().getDisguiseWrapper();
        var box = wrapper.getDimensions();
        cX = cZ = box.width();
        cY = box.height();

        spawnParticle(player, player.getLocation(), cX, cY, cZ);

        player.getWorld().playSound(
                player.getLocation(),
                Sound.UI_LOOM_TAKE_RESULT,
                SoundCategory.PLAYERS,
                1, 1
        );

        // 发送提示
        var isClientPlayer = clientHandler.clientConnected(player);
        if (isClientPlayer)
        {
            if (!playerOptions.shownClientSkillHint)
            {
                player.sendMessage(MessageUtils.prefixes(player, HintStrings.clientSkillString()));
                playerOptions.shownClientSkillHint = true;
            }
        }
        else
        {
            if (clientHandler.clientInitialized(player) && !playerOptions.shownDisplayToSelfHint)
            {
                player.sendMessage(MessageUtils.prefixes(player, HintStrings.morphVisibleAfterCommandString()));
                playerOptions.shownDisplayToSelfHint = true;
            }
        }

        if (this.hideDisguisedPlayers.get())
            PlayerListHandler.instance().hidePlayer(player);
    }

    //region Command generating

    /**
     * 生成用于橙字显示的map指令
     */
    public S2CMapCommand genMapCommand()
    {
        var map = new HashMap<Integer, String>();
        for (DisguiseState disguiseState : this.activeDisguises)
        {
            var player = disguiseState.getPlayer();
            map.put(player.getEntityId(), player.getName());
        }

        return new S2CMapCommand(map);
    }

    public S2CRenderMapSyncCommand genRenderSyncCommand()
    {
        var map = new HashMap<Integer, String>();
        for (DisguiseState disguiseState : this.activeDisguises)
        {
            var player = disguiseState.getPlayer();
            map.put(player.getEntityId(), disguiseState.getDisguiseIdentifier());
        }

        return S2CRenderMapSyncCommand.of(map);
    }

    /**
     * 生成用于橙字显示的部分map(mapp)指令
     * @param diff 用于生成的伪装状态
     */
    public S2CPartialMapCommand genPartialMapCommand(DisguiseState... diff)
    {
        return networkingHelper.genPartialMapCommand(diff);
    }

    public S2CRenderMapAddCommand genClientRenderAddCommand(DisguiseState diff)
    {
        return networkingHelper.genClientRenderAddCommand(diff);
    }

    //endregion Command generating

    /**
     * 检查某个伪装是否已被禁用
     * @param key 伪装ID
     * @return 此伪装是否已被禁用
     */
    public boolean disguiseDisabled(String key)
    {
        if (bannedDisguises.contains(key)) return true;

        var splitKey = key.split(":", 2);

        if (splitKey.length == 0) return false;

        return bannedDisguises.contains(splitKey[0] + ":" + disguiseFallbackName);
    }

    /**
     * 向客户端发送一组用于同步伪装状态的指令
     * @param state {@link DisguiseState}
     */
    public void refreshClientState(DisguiseState state)
    {
        var player = state.getPlayer();

        clientHandler.updateCurrentIdentifier(player, state.getDisguiseIdentifier());
        clientHandler.sendCommand(player, new S2CSetSNbtCommand(state.getCulledNbtString()));
        clientHandler.sendCommand(player, new S2CSetSelfViewIdentifierCommand(state.getProvider().getSelfViewIdentifier(state)));

        //刷新主动
        state.applyCooldownToClient();
        state.getSkill().applyToClient(state);

        //刷新被动
        state.getAbilityUpdater().getRegisteredAbilities().forEach(a -> a.onClientInit(state));

        var provider = state.getProvider();

        //和客户端同步数据
        provider.getInitialSyncCommands(state).forEach(c -> clientHandler.sendCommand(player, c));

        // 设置可用动作
        var availableAnimations = provider.getAnimationProvider().getAnimationSetFor(state.getDisguiseIdentifier()).getAvailableAnimationsForClient();
        clientHandler.sendCommand(player, new S2CSetAvailableAnimationsCommand(availableAnimations));

        //Profile
        if (state.haveProfile())
            clientHandler.sendCommand(player, new S2CSetProfileCommand(state.getProfileNbtString()));

        clientHandler.sendCommand(player, new S2CSetDisplayingFakeEquipCommand(state.showingDisguisedItems()));
    }

    /**
     * 取消所有玩家的伪装
     */
    public void unMorphAll(boolean ignoreOffline)
    {
        var players = new ObjectArrayList<>(activeDisguises);
        players.forEach(i ->
        {
            if (ignoreOffline && !i.getPlayer().isOnline()) return;

            unMorph(i.getPlayer(), i.getPlayer(), true, true);
        });
    }

    /**
     * 取消某一玩家的伪装
     *
     * @param player 目标玩家
     */
    public void unMorph(Player player)
    {
        this.unMorph(player, player, false, false);
    }

    /**
     * 取消某一玩家的伪装
     *
     * @param player 目标玩家
     * @param bypassPermission 是否绕过权限检查（强制取消伪装）
     */
    public void unMorph(Player player, boolean bypassPermission)
    {
        this.unMorph(player, player, bypassPermission, false);
    }

    public static final NilCommandSource nilCommandSource = new NilCommandSource();

    @Resolved
    private RevealingHandler revealingHandler;

    /**
     * 取消某一玩家的伪装
     *
     * @param player 目标玩家
     * @param bypassPermission 是否绕过权限检查
     * @param source 消息要发送的目标来源
     * @param forceUnmorph 是否强制执行操作
     *
     * @apiNote 如果 forceUnmorph 不为 true，则此操作可以被其他来源取消
     */
    public void unMorph(@Nullable CommandSender source, Player player, boolean bypassPermission, boolean forceUnmorph)
    {
        // 确保source不为null
        source = source == null ? nilCommandSource : source;

        // 检查玩家是否可以通过指令或客户端取消伪装
        if (!bypassPermission && !player.hasPermission(CommonPermissions.UNMORPH))
        {
            source.sendMessage(MessageUtils.prefixes(player, CommandStrings.noPermissionMessage()));

            return;
        }

        // 获取当前伪装状态
        var state = activeDisguises.stream()
                .filter(i -> i.getPlayer().getUniqueId().equals(player.getUniqueId())).findFirst().orElse(null);

        // 如果当前没有状态，则不做任何事
        if (state == null)
            return;

        // 调用早期事件
        var earlyEventPassed = new PlayerUnMorphEarlyEvent(player, state, forceUnmorph).callEvent();
        if (!earlyEventPassed && !forceUnmorph)
        {
            source.sendMessage(MessageUtils.prefixes(source, MorphStrings.operationCancelledString()));

            return;
        }

        // 后端的取消操作在Provider里，因此调用Provider的unMorph()
        // state.getProvider().unMorph(player, state);

        // 重置此State
        state.reset();

        // 如果玩家在线，则生成粒子
        if (player.isConnected())
        {
            spawnParticle(player, player.getLocation(), player.getWidth(), player.getHeight(), player.getWidth());

            player.getWorld().playSound(
                    player.getLocation(),
                    Sound.UI_LOOM_TAKE_RESULT,
                    SoundCategory.PLAYERS,
                    1, 1
            );
        }

        // 从disguiseStates里移除此状态
        activeDisguises.remove(state);

        // 更新最后操作时间
        updateLastPlayerMorphOperationTime(player);

        // 移除CD
        skillHandler.switchCooldown(player.getUniqueId(), null);

        // 移除Bossbar
        state.setBossbar(null);

        // 从材质map中移除此玩家
        uuidPlayerTexturesMap.remove(player.getUniqueId());

        // 向客户端同步伪装属性
        clientHandler.updateCurrentIdentifier(player, null);
        clientHandler.sendCommand(player, new S2CSetSelfViewIdentifierCommand(null));

        var revLevel = revealingHandler.getRevealingState(player).getBaseValue();
        clientHandler.sendCommand(player, new S2CSetRevealingCommand(revLevel));

        //发送消息以及重置actionbar
        source.sendMessage(MessageUtils.prefixes(player, MorphStrings.unMorphSuccessString().withLocale(MessageUtils.getLocale(player))));
        player.sendActionBar(Component.empty());

        // 设置可用动作
        clientHandler.sendCommand(player, new S2CSetAvailableAnimationsCommand(List.of()));

        // 调用事件
        new PlayerUnMorphEvent(player).callEvent();

        // 向管理员发送map移除指令
        networkingHelper.sendCommandToRevealablePlayers(new S2CMapRemoveCommand(player.getEntityId()));

        if (this.hideDisguisedPlayers.get())
            PlayerListHandler.instance().showPlayer(player);

        state.dispose();
    }

    public void spawnParticle(Player player, Location location, double collX, double collY, double collZ)
    {
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        location.setY(location.getY() + (collY / 2));

        //根据碰撞箱计算粒子数量缩放
        //缩放为碰撞箱体积的1/15，最小为1
        var particleScale = Math.max(1, (collX * collY * collZ) / 15);

        //显示粒子
        player.getWorld().spawnParticle(Particle.CLOUD, location, //类型和位置
                (int) (25 * particleScale), //数量
                collX * 0.6, collY / 4, collZ * 0.6, //分布空间
                particleScale >= 10 ? 0.2 : 0.05); //速度
    }

    /**
     * 客户端预览是否可用？
     *
     * @param player 目标玩家
     * @return 正在伪装时返回客户端预览是否可用并已启用，没在伪装时返回玩家的客户端设置
     */
    public boolean clientViewAvailable(Player player)
    {
        var state = this.getDisguiseStateFor(player);
        var playerOption = clientHandler.getPlayerOption(player, true);

        if (state == null)
            return playerOption.isClientSideSelfView();

        //logger.warn(player.getName() + " SV "
        //            + " Option? " + clientHandler.getPlayerOption(player).isClientSideSelfView()
        //            + " StateValid? " + state.getProvider().validForClient(state));

        return playerOption.isClientSideSelfView() && state.getProvider().validForClient(state);
    }

    public void setSelfDisguiseVisible(Player player, boolean val, boolean saveToConfig)
    {
        this.setSelfDisguiseVisible(player, val, saveToConfig, clientHandler.getPlayerOption(player, true).isClientSideSelfView(), false);
    }

    public void setSelfDisguiseVisible(Player player, boolean value, boolean saveToConfig, boolean dontSetServerSide, boolean noClientCommand)
    {
        var state = getDisguiseStateFor(player);
        var config = data.getPlayerMeta(player);

        if (state != null)
        {
            //如果客户端预览启用，则不要调整服务端预览
            if (!dontSetServerSide && !clientViewAvailable(player))
                state.setServerSideSelfVisible(value);
        }

        if (!noClientCommand)
            clientHandler.sendCommand(player, new S2CSetSelfViewingCommand(value));

        if (saveToConfig)
        {
            player.sendMessage(MessageUtils.prefixes(player, value
                    ? MorphStrings.selfVisibleOnString()
                    : MorphStrings.selfVisibleOffString()));

            config.showDisguiseToSelf = value;
        }
    }

    /**
     * 获取某一玩家的伪装状态
     *
     * @param player 目标玩家
     * @return 伪装状态，如果为null则表示玩家没有通过插件伪装
     */
    @Nullable
    public DisguiseState getDisguiseStateFor(@Nullable Player player)
    {
        if (player == null) return null;

        return this.activeDisguises.stream()
                .filter(i -> !i.disposed() && i.getPlayer().getUniqueId().equals(player.getUniqueId()))
                .findFirst().orElse(null);
    }

    @Nullable
    public DisguiseState getDisguiseStateFor(Entity entity)
    {
        if (!(entity instanceof Player player)) return null;

        return getDisguiseStateFor(player);
    }

    public void onPluginDisable()
    {
        getActiveDisguises().forEach(s ->
        {
            var player = s.getPlayer();

            player.sendMessage(MessageUtils.prefixes(player, MorphStrings.resetString()));
            if (!player.isOnline())
                offlineStorage.pushDisguiseState(s);
        });

        unMorphAll(false);
        saveConfiguration();

        offlineStorage.saveConfiguration();

        providers.clear();
    }

    public OfflineDisguiseState getOfflineState(Player player)
    {
        return offlineStorage.popDisguiseState(player.getUniqueId());
    }

    public List<OfflineDisguiseState> getAvaliableOfflineStates()
    {
        return offlineStorage.getAvaliableDisguiseStates();
    }

    public boolean disguiseFromState(DisguiseState state)
    {
        var meta = getDisguiseMeta(state.getDisguiseIdentifier());
        var result = DisguiseBuildResult.of(state, state.getProvider(), meta, null);
        var playerMeta = getPlayerMeta(state.getPlayer());
        var parameters = MorphParameters.create(state.getPlayer(), state.getDisguiseIdentifier());

        if (this.preDisguise(parameters) == null)
            return false;

        this.postBuildDisguise(result, parameters, playerMeta);

        if (!this.applyDisguise(parameters, state, meta, playerMeta))
            return false;

        this.afterDisguise(result, parameters, playerMeta);

        return true;
    }

    /**
     * 尝试从离线存储恢复伪装
     * @param player
     * @param offlineState
     * @return Disguise result
     */
    public OfflineDisguiseResult disguiseFromOfflineState(Player player, OfflineDisguiseState offlineState)
    {
        try
        {
            if (player.getUniqueId() == offlineState.playerUUID)
            {
                logger.error("OfflineState UUID mismatch: %s <-> %s".formatted(player.getUniqueId(), offlineState.playerUUID));
                return OfflineDisguiseResult.FAIL;
            }

            var key = offlineState.disguiseID;

            if (disguiseDisabled(key) || !getPlayerMeta(player).getUnlockedDisguiseIdentifiers().contains(key))
                return OfflineDisguiseResult.FAIL;

            if (DisguiseTypes.fromId(key) == DisguiseTypes.UNKNOWN) return OfflineDisguiseResult.FAIL;

            var provider = getProvider(DisguiseTypes.fromId(key).getNameSpace());

            var state = DisguiseStateGenerator.fromOfflineState(offlineState,
                    clientHandler.getPlayerOption(player, true), getPlayerMeta(player), skillHandler, provider.getPreferredBackend());

            if (state != null)
            {
                this.disguiseFromState(state);

                // 向管理员发送map消息
                networkingHelper.sendCommandToRevealablePlayers(genPartialMapCommand(state));

                new PlayerDisguisedFromOfflineStateEvent(player, state).callEvent();

                return OfflineDisguiseResult.SUCCESS;
            }

            //有限还原
            if (morph(player, player, key, null))
            {
                var newState = getDisguiseStateFor(player);

                if (newState != null)
                    new PlayerDisguisedFromOfflineStateEvent(player, newState).callEvent();

                return OfflineDisguiseResult.LIMITED;
            }
            else
            {
                return OfflineDisguiseResult.FAIL;
            }
        }
        catch (Throwable t)
        {
            logger.error("Unable to recover disguise from OfflineState: " + t.getMessage());
            t.printStackTrace();
        }

        return OfflineDisguiseResult.FAIL;
    }

    //endregion 玩家伪装相关

    //region Implementation of IManagePlayerData

    @Override
    @NotNull
    public DisguiseMeta getDisguiseMeta(String rawString)
    {
        return data.getDisguiseMeta(rawString);
    }

    @Override
    public ObjectArrayList<DisguiseMeta> getAvaliableDisguisesFor(Player player)
    {
        return data.getAvaliableDisguisesFor(player);
    }

    @Override
    public boolean grantMorphToPlayer(Player player, String disguiseIdentifier)
    {
        var success = data.grantMorphToPlayer(player, disguiseIdentifier);

        if (success)
        {
            clientHandler.sendDiff(List.of(disguiseIdentifier), null, player);
            multiInstanceService.notifyDisguiseMetaChange(player.getUniqueId(), Operation.ADD_IF_ABSENT, disguiseIdentifier);

            var config = data.getPlayerMeta(player);
            var locale = MessageUtils.getLocale(player);

            var meta = data.getDisguiseMeta(disguiseIdentifier);
            assert meta != null; // 这里不会出现meta是null的情况，除非抽了

            var message = MessageUtils.prefixes(player, MorphStrings.morphUnlockedString()
                    .withLocale(locale)
                    .resolve("what", meta.asComponent(locale)));
            player.sendMessage(message);

            if (clientHandler.clientConnected(player))
            {
                if (!config.shownMorphClientHint)
                {
                    player.sendMessage(MessageUtils.prefixes(player, HintStrings.firstGrantClientHintString()));
                    config.shownMorphClientHint = true;
                }
            }
            else if (!config.shownMorphHint)
            {
                player.sendMessage(MessageUtils.prefixes(player, HintStrings.firstGrantHintString()));
                config.shownMorphHint = true;
            }
        }

        return success;
    }

    @Override
    public boolean revokeMorphFromPlayer(Player player, String disguiseIdentifier)
    {
        var success = data.revokeMorphFromPlayer(player, disguiseIdentifier);

        if (success)
        {
            clientHandler.sendDiff(null, List.of(disguiseIdentifier), player);
            multiInstanceService.notifyDisguiseMetaChange(player.getUniqueId(), Operation.REMOVE, disguiseIdentifier);

            var locale = MessageUtils.getLocale(player);
            var meta = data.getDisguiseMeta(disguiseIdentifier);
            assert meta != null; // 和上面一样

            var message = MorphStrings.morphLockedString()
                    .resolve("what", meta.asComponent(locale))
                    .toComponent(locale);
            player.sendMessage(message);

            var disguiseState = this.getDisguiseStateFor(player);
            if (disguiseState != null && disguiseState.getDisguiseIdentifier().equalsIgnoreCase(disguiseIdentifier))
                this.unMorph(player, true);
        }

        return success;
    }

    @Override
    public PlayerMeta getPlayerMeta(OfflinePlayer player)
    {
        return data.getPlayerMeta(player);
    }

    @Override
    public boolean reloadConfiguration()
    {
        //重载完数据后要发到离线存储的人
        var stateToOfflineStore = new ObjectArrayList<DisguiseState>();

        activeDisguises.forEach(s ->
        {
            if (!s.getPlayer().isOnline())
                stateToOfflineStore.add(s);
        });

        activeDisguises.removeAll(stateToOfflineStore);
        var stateToRecover = getActiveDisguises();
        stateToRecover = stateToRecover.stream()
                .map(oldState -> oldState.createCopy(oldState.getPlayer()))
                .toList();

        unMorphAll(false);

        var success = data.reloadConfiguration() && offlineStorage.reloadConfiguration();

        stateToOfflineStore.forEach(offlineStorage::pushDisguiseState);

        //重载完成后恢复玩家伪装
        stateToRecover.forEach(s ->
        {
            var player = s.getPlayer();

            this.scheduleOn(player, () ->
            {
                var parameter = MorphParameters.create(player, s.getDisguiseIdentifier());
                if (this.preDisguise(parameter) == null)
                    return;

                if (disguiseFromState(s))
                {
                    refreshClientState(s);
                    player.sendMessage(MessageUtils.prefixes(player, MorphStrings.recoverString()));
                }
                else
                {
                    unMorph(nilCommandSource, player, true, true);
                }
            });
        });

        Bukkit.getOnlinePlayers().forEach(p -> clientHandler.refreshPlayerClientMorphs(this.getPlayerMeta(p).getUnlockedDisguiseIdentifiers(), p));

        return success;
    }

    @Override
    public boolean saveConfiguration()
    {
        return data.saveConfiguration() && offlineStorage.saveConfiguration();
    }
    //endregion Implementation of IManagePlayerData

    @ApiStatus.Internal
    public List<PlayerMeta> listAllPlayerMeta()
    {
        data.shouldLoadAllData(true);
        return data.getAll();
    }
}