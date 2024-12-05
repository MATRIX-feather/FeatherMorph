package xyz.nifeather.morph.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.misc.DisguiseMeta;
import xyz.nifeather.morph.misc.DisguiseTypes;

import java.util.concurrent.CompletableFuture;

public class MorphPlayerCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Resolved
    private MorphManager morphManager;

    @Override
    public String name()
    {
        return "morphplayer";
    }

    @Override
    public boolean register(Commands dispatcher)
    {
        dispatcher.register(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .then(
                                Commands.argument("who",  StringArgumentType.greedyString())
                                        .executes(this::executes)
                                        .suggests(this::suggests)
                        )
                        .build()
        );

        return true;
    }

    public int executes(CommandContext<CommandSourceStack> context)
    {
        if (!(context.getSource().getExecutor() instanceof Player player))
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;

        var targetName = StringArgumentType.getString(context, "name");

        player.performCommand("morph" + " " + DisguiseTypes.PLAYER.toId(targetName));

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        if (!(context.getSource().getExecutor() instanceof Player player))
            return CompletableFuture.completedFuture(suggestionsBuilder.build());

        var input = suggestionsBuilder.getRemainingLowerCase();

        var availableDisguises = morphManager.getAvaliableDisguisesFor(player);

        return CompletableFuture.supplyAsync(() ->
        {
            var infoStream = availableDisguises.stream().filter(DisguiseMeta::isPlayerDisguise);

            infoStream.forEach(info ->
            {
                if (info.playerDisguiseTargetName.toLowerCase().contains(input))
                    suggestionsBuilder.suggest(info.playerDisguiseTargetName);
            });

            return suggestionsBuilder.build();
        });
    }

    @Override
    public boolean checkPermission(CommandSourceStack cmdSourceStack)
    {
        return true;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.morphPlayerDescription();
    }
}
