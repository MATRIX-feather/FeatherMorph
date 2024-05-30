package xiamomc.morph.events;

import com.destroystokyo.paper.entity.ai.PaperVanillaGoal;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import xiamomc.morph.MorphManager;
import xiamomc.morph.MorphPlugin;
import xiamomc.morph.MorphPluginObject;
import xiamomc.morph.utilities.EntityTypeUtils;
import xiamomc.morph.utilities.ReflectionUtils;
import xiamomc.pluginbase.Annotations.Resolved;

import java.util.function.Predicate;

public class TargetingEventProcessor extends MorphPluginObject implements Listener
{
    @Resolved(shouldSolveImmediately = true)
    private MorphManager manager;

    @EventHandler
    public void onEntityAdded(EntityAddToWorldEvent e)
    {
        var entity = e.getEntity();
        if (!(entity instanceof Mob mob)) return;

        var nmsMob = ((CraftMob)mob).getHandle();
        var goalSelector = nmsMob.goalSelector;

        //TODO: test, remove this upon release
        //if (!(mob instanceof Fox) && !(mob instanceof IronGolem)) return;

        if (!(nmsMob instanceof PathfinderMob pathfinderMob)) return;

        // 添加AvoidEntityGoal
        addAvoidEntityGoal(goalSelector, pathfinderMob);

        // 添加TargetGoal
        var wrappedGoal = new PaperVanillaGoal<>(new AttackableGoal(manager, nmsMob, Player.class, true, le -> true));

        //globalGoals.addGoal(mob, 110, wrappedGoal);
        nmsMob.goalSelector.addGoal(-1, wrappedGoal.getHandle());
    }

