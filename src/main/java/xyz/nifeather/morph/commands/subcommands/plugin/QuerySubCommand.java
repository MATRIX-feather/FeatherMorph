package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.minecraft.network.chat.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.commands.brigadier.BrigadierCommand;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.concurrent.CompletableFuture;

public class QuerySubCommand extends BrigadierCommand
{
    @Override
    public String name()
    {
        return "query";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.queryDescription();
    }

    @Override
    public String getPermissionRequirement()
    {
        return CommonPermissions.QUERY_STATES;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", ArgumentTypes.player())
                                        .executes(this::executes)
                        )
        );

        super.registerAsChild(parentBuilder);
    }

    @Resolved
    private MorphManager manager;

    public int executes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var players = context.getArgument("who", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());

        String locale = null;

        var commandSender = context.getSource().getSender();

        if (commandSender instanceof Player player)
            locale = MessageUtils.getLocale(player);

        if (players.isEmpty())
        {
            return 0;
        }

        var targetPlayer = players.getFirst();

        var state = manager.getDisguiseStateFor(targetPlayer);

        if (state != null)
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender,
                    CommandStrings.qDisguisedString()
                            .withLocale(locale)
                            .resolve("who", targetPlayer.getName())
                            .resolve("what", state.getDisguiseIdentifier())
                            .resolve("storage_status",
                                    state.showingDisguisedItems()
                                            ? CommandStrings.qaShowingDisguisedItemsString()
                                            : CommandStrings.qaNotShowingDisguisedItemsString(),
                                    null)
            ));
        }
        else
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender,
                    CommandStrings.qNotDisguisedString().resolve("who", targetPlayer.getName())));
        }

        return 1;
    }
}
