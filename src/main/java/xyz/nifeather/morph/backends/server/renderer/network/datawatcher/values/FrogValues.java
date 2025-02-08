package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import org.bukkit.entity.Frog;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.AnimalValues;

public class FrogValues extends AnimalValues
{
    public final SingleValue<Frog.Variant> FROG_VARIANT = createSingle("frog_variant", Frog.Variant.TEMPERATE);

    public FrogValues()
    {
        super();

        FROG_VARIANT.setSerializer(CustomSerializeMethods.FROG_VARIANT);

        registerSingle(FROG_VARIANT);
    }
}
