package xyz.nifeather.morph.commands.subcommands.request;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.interfaces.IManageRequests;
import xyz.nifeather.morph.messages.CommonStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;

import java.util.concurrent.CompletableFuture;

public class AcceptSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.requestAcceptDescription();
    }

    @Resolved
    private IManageRequests requests;

    public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var source = context.getSource().getSender();

        if (!(source instanceof Player player))
            return CompletableFuture.completedFuture(suggestionsBuilder.build());

        var reqs = requests.getAvailableRequestsFor(player);

        return CompletableFuture.supplyAsync(() ->
        {
            reqs.forEach(r -> suggestionsBuilder.suggest(r.sourcePlayer.getName()));
            return suggestionsBuilder.build();
        });
    }

    @Override
    public String name()
    {
        return "accept";
    }

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        if (!(sender instanceof Player sourcePlayer))
            return Command.SINGLE_SUCCESS;

        var playerName = StringArgumentType.getString(context, "who");

        var targetPlayer = Bukkit.getPlayerExact(playerName);

        if (targetPlayer == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommonStrings.playerNotFoundString()));
            return Command.SINGLE_SUCCESS;
        }

        requests.acceptRequest(sourcePlayer, targetPlayer);

        return Command.SINGLE_SUCCESS;
    }
}
