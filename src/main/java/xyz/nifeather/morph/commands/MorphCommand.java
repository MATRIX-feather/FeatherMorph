package xyz.nifeather.morph.commands;

import com.mojang.brigadier.Command;
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
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.misc.DisguiseMeta;
import xyz.nifeather.morph.misc.gui.DisguiseSelectScreenWrapper;

import java.util.concurrent.CompletableFuture;

public class MorphCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Resolved
    private MorphManager morphManager;

    @Override
    public @NotNull String name()
    {
        return "morph";
    }

    @Resolved
    private MorphManager morphs;

    @Override
    public boolean register(Commands dispatcher)
    {
        dispatcher.register(Commands.literal(name())
                .requires(this::checkPermission)
                .executes(this::executeNoArg)
                        .then(
                                Commands.argument("id", StringArgumentType.greedyString())
                                        .suggests(this::suggests)
                                        .executes(this::execWithArg)
                        )
                .build());

        return true;
    }

    public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var source = context.getSource().getExecutor();

        if (!(source instanceof Player player))
            return CompletableFuture.completedFuture(suggestionsBuilder.build());

        String input = suggestionsBuilder.getRemainingLowerCase();

        var availableDisguises = morphs.getAvaliableDisguisesFor(player);

        return CompletableFuture.supplyAsync(() ->
        {
            for (DisguiseMeta disguiseMeta : availableDisguises)
            {
                var name = disguiseMeta.getKey();

                if (!name.toLowerCase().contains(input))
                    continue;

                suggestionsBuilder.suggest(name);
            }

            return suggestionsBuilder.build();
        });
    }

    public int executeNoArg(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getExecutor();

        if (!(sender instanceof Player player))
            return Command.SINGLE_SUCCESS;

        //伪装冷却
        if (!morphManager.canMorph(player))
        {
            sender.sendMessage(MessageUtils.prefixes(player, MorphStrings.disguiseCoolingDownString()));

            return Command.SINGLE_SUCCESS;
        }

        var gui = new DisguiseSelectScreenWrapper(player, 0);
        gui.show();

        return 1;
    }

    private int execWithArg(CommandContext<CommandSourceStack> context)
    {
        var sender = context.getSource().getExecutor();

        if (!(sender instanceof Player player))
            return Command.SINGLE_SUCCESS;

        //伪装冷却
        if (!morphManager.canMorph(player))
        {
            sender.sendMessage(MessageUtils.prefixes(player, MorphStrings.disguiseCoolingDownString()));

            return Command.SINGLE_SUCCESS;
        }

        String inputID = StringArgumentType.getString(context, "id");
        morphManager.morph(sender, player, inputID, player.getTargetEntity(5));

        return 1;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.morphDescription();
    }
}
