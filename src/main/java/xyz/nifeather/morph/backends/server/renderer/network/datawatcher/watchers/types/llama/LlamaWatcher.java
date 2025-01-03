package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.llama;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types.horses.ChestedHorseWatcher;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.disguiseProperty.values.LlamaProperties;

public class LlamaWatcher extends ChestedHorseWatcher
{
    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.LLAMA);
    }

    public LlamaWatcher(Player bindingPlayer, EntityType type)
    {
        super(bindingPlayer, type);
    }

    public LlamaWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.LLAMA);
    }

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(LlamaProperties.class);

        if (property.equals(properties.COLOR))
        {
            var val = (Llama.Color) value;

            writePersistent(ValueIndex.LLAMA.VARIANT, val.ordinal());
        }

        super.onPropertyWrite(property, value);
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("Strength"))
            writePersistent(ValueIndex.LLAMA.SLOTS, nbt.getInt("Strength"));

        if (nbt.contains("DecorItem"))
            logger.warn("todo: Llama DecorItem is not implemented.");

        if (nbt.contains("Variant"))
            writePersistent(ValueIndex.LLAMA.VARIANT, nbt.getInt("Variant"));
    }
}
