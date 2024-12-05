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
import xyz.nifeather.morph.messages.CommonStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.MorphParameters;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.util.concurrent.CompletableFuture;

public class ForceMorphSubCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public @NotNull String name()
    {
        return "morph";
    }

    @Override
    public @Nullable String permission()
    {
        return CommonPermissions.MANAGE_MORPH_DISGUISE;
    }

    @Resolved
    private MorphManager manager;

    @Override
    public void registerAsChild(ArgumentBuilder<CommandSourceStack, ?> parentBuilder)
    {
        parentBuilder.then(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who", StringArgumentType.string())
                                        .suggests(this::suggestPlayer)
                                        .then(
                                                Commands.argument("as_what", StringArgumentType.greedyString())
                                                        .suggests(this::suggestDisguise)
                                                        .executes(this::execute)
                                        )
                        )
        );
    }

    private CompletableFuture<Suggestions> suggestDisguise(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            var input = suggestionsBuilder.getRemainingLowerCase();

            for (var p : MorphManager.getProviders())
            {
                if (p == MorphManager.fallbackProvider) continue;

                var ns = p.getNameSpace();
                p.getAllAvailableDisguises().forEach(s ->
                {
                    var str = ns + ":" + s;
                    if (str.toLowerCase().contains(input))
                        suggestionsBuilder.suggest(str);
                });
            }

            return suggestionsBuilder.build();
        });
    }

    private CompletableFuture<Suggestions> suggestPlayer(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var online = Bukkit.getOnlinePlayers();

        return CompletableFuture.supplyAsync(() ->
        {
            var input = suggestionsBuilder.getRemainingLowerCase();

            online.stream().forEach(player ->
            {
                var name = player.getName();
                if (name.toLowerCase().contains(input))
                    suggestionsBuilder.suggest(name);
            });

            return suggestionsBuilder.build();
        });
    }

    private int execute(CommandContext<CommandSourceStack> context)
    {
        var who = Bukkit.getPlayerExact(StringArgumentType.getString(context, "who"));
        var targetName = StringArgumentType.getString(context, "as_what");

        var commandSender = context.getSource().getSender();
        if (who == null || !who.isOnline())
        {
            commandSender.sendMessage(MessageUtils.prefixes(commandSender, CommonStrings.playerNotFoundString()));
            return 1;
        }

        var parameters = MorphParameters
                .create(who, targetName)
                .setSource(commandSender)
                //.setTargetedEntity(who.getTargetEntity(3))
                .setForceExecute(true)
                .setBypassAvailableCheck(true)
                .setBypassPermission(true);

        manager.morph(parameters);
        return 1;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.manageUnmorphDescription();
    }
}
