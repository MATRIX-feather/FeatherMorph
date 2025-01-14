package xyz.nifeather.morph.commands.brigadier;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.MorphPluginObject;

public abstract class BrigadierCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    public abstract String getPermissionRequirement();

    @Override
    public @Nullable final String permission()
    {
        return getPermissionRequirement();
    }

    @Override
    public boolean checkPermission(CommandSourceStack cmdSourceStack)
    {
        var perm = this.getPermissionRequirement();
        return perm == null || cmdSourceStack.getSender().hasPermission(perm);
    }
}
