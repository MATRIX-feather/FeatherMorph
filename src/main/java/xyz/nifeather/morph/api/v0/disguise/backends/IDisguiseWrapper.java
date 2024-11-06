package xyz.nifeather.morph.api.v0.disguise.backends;

public interface IDisguiseWrapper<TInstance>
{
    /**
     * @return The underlying disguise instance
     */
    TInstance getInstance();

    IDisguiseBackend<TInstance, ? extends IDisguiseWrapper<TInstance>> getBackend();
}
