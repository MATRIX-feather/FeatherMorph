package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

public class QueryAllSubCommand extends BrigadierCommand
{
    @Override
    public String name()
    {
        return "queryall";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.queryAllDescription();
    }

    @Override
    public String getPermissionRequirement()
    {
        return CommonPermissions.QUERY_STATES;
    }

    @Resolved
    private MorphManager manager;

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .executes(this::executes)
        );

        super.registerAsChild(parentBuilder);
    }

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var list = manager.getActiveDisguises();
        var offlineStates = manager.getAvaliableOfflineStates();

        var commandSender = context.getSource().getSender();

        if (list.size() == 0 && offlineStates.size() == 0)
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender, CommandStrings.qaNoBodyDisguisingString()));
            return 1;
        }

        var msg = CommandStrings.qaDisguisedString();
        var locale = MessageUtils.getLocale(commandSender);

        for (var i : list)
        {
            var player = i.getPlayer();
            msg.withLocale(locale)
                    .resolve("who", player.getName())
                    .resolve("status", player.isOnline()
                            ? CommandStrings.qaOnlineString()
                            : CommandStrings.qaOfflineString(), null)
                    .resolve("what", i.getDisguiseIdentifier())
                    .resolve("storage_status", i.showingDisguisedItems()
                            ? CommandStrings.qaShowingDisguisedItemsString()
                            : CommandStrings.qaNotShowingDisguisedItemsString(), null);

            commandSender.sendMessage(MessageUtils.prefixes(commandSender, msg));
        }

        for (var s : offlineStates)
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender,
                    msg.withLocale(locale)
                            .resolve("who", s.playerName)
                            .resolve("status", CommandStrings.qaIsOfflineStoreString(), null)
                            .resolve("storage_status", "")
                            .resolve("what", s.disguiseID)));
        }

        return 1;
    }
}
