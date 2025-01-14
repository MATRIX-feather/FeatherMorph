package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CreeperProperties extends AbstractProperties
{
    public final SingleProperty<Boolean> CHARGED = getSingle("creeper_charged", false)
            .withRandom(false, false, false, true)
            .withValidInput("true", "false");

    public CreeperProperties()
    {
        registerSingle(CHARGED);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(CHARGED.id()))
            return Pair.of(CHARGED, Boolean.valueOf(value));

        return null;
    }
}
