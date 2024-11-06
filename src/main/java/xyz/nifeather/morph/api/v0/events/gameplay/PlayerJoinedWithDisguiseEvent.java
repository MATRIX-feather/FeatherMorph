package xyz.nifeather.morph.api.v0.events.gameplay;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.misc.DisguiseState;

public class PlayerJoinedWithDisguiseEvent extends PlayerEvent
{
    private static final HandlerList handlers = new HandlerList();

    public final DisguiseState state;

    /**
     * 会在带有 {@link DisguiseState} 的玩家加入时触发
     *
     * @param who 玩家
     * @param state 玩家的{@link DisguiseState}
     */
    public PlayerJoinedWithDisguiseEvent(@NotNull Player who, @NotNull DisguiseState state)
    {
        super(who);

        this.state = state;
    }

    public @NotNull DisguiseState getState()
    {
        return state;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList() { return handlers; }
}
