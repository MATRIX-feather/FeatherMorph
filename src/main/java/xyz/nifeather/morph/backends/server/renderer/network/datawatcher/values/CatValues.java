package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import org.bukkit.entity.Cat;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.TameableAnimalValues;

public class CatValues extends TameableAnimalValues
{
    public final SingleValue<Cat.Type> CAT_VARIANT = createSingle("cat_variant", Cat.Type.TABBY);
    public final SingleValue<Boolean> IS_LYING = createSingle("cat_is_lying", false);
    public final SingleValue<Boolean> RELAXED = createSingle("cat_relaxed", false);
    public final SingleValue<Integer> COLLAR_COLOR = createSingle("cat_collar_color", 14);

    public CatValues()
    {
        super();

        CAT_VARIANT.setSerializer(CustomSerializeMethods.CAT_VARIANT);
        registerSingle(CAT_VARIANT, IS_LYING, RELAXED, COLLAR_COLOR);
    }
}
