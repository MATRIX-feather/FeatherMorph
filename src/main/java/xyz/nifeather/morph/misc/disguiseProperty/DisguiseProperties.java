package xyz.nifeather.morph.misc.disguiseProperty;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.misc.disguiseProperty.values.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseProperties
{
    public static final DisguiseProperties INSTANCE = new DisguiseProperties();
    private static final Logger log = LoggerFactory.getLogger(DisguiseProperties.class);

    private final Map<EntityType, AbstractProperties> handlerMap = new ConcurrentHashMap<>();

    public DisguiseProperties()
    {
        register(EntityType.FROG, new FrogProperties());
        register(EntityType.CAT, new CatProperties());
        register(EntityType.AXOLOTL, new AxolotlProperties());
        register(EntityType.FOX, new FoxProperties());
        register(EntityType.GOAT, new GoatProperties());
        register(EntityType.MOOSHROOM, new MooshroomProperties());
        register(EntityType.PARROT, new ParrotProperties());
        register(EntityType.RABBIT, new RabbitProperties());
        register(EntityType.WOLF, new WolfProperties());
        register(EntityType.LLAMA, new LlamaProperties());
        register(EntityType.HORSE, new HorseProperties());
        register(EntityType.PANDA, new PandaProperties());
        register(EntityType.VILLAGER, new VillagerProperties());
        register(EntityType.ZOMBIE_VILLAGER, new VillagerProperties());
        register(EntityType.ARMOR_STAND, new ArmorStandProperties());
        register(EntityType.CREEPER, new CreeperProperties());
    }

    public Map<EntityType, AbstractProperties> getAll()
    {
        return new Object2ObjectOpenHashMap<>(handlerMap);
    }

    public void register(EntityType type, AbstractProperties properties)
    {
        if (handlerMap.containsKey(type))
            throw new IllegalArgumentException("Already contains properties setup for type " + type);

        handlerMap.put(type, properties);
    }

    private static final AbstractProperties defaultProperties = new DefaultProperties();

    public <X> X getOrThrow(Class<X> expectedClass)
    {
        var find = handlerMap.values().stream().filter(expectedClass::isInstance)
                .findFirst()
                .orElse(null);

        if (find == null)
            throw new NullDependencyException("Can't find '%s' in registered properties.".formatted(expectedClass));

        return (X) find;
    }

    @NotNull
    public <X> X getOrThrow(EntityType type, Class<X> expetedClass)
    {
        var raw = get(type);

        if (!expetedClass.isInstance(raw))
            throw new NullDependencyException("Can't get disguise properties for type '%s', expected '%s' but got '%s'".formatted(type, expetedClass, raw.getClass()));

        return (X) raw;
    }

    @NotNull
    public AbstractProperties get(EntityType type)
    {
        return handlerMap.getOrDefault(type, defaultProperties);
    }
}
