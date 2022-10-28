package xiamomc.morph.commands.subcommands.plugin;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import xiamomc.morph.MorphPluginObject;
import xiamomc.morph.commands.subcommands.plugin.chatoverride.QuerySubCommand;
import xiamomc.morph.commands.subcommands.plugin.chatoverride.ToggleSubCommand;
import xiamomc.morph.messages.HelpStrings;
import xiamomc.pluginbase.Command.ISubCommand;
import xiamomc.pluginbase.messages.FormattableMessage;

import java.util.List;

public class ChatOverrideSubCommand extends MorphPluginObject implements ISubCommand
{
    @Override
    public @NotNull String getCommandName()
    {
        return "chatoverride";
    }

    private final List<ISubCommand> subCommands = ObjectList.of(
            new QuerySubCommand(),
            new ToggleSubCommand()
    );

    @Override
    public String getPermissionRequirement()
    {
        return "xiamomc.morph.chatoverride";
    }

    @Override
    public List<ISubCommand> getSubCommands()
    {
        return subCommands;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.chatOverrideDescription();
    }
}
