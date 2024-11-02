package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.WolfVariants;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;
import xyz.nifeather.morph.misc.AnimationNames;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.disguiseProperty.values.WolfProperties;

public class WolfWatcher extends TameableAnimalWatcher
{
    public WolfWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.WOLF);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.WOLF);
    }

    public Holder<WolfVariant> getVariant(Wolf.Variant bukkitVariant)
    {
        var bukkitKey = bukkitVariant.getKey();

        return HolderUtils.getHolderOrThrow(ResourceLocation.parse(bukkitKey.asString()), Registries.WOLF_VARIANT);
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(WolfProperties.class);

        if (property.equals(properties.VARIANT))
        {
            var val = (Wolf.Variant) value;

            this.writePersistent(ValueIndex.WOLF.WOLF_VARIANT, getVariant(val));
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
            ResourceLocation rl;
            ResourceKey<WolfVariant> type = WolfVariants.PALE;

            try
            {
                rl = ResourceLocation.parse(typeString);
                type = ResourceKey.create(Registries.WOLF_VARIANT, rl);
            }
            catch (Throwable t)
            {
                logger.error("Failed reading FrogVariant from NBT: " + t.getMessage());
            }

            writePersistent(ValueIndex.WOLF.WOLF_VARIANT, getVariant(type));
        }
    }

    private Holder<WolfVariant> getVariant(ResourceKey<WolfVariant> key)
    {
        try
        {
            return HolderUtils.getHolderOrThrow(key, Registries.WOLF_VARIANT);
        }
        catch (Throwable t)
        {
            logger.warn("Can't find Holder for key '%s', trying default value...".formatted(key));
            return ValueIndex.WOLF.WOLF_VARIANT.defaultValue();
        }
    }

    private Wolf.Variant getBukkitVariant(Holder<WolfVariant> holder)
    {
        var variants = new Wolf.Variant[]
        {
                Wolf.Variant.WOODS,
                Wolf.Variant.ASHEN,
                Wolf.Variant.BLACK,
                Wolf.Variant.PALE,
                Wolf.Variant.RUSTY,
                Wolf.Variant.CHESTNUT,
                Wolf.Variant.SNOWY,
                Wolf.Variant.SPOTTED,
                Wolf.Variant.STRIPED
        };

        var idOptional = holder.unwrapKey();
        if (idOptional.isEmpty())
        {
            logger.error("Empty ID for holder " + holder + ", can't get bukkit variant");
            return Wolf.Variant.PALE;
        }

        var id = holder.unwrapKey().get().location().toString();
        for (Wolf.Variant variant : variants)
        {
            if (variant.key().toString().equalsIgnoreCase(id))
                return variant;
        }

        logger.error("No suitable bukkit variant for id " + id);
        return Wolf.Variant.PALE;
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        nbt.putByte("CollarColor", read(ValueIndex.WOLF.COLLAR_COLOR).byteValue());
        nbt.putString("variant", getBukkitVariant(read(ValueIndex.WOLF.WOLF_VARIANT)).key().asString());
    }
}
