package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import org.bukkit.entity.Wolf;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.TameableAnimalValues;

public class WolfValues extends TameableAnimalValues
{
    public final SingleValue<Boolean> BEGGING = createSingle("wolf_begging", false);
    public final SingleValue<Integer> COLLAR_COLOR = createSingle("wolf_collar_color", 14);
    public final SingleValue<Integer> ANGER_TIME = createSingle("wolf_anger_time", 0);
    public final SingleValue<Wolf.Variant> WOLF_VARIANT = createSingle("wolf_variant", Wolf.Variant.PALE);

    public WolfValues()
    {
        super();

        WOLF_VARIANT.setSerializeMethod(CustomSerializeMethods.WOLF_VARIANT);
        registerSingle(WOLF_VARIANT, BEGGING, COLLAR_COLOR, ANGER_TIME);
    }
}
