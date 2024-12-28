package xyz.nifeather.morph.storage;

import xyz.nifeather.morph.FeatherMorphMain;

public abstract class MorphJsonBasedStorage<T> extends xiamomc.pluginbase.storage.JsonBasedStorage<T, FeatherMorphMain>
{
    @Override
    protected String getPluginNamespace()
    {
        return FeatherMorphMain.getMorphNameSpace();
    }
}
