package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Registry;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.utilities.MathUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerProperties extends AbstractProperties
{
    private final Map<String, Villager.Type> typeMap = new ConcurrentHashMap<>();
    private final Map<String, Villager.Profession> professionMap = new ConcurrentHashMap<>();

    private void initMaps()
    {
        for (var type : Registry.VILLAGER_TYPE)
            typeMap.put(type.key().asString(), type);

        for (var profession : Registry.VILLAGER_PROFESSION)
            professionMap.put(profession.key().asString(), profession);
    }

    public final SingleProperty<Villager.Type> TYPE = getSingle("villager_type", Villager.Type.PLAINS)
            .withRandom(Registry.VILLAGER_TYPE.stream().toList());

    public final SingleProperty<Villager.Profession> PROFESSION = getSingle("villager_profession", Villager.Profession.NONE)
            .withRandom(Registry.VILLAGER_PROFESSION.stream().toList());

    public final SingleProperty<Integer> LEVEL = getSingle("villager_level", 1)
            .withRandom(1, 2, 3, 4, 5, 6);

    public VillagerProperties()
    {
        initMaps();
        TYPE.withValidInput(typeMap.keySet());
        PROFESSION.withValidInput(professionMap.keySet());
        LEVEL.withValidInput("1", "2", "3", "4", "5", "6");

        registerSingle(TYPE, PROFESSION, LEVEL);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        switch (key)
        {
            case "villager_type" ->
            {
                var type = typeMap.getOrDefault(value, null);

                if (type != null)
                    return Pair.of(TYPE, type);
            }

            case "villager_profession" ->
            {
                var profession = professionMap.getOrDefault(value, null);

                if (profession != null)
                    return Pair.of(PROFESSION, profession);
            }

            case "villager_level" ->
            {
                int level = 1;

                try
                {
                    level = Integer.parseInt(value);
                }
                catch (Throwable ignored)
                {
                }

                level = MathUtils.clamp(1, 6, level);

                return Pair.of(LEVEL, level);
            }
        }

        return null;
    }
}
