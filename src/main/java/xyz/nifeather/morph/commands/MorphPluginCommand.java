package xyz.nifeather.morph.commands;

import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.subcommands.plugin.*;
import xyz.nifeather.morph.messages.HelpStrings;

import java.util.List;

public class MorphPluginCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public String name()
    {
        return "feathermorph";
    }

    private final List<String> aliases = List.of("fm");

    @Override
    public boolean register(Commands dispatcher)
    {
        this.registerAs("fm", dispatcher);
        this.registerAs("feathermorph", dispatcher);

        return true;
    }

    private void registerAs(String name, Commands dispatcher)
    {
        var cmd = Commands.literal(name);

        for (IConvertibleBrigadier child : this.children)
            child.registerAsChild(cmd);

        dispatcher.register(cmd.build());
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.mmorphDescription();
    }

    private final List<IConvertibleBrigadier> children = ObjectList.of(
            new ReloadSubCommand(),
            new HelpSubCommand(),
            new ToggleSelfSubCommand(),
            new QuerySubCommand(),
            new QueryAllSubCommand(),
            new DisguiseManageSubCommand(),
            new OptionSubCommand(),
            new StatSubCommand(),
            new CheckUpdateSubCommand(),
            new LookupSubCommand(),
            new SkinCacheSubCommand(),
            new MakeSkillItemSubCommand()
    );

    @Override
    public @Unmodifiable List<? extends IHaveFormattableHelp> children()
    {
        return children;
    }

    private final List<FormattableMessage> notes = List.of();

    @Override
    public List<FormattableMessage> getNotes()
    {
        return notes;
    }
}
