package xyz.nifeather.morph.commands.subcommands.plugin.management;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.CommonStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.concurrent.CompletableFuture;

public class ForceUnmorphSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "unmorph";
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_UNMORPH_DISGUISE;
    }

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", StringArgumentType.greedyString())
                                        .suggests(this::suggestPlayer)
                                        .executes(this::execute)
                        )
        );
    }

    private int execute(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getSender();
        var who = StringArgumentType.getString(context, "who");

        if (who.equals("*"))
        {
            manager.unMorphAll(true);
            sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.unMorphedAllString()));

            return 1;
        }

        var player = Bukkit.getPlayerExact(who);
        if (player == null)
        {
            sender.sendMessage(MessageUtils.prefixes(sender, CommonStrings.playerNotFoundString()));

            return 1;
        }

        manager.unMorph(sender, player, true, true);

        sender.sendMessage(MessageUtils.prefixes(sender, CommandStrings.unMorphedSomeoneString()
                .resolve("who", player.getName())));

        return 1;
    }

    private CompletableFuture<Suggestions> suggestPlayer(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var online = Bukkit.getOnlinePlayers();

        return CompletableFuture.supplyAsync(() ->
        {
            var input = suggestionsBuilder.getRemainingLowerCase();

            suggestionsBuilder.suggest("*");

            online.stream().forEach(player ->
            {
                var name = player.getName();
                if (name.toLowerCase().contains(input))
                    suggestionsBuilder.suggest(name);
            });

            return suggestionsBuilder.build();
        });
    }

    @Resolved
    private MorphManager manager;

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageUnmorphDescription();
    }
}
