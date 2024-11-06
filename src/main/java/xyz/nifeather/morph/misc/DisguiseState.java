package xyz.nifeather.morph.misc;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.morph.network.PlayerOptions;
import xiamomc.morph.network.commands.S2C.S2CAnimationCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetAnimationDisplayNameCommand;
import xiamomc.morph.network.commands.S2C.set.S2CSetSkillCooldownCommand;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.abilities.AbilityUpdater;
import xyz.nifeather.morph.api.v0.disguise.IDisguiseState;
import xyz.nifeather.morph.backends.DisguiseWrapper;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.EmoteStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.disguiseProperty.PropertyHandler;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.network.server.MorphClientHandler;
import xyz.nifeather.morph.providers.animation.SingleAnimation;
import xyz.nifeather.morph.providers.disguise.DisguiseProvider;
import xyz.nifeather.morph.skills.*;
import xyz.nifeather.morph.skills.impl.NoneMorphSkill;
import xyz.nifeather.morph.storage.playerdata.PlayerMeta;
import xyz.nifeather.morph.storage.skill.ISkillOption;
import xyz.nifeather.morph.storage.skill.SkillAbilityConfiguration;
import xyz.nifeather.morph.utilities.ItemUtils;
import xyz.nifeather.morph.utilities.NbtUtils;
import xyz.nifeather.morph.utilities.PermissionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static xyz.nifeather.morph.utilities.DisguiseUtils.itemOrAir;

public class DisguiseState extends MorphPluginObject implements IDisguiseState
{
    public DisguiseState(Player player, @NotNull String identifier, @NotNull String skillIdentifier,
                         @NotNull DisguiseWrapper<?> wrapper, @NotNull DisguiseProvider provider,
                         @Nullable EntityEquipment targetEquipment, @NotNull PlayerOptions<Player> playerOptions,
                         @NotNull PlayerMeta playerMeta)
    {
        Objects.requireNonNull(wrapper, "Wrapper cannot be null.");
        Objects.requireNonNull(identifier, "Disguise identifier cannot be null.");
        Objects.requireNonNull(skillIdentifier, "Skill identifier cannot be null.");
        Objects.requireNonNull(provider, "Disguise provider cannot be null");
        Objects.requireNonNull(playerOptions, "Player options cannot be null");
        Objects.requireNonNull(playerMeta, "Player metadata cannot be null");

        this.playerUUID = player.getUniqueId();
        this.provider = provider;
        this.playerOptions = playerOptions;
        this.morphConfiguration = playerMeta;

        this.soundHandler = new SoundHandler(player);
        this.abilityUpdater = new AbilityUpdater(this);

        this.disguiseWrapper = wrapper;
        this.disguiseIdentifier = identifier;
        skillLookupIdentifier(skillIdentifier);

        disguiseType = DisguiseTypes.fromId(identifier);
        this.provider = MorphManager.getProvider(identifier);

        //设置声音
        this.soundHandler.refreshSounds(wrapper.getEntityType(), wrapper.isBaby());

        //伪装类型是否支持设置伪装物品
        supportsDisguisedItems = skillHandler.hasSpeficSkill(skillIdentifier, SkillType.INVENTORY);

        //更新伪装物品
        if (supportsDisguisedItems)
            refreshDisguiseItems(targetEquipment, wrapper);

        this.cachedPlayer = player;

        animationSequence.setCooldown(10);
        animationSequence.onNewAnimation(anim ->
        {
            var animSubId = anim.subId();

            if (anim.availableForClient())
                clientHandler.sendCommand(getPlayer(), new S2CAnimationCommand(animSubId));

            this.getDisguiseWrapper().playAnimation(animSubId);

            if (animSubId.startsWith("exec_"))
                handleInternalExec(animSubId);
        });
        animationSequence.onNewAnimationSequence(newAnimSeqId ->
        {
            clientHandler.sendCommand(getPlayer(), new S2CSetAnimationDisplayNameCommand(newAnimSeqId));

            // Not done yet...
            /*
            if (newAnimSeqId.equals(AnimationNames.NONE))
            {
                var isPersistent = this.sequencePersistent.get();
                this.sequencePersistent.set(false);

                if (!isPersistent)
                    clientHandler.sendCommand(getPlayer(), new S2CSetAnimationDisplayNameCommand(newAnimSeqId));
            }
            else
            {
                clientHandler.sendCommand(getPlayer(), new S2CSetAnimationDisplayNameCommand(newAnimSeqId));
            }
            */
        });
    }

