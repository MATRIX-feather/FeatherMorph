package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Registry;
import org.bukkit.entity.Frog;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrogProperties extends AbstractProperties
{
    private final Map<String, Frog.Variant> variantMap = new ConcurrentHashMap<>();

    private void initMap()
    {
        for (Frog.Variant variant : Registry.FROG_VARIANT.stream().toList())
            variantMap.put(variant.key().asString(), variant);
    }

    public final SingleProperty<Frog.Variant> VARIANT = getSingle("frog_variant", Frog.Variant.TEMPERATE)
            .withRandom(Frog.Variant.TEMPERATE, Frog.Variant.COLD, Frog.Variant.WARM);

    public FrogProperties()
    {
        initMap();
        VARIANT.withValidInput(variantMap.keySet());

        registerSingle(
                VARIANT
        );
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(VARIANT.id()))
        {
            var match = variantMap.getOrDefault(value, null);

            if (match != null)
                return Pair.of(VARIANT, match);
        }
        return null;
    }
}
