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
                        // Kept as intended
                        //.then(
                        //        Commands.literal("testGoal")
                        //                .executes(this::testGoal)
                        //)
                .build());

        return true;
    }
/*
    private int testGoal(CommandContext<CommandSourceStack> context)
    {
        var executor = context.getSource().getExecutor();
        if (executor == null)
        {
            context.getSource().getSender().sendMessage("Executor is null!");
            return 0;
        }

        executor.sendMessage("Test rabbit!");

        var world = executor.getWorld();
        var rabbit = world.spawn(executor.getLocation(), EntityType.RABBIT.getEntityClass());
        testGoal1(((CraftRabbit) rabbit).getHandle());
        rabbit.remove();

        executor.sendMessage("Test ocelot!");

        var ocelot = world.spawn(executor.getLocation(), EntityType.OCELOT.getEntityClass());
        testGoal1(((CraftOcelot) ocelot).getHandle());
        ocelot.remove();

        executor.sendMessage("Test Panda!");

        var panda = world.spawn(executor.getLocation(), EntityType.PANDA.getEntityClass());
        testGoal1(((CraftPanda) panda).getHandle());
        panda.remove();

        executor.sendMessage("Test Cat!");

        var tamable = world.spawn(executor.getLocation(), EntityType.CAT.getEntityClass());
        var asTamable = (CraftTameableAnimal) tamable;
        asTamable.setTamed(true);
        asTamable.setOwner((Player)executor);

        testGoal1(asTamable.getHandle());
        tamable.remove();

        return 1;
    }

    private void testGoal1(PathfinderMob pathfinderMob)
    {
        var goal = MorphBasicAvoidPlayerGoal.findGoalForEntity(pathfinderMob, morphManager, 0, 1, 2);
        Bukkit.broadcastMessage("Got goal " + goal.getClass().getSimpleName());

        if (goal.getRecoverGoalOrNull() == null)
            Bukkit.broadcastMessage("Mob '%s' is null!".formatted(pathfinderMob.getBukkitEntity().getType()));
        else
            Bukkit.broadcastMessage("Got recover goal " + goal.getRecoverGoalOrNull().getClass().getSimpleName());
    }
*/
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
