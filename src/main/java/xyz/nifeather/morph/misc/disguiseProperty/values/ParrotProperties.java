package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Parrot.Variant;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParrotProperties extends AbstractProperties
{
    private final Map<String, Variant> variantMap = new ConcurrentHashMap<>();

    private void initMap()
    {
        for (var variant : Variant.values())
            variantMap.put(variant.name().toLowerCase(), variant);
    }

    public final SingleProperty<Parrot.Variant> VARIANT = getSingle("parrot_variant", Variant.RED)
            .withRandom(Variant.values());

    public ParrotProperties()
    {
        initMap();
        VARIANT.withValidInput(variantMap.keySet());

        registerSingle(VARIANT);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(VARIANT.id()))
        {
            var variant = variantMap.getOrDefault(value, null);

            if (variant != null)
                return Pair.of(VARIANT, variant);
        }

        return null;
    }
}
