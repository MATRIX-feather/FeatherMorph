package xyz.nifeather.morph.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.help.FormattableHelpContainer;
import xyz.nifeather.morph.commands.subcommands.request.AcceptSubCommand;
import xyz.nifeather.morph.commands.subcommands.request.DenySubCommand;
import xyz.nifeather.morph.commands.subcommands.request.SendSubCommand;
import xyz.nifeather.morph.messages.HelpStrings;

import java.util.List;

public class RequestCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    private final List<IHaveFormattableHelp> children = List.of(
            new FormattableHelpContainer("send", HelpStrings.requestSendDescription()),
            new FormattableHelpContainer("accept", HelpStrings.requestAcceptDescription()),
            new FormattableHelpContainer("deny", HelpStrings.requestDenyDescription())
    );

    @Override
    public @Unmodifiable List<IHaveFormattableHelp> children()
    {
        return children;
    }

    @Override
    public boolean register(Commands dispatcher)
    {
        var sendCommand = new SendSubCommand();
        var acceptSubCommand = new AcceptSubCommand();
        var denySubCommand = new DenySubCommand();

        dispatcher.register(
                Commands.literal("request")
                        .requires(this::checkPermission)
                        .then(
                                Commands.literal("send")
                                        .then(
                                                Commands.argument("who", StringArgumentType.greedyString())
                                                        .suggests(sendCommand::suggests)
                                                        .executes(sendCommand::executes)
                                        )
                        )
                        .then(
                                Commands.literal("accept")
                                        .then(
                                                Commands.argument("who", StringArgumentType.greedyString())
                                                        .suggests(acceptSubCommand::suggests)
                                                        .executes(acceptSubCommand::executes)
                                        )
                        )
                        .then(
                                Commands.literal("deny")
                                        .then(
                                                Commands.argument("who", StringArgumentType.greedyString())
                                                        .suggests(denySubCommand::suggests)
                                                        .executes(denySubCommand::executes)
                                        )
                        )
                        .build()
        );

        return true;
    }

    private final List<FormattableMessage> notes = ObjectList.of(
            HelpStrings.requestDescriptionSpecialNote()
    );

    @Override
    public List<FormattableMessage> getNotes()
    {
        return notes;
    }

    @Override
    public String name()
    {
        return "request";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.requestDescription();
    }
}
