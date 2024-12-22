package xyz.nifeather.morph.storage;

import xiamomc.pluginbase.storage.JsonBasedStorage;
import xyz.nifeather.morph.MorphPlugin;

public abstract class MorphJsonBasedStorage<T> extends JsonBasedStorage<T, MorphPlugin>
{
    @Override
    protected String getPluginNamespace()
    {
        return MorphPlugin.getMorphNameSpace();
    }
}
