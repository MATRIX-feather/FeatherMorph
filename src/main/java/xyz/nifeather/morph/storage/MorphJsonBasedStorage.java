package xyz.nifeather.morph.storage;

import xiamomc.pluginbase.storage.JsonBasedStorage;
import xyz.nifeather.morph.MorphPlugin;

public abstract class MorphJsonBasedStorage<T> extends xiamomc.pluginbase.storage.JsonBasedStorage<T, MorphPlugin>
{
    @Override
    protected String getPluginNamespace()
    {
        return MorphPlugin.getMorphNameSpace();
    }
}
