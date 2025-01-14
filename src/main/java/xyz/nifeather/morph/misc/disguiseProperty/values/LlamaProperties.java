package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Llama.Color;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LlamaProperties extends AbstractProperties
{
    private final Map<String, Color> colorMap = new ConcurrentHashMap<>();

    private void initMaps()
    {
        for (var value : Color.values())
            colorMap.put(value.name().toLowerCase(), value);
    }

    public final SingleProperty<Llama.Color> COLOR = getSingle("llama_color", Color.CREAMY)
            .withRandom(Color.values());

    public LlamaProperties()
    {
        initMaps();
        COLOR.withValidInput(colorMap.keySet());

        registerSingle(COLOR);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        if (key.equals(COLOR.id()))
        {
            var color = colorMap.getOrDefault(value, null);

            if (color != null)
                return Pair.of(COLOR, color);
        }

        return null;
    }
}
