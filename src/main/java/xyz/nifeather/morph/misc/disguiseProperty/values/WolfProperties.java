package xyz.nifeather.morph.misc.disguiseProperty.values;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Wolf.Variant;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WolfProperties extends AbstractProperties
{
    private final Map<String, Wolf.Variant> variantMap = new ConcurrentHashMap<>();

    private void initMap()
    {
        for (var variant : RegistryAccess.registryAccess().getRegistry(RegistryKey.WOLF_VARIANT))
            variantMap.put(variant.key().asString(), variant);
    }

    public final SingleProperty<Wolf.Variant> VARIANT = getSingle("wolf_variant", Variant.PALE)
            .withRandom(
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.WOLF_VARIANT).stream().toList()
            );

    public WolfProperties()
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
            var variant = this.variantMap.getOrDefault(value, null);

            if (variant != null)
                return Pair.of(VARIANT, variant);
        }

        return null;
    }
}
