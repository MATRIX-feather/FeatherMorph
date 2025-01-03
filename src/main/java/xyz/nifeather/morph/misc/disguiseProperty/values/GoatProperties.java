package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

public class GoatProperties extends AbstractProperties
{
    public final SingleProperty<Boolean> HAS_LEFT_HORN = getSingle("goat_has_left_horn", true)
            .withRandom(true, true, true, false)
            .withValidInput("false", "true");

    public final SingleProperty<Boolean> HAS_RIGHT_HORN = getSingle("goat_has_right_horn", true)
            .withRandom(true, true, true, false)
            .withValidInput("false", "true");

    public GoatProperties()
    {
        registerSingle(HAS_LEFT_HORN, HAS_RIGHT_HORN);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        return switch (key)
        {
            case "goat_has_left_horn" -> Pair.of(HAS_LEFT_HORN, Boolean.valueOf(value));
            case "goat_has_right_horn" -> Pair.of(HAS_RIGHT_HORN, Boolean.valueOf(value));

            default -> null;
        };
    }
}
