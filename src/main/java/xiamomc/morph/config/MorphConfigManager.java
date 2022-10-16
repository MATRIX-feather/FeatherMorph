package xiamomc.morph.config;

import org.jetbrains.annotations.Nullable;
import xiamomc.morph.MorphPlugin;
import xiamomc.pluginbase.Configuration.PluginConfigManager;

public class MorphConfigManager extends PluginConfigManager
{
    public MorphConfigManager(MorphPlugin plugin)
    {
        super(plugin);

        instance = this;
    }

    private static MorphConfigManager instance;

    public static MorphConfigManager getInstance()
    {
        return instance;
    }

    public <T> T getOrDefault(Class<T> type, ConfigOption option, @Nullable T defaultValue)
    {
        var val = get(type, option);

        if (val == null)
        {
            set(option, defaultValue);
            return defaultValue;
        }

        return val;
    }

    public <T> T getOrDefault(Class<T> type, ConfigOption option)
    {
        var val = get(type, option);

        if (val == null)
        {
            set(option, option.defaultValue);
            return (T) option.defaultValue;
        }

        return val;
    }

    public <T> T get(Class<T> type, ConfigOption option)
    {
        return get(type, option.node);
    }

    public void set(ConfigOption option, Object val)
    {
        this.set(option.node, val);
    }
}