    private final AtomicBoolean sequencePersistent = new AtomicBoolean(false);

    @Resolved(shouldSolveImmediately = true)
    private MorphSkillHandler skillHandler;

    @Resolved(shouldSolveImmediately = true)
    private MorphClientHandler clientHandler;

    private void handleInternalExec(String animationSubId)
    {
        switch (animationSubId)
        {
            case AnimationNames.INTERNAL_DISABLE_AMBIENT -> this.requestAmbientState(this, true);
            case AnimationNames.INTERNAL_ENABLE_AMBIENT -> this.requestAmbientState(this, false);
            case AnimationNames.INTERNAL_DISABLE_SKILL -> this.requestSkillState(this, true);
            case AnimationNames.INTERNAL_ENABLE_SKILL -> this.requestSkillState(this, false);
            case AnimationNames.INTERNAL_DISABLE_BOSSBAR -> this.requestBossbarState(this, true);
            case AnimationNames.INTERNAL_ENABLE_BOSSBAR -> this.requestBossbarState(this, false);
        }
    }

    private final List<Object> disableSkillRequests = Collections.synchronizedList(new ObjectArrayList<>());
    private final List<Object> disableAmbientRequests = Collections.synchronizedList(new ObjectArrayList<>());
    private final List<Object> disableBossbarRequests = Collections.synchronizedList(new ObjectArrayList<>());

    public void requestSkillState(Object source, boolean shouldDisable)
    {
        if (shouldDisable)
        {
            if (!disableSkillRequests.contains(source))
                disableSkillRequests.add(source);
        }
        else
        {
            disableSkillRequests.remove(source);
        }
    }

    public boolean canActivateSkill()
    {
        return disableSkillRequests.isEmpty();
    }

    public void requestAmbientState(Object source, boolean shouldDisable)
    {
        if (shouldDisable)
        {
            if (!disableAmbientRequests.contains(source))
                disableAmbientRequests.add(source);
        }
        else
        {
            disableAmbientRequests.remove(source);
        }
    }

    public boolean canPlayAmbient()
    {
        return disableSkillRequests.isEmpty();
    }

    public void requestBossbarState(Object source, boolean shouldDisable)
    {
        if (shouldDisable)
        {
            if (!disableBossbarRequests.contains(source))
                disableBossbarRequests.add(source);
        }
        else
        {
            disableBossbarRequests.remove(source);
        }
    }

    public boolean canDisplayBossbar()
    {
        return disableBossbarRequests.isEmpty();
    }

    private final PlayerOptions<Player> playerOptions;

    private final PlayerMeta morphConfiguration;

    private final AnimationSequence animationSequence = new AnimationSequence();

    public void stopAnimations()
    {
        animationSequence.reset();
    }

    public void onOffline()
    {
        this.stopAnimations();
        this.getDisguiseWrapper().onPlayerOffline();
    }

    public boolean canScheduleSequence()
    {
        var cooldown = skillHandler.getCooldownInfo(playerUUID, disguiseIdentifier);
        return cooldown.getCooldown() <= 0;
    }

    /**
     * @return Whether success.
     */
    public boolean tryScheduleSequence(@NotNull String sequenceIdentifier,
                                       List<SingleAnimation> sequence,
                                       boolean persistent)
    {
        if (!canScheduleSequence()) return false;
        this.scheduleSequence(sequenceIdentifier, sequence, persistent);

        return true;
    }

    public void scheduleSequence(String sequenceIdentifier,
                                 List<SingleAnimation> sequence,
                                 boolean persistent)
    {
        this.scheduleSequence(sequenceIdentifier, sequence, true, persistent);
    }

