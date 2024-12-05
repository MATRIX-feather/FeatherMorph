package xyz.nifeather.morph.commands.subcommands.plugin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
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
                                Commands.argument("who", StringArgumentType.greedyString())
                                        .suggests(this::suggests)
                                        .executes(this::executes)
                        )
        );

        super.registerAsChild(parentBuilder);
    }

    public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            var name = suggestionsBuilder.getRemainingLowerCase();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers())
            {
                var playerName = onlinePlayer.getName();
                if (playerName.toLowerCase().startsWith(name.toLowerCase()))
                    suggestionsBuilder.suggest(playerName);
            }

            return suggestionsBuilder.build();
        });
    }

    @Resolved
    private MorphManager manager;

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var targetPlayer = Bukkit.getPlayerExact(StringArgumentType.getString(context, "who"));
        String locale = null;

        var commandSender = context.getSource().getSender();

        if (commandSender instanceof Player player)
            locale = MessageUtils.getLocale(player);

        if (targetPlayer != null)
        {
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
        }

        return 1;
    }
}
