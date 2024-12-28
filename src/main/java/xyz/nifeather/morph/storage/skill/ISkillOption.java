package xyz.nifeather.morph.storage.skill;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.FeatherMorphMain;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * 技能设置
 */
public interface ISkillOption
{
    /**
     * 检查此Option是否合法
     * @return 此Option是否合法
     */
    boolean isValid();

    /**
     * 将此技能设置转换为Map用于储存
     *
     * @return 一个Map
     * @apiNote Map中的所有对象都必须可序列化为JSON
     */
    default Map<String, Object> toMap()
    {
        var map = new Object2ObjectOpenHashMap<String, Object>();
        var fields = Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Expose.class))
                .toList();

        fields.forEach(f ->
        {
            var name = f.getName();

            if (f.isAnnotationPresent(SerializedName.class))
                name = f.getAnnotation(SerializedName.class).value();

            if (f.getType() == byte.class)
                throw new IllegalArgumentException("Don't use byte to store options");

            try
            {
                f.setAccessible(true);
                map.put(name, f.get(this));
            }
            catch (Throwable t)
            {
                var logger = FeatherMorphMain.getInstance().getSLF4JLogger();
                logger.warn("Can't serialize option %s to map: %s".formatted(this, t.getMessage()));
                t.printStackTrace();
            }
        });

        return map;
    }

    /**
     * 从Map创建一个新实例
     *
     * @param map Map
     * @return 实例
     */
    @Nullable
    default ISkillOption fromMap(@Nullable Map<String, Object> map)
    {
        try
        {
            var instance = this.getClass().getDeclaredConstructor().newInstance();
            if (map == null) return instance;

            var fields = Arrays.stream(this.getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Expose.class))
                    .toList();

            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            map.forEach((n, v) ->
            {
                var field = fields.stream()
                        .filter(f -> f.getName().equals(n)).findFirst().orElse(null);

                if (field == null)
                    field = fields.stream()
                            .filter(f -> f.isAnnotationPresent(SerializedName.class) && f.getAnnotation(SerializedName.class).value().equals(n))
                            .findFirst().orElse(null);

                if (field == null)
                {
                    logger.warn("No such field %s in %s".formatted(n, instance));
                    return;
                }

                if (v != null)
                {
                    var targetClass = field.getType();

                    if (v.getClass() != targetClass)
                    {
                        try
                        {
                            Object valueCasted = null;

                            if (targetClass.isEnum())
                            {
                                var enumValue = Arrays.stream(targetClass.getEnumConstants())
                                        .filter(e -> e.toString().equals(v))
                                        .findFirst().orElse(null);

                                if (enumValue == null)
                                    throw new RuntimeException("No such enum %s for class %s".formatted(v, targetClass));

                                valueCasted = enumValue;
                            }
                            else
                            {
                                if (v instanceof Number number)
                                {
                                    if (targetClass == double.class)
                                        valueCasted = number.doubleValue();
                                    else if (targetClass == int.class)
                                        valueCasted = number.intValue();
                                    else if (targetClass == long.class)
                                        valueCasted = number.longValue();
                                    else if (targetClass == short.class)
                                        valueCasted = number.shortValue();
                                    else if (targetClass == float.class)
                                        valueCasted = number.floatValue();
                                }
                                else if (v instanceof Boolean booleanValue)
                                {
                                    valueCasted = booleanValue;
                                }
                                else
                                {
                                    valueCasted = targetClass.cast(v);
                                }
                            }

                            field.setAccessible(true);
                            field.set(instance, valueCasted);
                        }
                        catch (Throwable t)
                        {
                            logger.warn("Can't cast value %s to type %s: %s".formatted(v, targetClass, t.getMessage()));
                            t.printStackTrace();
                        }
                    }
                    else
                    {
                        try
                        {
                            field.setAccessible(true);
                            field.set(instance, v);
                        }
                        catch (IllegalAccessException e)
                        {
                            logger.warn("Can't set value for %s: %s".formatted(field, e.getMessage()));
                            e.printStackTrace();
                        }
                    }
                }
            });

            return instance;
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();
            logger.warn("Can't deserialize option %s from map: %s".formatted(this, t.getMessage()));
            t.printStackTrace();
        }

        throw new RuntimeException("Can't initialize option for %s".formatted(this));
    }

    /**
     * 获取技能设置的默认值
     *
     * @return 默认值
     */
    default Map<String, Object> getDefault()
    {
        return new Object2ObjectOpenHashMap<>();
    }

    /**
     * 尝试从map中获取值
     *
     * @param map {@link Map}
     * @param key 目标键
     * @param defaultValue 默认值
     *
     * @return 目标值
     * @param <T> 值的类型
     */
    default <T> T tryGet(Map<String, Object> map, String key, T defaultValue)
    {
        return tryGet(map, key, defaultValue, o -> (T) defaultValue.getClass().cast(o));
    }

    default <T> T tryGet(@NotNull Map<String, Object> map, String key, Class<T> tClass)
    {
        return tryGet(map, key, null, tClass::cast);
    }

    /**
     * 尝试从map中获取值
     *
     * @param map {@link Map}
     * @param key 目标键
     * @param defaultValue 默认值
     * @param castMethod 转换方法
     *
     * @return 目标值
     * @param <T> 值的类型
     */
    default <T> T tryGet(@NotNull Map<String, Object> map, String key, T defaultValue, Function<Object, T> castMethod)
    {
        var val = map.get(key);

        if (val == null) return defaultValue;

        T value;

        try
        {
            value = castMethod.apply(val);
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();
            logger.warn("Unable to parse key '%s': %s".formatted(key, t.getMessage()));
            t.printStackTrace();

            value = null;
        }

        return value == null ? defaultValue : value;
    }

    default int tryGetInt(Map<String, Object> map, String key, int defaultValue)
    {
        return tryGet(map, key, defaultValue, o -> Double.valueOf("" + o).intValue());
    }

    default float tryGetFloat(Map<String, Object> map, String key, float defaultValue)
    {
        return tryGet(map, key, defaultValue, o -> Float.valueOf("" + o));
    }
}