    private void scheduleSequence(String sequenceIdentifier,
                                  List<SingleAnimation> sequence,
                                  boolean checkPermission,
                                  boolean persistent)
    {
        if (checkPermission && sequenceIdentifier.equals(AnimationNames.RESET)
                || !PermissionUtils.hasPermission(
                        getPlayer(),
                        CommonPermissions.animationPermissionOf(sequenceIdentifier, this.getDisguiseIdentifier()),
                        true))
        {
            var player = getPlayer();
            player.sendMessage(MessageUtils.prefixes(player, CommandStrings.noPermissionMessage()));
            return;
        }

        this.animationSequence.scheduleNext(sequenceIdentifier, sequence);
        this.sequencePersistent.set(persistent);

        var player = getPlayer();
        var animationString = CommandStrings.goingToPlayAnimation().resolve("what", EmoteStrings.get(sequenceIdentifier).withLocale(MessageUtils.getLocale(player)));
        player.sendMessage(MessageUtils.prefixes(player, animationString));
    }

    public AnimationSequence getAnimationSequence()
    {
        return animationSequence;
    }

    /**
     * 谁在伪装
     */
    private final UUID playerUUID;

    @Nullable
    private Player cachedPlayer;

    @Nullable
    @Override
    public Player tryGetPlayer()
    {
        // 如果缓存的玩家实例在线，那么返回缓存
        if (cachedPlayer != null && cachedPlayer.isConnected())
            return cachedPlayer;

        // 否则，从BukkitAPI获取玩家
        var player = Bukkit.getPlayer(playerUUID);

        // 如果玩家不为null，则返回获取到的玩家实例
        if (player != null)
        {
            cachedPlayer = Bukkit.getPlayer(playerUUID);
            return player;
        }

        // 如果BukkitAPI没找到玩家，但是缓存有实例，那么返回缓存
        if (cachedPlayer != null)
            return cachedPlayer;

        // 啥都没有！返回null
        // 不过这真的会发生吗？我们都在一开始就设置缓存字段了。
        return null;
    }

    /**
     *
     * @return The player that matches the UUID stored in this DisguiseState
     * @throws NullDependencyException If the player was not found. For nullable method, check {@link DisguiseState#tryGetPlayer()}
     */
    @NotNull
    @Override
    public Player getPlayer() throws NullDependencyException
    {
        var player = tryGetPlayer();

        if (player != null) return player;

        throw new NullDependencyException("Can't find player with UUID " + playerUUID);
    }

    /**
     * 自身可见
     */
    private boolean serverSideSelfVisible;

    public boolean getServerSideSelfVisible()
    {
        return serverSideSelfVisible;
    }

    public boolean isClientSideSelfViewing()
    {
        return playerOptions.isClientSideSelfView();
    }

    @Override
    public boolean isSelfViewing()
    {
        return playerOptions.isClientSideSelfView() ? morphConfiguration.showDisguiseToSelf : serverSideSelfVisible;
    }

    public void setServerSideSelfVisible(boolean val)
    {
        disguiseWrapper.setServerSelfView(val);
        serverSideSelfVisible = val;
    }

    private static final Component fallbackDisplay = Component.text("~UNDEFINED~");

    /**
     * 此伪装面向玩家自己的显示名称。
     */
    @Nullable
    private Component playerDisplay;

    /**
     * 获取此伪装面向玩家自己的显示名称
     * @return {@link DisguiseState#playerDisplay}
     * @apiNote 对于要显示到服务器公屏上的内容，请使用 {@link DisguiseState#getServerDisplay()}
     */
    @NotNull
    @Override
    public Component getPlayerDisplay()
    {
        return playerDisplay == null ? fallbackDisplay : playerDisplay;
    }

    public void setPlayerDisplay(@NotNull Component newName)
    {
        playerDisplay = newName;
    }

    /**
     * 此伪装面向服务器其他人的显示名称
     */
    @Nullable
    private Component serverDisplay;

    /**
     * 获取此伪装面向服务器其他人的显示名称
     * @return {@link DisguiseState#serverDisplay}
     * @apiNote 对于要显示给玩家自己的内容，请使用 {@link DisguiseState#getPlayerDisplay()}
     */
    @NotNull
    @Override
    public Component getServerDisplay()
    {
        return serverDisplay == null ? fallbackDisplay : serverDisplay;
    }

    @Override
    public void setServerDisplay(@NotNull Component newName)
    {
        serverDisplay = newName;
    }

    @Override
    public void setCustomDisplayName(Component newName)
    {
        setPlayerDisplay(newName);
        setServerDisplay(newName);
    }

    public Component entityCustomName;

    /**
     * 伪装的{@link DisguiseWrapper}实例
     */
    @NotNull
    private final DisguiseWrapper<?> disguiseWrapper;

