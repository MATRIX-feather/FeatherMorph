package xyz.nifeather.morph.api.v0.events.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.misc.DisguiseState;

public class PlayerExecuteSkillEvent extends PlayerEvent implements Cancellable
{
    public final DisguiseState state;

    public PlayerExecuteSkillEvent(@NotNull Player who, @NotNull DisguiseState state)
    {
        super(who);

        this.state = state;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    private boolean cancelled = false;

    /**
     * Sets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins.
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel)
    {
        this.cancelled = cancel;
    }

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
}
