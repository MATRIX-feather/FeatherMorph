package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Fox;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;

public class FoxProperties extends AbstractProperties
{
    public final SingleProperty<Fox.Type> VARIANT = getSingle("fox_variant", Fox.Type.RED)
            .withRandom(Fox.Type.values())
            .withValidInput("default", "snow");

    public FoxProperties()
    {
        registerSingle(VARIANT);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(VARIANT.id()))
        {
            var type = value.equals("default") ? Fox.Type.RED : Fox.Type.SNOW;

            return Pair.of(VARIANT, type);
        }

        return null;
    }
}
