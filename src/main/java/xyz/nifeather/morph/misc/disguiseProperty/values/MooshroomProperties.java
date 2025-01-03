package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.MushroomCow;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

public class MooshroomProperties extends AbstractProperties
{
    public final SingleProperty<MushroomCow.Variant> VARIANT = getSingle("mooshroom_variant", MushroomCow.Variant.RED)
            .withRandom(MushroomCow.Variant.RED, MushroomCow.Variant.RED, MushroomCow.Variant.RED, MushroomCow.Variant.BROWN)
            .withValidInput("red", "brown");

    public MooshroomProperties()
    {
        registerSingle(VARIANT);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(VARIANT.id()))
        {
            if (value.equals("red"))
                return Pair.of(VARIANT, MushroomCow.Variant.RED);
            else if (value.equals("brown"))
                return Pair.of(VARIANT, MushroomCow.Variant.BROWN);
        }

        return null;
    }
}
