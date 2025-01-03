package xyz.nifeather.morph.misc.disguiseProperty;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SingleProperty<T>
{
    private final String identifier;
    private final T defaultVal;
    private final Class<T> type;

    public String id()
    {
        return identifier;
    }

    public T defaultVal()
    {
        return defaultVal;
    }

    public Class<T> type()
    {
        return type;
    }

    public SingleProperty(String identifier, T defaultValue, Class<T> type)
    {
        this.identifier = identifier;
        this.defaultVal = defaultValue;
        this.type = type;
    }

    private final List<String> validValues = new ObjectArrayList<>();

    public List<String> validInputs()
    {
        return new ObjectArrayList<>(validValues);
    }

    public SingleProperty<T> withValidInput(Collection<String> input)
    {
        this.validValues.addAll(input);

        return this;
    }
    public SingleProperty<T> withValidInput(String... input)
    {
        this.validValues.addAll(Arrays.stream(input).toList());

        return this;
    }

    private final List<T> randomValues = new ObjectArrayList<>();

    public List<T> getRandomValues()
    {
        return new ObjectArrayList<>(randomValues);
    }

    public SingleProperty<T> withRandom(List<T> values)
    {
        this.randomValues.clear();
        this.randomValues.addAll(values);

        return this;
    }

    public SingleProperty<T> withRandom(T... randomValues)
    {
        return withRandom(Arrays.stream(randomValues).toList());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof SingleProperty<?> other)) return false;

        return this.identifier.equals(other.identifier) && this.type.equals(other.type);
    }

    public static <T> SingleProperty<T> of(String id, T val)
    {
        return new SingleProperty<>(id, val, (Class<T>) val.getClass());
    }
}
