package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Rabbit.Type;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RabbitProperties extends AbstractProperties
{
    private final Map<String, Type> typeMap = new ConcurrentHashMap<>();

    private void initMap()
    {
        for (var type : Type.values())
            typeMap.put(type.name().toLowerCase(), type);
    }

    public final SingleProperty<Rabbit.Type> VARIANT = getSingle("rabbit_type", Type.BROWN)
            .withRandom(Type.values());

    public RabbitProperties()
    {
        registerSingle(VARIANT);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(VARIANT.id()))
        {
            var variant = typeMap.getOrDefault(value, null);

            if (variant != null)
                return Pair.of(VARIANT, variant);
        }

        return null;
    }
}
