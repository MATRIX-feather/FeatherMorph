package xyz.nifeather.morph.backends.server;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.MorphPlugin;
import xyz.nifeather.morph.backends.DisguiseWrapper;
import xyz.nifeather.morph.backends.EventWrapper;
import xyz.nifeather.morph.backends.WrapperEvent;
import xyz.nifeather.morph.backends.WrapperProperties;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.SingleWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.AgeableMobWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.InventoryLivingWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.WatcherUtils;
import xyz.nifeather.morph.misc.DisguiseEquipment;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.playerList.PlayerListHandler;
import xyz.nifeather.morph.utilities.NbtUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerDisguiseWrapper extends EventWrapper<ServerDisguise>
{
    private final ServerBackend backend;

    public ServerDisguiseWrapper(@NotNull ServerDisguise instance, ServerBackend backend)
    {
        super(instance, backend);

        this.backend = backend;
    }

    private final DisguiseEquipment equipment = new DisguiseEquipment();

    @Override
    public void mergeCompound(CompoundTag compoundTag)
    {
        this.instance.compoundTag.merge(compoundTag);
        this.instance.isBaby = NbtUtils.isBabyForType(getEntityType(), compoundTag);

        if (this.getEntityType() == EntityType.MAGMA_CUBE || this.getEntityType() == EntityType.SLIME)
            resetDimensions();

        if (bindingWatcher != null)
        {
            bindingWatcher.mergeFromCompound(compoundTag);

            if (bindingWatcher instanceof AgeableMobWatcher)
                bindingWatcher.writePersistent(ValueIndex.AGEABLE_MOB.IS_BABY, instance.isBaby);

            if (compoundTag.contains("Small")) instance.armorStandSmall = compoundTag.getBoolean("Small");
            if (compoundTag.contains("NoBasePlate")) instance.armorStandNoBasePlate = compoundTag.getBoolean("NoBasePlate");
            if (compoundTag.contains("ShowArms")) instance.armorStandShowArms = compoundTag.getBoolean("ShowArms");
        }
    }

    private CompoundTag getCompound(boolean includeWatcher)
    {
        var tagCopy = this.instance.compoundTag.copy();

        if (bindingWatcher != null && includeWatcher)
            tagCopy.merge(WatcherUtils.buildCompoundFromWatcher(bindingWatcher));

        return tagCopy;
    }

    @Override
    public CompoundTag getCompound()
    {
        return this.getCompound(true);
    }

    /**
     * Gets network id of this disguise displayed to other players
     *
     * @return The network id of this disguise
     */
    @Override
    public int getNetworkEntityId()
    {
        return bindingPlayer.getEntityId();
    }

    @Nullable
    @Override
    public <R extends Tag> R getTag(@NotNull String path, TagType<R> type)
    {
        try
        {
            var obj = instance.compoundTag.get(path);

            if (obj != null && obj.getType() == type)
                return (R) obj;

            return null;
        }
        catch (Throwable t)
        {
            logger.error("Unable to read NBT '%s' from instance:".formatted(path));
            t.printStackTrace();

            return null;
        }
    }

    private static final Logger logger = MorphPlugin.getInstance().getSLF4JLogger();

    @Override
    public EntityEquipment getFakeEquipments()
    {
        return equipment;
    }

    @Override
    public void setFakeEquipments(@NotNull EntityEquipment newEquipment)
    {
        this.equipment.setArmorContents(newEquipment.getArmorContents());

        this.equipment.setHandItems(newEquipment.getItemInMainHand(), newEquipment.getItemInOffHand());

        if (bindingWatcher != null)
            bindingWatcher.writeEntry(CustomEntries.EQUIPMENT, this.equipment);
    }

    @Override
    public void setDisplayingFakeEquipments(boolean newVal)
    {
        super.setDisplayingFakeEquipments(newVal);

        if (bindingWatcher != null)
            bindingWatcher.writeEntry(CustomEntries.DISPLAY_FAKE_EQUIPMENT, newVal);
    }

    @Override
    public void setServerSelfView(boolean enabled)
    {
    }

    @Override
    public EntityType getEntityType()
    {
        return instance.type;
    }

    @Override
    public ServerDisguise copyInstance()
    {
        return instance.clone();
    }

    @Override
    public DisguiseWrapper<ServerDisguise> clone()
    {
        var newInstance = cloneFromExternal(this, (ServerBackend) getBackend());

        newInstance.mergeCompound(this.getCompound());
        newInstance.setFakeEquipments(this.equipment);

        return newInstance;
    }

    public static ServerDisguiseWrapper cloneFromExternal(DisguiseWrapper<?> other, ServerBackend backend)
    {
        var newInstance = new ServerDisguiseWrapper(new ServerDisguise(other.getEntityType()), backend);

        newInstance.disguiseProperties.putAll(other.getProperties());

        return newInstance;
    }

    private final Map<SingleProperty<?>, Object> disguiseProperties = new ConcurrentHashMap<>();

    @Override
    public <X> void writeProperty(SingleProperty<X> property, X value)
    {
        disguiseProperties.put(property, value);

        if (bindingWatcher != null)
            bindingWatcher.writeProperty(property, value);
    }

    @Override
    public <X> @NotNull X readProperty(SingleProperty<X> property)
    {
        return this.readPropertyOr(property, property.defaultVal());
    }

    @Override
    public <X> X readPropertyOr(SingleProperty<X> property, X defaultVal)
    {
        return (X) disguiseProperties.getOrDefault(property, defaultVal);
    }

    @Override
    public <X> X readPropertyOrThrow(SingleProperty<X> property)
    {
        var val = disguiseProperties.getOrDefault(property, null);
        if (val == null) throw new NullDependencyException("The requested property '%s' was not found in %s".formatted(property.id(), this));

        return (X) val;
    }

    @Override
    public Map<SingleProperty<?>, Object> getProperties()
    {
        return new Object2ObjectOpenHashMap<>(this.disguiseProperties);
    }

    @Override
    public void setDisguiseName(String name)
    {
        super.setDisguiseName(name);

        if (bindingWatcher != null)
            bindingWatcher.writeEntry(CustomEntries.DISGUISE_NAME, name);
    }

    @Override
    public boolean isBaby()
    {
        return instance.isBaby;
    }

    @Override
    public void applySkin(GameProfile profile)
    {
        if (this.getEntityType() != EntityType.PLAYER) return;

        writeProperty(WrapperProperties.PROFILE, Optional.of(profile));

        if (bindingWatcher != null)
            bindingWatcher.writeEntry(CustomEntries.PROFILE, profile);

        callEvent(WrapperEvent.SKIN_SET, profile);
    }

    @Override
    public void onPostConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
    }

    @Override
    public void update(DisguiseState state, Player player)
    {
    }

    @Override
    public void onPlayerOffline()
    {
        PlayerListHandler.instance().hideFakePlayer(bindingWatcher.readEntryOrThrow(CustomEntries.SPAWN_UUID));

        super.onPlayerOffline();
    }

    private boolean aggressive;

    @Override
    public void setAggressive(boolean aggressive)
    {
        super.setAggressive(aggressive);

        this.aggressive = aggressive;
        if (getEntityType() == EntityType.GHAST)
            bindingWatcher.writePersistent(ValueIndex.GHAST.CHARGING, aggressive);

        if (getEntityType() == EntityType.CREEPER)
        {
            bindingWatcher.writePersistent(ValueIndex.CREEPER.STATE, aggressive ? 1 : -1);
            bindingWatcher.writePersistent(ValueIndex.CREEPER.IGNITED, aggressive);
        }

        if (getEntityType() == EntityType.WARDEN)
            bindingWatcher.writeEntry(CustomEntries.WARDEN_CHARGING_ATTACK, aggressive);
    }

    @Override
    public void playAttackAnimation()
    {
        super.playAttackAnimation();
        bindingWatcher.writeEntry(CustomEntries.ATTACK_ANIMATION, true);
    }

    private Player bindingPlayer;

    public Player getBindingPlayer()
    {
        return bindingPlayer;
    }

    private SingleWatcher bindingWatcher;

    @Nullable
    public SingleWatcher getBindingWatcher()
    {
        return bindingWatcher;
    }

    public void setRenderParameters(@NotNull Player newBinding, @NotNull SingleWatcher bindingWatcher)
    {
        Objects.requireNonNull(bindingWatcher, "Null Watcher!");

        bindingPlayer = newBinding;

        if (this.bindingWatcher != null)
        {
            this.bindingWatcher.dispose();
            this.bindingWatcher = null;
        }

        refreshRegistry(newBinding, bindingWatcher);

        this.bindingWatcher = bindingWatcher;
    }

    private void refreshRegistry(@NotNull Player bindingPlayer, @NotNull SingleWatcher bindingWatcher)
    {
        //和watcher同步我们的NBT
        bindingWatcher.mergeFromCompound(getCompound(false));

        this.disguiseProperties.forEach((property, value) ->
        {
            bindingWatcher.writeProperty((SingleProperty<Object>) property, value);
        });

        if (getEntityType() == EntityType.PLAYER)
        {
            var profileOptional = readProperty(WrapperProperties.PROFILE);
            profileOptional.ifPresent(p -> bindingWatcher.writeEntry(CustomEntries.PROFILE, p));
        }

        //todo: 激活刷新时也刷新到玩家
        if (bindingWatcher instanceof InventoryLivingWatcher)
        {
            bindingWatcher.writeEntry(CustomEntries.DISPLAY_FAKE_EQUIPMENT, readProperty(WrapperProperties.DISPLAY_FAKE_EQUIP));
            bindingWatcher.writeEntry(CustomEntries.EQUIPMENT, this.equipment);
        }

        if (bindingWatcher.getEntityType() == EntityType.GHAST)
            bindingWatcher.writePersistent(ValueIndex.GHAST.CHARGING, aggressive);
    }

    @Override
    public void playAnimation(String animationId)
    {
        if (bindingWatcher != null)
            bindingWatcher.writeEntry(CustomEntries.ANIMATION, animationId);
    }

    @Override
    public void onPlayerJoin(Player newInstance)
    {
        if (bindingWatcher != null)
        {
            this.bindingWatcher.writeEntry(CustomEntries.SPAWN_ID, newInstance.getEntityId());
            this.bindingPlayer = newInstance;

            if (this.getEntityType() == EntityType.PLAYER && backend.serverRenderer.showPlayerDisguises.get())
            {
                PlayerListHandler.instance().showFakePlayer(
                        bindingWatcher.readEntryOrThrow(CustomEntries.SPAWN_UUID),
                        bindingWatcher.readEntryOrThrow(CustomEntries.PROFILE)
                );
            }
        }

        super.onPlayerJoin(newInstance);
    }
}
