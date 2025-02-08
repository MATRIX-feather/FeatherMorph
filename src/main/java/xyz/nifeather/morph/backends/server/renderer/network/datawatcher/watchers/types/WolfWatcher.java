package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.WolfVariants;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.ICustomSerializeMethod;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;
import xyz.nifeather.morph.misc.AnimationNames;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.disguiseProperty.values.WolfProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class WolfWatcher extends TameableAnimalWatcher
{
    public WolfWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.WOLF);

        var nmsWolfVariantHandle = WrappedDataWatcher.Registry.fromHandle(EntityDataSerializers.WOLF_VARIANT);
        customSerializeMethods.put(ValueIndex.WOLF.WOLF_VARIANT, CustomSerializeMethods.WOLF_VARIANT);
    }

    private final Map<SingleValue<?>, ICustomSerializeMethod<?>> customSerializeMethods = new ConcurrentHashMap<>();

    @Override
    public Map<SingleValue<?>, ICustomSerializeMethod<?>> customSerializeMethods()
    {
        return customSerializeMethods;
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.WOLF);
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(WolfProperties.class);

        if (property.equals(properties.VARIANT))
        {
            var val = (Wolf.Variant) value;

            this.writePersistent(ValueIndex.WOLF.WOLF_VARIANT, val);
        }

        super.onPropertyWrite(property, value);
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.ANIMATION))
        {
            var animId = newVal.toString();

            switch (animId)
            {
                case AnimationNames.SIT -> this.writePersistent(ValueIndex.WOLF.TAMEABLE_FLAGS, (byte)0x01);
                case AnimationNames.STANDUP -> this.writePersistent(ValueIndex.WOLF.TAMEABLE_FLAGS, (byte)0x00);
            }
        }
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("CollarColor"))
            writePersistent(ValueIndex.WOLF.COLLAR_COLOR, (int)nbt.getByte("CollarColor"));

        if (nbt.contains("variant"))
        {
            var typeString = nbt.getString("variant");
            NamespacedKey key = NamespacedKey.fromString(typeString);;

            if (key == null && FeatherMorphMain.getInstance().doInternalDebugOutput)
            {
                logger.warn("[DEBUG] Ignoring unknown variant: " + typeString);
                return;
            }

            writePersistent(ValueIndex.WOLF.WOLF_VARIANT, getVariant(key));
        }
    }

    private Wolf.Variant getVariant(NamespacedKey key)
    {
        try
        {
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.WOLF_VARIANT)
                    .getOrThrow(key);
        }
        catch (Throwable t)
        {
            logger.warn("Can't find Holder for key '%s', trying default value...".formatted(key));
            return ValueIndex.WOLF.WOLF_VARIANT.defaultValue();
        }
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        nbt.putByte("CollarColor", read(ValueIndex.WOLF.COLLAR_COLOR).byteValue());
        nbt.putString("variant", read(ValueIndex.WOLF.WOLF_VARIANT).key().asString());
    }
}
