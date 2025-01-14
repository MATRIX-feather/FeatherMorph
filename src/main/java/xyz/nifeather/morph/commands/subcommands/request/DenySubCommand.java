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

public class DenySubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
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

    public int executes(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();

        if (!(sender instanceof Player sourcePlayer))
            return Command.SINGLE_SUCCESS;

        var targetPlayer = Bukkit.getPlayerExact(StringArgumentType.getString(context, "who"));

        if (targetPlayer == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommonStrings.playerNotFoundString()));
            return Command.SINGLE_SUCCESS;
        }

        requests.denyRequest(sourcePlayer, targetPlayer);

        return Command.SINGLE_SUCCESS;
    }

    @Override
    public String name()
    {
        return "deny";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.requestDenyDescription();
    }
}
