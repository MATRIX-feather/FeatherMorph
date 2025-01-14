package xyz.nifeather.morph.abilities.options;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.storage.skill.ISkillOption;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AttributeModifyOption implements ISkillOption
{
    @Expose
    @SerializedName("modifiers")
    public List<AttributeInfo> modifiers = new ObjectArrayList<>();

    public boolean isValid()
    {
        return modifiers != null && modifiers.stream().allMatch(AttributeInfo::isValid);
    }

    public static AttributeModifyOption from(Attribute attribute, OperationType operationType, double value)
    {
        var info = new AttributeInfo();

        info.attributeName = attribute.key().asString();
        info.operationType = operationType;
        info.value = value;

        var instance = new AttributeModifyOption();
        instance.modifiers.add(info);

        return instance;
    }

    public AttributeModifyOption with(Attribute attribute, OperationType operationType, double value)
    {
        var info = new AttributeInfo();

        info.attributeName = attribute.key().asString();
        info.operationType = operationType;
        info.value = value;

        try
        {
            this.modifiers.add(info);
        }
        catch (Throwable ignored)
        {
            var list = new ObjectArrayList<>(modifiers);
            list.add(info);

            this.modifiers = list;
        }

        return this;
    }

    @Override
    public ISkillOption fromMap(@Nullable Map<String, Object> map)
    {
        var instance = new AttributeModifyOption();

        var modifiers = map == null ? null : tryGet(map, "modifiers", List.class);
        var defaultInfo = new AttributeInfo();

        if (modifiers != null)
        {
            modifiers.forEach(o ->
            {
                if (!(o instanceof Map<?, ?> optionMap)) return;

                var mmap = new Object2ObjectOpenHashMap<String, Object>();

                optionMap.forEach((k, v) ->
                {
                    if (!(k instanceof String kStr)) return;
                    mmap.put(kStr, v);
                });

                instance.modifiers.add((AttributeInfo) defaultInfo.fromMap(mmap));
            });
        }
        else
        {
            return null;
        }

        return instance;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;

        if (!(obj instanceof AttributeModifyOption option)) return false;

        return this.modifiers.equals(option.modifiers);
    }

    public enum OperationType
    {
        invalid,
        add,
        multiply,
        multiply_base;

        public AttributeModifier.Operation toBukkitOperation()
        {
            return switch (this)
            {
                case add -> AttributeModifier.Operation.ADD_NUMBER;
                case multiply -> AttributeModifier.Operation.ADD_SCALAR;
                case multiply_base -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                default -> null;
            };
        }
    }

    public static class AttributeInfo implements ISkillOption
    {
        @Expose
        @SerializedName("name")
        public String attributeName;

        @Expose
        @SerializedName("type")
        public OperationType operationType = OperationType.invalid;

        @Expose
        public double value = Double.NaN;

        public boolean isValid()
        {
            return !Double.isNaN(value)
                    && operationType != OperationType.invalid
                    && attributeName != null && !attributeName.isEmpty() && !attributeName.isBlank();
        }

        @Override
        public String toString()
        {
            return "AttributeInfo{name=%s, type=%s, value=%s}".formatted(attributeName, operationType, value);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;

            if (!(obj instanceof AttributeInfo info)) return false;

            return this.value == info.value
                    && Objects.equals(this.attributeName, info.attributeName)
                    && this.operationType == info.operationType;
        }
    }
}
