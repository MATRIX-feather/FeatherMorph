package xyz.nifeather.morph.api.v0.events.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.DisguiseState;

public class PlayerMorphEarlyEvent extends PlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    public final DisguiseState state;

    public final String targetId;

    public final boolean isForce;

    /**
     * 会在玩家正式进行伪装或更换伪装前触发
     * @param who 玩家
     * @param state 玩家当前活动的{@link DisguiseState}，如果有
     * @param isForce 此操作是否为强制执行，若为true则无法取消
     */
    public PlayerMorphEarlyEvent(@NotNull Player who, @Nullable DisguiseState state, @NotNull String targetId, boolean isForce)
    {
        super(who);

        this.targetId = targetId;
        this.state = state;
        this.isForce = isForce;
    }

    public @NotNull String getTargetId()
    {
        return targetId;
    }

    public @Nullable DisguiseState getState()
    {
        return state;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList() { return handlers; }

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

