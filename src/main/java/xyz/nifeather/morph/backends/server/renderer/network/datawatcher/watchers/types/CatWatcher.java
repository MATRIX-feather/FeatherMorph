package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.CatVariant;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
import xyz.nifeather.morph.misc.disguiseProperty.values.CatProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CatWatcher extends TameableAnimalWatcher
{
    public CatWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.CAT);

        customSerializeMethods.put(ValueIndex.CAT.CAT_VARIANT, CustomSerializeMethods.CAT_VARIANT);
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

        register(ValueIndex.CAT);
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(CatProperties.class);

        if (property.equals(properties.CAT_VARIANT))
        {
            var variant = (Cat.Type) value;
            writePersistent(ValueIndex.CAT.CAT_VARIANT, variant);
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
                case AnimationNames.LAY_START -> this.writePersistent(ValueIndex.CAT.IS_LYING, true);
                case AnimationNames.SIT -> this.writePersistent(ValueIndex.CAT.TAMEABLE_FLAGS, (byte)0x01);
                case AnimationNames.STANDUP, AnimationNames.RESET ->
                {
                    if (this.readOr(ValueIndex.CAT.IS_LYING, false))
                        this.writePersistent(ValueIndex.CAT.IS_LYING, false);

                    if ((this.readOr(ValueIndex.CAT.TAMEABLE_FLAGS, (byte)0x00) & 1) != 0)
                        this.writePersistent(ValueIndex.CAT.TAMEABLE_FLAGS, (byte)0x00);
                }
            }
        }
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("variant"))
        {
            var name = nbt.getString("variant");
            var key = NamespacedKey.fromString(name);

            if (key != null)
            {
                var bukkitMatch = Registry.CAT_VARIANT.get(key);

                if (bukkitMatch != null)
                    this.writePersistent(ValueIndex.CAT.CAT_VARIANT, bukkitMatch);
            }
            else
            {
                logger.warn("Invalid cat variant: '%s', ignoring...".formatted(name));
            }
        }

        if (nbt.contains("CollarColor"))
            writePersistent(ValueIndex.CAT.COLLAR_COLOR, (int)nbt.getByte("CollarColor"));
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        var variant = read(ValueIndex.CAT.CAT_VARIANT).getKey().asString();
        nbt.putString("variant", variant);

        var collarColor = read(ValueIndex.CAT.COLLAR_COLOR);
        nbt.putInt("CollarColor", collarColor);
    }
}
