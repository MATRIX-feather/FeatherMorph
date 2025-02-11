package xyz.nifeather.morph.events;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.CommandStrings;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.misc.NmsRecord;
import xyz.nifeather.morph.misc.mobs.ai.FeatherMorphNearestAttackableGoal;
import xyz.nifeather.morph.misc.mobs.ai.MorphBasicAvoidPlayerGoal;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;
import xyz.nifeather.morph.utilities.EntityTypeUtils;
import xyz.nifeather.morph.utilities.ReflectionUtils;

import java.util.function.Predicate;

public class EntityProcessor extends MorphPluginObject implements Listener
{
    @Resolved(shouldSolveImmediately = true)
    private MorphManager manager;

    @Resolved(shouldSolveImmediately = true)
    private MorphConfigManager config;

    private final boolean doModifyAI;
    private final Bindable<Boolean> debugOutput = new Bindable<>(false);

    public boolean currentlyDoModifyAI()
    {
        return doModifyAI;
    }

    public EntityProcessor()
    {
        doModifyAI = config.get(Boolean.class, ConfigOption.DO_MODIFY_AI);
        config.bind(debugOutput, ConfigOption.DEBUG_OUTPUT);

        config.getBindable(Boolean.class, ConfigOption.DO_MODIFY_AI).onValueChanged((o, n) ->
        {
            if (doModifyAI == n) return;

            logger.warn("- x - x - x - x - x - x - x - x - x - x - x - x -");
            logger.warn("");
            logger.warn("Changes were made about the option of modifying Mobs' AI.");
            logger.warn("And this requires a server restart!");
            logger.warn("");
            logger.warn("- x - x - x - x - x - x - x - x - x - x - x - x -");

            for (var player : Bukkit.getOnlinePlayers())
            {
                if (player.hasPermission(CommonPermissions.ADMIN))
                {
                    player.sendMessage(MessageUtils.prefixes(player, CommandStrings.aiWarningPrimary()));
                    player.sendMessage(MessageUtils.prefixes(player, CommandStrings.aiWarningSecondary()));
                }
            }
        });
    }

    @EventHandler
    public void onEntityAdded(EntityAddToWorldEvent e)
    {
        if (!doModifyAI) return;

        var entity = e.getEntity();
        if (!(entity instanceof Mob mob)) return;

        var nmsMob = ((CraftMob)mob).getHandle();
        var goalSelector = nmsMob.goalSelector;

        if (!(nmsMob instanceof PathfinderMob pathfinderMob)) return;

        // 添加AvoidEntityGoal
        addAvoidEntityGoal(goalSelector, pathfinderMob);

        // 添加TargetGoal
        nmsMob.goalSelector.addGoal(-1, new FeatherMorphNearestAttackableGoal(manager, nmsMob, Player.class, true, (living, world) -> true));
    }

    private void addAvoidEntityGoal(GoalSelector goalSelector, PathfinderMob sourceMob)
    {
        var availableGoals = goalSelector.getAvailableGoals();

        Goal replacingGoal = null;
        Goal goalFound = null;
        int goalPriority = 0;

        float distance = 16F;
        double slowSpeed = 1D, fastSpeed = 1D;

        // 遍历实体已有的Goal
        for (WrappedGoal wrapped : availableGoals)
        {
            // 跳过不是AvoidEntityGoal的对象
            if (!(wrapped.getGoal() instanceof AvoidEntityGoal<?> avoidEntityGoal)) continue;

            if (wrapped.getGoal() instanceof MorphBasicAvoidPlayerGoal)
            {
                if (debugOutput.get())
                    logger.warn("We are processing entity that's already processed?! Found FeatherMorphAvoidPlayerGoal in entity " + sourceMob);

                return;
            }

            // 尝试获取他所要避免的目标类型
            //
            // Class<?>
            // ^
            var fields = ReflectionUtils.getFields(avoidEntityGoal, Class.class, false);
            if (fields.isEmpty()) continue;

            var field = fields.get(0);
            field.setAccessible(true);

            // 创建用于替代它的Goal
            try
            {
                // Class<?>
                //       ^
                var v = field.get(avoidEntityGoal);

                if (v != Player.class) continue;

                // 类型符合，标记移除此Goal
                distance = ReflectionUtils.getValue(avoidEntityGoal, "maxDist", float.class);
                slowSpeed = ReflectionUtils.getValue(avoidEntityGoal, "walkSpeedModifier", double.class);
                fastSpeed = ReflectionUtils.getValue(avoidEntityGoal, "sprintSpeedModifier", double.class);

                goalFound = wrapped;
                goalPriority = wrapped.getPriority();

                break;
            }
            catch (Throwable throwable)
            {
                logger.warn("Failed to modify goal: " + throwable.getMessage());
                throwable.printStackTrace();
            }
        }

        // 移除并添加我们自己的Goal (如果有找到)
        if (goalFound != null)
            goalSelector.getAvailableGoals().remove(goalFound);

        replacingGoal = MorphBasicAvoidPlayerGoal.findGoalForEntity(
                sourceMob, manager, distance, (float)slowSpeed, (float)fastSpeed
        );

        goalSelector.addGoal(goalPriority, replacingGoal);
    }

    private void doRecoverGoal()
    {
        Bukkit.getWorlds().forEach(w ->
        {
            // Use NMS cause getEntities() in Bukkit's world will throw an async exception.
            // I doubt that doing this as a workaround will cause problems
            var nmsWorld = ((CraftWorld) w).getHandle();
            var entities = nmsWorld.getEntities().getAll();

            entities.forEach(e ->
            {
                // 只修改Mob类型
                if (!(e instanceof net.minecraft.world.entity.Mob mob)) return;

                this.scheduleOn(e.getBukkitEntity(), () -> recoverSingleMob(mob));
            });
        });
    }

    private void recoverSingleMob(net.minecraft.world.entity.Mob handle)
    {
        var availableGoals = new ObjectArrayList<>(handle.goalSelector.getAvailableGoals());

        // 遍历所有Goal
        for (var wrappedGoal : availableGoals)
        {
            // 只替代我们想替代的
            if (!(wrappedGoal.getGoal() instanceof MorphBasicAvoidPlayerGoal)
                    && !(wrappedGoal.getGoal() instanceof FeatherMorphNearestAttackableGoal))
            {
                return;
            }

            // 先移除此Goal
            handle.goalSelector.removeGoal(wrappedGoal.getGoal());

            // 如果是AvoidPlayer, 那么重构
            if (wrappedGoal.getGoal() instanceof MorphBasicAvoidPlayerGoal avoidPlayerGoal && avoidPlayerGoal.canBeUsedForRecover())
            {
                var recoverGoal = avoidPlayerGoal.getRecoverGoalOrNull();
                var priority = wrappedGoal.getPriority();

                if (recoverGoal != null)
                    handle.goalSelector.addGoal(priority, recoverGoal);
                else
                    logger.warn("Null recover goal for entity " + handle.getType());
            }
        }
    }

    public void recoverGoals()
    {
        logger.info("Recovering mob goals...");

        try
        {
            doRecoverGoal();
        }
        catch (Throwable t)
        {
            logger.error("Failed to recover goals: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
