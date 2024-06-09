package xiamomc.morph.misc;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 用于传递给morph方法的杂项参数
 */
public class MorphParameters
{
    public boolean bypassPermission = false;
    public boolean bypassAvailableCheck = false;
    public boolean forceExecute = false;

    public final Player targetPlayer;
    private String targetDisguiseIdentifier;

    public String targetDisguiseIdentifier()
    {
        return targetDisguiseIdentifier;
    }

    public void setDisguiseIdentifier(String id)
    {
        targetDisguiseIdentifier = id;
    }

    @Nullable
    public CommandSender commandSource;

    @Nullable
    public Entity targetedEntity;

    public MorphParameters setSource(CommandSender sender)
    {
        this.commandSource = sender;

        return this;
    }

    public MorphParameters setTargetedEntity(Entity entity)
    {
        this.targetedEntity = entity;

        return this;
    }

    public MorphParameters setBypassPermission(boolean val)
    {
        this.bypassPermission = val;

        return this;
    }

    public MorphParameters setBypassAvailableCheck(boolean val)
    {
        this.bypassAvailableCheck = val;

        return this;
    }

    public MorphParameters setForceExecute(boolean val)
    {
        this.forceExecute = val;

        return this;
    }

    private MorphParameters(Player targetPlayer, String disguiseID)
    {
        this.targetPlayer = targetPlayer;
        this.targetDisguiseIdentifier = disguiseID;
    }

    public static MorphParameters create(Player targetPlayer, String disguiseIdentifier)
    {
        return new MorphParameters(targetPlayer, disguiseIdentifier);
    }
}
