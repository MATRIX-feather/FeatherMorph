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
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.EmoteStrings;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.gui.AnimSelectScreenWrapper;

import java.util.concurrent.CompletableFuture;

public class AnimationCommand extends MorphPluginObject implements IConvertibleBrigadier
{
    @Override
    public String name()
    {
        return "play-action";
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.animationDescription();
    }

    @Override
    public boolean register(Commands dispatcher)
    {
        dispatcher.register(
                Commands.literal(name())
                        .requires(this::checkPermission)
                        .executes(this::executeOpenGui)
                        .then(
                                Commands.argument("action", StringArgumentType.greedyString())
                                        .suggests(this::suggests)
                                        .executes(this::execWithArg)
                        )
                        .build()
        );

        return IConvertibleBrigadier.super.register(dispatcher);
    }

    @Resolved
    private MorphManager morphManager;

    public @NotNull CompletableFuture<Suggestions> suggests(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
    {
        var source = context.getSource().getExecutor();

        if (!(source instanceof Player player))
            return CompletableFuture.completedFuture(suggestionsBuilder.build());

        var state = morphManager.getDisguiseStateFor(player);
        if (state == null) return CompletableFuture.completedFuture(suggestionsBuilder.build());

        var animations = state.getProvider()
                .getAnimationProvider()
                .getAnimationSetFor(state.getDisguiseIdentifier())
                .getAvailableAnimationsForClient();

        var name = suggestionsBuilder.getRemainingLowerCase();
        return CompletableFuture.supplyAsync(() ->
        {
            animations.stream().filter(id -> id.startsWith(name))
                    .forEach(suggestionsBuilder::suggest);

            return suggestionsBuilder.build();
        });
    }

    public int executeOpenGui(CommandContext<CommandSourceStack> context)
    {
        var commandSender = context.getSource().getExecutor();

        if (!(commandSender instanceof Player player))
            return Command.SINGLE_SUCCESS;

        var state = morphManager.getDisguiseStateFor(player);
        if (state == null)
        {
            player.sendMessage(MessageUtils.prefixes(player, CommandStrings.notDisguised()));
            return Command.SINGLE_SUCCESS;
        }

        var animationSet = state.getProvider()
                .getAnimationProvider()
                .getAnimationSetFor(state.getDisguiseIdentifier());

        var screen = new AnimSelectScreenWrapper(state, animationSet.getAvailableAnimationsForClient());
        screen.show();

        return Command.SINGLE_SUCCESS;
    }

    private int execWithArg(CommandContext<CommandSourceStack> context)
    {
        var commandSender = context.getSource().getExecutor();

        if (!(commandSender instanceof Player player))
            return Command.SINGLE_SUCCESS;

        var state = morphManager.getDisguiseStateFor(player);
        if (state == null)
        {
            player.sendMessage(MessageUtils.prefixes(player, CommandStrings.notDisguised()));
            return Command.SINGLE_SUCCESS;
        }

        var animationSet = state.getProvider()
                .getAnimationProvider()
                .getAnimationSetFor(state.getDisguiseIdentifier());

        String animationId = StringArgumentType.getString(context, "action");

        var animations = animationSet.getAvailableAnimationsForClient();

        if (!animations.contains(animationId))
        {
            player.sendMessage(MessageUtils.prefixes(player, CommandStrings.noSuchAnimation()));
            return Command.SINGLE_SUCCESS;
        }

        var sequencePair = animationSet.sequenceOf(animationId);
        if (!state.tryScheduleSequence(animationId, sequencePair.left(), sequencePair.right()))
            player.sendMessage(MessageUtils.prefixes(player, EmoteStrings.notAvailable()));

        return Command.SINGLE_SUCCESS;
    }
}
