package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.List;

public abstract class AbstractProperties
{
    protected <X> SingleProperty<X> getSingle(String name, X val)
    {
        if (val == null)
            throw new IllegalArgumentException("May not pass a null value to getSingle()");

        return SingleProperty.of(name, val);
    }

    protected final Logger logger = FeatherMorphMain.getInstance().getSLF4JLogger();

    protected final List<SingleProperty<?>> values = new ObjectArrayList<>();

    protected void registerSingle(SingleProperty<?>... value)
    {
        for (SingleProperty<?> property : value)
            registerSingle(property);
    }

    protected void registerSingle(SingleProperty<?> value)
    {
        var duplicateValue = values.stream().filter(p -> p.id().equals(value.id())).findFirst().orElse(null);
        if (duplicateValue != null)
            throw new IllegalArgumentException("Already contains a value with ID '%s'".formatted(value.id()));

        values.add(value);
    }

    public List<SingleProperty<?>> getValues()
    {
        return new ObjectArrayList<>(values);
    }
}
