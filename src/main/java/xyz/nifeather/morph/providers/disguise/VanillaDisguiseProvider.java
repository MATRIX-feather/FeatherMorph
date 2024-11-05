package xyz.nifeather.morph.providers.disguise;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.backends.DisguiseWrapper;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.messages.vanilla.VanillaMessageStore;
import xyz.nifeather.morph.misc.DisguiseMeta;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.DisguiseTypes;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.values.ArmorStandProperties;
import xyz.nifeather.morph.providers.animation.AnimationProvider;
import xyz.nifeather.morph.providers.animation.provider.VanillaAnimationProvider;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.utilities.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class VanillaDisguiseProvider extends DefaultDisguiseProvider
{
    @Override
    public @NotNull String getNameSpace()
    {
        return DisguiseTypes.VANILLA.getNameSpace();
    }

    private final AnimationProvider animationProvider = new VanillaAnimationProvider();

    /**
     * 获取此DisguiseProvider的动画提供器
     */
    @Override
    public AnimationProvider getAnimationProvider()
    {
        return animationProvider;
    }

    @Override
    public boolean allowSwitchingWithoutUndisguise(DisguiseProvider other, DisguiseMeta meta)
    {
        return other.getPreferredBackend() == this.getPreferredBackend()
                && (meta.getDisguiseType() == DisguiseTypes.VANILLA || meta.getDisguiseType() == DisguiseTypes.PLAYER);
    }

    @Override
    public boolean isValid(String rawIdentifier)
    {
        var idStripped = DisguiseTypes.VANILLA.toStrippedId(rawIdentifier);
        return getAllAvailableDisguises().contains(idStripped);
    }

    public VanillaDisguiseProvider()
    {
        var list = new ObjectArrayList<String>();

        for (var eT : EntityType.values())
        {
            if (eT == EntityType.UNKNOWN || !eT.isAlive()) continue;

            list.add(eT.getKey().getKey());
        }

        list.removeIf(s -> s.equals("player"));

        vanillaIdentifiers = list;
    }

    private final Bindable<Boolean> armorStandShowArms = new Bindable<>(false);
    private final Bindable<Boolean> doHealthScale = new Bindable<>(false);
    private final Bindable<Integer> healthCap = new Bindable<>(60);
    private final Bindable<Boolean> modifyBoundingBoxes = new Bindable<>(false);
    private final Bindable<Boolean> checkSpaceBoundingBox = new Bindable<>(true);

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        configManager.bind(armorStandShowArms, ConfigOption.ARMORSTAND_SHOW_ARMS);
        configManager.bind(doHealthScale, ConfigOption.HEALTH_SCALE);
        configManager.bind(healthCap, ConfigOption.HEALTH_SCALE_MAX_HEALTH);
        configManager.bind(modifyBoundingBoxes, ConfigOption.MODIFY_BOUNDING_BOX);
        configManager.bind(checkSpaceBoundingBox, ConfigOption.CHECK_AVAILABLE_SPACE);

        modifyBoundingBoxes.onValueChanged((o, n) ->
        {
            if (o && !n)
                Bukkit.getOnlinePlayers().forEach(p -> NmsRecord.ofPlayer(p).refreshDimensions());
        });
    }

    private final List<String> vanillaIdentifiers;

    @Override
    public List<String> getAllAvailableDisguises()
    {
        return vanillaIdentifiers;
    }

    @Override
    @NotNull
    public DisguiseResult makeWrapper(Player player, DisguiseMeta disguiseMeta, @Nullable Entity targetEntity)
    {
        var identifier = disguiseMeta.getIdentifier();

        DisguiseWrapper<?> constructedDisguise;
        var backend = getPreferredBackend();

        var entityType = EntityTypeUtils.fromString(identifier, true);

        if (entityType == null || entityType == EntityType.PLAYER || !entityType.isAlive())
        {
            logger.error("Illegal mob type: " + identifier + "(" + entityType + ")");
            return DisguiseResult.fail();
        }

        var copyResult = constructFromEntity(disguiseMeta, targetEntity);

        constructedDisguise = copyResult.success()
                ? copyResult.wrapperInstance() //copyResult.success() -> wrapperInstance() != null
                : backend.createInstance(entityType);

        // Make IDE happy
        Objects.requireNonNull(constructedDisguise);

        var canConstructFromEntity = canConstruct(disguiseMeta, targetEntity, null);

        //手动指定史莱姆和岩浆怪的大小
        if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE)
        {
            if (canConstructFromEntity)
            {
                var size = (targetEntity != null && targetEntity.getType() == entityType)
                        ? NbtUtils.getRawTagCompound(targetEntity).getInt("Size")
                        : new Random().nextInt(0, 4); //史莱姆的大小其实是0~3

                var initialTag = new CompoundTag();

                initialTag.putInt("Size", size);
                constructedDisguise.mergeCompound(initialTag);
            }
        }

        // 检查是否有足够的空间
        if (modifyBoundingBoxes.get() && checkSpaceBoundingBox.get())
        {
            var loc = player.getLocation();
            var box = constructedDisguise.getBoundingBoxAt(loc.x(), loc.y(), loc.z());

            var hasCollision = CollisionUtils.hasCollisionWithBlockOrBorder(player, box);
            if (hasCollision)
            {
                player.sendMessage(MessageUtils.prefixes(player, MorphStrings.noEnoughSpaceString()));
                return DisguiseResult.FAIL_SILENT;
            }
        }

        return DisguiseResult.success(constructedDisguise, copyResult.isCopy());
    }

    @Override
    public boolean updateDisguise(Player player, DisguiseState state)
    {
        if (super.updateDisguise(player, state))
        {
            if (modifyBoundingBoxes.get())
                tryModifyPlayerDimensions(player, state.getDisguiseWrapper());

            if (plugin.getCurrentTick() % 20 == 0)
                ReflectionUtils.cleanCaches();

            return true;
        }
        else
            return false;
    }

    @Override
    public void postConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
        super.postConstructDisguise(state, targetEntity);

        var wrapper = state.getDisguiseWrapper();
        var theirDisguise = getMorphManager().getDisguiseStateFor(targetEntity);

        if (wrapper.getEntityType() == EntityType.ARMOR_STAND)
        {
            var properties = DisguiseProperties.INSTANCE.getOrThrow(ArmorStandProperties.class);
            var wrapperShowArms = wrapper.readPropertyOr(properties.SHOW_ARMS, null);

            //盔甲架加上手臂
            if (wrapperShowArms == null)
            {
                var showArm = theirDisguise != null
                        ? theirDisguise.disguisePropertyHandler().getOr(properties.SHOW_ARMS, false)
                        : targetEntity instanceof ArmorStand armorStand
                            ? armorStand.hasArms()
                            : this.armorStandShowArms.get();

                wrapper.writeProperty(properties.SHOW_ARMS, showArm);
            }
        }

        var player = state.getPlayer();
        if (doHealthScale.get())
        {
            removeAllHealthModifiers(player);

            var entityClazz = state.getEntityType().getEntityClass();
            if (entityClazz != null)
                tryAddModifier(state);
        }

        if (modifyBoundingBoxes.get())
            this.tryModifyPlayerDimensions(player, state.getDisguiseWrapper());
    }

    private void tryAddModifier(DisguiseState state)
    {
        try
        {
            var player = state.getPlayer();
            var loc = player.getLocation();
            loc.setY(-8192);

            var entity = NmsUtils.spawnEntity(state.getEntityType(), state.getPlayer().getWorld(), loc);

            if (!(entity instanceof LivingEntity living))
            {
                entity.remove();
                return;
            }

            var craftLiving = (net.minecraft.world.entity.LivingEntity) ((CraftLivingEntity)living).getHandleRaw();
            var mobMaxHealth = craftLiving.craftAttributes.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            var playerAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

            if (mobMaxHealth <= 0d)
            {
                logger.warn("Entity has a max health that's lower than 0? Not applying...");
                return;
            }

            assert playerAttribute != null;
            var diff = mobMaxHealth - playerAttribute.getBaseValue();

            //确保血量不会超过上限
            if (playerAttribute.getBaseValue() + diff > healthCap.get())
                diff = healthCap.get() - playerAttribute.getBaseValue();

            //缩放生命值
            double diffFinal = diff;
            this.executeThenScaleHealth(player, playerAttribute, () ->
            {
                var modifier = new AttributeModifier(healthModifierKey, diffFinal, AttributeModifier.Operation.ADD_NUMBER);

                // In case some data sync plugins not processing correctly, also check for minecraft namespace.
                playerAttribute.removeModifier(healthModifierKeyVanilla);
                playerAttribute.removeModifier(healthModifierKey);

                // Also handle legacy keys
                playerAttribute.removeModifier(healthModifierKeyLegacy);

                playerAttribute.addModifier(modifier);
            });

            entity.remove();
        }
        catch (Throwable t)
        {
            logger.error("Error occurred trying to modify player's health attribute: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @NotNull
    public static final NamespacedKey healthModifierKeyLegacy = Objects.requireNonNull(NamespacedKey.fromString("feathermorph:health_modifier"), "How?!");

    @NotNull
    public static final NamespacedKey healthModifierKey = Objects.requireNonNull(NamespacedKey.fromString("feathermorph:fm_health_modifier"), "How?!");

    @NotNull
    public static final NamespacedKey healthModifierKeyVanilla = Objects.requireNonNull(NamespacedKey.fromString("minecraft:fm_health_modifier"), "How?!");

    private void resetPlayerDimensions(Player player)
    {
        var nmsPlayer = NmsRecord.ofPlayer(player);

        //Find dimensions
        Field targetField = null;

        try
        {
            targetField = ReflectionUtils.getPlayerDimensionsField(nmsPlayer);
        }
        catch (NullDependencyException t)
        {
            logger.error("Can't read player dimension.");
        }
        catch (Throwable t)
        {
            logger.error("Can't read player dimension: " + t.getMessage());
            t.printStackTrace();
        }

        if (targetField == null)
            return;

        try
        {
            var dimension = net.minecraft.world.entity.player.Player.STANDING_DIMENSIONS;

            targetField.setAccessible(true);
            targetField.set(nmsPlayer, dimension);

            nmsPlayer.refreshDimensions();
        }
        catch (Throwable t)
        {
            logger.error("Unable to reset player's bounding box: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void tryModifyPlayerDimensions(Player player, DisguiseWrapper<?> wrapper)
    {
        var nmsPlayer = NmsRecord.ofPlayer(player);

        //Find dimensions
        Field targetField = null;

        try
        {
            targetField = ReflectionUtils.getPlayerDimensionsField(nmsPlayer);
        }
        catch (NullDependencyException t)
        {
            logger.error("Can't read player dimension.");
        }
        catch (Throwable t)
        {
            logger.error("Can't read player dimension: " + t.getMessage());
            t.printStackTrace();
        }

        if (targetField == null) return;

        try
        {
            var box = wrapper.getBoundingBoxAt(nmsPlayer.getX(), nmsPlayer.getY(), nmsPlayer.getZ());
            var dimensions = wrapper.getDimensions();

            // Update dimensions
            targetField.set(nmsPlayer, dimensions);

            nmsPlayer.setBoundingBox(box);

            // Update eye height
            var eyeHeightField = ReflectionUtils.getPlayerEyeHeightField(NmsRecord.ofPlayer(player));
            eyeHeightField.set(nmsPlayer, dimensions.height() * 0.85F);
        }
        catch (Throwable t)
        {
            logger.warn("Unable to modify player's bounding box: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void executeThenScaleHealth(Player player, AttributeInstance attributeInstance, Runnable runnable)
    {
        var currentPercent = player.getHealth() / attributeInstance.getValue();

        runnable.run();

        if (player.getHealth() > 0)
            player.setHealth(Math.min(player.getMaxHealth(), attributeInstance.getValue() * currentPercent));
    }

    private void removeAllHealthModifiers(Player player)
    {
        var attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert attribute != null;

        this.executeThenScaleHealth(player, attribute, () -> attribute.removeModifier(healthModifierKey));
    }

    @Override
    public void resetDisguise(DisguiseState state)
    {
        var player = state.getPlayer();

        removeAllHealthModifiers(player);
        resetPlayerDimensions(player);
    }

    @Override
    public boolean unMorph(Player player, DisguiseState state)
    {
        if (super.unMorph(player, state))
        {
            resetDisguise(state);
            return true;
        }
        else
            return false;
    }

    @Override
    public @Nullable CompoundTag getInitialNbtCompound(DisguiseState state, Entity targetEntity, boolean enableCulling)
    {
        var info = getMorphManager().getDisguiseMeta(state.getDisguiseIdentifier());

        var rawCompound = targetEntity != null && canConstruct(info, targetEntity, null)
                ? NbtUtils.getRawTagCompound(targetEntity)
                : new CompoundTag();

        var theirDisguise = getMorphManager().getDisguiseStateFor(targetEntity);

        if (theirDisguise != null)
            rawCompound = theirDisguise.getDisguiseWrapper().getCompound();

        // ???
        // if (targetEntity == null || targetEntity.getType() != state.getEntityType())
        //    rawCompound.merge(state.getDisguiseWrapper().getCompound());

        return enableCulling ? cullNBT(rawCompound) : rawCompound;
    }

    @Override
    public boolean validForClient(DisguiseState state)
    {
        return true;
    }

    @Override
    public boolean canConstruct(DisguiseMeta info, @Nullable Entity targetEntity, DisguiseState theirState)
    {
        return theirState != null
                ? theirState.getDisguiseWrapper().getEntityType().equals(info.getEntityType())
                : targetEntity == null || targetEntity.getType().equals(info.getEntityType());
    }

    /**
     * 我们是否可以克隆目标实体/玩家的伪装？
     *
     * @param info         {@link DisguiseMeta}
     * @param targetEntity 目标实体
     * @param theirState   他们的{@link DisguiseState}，如果有
     * @return 是否允许克隆他们的装备进行显示
     */
    @Override
    public boolean canCloneEquipment(DisguiseMeta info, Entity targetEntity, DisguiseState theirState)
    {
        return canConstruct(info, targetEntity, theirState);
    }

    @Override
    protected boolean canCloneDisguise(DisguiseMeta info, Entity targetEntity,
                                       @NotNull DisguiseState theirDisguiseState, @NotNull DisguiseWrapper<?> theirDisguise)
    {
        return theirDisguise.getEntityType().equals(info.getEntityType());
    }

    @Resolved
    private VanillaMessageStore vanillaMessageStore;

    @Override
    public Component getDisplayName(String disguiseIdentifier, String locale)
    {
        var type = EntityTypeUtils.fromString(disguiseIdentifier, true);

        if (type == null)
            return Component.text("???");
        else
            return vanillaMessageStore.getComponent(type.translationKey(), null, locale);
    }
}
