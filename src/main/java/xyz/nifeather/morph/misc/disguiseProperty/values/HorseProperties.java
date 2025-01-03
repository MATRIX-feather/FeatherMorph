package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Horse;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HorseProperties extends AbstractProperties
{
    private final Map<String, Horse.Color> colorMap = new ConcurrentHashMap<>();
    private final Map<String, Horse.Style> styleMap = new ConcurrentHashMap<>();

    private void initMaps()
    {
        for (var color : Horse.Color.values())
            colorMap.put(color.name().toLowerCase(), color);

        for (var style : Horse.Style.values())
            styleMap.put(style.name().toLowerCase(), style);
    }

    public final SingleProperty<Horse.Color> COLOR = getSingle("horse_color", Horse.Color.WHITE)
            .withRandom(Horse.Color.values());

    public final SingleProperty<Horse.Style> STYLE = getSingle("horse_style", Horse.Style.NONE)
            .withRandom(Horse.Style.values());

    public HorseProperties()
    {
        initMaps();

        COLOR.withValidInput(colorMap.keySet());
        STYLE.withValidInput(styleMap.keySet());

        registerSingle(COLOR, STYLE);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        switch (key)
        {
            case "horse_color" ->
            {
                var color = colorMap.getOrDefault(value, null);

                if (color != null)
                    return Pair.of(COLOR, color);
            }

            case "horse_style" ->
            {
                var style = styleMap.getOrDefault(value, null);

                if (style != null)
                    return Pair.of(STYLE, style);
            }
        }

        return null;
    }
}
