package xyz.nifeather.morph.misc.disguiseProperty;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.misc.disguiseProperty.values.AbstractProperties;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PropertyHandler
{
    private final Map<SingleProperty<?>, Object> propertyMap = new ConcurrentHashMap<>();
    private final List<SingleProperty<?>> validProperties = new ObjectArrayList<>();

    private final Random random = ThreadLocalRandom.current();

    @Nullable
    private AbstractProperties properties;

    public void setProperties(AbstractProperties properties)
    {
        reset();

        this.properties = properties;
        validProperties.addAll(properties.getValues());
        properties.getValues().forEach(this::addProperty);
    }

    private void addProperty(SingleProperty<?> property)
    {
        var random = property.getRandomValues();
        if (!random.isEmpty())
        {
            var index = this.random.nextInt(random.size());
            this.writeGeneric(property, random.get(index));
        }
    }

    public void reset()
    {
        this.validProperties.clear();
        this.properties = null;
        propertyMap.clear();
    }

    private void writeGeneric(SingleProperty<?> property, Object value)
    {
        if (!property.defaultVal().getClass().isInstance(value))
            throw new IllegalArgumentException("Incompatible value for id '%s', excepted for '%s', but got '%s'".formatted(property.id(), property.defaultVal().getClass(), value.getClass()));

        set((SingleProperty<Object>)property, value);
    }

    public <X> void set(SingleProperty<X> property, X value)
    {
        if (!validProperties.contains(property))
        {
            FeatherMorphMain.getInstance().getSLF4JLogger().warn("The given property '%s' doesn't exist in '%s'".formatted(property.id(), this.properties));
            return;
        }

        propertyMap.put(property, value);
    }

    @NotNull
    public <X> X get(SingleProperty<X> property)
    {
        return this.getOr(property, property.defaultVal());
    }

    @Nullable
    @Contract("_, null -> null; _, !null -> !null")
    public <X> X getOr(SingleProperty<X> property, X defaultVal)
    {
        return (X) propertyMap.getOrDefault(property, defaultVal);
    }

    public Map<SingleProperty<?>, ?> getAll()
    {
        return new Object2ObjectArrayMap<>(propertyMap);
    }
}
