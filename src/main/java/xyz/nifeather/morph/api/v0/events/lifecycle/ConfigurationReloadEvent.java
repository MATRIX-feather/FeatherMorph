package xyz.nifeather.morph.api.v0.events.lifecycle;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigurationReloadEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public final boolean reloadConfigurations;
    public final boolean reloadLanguages;

    public ConfigurationReloadEvent(boolean reloadsConfigurations, boolean reloadsLanguages)
    {
        this.reloadConfigurations = reloadsConfigurations;
        this.reloadLanguages = reloadsLanguages;
    }
}
