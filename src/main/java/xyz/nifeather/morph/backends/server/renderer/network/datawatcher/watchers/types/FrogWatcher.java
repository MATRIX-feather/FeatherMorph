package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.FrogVariant;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.utilties.HolderUtils;
import xyz.nifeather.morph.misc.AnimationNames;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.disguiseProperty.values.FrogProperties;

public class FrogWatcher extends LivingEntityWatcher
{
    public FrogWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.FROG);
    }

    private Holder<FrogVariant> getFrogVariant(ResourceKey<FrogVariant> key)
    {
        return HolderUtils.getHolderOrThrow(key, Registries.FROG_VARIANT);
    }

    public Holder<FrogVariant> getFrogVariant(Frog.Variant bukkitVariant)
    {
        var bukkitKey = bukkitVariant.getKey();

        return HolderUtils.getHolderOrThrow(ResourceLocation.parse(bukkitKey.asString()), Registries.FROG_VARIANT);
    }

    public Frog.Variant getBukkitFrogVariant()
    {
        var type = read(ValueIndex.FROG.FROG_VARIANT);

        var keyOptional = type.unwrapKey();
        if (keyOptional.isEmpty())
        {
            logger.warn("Empty key for value '%s'?!".formatted(type));
            return Frog.Variant.TEMPERATE;
        }

        var bukkitMatch = Registry.FROG_VARIANT.get(NamespacedKey.fromString(keyOptional.get().location().toString()));
        if (bukkitMatch != null)
            return bukkitMatch;

        logger.warn("No suitable Variant for FrogVariant '%s'".formatted(type));
        return Frog.Variant.TEMPERATE;
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.FROG);
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("variant"))
        {
            var typeString = nbt.getString("variant");
            ResourceLocation rl;
            ResourceKey<FrogVariant> type = FrogVariant.TEMPERATE;

            try
            {
                rl = ResourceLocation.parse(typeString);
                type = ResourceKey.create(Registries.FROG_VARIANT, rl);
            }
            catch (Throwable t)
            {
                logger.error("Failed reading FrogVariant from NBT: " + t.getMessage());
            }

            writePersistent(ValueIndex.FROG.FROG_VARIANT, getFrogVariant(type));
        }
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(FrogProperties.class);

        if (property.equals(properties.VARIANT))
        {
            var variant = (Frog.Variant) value;
            writePersistent(ValueIndex.FROG.FROG_VARIANT, getFrogVariant(variant));
        }
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.ANIMATION))
        {
            var animId = newVal.toString();
            var player = getBindingPlayer();
            var world = player.getWorld();

            switch (animId)
            {
                case AnimationNames.EAT ->
                {
                    this.writePersistent(ValueIndex.FROG.POSE, Pose.USING_TONGUE);
                    world.playSound(player.getLocation(), Sound.ENTITY_FROG_EAT, 1, 1);
                }
                case AnimationNames.RESET -> this.remove(ValueIndex.FROG.POSE);
            }
        }
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        var variant = this.getBukkitFrogVariant().getKey().asString();
        nbt.putString("variant", variant);
    }
}
