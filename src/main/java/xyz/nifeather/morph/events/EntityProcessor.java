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

            if (wrapped.getGoal() instanceof FeatherMorphAvoidPlayerGoal)
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
                        && !(wrappedGoal.getGoal() instanceof FeatherMorphNearestAttackableGoal))
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
    public static class FeatherMorphNearestAttackableGoal extends NearestAttackableTargetGoal<net.minecraft.world.entity.player.Player>
    {
        private final MorphManager morphManager;

        public FeatherMorphNearestAttackableGoal(MorphManager morphManager,
                                                 net.minecraft.world.entity.Mob mob, Class<Player> targetClass,
                                                 boolean checkVisibility, TargetingConditions.Selector targetPredicate)
        {
            super(mob, targetClass, checkVisibility, targetPredicate);

            this.morphManager = morphManager;
        }

        private boolean isMobTargetingOurs()
        {
            var mobTarget = this.mob.getTarget();

            // 检查当前生物是否在target我们的目标
            return mobTarget == null || mobTarget == this.target;
        }

        @Override
        public boolean canUse()
        {
            var superCanUse = super.canUse();

            return this.target != null || superCanUse;
        }

        @Override
        public void tick()
        {
            super.tick();

            // 如果生物的目标不是我们的目标，则不要处理
            if (!isMobTargetingOurs()) return;

            // 如果我们期望的目标是null，则跳过
            if (this.target == null)
                return;

            // 获取跟随距离
            double followRange = 16D;
            var followRangeAttribute = mob.getAttribute(Attributes.FOLLOW_RANGE);

            if (followRangeAttribute != null)
                followRange = followRangeAttribute.getValue();

            var playerTarget = (CraftPlayer) target.getBukkitEntityRaw();

            var cancelTarget = false;

            // 当满足以下任一条件时，取消仇恨：
            // 处于不同的世界
            // 玩家超过跟随距离
            // 玩家不在线
            // 玩家不是生存模式
            cancelTarget = (this.mob.level() != this.target.level());
            cancelTarget = cancelTarget || (this.mob.distanceTo(this.target) > followRange);
            cancelTarget = cancelTarget || !playerTarget.isOnline();
            cancelTarget = cancelTarget || !((ServerPlayer)target).gameMode.isSurvival();

            // 如果玩家后来变成了其他会导致恐慌的类型，也取消仇恨
            var disguise = morphManager.getDisguiseStateFor(this.target.getBukkitEntity());
            if (disguise != null)
                cancelTarget = cancelTarget || panics(this.mob.getBukkitEntity().getType(), disguise.getEntityType());
            else
                cancelTarget = true;

            if (!cancelTarget)
                return;

            // 如果当前的目标不再伪装，则取消对此人的target

            // Forget our target
            this.target = null;

            this.mob.setTarget(null, EntityTargetEvent.TargetReason.CUSTOM, true);

            if (mob instanceof NeutralMob neutralMob)
                neutralMob.forgetCurrentTargetAndRefreshUniversalAnger();
        }

        @Override
        protected void findTarget()
        {
            var target = this.mob.level().getNearestPlayer(this.mob, this.getFollowDistance());

            if (target == null) return;

            // 忽略非生存玩家
            if (target instanceof ServerPlayer serverPlayer && !serverPlayer.gameMode.isSurvival())
                return;

            // 我们只想确认玩家的伪装是否为生物的敌对类型
            var disguise = morphManager.getDisguiseStateFor(target.getBukkitEntity());
            if (disguise == null) return;

            if (hostiles(mob.getBukkitEntity().getType(), disguise.getEntityType()))
                this.target = target;
        }

        @Override
        public void start()
        {
            if (mob.getTarget() == this.target)
                return;

            super.start();

            // 算了就让他优先攻击玩家吧
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

            if (findAvoidPath(this.toAvoid))
                this.pathNav.moveTo(this.path, this.slowSpeed);
        }

        /**
         * @return Whether success
         */
        private boolean findAvoidPath(Player toAvoid)
        {
            var targetPosition = DefaultRandomPos.getPosAway(this.mob, 16, 7, toAvoid.position());

            if (targetPosition == null)
                return false;

            // 如果目标位置离要逃离的目标更近，那么不要去那里
            if (toAvoid.distanceToSqr(targetPosition) < toAvoid.distanceToSqr(this.mob))
                return false;

            this.path = this.pathNav.createPath(targetPosition.x, targetPosition.y, targetPosition.z, 0);
            return this.path != null;
        }

        private void testPlayer()
        {
        }

        private void findEntityToAvoid()
        {
            var entities = mob.level()
                    .getEntitiesOfClass(avoidClass, mob.getBoundingBox().inflate(maxDist, 3.0, maxDist), living -> true);

            Player entityFound = null;
            double currentDistance = Double.MAX_VALUE;
            for (Player entity : entities)
            {
                if (!EntitySelector.NO_SPECTATORS.test(entity)) continue;

                var distance = entity.distanceToSqr(mob);
                if (distance < currentDistance)
                    entityFound = entity;
            }

            if (entityFound == null) return;

            if (!(entityFound.getBukkitEntityRaw() instanceof org.bukkit.entity.Player currentPlayer))
                return;

            // This will happen when you try to cover all the vanilla behavior with only one class...
            switch (this.mob)
            {
                case TamableAnimal tamableAnimal when tamableAnimal.isTame() -> { return; }
                case Ocelot ocelot when ocelot.isTrusting() -> { return; }
                case Panda panda when (!panda.isWorried() || !panda.canPerformAction()) -> { return; }
                case Rabbit rabbit when rabbit.getVariant() == Rabbit.Variant.EVIL -> { return; }
                default -> { }
            }

            var state = morphs.getDisguiseStateFor(currentPlayer);

            if (state == null && this.isReplacement)
            {
                this.toAvoid = NmsRecord.ofPlayer(currentPlayer);
                return;
            }

            if (state == null) return;

            if (panics(this.mob.getBukkitEntity().getType(), state.getEntityType()))
                this.toAvoid = NmsRecord.ofPlayer(currentPlayer);
        }

        /**
         * @return 是否逃跑
         */
        @Override
        public boolean canUse()
        {
            this.toAvoid = null;
            this.findEntityToAvoid();

            return this.toAvoid != null;
        }
    }

    public static boolean panics(EntityType sourceType, EntityType targetType)
    {
        return switch (sourceType)
        {
            case CREEPER -> targetType == EntityType.CAT || targetType == EntityType.OCELOT;
            case PHANTOM -> targetType == EntityType.CAT;
            case SPIDER -> targetType == EntityType.ARMADILLO;
            case SKELETON, WITHER_SKELETON -> targetType == EntityType.WOLF;
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
