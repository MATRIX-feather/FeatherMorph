package xyz.nifeather.morph.backends.fallback;

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
import xyz.nifeather.morph.misc.DisguiseEquipment;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.utilities.NbtUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NilWrapper extends EventWrapper<NilDisguise>
{
    public NilWrapper(@NotNull NilDisguise instance, NilBackend backend)
    {
        super(instance, backend);

        this.backend = backend;
    }

    private final NilBackend backend;

    private final DisguiseEquipment equipment = new DisguiseEquipment();

    @Override
    public void mergeCompound(CompoundTag compoundTag)
    {
        var compound = readPropertyOr(WrapperProperties.NBT, null);

        if (compound == null)
        {
            compound = WrapperProperties.NBT.defaultVal().copy();
            writeProperty(WrapperProperties.NBT, compound);
        }

        compound.merge(compoundTag);
        this.instance.isBaby = NbtUtils.isBabyForType(getEntityType(), compoundTag);

        if (this.getEntityType() == EntityType.MAGMA_CUBE || this.getEntityType() == EntityType.SLIME)
            resetDimensions();
    }

    @Override
    public CompoundTag getCompound()
    {
        return readPropertyOr(WrapperProperties.NBT, WrapperProperties.NBT.defaultVal().copy());
    }

    private static final UUID nilUUID = UUID.fromString("0-0-0-0-0");

    /**
     * Gets network id of this disguise displayed to other players
     *
     * @return The network id of this disguise
     */
    @Override
    public int getNetworkEntityId()
    {
        return -1;
    }

    private final Map<SingleProperty<?>, Object> disguiseProperties = new ConcurrentHashMap<>();

    @Override
    public Map<SingleProperty<?>, Object> getProperties()
    {
        return new Object2ObjectOpenHashMap<>(disguiseProperties);
    }

    @Override
    public <X> void writeProperty(SingleProperty<X> property, X value)
    {
        disguiseProperties.put(property, value);

        if (property.equals(WrapperProperties.PROFILE))
        {
            var val = ((Optional<GameProfile>) value).orElse(null);

            callEvent(WrapperEvent.SKIN_SET, val);
            return;
        }

        if (property.equals(WrapperProperties.DISPLAY_FAKE_EQUIP) && getBindingPlayer() != null)
        {
            backend.getNetworkingHelper().prepareMeta(getBindingPlayer())
                    .setDisguiseEquipmentShown(Boolean.TRUE.equals(value))
                    .send();

            return;
        }
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

    @Nullable
    @Override
    public <R extends Tag> R getTag(@NotNull String path, TagType<R> type)
    {
        try
        {
            var obj = getCompound().get(path);

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
    public NilDisguise copyInstance()
    {
        return instance.clone();
    }

    @Override
    public DisguiseWrapper<NilDisguise> clone()
    {
        var instance = new NilWrapper(this.copyInstance(), (NilBackend) getBackend());

        instance.disguiseProperties.putAll(this.disguiseProperties);

        return instance;
    }

    public static NilWrapper fromExternal(DisguiseWrapper<?> other, NilBackend backend)
    {
        var instance = new NilWrapper(new NilDisguise(other.getEntityType()), backend);

        instance.disguiseProperties.putAll(other.getProperties());

        return instance;
    }

    @Override
    public boolean isBaby()
    {
        return instance.isBaby;
    }

    @Override
    public void onPostConstructDisguise(DisguiseState state, @Nullable Entity targetEntity)
    {
    }

    @Override
    public void update(DisguiseState state, Player player)
    {
    }

    @Nullable
    private Player bindingPlayer;

    @Nullable
    public Player getBindingPlayer()
    {
        return bindingPlayer;
    }

    public void setBindingPlayer(@Nullable Player player)
    {
        this.bindingPlayer = player;
    }
}
