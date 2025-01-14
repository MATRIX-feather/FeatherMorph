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
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;
import xyz.nifeather.morph.misc.AnimationNames;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.disguiseProperty.values.CatProperties;

public class CatWatcher extends TameableAnimalWatcher
{
    public CatWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.CAT);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.CAT);
    }

    public Cat.Type getCatType()
    {
        var value = read(ValueIndex.CAT.CAT_VARIANT);
        var key = value.unwrapKey().orElse(null);
        if (key == null)
            logger.warn("Null Key for holder " + value);

        return Registry.CAT_VARIANT.get(Key.key(key.location().toString()));
    }

    private Holder<CatVariant> bukkitTypeToNmsHolder(Cat.Type bukkitType)
    {
        var bukkitKey = bukkitType.getKey();
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath(bukkitKey.namespace(), bukkitKey.getKey());

        try
        {
            return HolderUtils.getHolderOrThrow(key, Registries.CAT_VARIANT);
        }
        catch (Throwable t)
        {
            logger.warn("Bukkit type '%s' is not in the registries, trying default value...".formatted(bukkitType));

            return ValueIndex.CAT.CAT_VARIANT.defaultValue();
        }
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(CatProperties.class);

        if (property.equals(properties.CAT_VARIANT))
        {
            var variant = (Cat.Type) value;
            writePersistent(ValueIndex.CAT.CAT_VARIANT, bukkitTypeToNmsHolder(variant));
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
                {
                    var finalValue = bukkitTypeToNmsHolder(bukkitMatch);
                    this.writePersistent(ValueIndex.CAT.CAT_VARIANT, finalValue);
                }
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

        var variant = this.getCatType().getKey().asString();
        nbt.putString("variant", variant);

        var collarColor = read(ValueIndex.CAT.COLLAR_COLOR);
        nbt.putInt("CollarColor", collarColor);
    }
}
