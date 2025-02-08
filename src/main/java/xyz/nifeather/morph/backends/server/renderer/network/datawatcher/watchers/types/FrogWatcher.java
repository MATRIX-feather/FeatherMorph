package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
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

    private Frog.Variant getFrogVariant(NamespacedKey key)
    {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.FROG_VARIANT)
                .getOrThrow(key);
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
            NamespacedKey key = NamespacedKey.fromString(typeString);

            writePersistent(ValueIndex.FROG.FROG_VARIANT, getFrogVariant(key));
        }
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(FrogProperties.class);

        if (property.equals(properties.VARIANT))
        {
            var variant = (Frog.Variant) value;
            writePersistent(ValueIndex.FROG.FROG_VARIANT, variant);
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

        var variant = read(ValueIndex.FROG.FROG_VARIANT).getKey().asString();
        nbt.putString("variant", variant);
    }
}
