package xyz.nifeather.morph.api.v0.events.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.misc.DisguiseState;

public class PlayerUnMorphEarlyEvent extends PlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * 会在取消伪装的早期处理过程中触发，此时玩家尚未正式取消伪装
     * @param who 玩家
     * @param isForceUnmorph 此操作是否为强制执行，若为true则无法取消
     */
    public PlayerUnMorphEarlyEvent(@NotNull Player who, @NotNull DisguiseState state, boolean isForceUnmorph)
    {
        super(who);

        this.state = state;
        this.isForceUnmorph = isForceUnmorph;
    }

    private final boolean isForceUnmorph;

    private final DisguiseState state;

    public DisguiseState getState()
    {
        return state;
    }

    public boolean isForceUnmorph()
    {
        return isForceUnmorph;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    private boolean cancelled = false;

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
}
