package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorStandProperties extends AbstractProperties
{
    public final SingleProperty<Boolean> SHOW_ARMS = getSingle("armor_stand_show_arms", false).withValidInput("true", "false");

    public ArmorStandProperties()
    {
        registerSingle(SHOW_ARMS);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(SHOW_ARMS.id()))
            return Pair.of(SHOW_ARMS, Boolean.valueOf(value));

        return null;
    }
}