    /**
     * 获取此State的伪装Wrapper
     */
    @NotNull
    public DisguiseWrapper<?> getDisguiseWrapper()
    {
        return disguiseWrapper;
    }

    // 伪装ID
    private String disguiseIdentifier = SkillType.UNKNOWN.asString();

    /**
     * 获取此伪装的ID
     */
    public String getDisguiseIdentifier()
    {
        return disguiseIdentifier;
    }

    public EntityType getEntityType()
    {
        return disguiseWrapper.getEntityType();
    }

    private DisguiseTypes disguiseType;

    /**
     * 获取此伪装的{@link DisguiseTypes}
     */
    public DisguiseTypes getDisguiseType()
    {
        return disguiseType;
    }

    /**
     * 伪装的构建器（提供器）
     */
    private DisguiseProvider provider;

    @NotNull
    public DisguiseProvider getProvider()
    {
        return provider;
    }

    /**
     * 伪装的Bossbar
     */
    @Nullable
    private BossBar bossbar;

    @Nullable
    public BossBar getBossbar()
    {
        return bossbar;
    }

    public void setBossbar(@Nullable BossBar bossbar)
    {
        if (this.bossbar != null)
            Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(this.bossbar));

        this.bossbar = bossbar;
    }

    //region Disguise Property

    private final PropertyHandler propertyHandler = new PropertyHandler();

    public PropertyHandler disguisePropertyHandler()
    {
        return propertyHandler;
    }

    //endregion Disguise Property

    //region CustomProperty

    private final Map<String, Object> propertiesMap = new Object2ObjectArrayMap<>();

    /**
     * @apiNote 这不是伪装的属性，仅仅针对此DisguiseState会话！
     */
    public void setSessionData(String name, Object value)
    {
        if (value == null)
        {
            this.removeSessionData(name);
            return;
        }

        propertiesMap.put(name, value);
    }

    /**
     * @apiNote 这不是伪装的属性，仅仅针对此DisguiseState会话！
     */
    @Nullable
    public <T> T getSessionData(String name, Class<T> type)
    {
        var value = propertiesMap.getOrDefault(name, null);
        if (value == null) return null;

        if (type.isInstance(value)) return (T) value;

        return null;
    }

    @NotNull
    public <T> T getSessionDataOr(String name, Class<T> type, @NotNull T defaultVal)
    {
        var data = this.getSessionData(name, type);
        return data == null ? defaultVal : data;
    }

    /**
     * @apiNote 这不是伪装的属性，仅仅针对此DisguiseState会话！
     */
    public void removeSessionData(String name)
    {
        propertiesMap.remove(name);
    }

    //endregion CustomProperty

    /**
     * 技能查询ID
     */
    @Nullable
    private String skillLookupIdentifier = null;

    private static final String DEFAULT_SKILL_LOOKUP = NamespacedKey.MINECRAFT + ":" + MorphManager.disguiseFallbackName;

    /**
     * 获取用于查询技能的ID
     *
     * @return 技能ID
     */
    @NotNull
    public String skillLookupIdentifier()
    {
        return skillLookupIdentifier == null ? DEFAULT_SKILL_LOOKUP : skillLookupIdentifier;
    }

    /**
     * 设置技能查询ID
     *
     * @param newSkillID 技能ID
     */
    public void skillLookupIdentifier(@NotNull String newSkillID)
    {
        this.skillLookupIdentifier = newSkillID;
    }

    @Nullable
    private SkillAbilityConfiguration skillAbilityConfiguration;

    @Nullable
    public SkillAbilityConfiguration getSkillAbilityConfiguration()
    {
        return skillAbilityConfiguration;
    }

    @NotNull
    private IMorphSkill<?> skill = NoneMorphSkill.instance;

    /**
     * 设置此伪装的技能
     * @param newSkill 目标技能
     * @param config 与技能对应的配置
     * @apiNote 如果目标技能是null，则会fallback到 {@link NoneMorphSkill#instance}，并一并清除技能配置
     */
    public <X extends ISkillOption> void setSkill(@Nullable IMorphSkill<X> newSkill,
                                                  SkillAbilityConfiguration config)
    {
        if (newSkill == null)
        {
            this.skill = NoneMorphSkill.instance;
            this.skillAbilityConfiguration = null;
            return;
        }

        this.skill.onDeEquip(this);

        this.skillAbilityConfiguration = config;
        newSkill.onInitialEquip(this);
        this.skill = newSkill;
    }

    /**
     * 获取此伪装的技能
     * @return {@link IMorphSkill}
     */
    @NotNull
    public IMorphSkill<?> getSkill()
    {
        return skill;
    }

    /**
     * 此伪装是否拥有技能
     * @return 伪装技能是否为 {@link NoneMorphSkill} 的实例
     */
    public boolean haveSkill()
    {
        return skill != NoneMorphSkill.instance;
    }

    /**
     * 伪装技能冷却
     */
    @Nullable
    private SkillCooldownInfo cooldownInfo;

    public long getSkillCooldown()
    {
        return cooldownInfo == null ? -1 : cooldownInfo.getCooldown();
    }

    public long getSkillLastInvoke()
    {
        return cooldownInfo == null ? Long.MIN_VALUE : cooldownInfo.getLastInvoke();
    }

    public boolean haveCooldown()
    {
        return cooldownInfo != null;
    }

    public void setSkillCooldown(long val, boolean notifyClient)
    {
        if (haveCooldown())
            cooldownInfo.setCooldown(val);

        if (notifyClient)
            this.applyCooldownToClient();
    }

    public void setCooldownInfo(@Nullable SkillCooldownInfo info, boolean notifyClient)
    {
        this.cooldownInfo = info;

        if (notifyClient)
            this.applyCooldownToClient();
    }

    public void applyCooldownToClient()
    {
        long cd = -1;

        if (haveCooldown())
            cd = this.getSkillCooldown();

        clientHandler.sendCommand(getPlayer(), new S2CSetSkillCooldownCommand(cd));
    }

    //region 被动技能

    @NotNull
    private final AbilityUpdater abilityUpdater;

    @NotNull
    public AbilityUpdater getAbilityUpdater()
    {
        return abilityUpdater;
    }

    @ApiStatus.Internal
    public void onPlayerJoin()
    {
        this.abilityUpdater.reApplyAbility();
        this.skill.onInitialEquip(this);

        this.getDisguiseWrapper().onPlayerJoin(this.getPlayer());
    }

    /**
     * 检查某个被动能力是否设置
     * @param key {@link NamespacedKey}
     * @return 是否设置
     */
    public boolean containsAbility(NamespacedKey key)
    {
        return abilityUpdater.containsAbility(key);
    }

    //endregion abilityFlag

    //region NBT

    public String getFullNbtString()
    {
        return NbtUtils.getCompoundString(disguiseWrapper.getCompound());
    }

    public String getCulledNbtString()
    {
        return NbtUtils.getCompoundString(DisguiseProvider.cullNBT(disguiseWrapper.getCompound()));
    }

    //endregion

    //region ProfileNBT

    public String getProfileNbtString()
    {
        if (!haveProfile())
             return "{}";

        var s = NbtUtils.getCompoundString(NbtUtils.toCompoundTag(disguiseWrapper.getSkin()));
        return s;
    }

    public boolean haveProfile()
    {
        return disguiseWrapper.getSkin() != null;
    }

    //endregion ProfileNBT

    public boolean selfUpdate()
    {
        if (this.canPlayAmbient())
            this.getSoundHandler().update();

        this.animationSequence.update();

        return this.abilityUpdater.update();
    }

    private void refreshDisguiseItems(EntityEquipment targetEquipment, DisguiseWrapper<?> disguiseWrapper)
    {
        EntityEquipment equipment = targetEquipment != null ? targetEquipment : disguiseWrapper.getFakeEquipments();

        //设置默认盔甲
        var armors = new ItemStack[]
                {
                        itemOrAir(equipment.getBoots()),
                        itemOrAir(equipment.getLeggings()),
                        itemOrAir(equipment.getChestplate()),
                        itemOrAir(equipment.getHelmet())
                };

        //设置默认手持物
        var handItems = new ItemStack[]
                {
                        itemOrAir(equipment.getItemInMainHand()),
                        itemOrAir(equipment.getItemInOffHand())
                };

        armors = ItemUtils.asCopy(armors);
        handItems = ItemUtils.asCopy(handItems);

        //全是空的，则默认显示自身装备
        var emptyEquipment = Arrays.stream(armors).allMatch(i -> i != null && i.getType().isAir())
                && Arrays.stream(handItems).allMatch(i -> i != null && i.getType().isAir());

        disguiseEquipments.allowNull = true;
        disguiseEquipments.setArmorContents(armors);
        disguiseEquipments.setHandItems(handItems);

        //开启默认装备显示或者更新显示
        setShowingDisguisedItems(showDisguisedItems || !emptyEquipment);
    }

    private final DisguiseEquipment disguiseEquipments = new DisguiseEquipment();

    private boolean showDisguisedItems = false;

    private boolean supportsDisguisedItems = false;

    /**
     * 此阶段是否支持显示伪装物品
     * @return 是否支持
     */
    public boolean supportsShowingDefaultItems()
    {
        return supportsDisguisedItems;
    }

    /**
     * 此阶段是否正在显示伪装物品
     * @return 是否正在显示
     */
    public boolean showingDisguisedItems()
    {
        return showDisguisedItems;
    }

    /**
     * 设置是否要显示伪装物品
     * @param value 值
     */
    public void setShowingDisguisedItems(boolean value)
    {
        updateEquipment(value);
        showDisguisedItems = value;

        this.disguiseWrapper.setDisplayingFakeEquipments(value);
    }

    /**
     * 获取此State的伪装物品
     * @return 此State的伪装物品
     */
    public EntityEquipment getDisguisedItems()
    {
        var eq = new DisguiseEquipment();

        eq.setArmorContents(ItemUtils.asCopy(disguiseEquipments.getArmorContents()));
        eq.setHandItems(ItemUtils.asCopy(disguiseEquipments.getHandItems()));

        return eq;
    }

    @ApiStatus.Internal
    public void swapHands()
    {
        var handItems = disguiseEquipments.getHandItems();

        if (handItems.length == 2)
        {
            var mainHand = handItems[0];
            var offHand = handItems[1];

            disguiseEquipments.setHandItems(offHand, mainHand);
        }
    }

    /**
     * 切换伪装物品是否可见
     * @return 切换后的值
     */
    public boolean toggleDisguisedItems()
    {
        setShowingDisguisedItems(!showDisguisedItems);

        return showDisguisedItems;
    }

    private static final ItemStack[] emptyArmorStack = new ItemStack[]{ null, null, null, null };

    //region Sound Handling

    private final SoundHandler soundHandler;

    public SoundHandler getSoundHandler()
    {
        return soundHandler;
    }

    //endregion Sound Handling

    /**
     * 更新伪装物品显示
     * @param showDisguised 是否显示默认盔甲
     * @apiNote 此方法在将状态转换为离线存储的过程中才会直接调用，其他情况下请用不带参数的方法
     */
    private void updateEquipment(boolean showDisguised)
    {
        var handItems = disguiseEquipments.getHandItems();

        var eq = new DisguiseEquipment();
        eq.setArmorContents(showDisguised ? disguiseEquipments.getArmorContents() : emptyArmorStack);
        eq.setItemInMainHand(showDisguised ? handItems[0] : null);
        eq.setItemInOffHand(showDisguised ? handItems[1] : null);
        eq.allowNull = true;

        disguiseWrapper.setFakeEquipments(eq);
    }

    public DisguiseState createCopy(Player player)
    {
        if (disposed())
            throw new RuntimeException("Can't create a copy of a disposed DisguiseState");

        var wrapper = this.disguiseWrapper.clone();

        var newInstance = new DisguiseState(player, this.disguiseIdentifier, this.skillLookupIdentifier(),
                wrapper, provider, getDisguisedItems(), this.playerOptions, morphConfiguration);

        newInstance.playerDisplay = this.playerDisplay;
        newInstance.serverDisplay = this.serverDisplay;

        return newInstance;
    }

    private final AtomicBoolean disposed = new AtomicBoolean(false);

    public boolean disposed()
    {
        return disposed.get();
    }

    @Override
    public void dispose()
    {
        disposed.set(true);
        this.disguiseWrapper.dispose();
        this.abilityUpdater.dispose();
    }

    public void reset(boolean unDisguise)
    {
        this.provider.resetDisguise(this);

        if (unDisguise)
            this.provider.unMorph(getPlayer(), this);

        this.abilityUpdater.setAbilities(List.of());
        this.setSkill(null, null);
    }

    public void reset()
    {
        this.reset(true);
    }
}