    private void addAvoidEntityGoal(GoalSelector goalSelector, PathfinderMob sourceMob)
    {
        var availableGoals = goalSelector.getAvailableGoals();

        Goal replacingGoal = null;
        Goal goalFound = null;
        int goalPriority = 0;

        float distance = 16F; double slowSpeed = 1D, fastSpeed = 1D;

        // 遍历实体已有的Goal
        for (WrappedGoal g : availableGoals)
        {
            // 跳过不是AvoidEntityGoal的对象
            if (!(g.getGoal() instanceof AvoidEntityGoal<?> avoidEntityGoal)) continue;

            // 尝试获取他所要避免的目标类型
            var fields = ReflectionUtils.getFields(avoidEntityGoal, Class.class, false);
            if (fields.isEmpty()) continue;

            var field = fields.get(0);
            field.setAccessible(true);

            var goal = g.getGoal();

            // 创建用于替代它的Goal
            try
            {
                var v = field.get(avoidEntityGoal);

                if (v != Player.class) continue;

                // 类型符合，标记移除此Goal
                distance = ReflectionUtils.getValue(goal, "maxDist", float.class);
                slowSpeed = ReflectionUtils.getValue(goal, "walkSpeedModifier", double.class);
                fastSpeed = ReflectionUtils.getValue(goal, "sprintSpeedModifier", double.class);

                goalFound = g;
                goalPriority = g.getPriority();

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

        replacingGoal = new FeatherMorphAvoidPlayerGoal(manager, goalFound != null, sourceMob, Player.class, distance, slowSpeed, fastSpeed);

        goalSelector.addGoal(goalPriority, replacingGoal);
    }

    private void doRecoverGoal()
    {
        Bukkit.getWorlds().forEach(w ->
        {
            var entities = w.getEntities();

            entities.forEach(e ->
            {
                // 只修改Mob类型
                if (!(e instanceof Mob mob)) return;

                var handle = (net.minecraft.world.entity.Mob) ((CraftMob)mob).getHandleRaw();
                var availableGoals = new ObjectArrayList<>(handle.goalSelector.getAvailableGoals());

                // 遍历所有Goal
                for (var wrappedGoal : availableGoals)
                {
                    // 只替代我们想替代的
                    if (!(wrappedGoal.getGoal() instanceof FeatherMorphAvoidPlayerGoal)
                        && !(wrappedGoal.getGoal() instanceof AttackableGoal))
                    {
                        return;
                    }

                    // 先移除此Goal
                    handle.goalSelector.removeGoal(wrappedGoal.getGoal());

                    // 如果是AvoidPlayer, 那么重构
                    if (wrappedGoal.getGoal() instanceof FeatherMorphAvoidPlayerGoal avoidPlayerGoal && avoidPlayerGoal.isReplacement)
                    {
                        var priority = wrappedGoal.getPriority();
                        var vanillaGoal = new AvoidEntityGoal<>(
                                avoidPlayerGoal.pathfinderMob, Player.class,
                                avoidPlayerGoal.distance, avoidPlayerGoal.slowSpeed, avoidPlayerGoal.fastSpeed
                        );

                        handle.goalSelector.addGoal(priority, vanillaGoal);
                    }
                }
            });
        });
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

    /**
     * 此Goal将被添加到生物，作为附加的TargetGoal执行。
     */
    public static class AttackableGoal extends NearestAttackableTargetGoal<net.minecraft.world.entity.player.Player>
    {
        private final MorphManager morphManager;

        public AttackableGoal(MorphManager morphManager,
                              net.minecraft.world.entity.Mob mob, Class<Player> targetClass,
                              boolean checkVisibility, Predicate<LivingEntity> targetPredicate)
        {
            super(mob, targetClass, checkVisibility, targetPredicate);

            this.morphManager = morphManager;
        }

        @Override
        public void tick()
        {
            super.tick();
            updateTarget();
        }

        // 更新此生物的Target状态
        private void updateTarget()
        {
            // 如果target是null，则跳过
            if (this.target == null)
                return;

            // 如果当前的目标不再伪装，则取消对此人的target
            var disguise = morphManager.getDisguiseStateFor(this.target.getBukkitEntity());
            if (disguise == null)
            {
                this.mob.setTarget(null, EntityTargetEvent.TargetReason.CUSTOM, true);

                if (mob instanceof net.minecraft.world.entity.animal.IronGolem ironGolem)
                    ironGolem.forgetCurrentTargetAndRefreshUniversalAnger();
            }
        }

        @Override
        protected void findTarget()
        {
            updateTarget();

            this.target = null;
            var target = this.mob.level().getNearestPlayer(this.mob, this.getFollowDistance());

            if (target == null) return;

            var disguise = morphManager.getDisguiseStateFor(target.getBukkitEntity());
            if (disguise == null) return;

            if (hostiles(mob.getBukkitMob().getType(), disguise.getEntityType()))
                this.target = target;
        }

        @Override
        public void start()
        {
            super.start();

            // We cancels reason with CLOSEST_PLAYER, so we need to target again with CUSTOM
            // See CommonEventProcessor#onEntityTarget()
            mob.setTarget(this.target, EntityTargetEvent.TargetReason.CUSTOM, true);
        }
    }

    /**
     * 此Goal会作为生物的<b>额外AvoidGoal</b>进行。
     * 如果生物之前有一个对Player的AvoidGoal，<b>则此Goal会替代他</b>。
     */
    public static class FeatherMorphAvoidPlayerGoal extends AvoidEntityGoal<Player>
    {
        private final MorphManager morphs;

        public final PathfinderMob pathfinderMob;
        public final float distance;
        public final double slowSpeed;
        public final double fastSpeed;

        public final boolean isReplacement;

        public FeatherMorphAvoidPlayerGoal(MorphManager morphs, boolean isReplace, PathfinderMob mob, Class<Player> fleeFromType, float distance, double slowSpeed, double fastSpeed)
        {
            super(mob, fleeFromType, distance, slowSpeed, fastSpeed);
            this.morphs = morphs;
            this.pathfinderMob = mob;
            this.distance = distance;
            this.slowSpeed = slowSpeed;
            this.fastSpeed = fastSpeed;

            this.isReplacement = isReplace;
        }

        @Override
        public void start()
        {
            if (this.toAvoid == null)
                return;

            super.start();
        }

        /**
         * @return 是否逃跑
         */
        @Override
        public boolean canUse()
        {
            // avoidClass是玩家的Player.class, 因此super.canUse()当玩家在范围里时永远会是true
            // 因为这个Goal的avoidClass永远是Player，因此super.canUse()的结果不重要。
            super.canUse();

            if (this.toAvoid == null)
                return false;

            boolean panics;
            var state = morphs.getDisguiseStateFor(toAvoid.getBukkitEntity());
            if (state == null) panics = false;
            else panics = panics(mob.getBukkitMob().getType(), state.getEntityType());

            if (!panics) this.toAvoid = null;
            return panics;
        }
    }

    public static boolean panics(EntityType sourceType, EntityType targetType)
    {
        return switch (sourceType)
        {
            case CREEPER -> targetType == EntityType.CAT || targetType == EntityType.OCELOT;
            case PHANTOM -> targetType == EntityType.CAT;
            case SPIDER -> targetType == EntityType.ARMADILLO;
            case SKELETON -> targetType == EntityType.WOLF;
            case VILLAGER -> targetType == EntityType.ZOMBIE || targetType == EntityType.ZOMBIE_VILLAGER;

            default -> false;
        };
    }

    /**
     * 检查源生物和目标生物类型是否敌对
     * @param sourceType 源生物的类型
     * @param targetType 目标生物的类型
     */
    public static boolean hostiles(EntityType sourceType, EntityType targetType)
    {
        return switch (sourceType)
        {
            case IRON_GOLEM, SNOW_GOLEM -> EntityTypeUtils.isEnemy(targetType) && targetType != EntityType.CREEPER;

            case FOX -> targetType == EntityType.CHICKEN || targetType == EntityType.RABBIT
                    || targetType == EntityType.COD || targetType == EntityType.SALMON
                    || targetType == EntityType.TROPICAL_FISH || targetType == EntityType.PUFFERFISH;

            case CAT -> targetType == EntityType.CHICKEN || targetType == EntityType.RABBIT;

            case WOLF -> EntityTypeUtils.isSkeleton(targetType) || targetType == EntityType.RABBIT
                    || targetType == EntityType.LLAMA || targetType == EntityType.SHEEP
                    || targetType == EntityType.FOX;

            case GUARDIAN, ELDER_GUARDIAN -> targetType == EntityType.AXOLOTL || targetType == EntityType.SQUID
                    || targetType == EntityType.GLOW_SQUID;

            // Doesn't work for somehow
            case AXOLOTL -> targetType == EntityType.SQUID || targetType == EntityType.GLOW_SQUID
                    || targetType == EntityType.GUARDIAN || targetType == EntityType.ELDER_GUARDIAN
                    || targetType == EntityType.TADPOLE || targetType == EntityType.DROWNED
                    || targetType == EntityType.COD || targetType == EntityType.SALMON
                    || targetType == EntityType.TROPICAL_FISH || targetType == EntityType.PUFFERFISH;

            default -> false;
        };
    }
}
