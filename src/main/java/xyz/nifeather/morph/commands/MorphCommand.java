package xyz.nifeather.morph.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.commands.brigadier.IConvertibleBrigadier;
import xyz.nifeather.morph.commands.brigadier.arguments.ValueMapArgumentType;
import xyz.nifeather.morph.messages.HelpStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.MorphStrings;
import xyz.nifeather.morph.misc.DisguiseMeta;
import xyz.nifeather.morph.misc.gui.DisguiseSelectScreenWrapper;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
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
                                Commands.argument("id", ArgumentTypes.key())
                                        .suggests(this::suggestID)
                                        .executes(this::execWithID)
                                        //.then(
                                        //        Commands.argument("properties", new ValueMapArgumentType())
                                        //                .executes(this::execExperimental)
                                        //                .then(
                                        //                        Commands.argument("extra", IntegerArgumentType.integer())
                                        //                                .executes(this::execExperimentala)
                                        //                )
                                        //)
                        )
                .build());

        return true;
    }

    private int execExperimental(CommandContext<CommandSourceStack> context)
    {
        var input = ValueMapArgumentType.get("properties", context);

        input.forEach((k, v) ->
        {
            context.getSource().getSender().sendMessage("Key '%s', Value '%s'".formatted(k, v));
        });

        return 1;
    }

    private int execExperimentala(CommandContext<CommandSourceStack> context)
    {
        var input = ValueMapArgumentType.get("properties", context);

        input.forEach((k, v) ->
        {
            context.getSource().getSender().sendMessage("Key '%s', Value '%s'".formatted(k, v));
        });

        try
        {
            context.getSource().getSender().sendPlainMessage("Extra is " + IntegerArgumentType.getInteger(context, "extra"));
        }
        catch (Throwable t)
        {
            context.getSource().getSender().sendPlainMessage("No extra: " + t.getMessage());
        }

        return 1;
    }

    public @NotNull CompletableFuture<Suggestions> suggestID(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder)
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

    private int execWithID(CommandContext<CommandSourceStack> context)
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

        String inputID = context.getArgument("id", Key.class).toString();
        morphManager.morph(sender, player, inputID, player.getTargetEntity(5));

        return 1;
    }

    @Override
    public FormattableMessage getHelpMessage()
    {
        return HelpStrings.morphDescription();
    }
}
