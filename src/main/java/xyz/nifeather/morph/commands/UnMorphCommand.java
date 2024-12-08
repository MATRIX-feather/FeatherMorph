package xyz.nifeather.morph.commands;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.HelpStrings;

public class UnMorphCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public String name()
    {
        return "unmorph";
    }

    @Override
    public boolean register(Commands dispatcher)
    {
        dispatcher.register(
                Commands.literal("unmorph")
                        .executes(this::executes)
                        .build()
        );

        return true;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.unMorphDescription();
    }

    @Resolved
    private MorphManager morphs;

    public int executes(CommandContext<CommandSourceStack> context)
    {
        if (!(context.getSource().getExecutor() instanceof Player player))
            return 1;

        morphs.unMorph(player);
        return 1;
    }
}
