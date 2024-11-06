package xyz.nifeather.morph.api.v0.events.lifecycle;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.abilities.AbilityManager;

public class AbilitiesFinishedInitializeEvent extends Event
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

    public final AbilityManager manager;

    public AbilitiesFinishedInitializeEvent(AbilityManager instance)
    {
        this.manager = instance;
    }
}
