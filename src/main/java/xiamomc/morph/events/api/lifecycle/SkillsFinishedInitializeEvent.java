package xiamomc.morph.events.api.lifecycle;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.skills.MorphSkillHandler;

public class SkillsFinishedInitializeEvent extends Event
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

    public final MorphSkillHandler manager;

    public SkillsFinishedInitializeEvent(MorphSkillHandler instance)
    {
        this.manager = instance;
    }
}
