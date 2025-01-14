package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.entity.EntityTargetEvent;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.utilities.EntityTypeUtils;

/**
 * 此Goal将被添加到生物，作为附加的TargetGoal执行。
 */
public class FeatherMorphNearestAttackableGoal extends NearestAttackableTargetGoal<Player>
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
            cancelTarget = cancelTarget || EntityTypeUtils.panicsFrom(this.mob.getBukkitEntity().getType(), disguise.getEntityType());
        else
            cancelTarget = true;

        if (!cancelTarget)
            return;

        // 如果当前的目标不再伪装，则取消对此人的target

        // Forget our target
        this.target = null;

        if (this.mob.getTarget() == this.target)
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

        if (EntityTypeUtils.hostiles(mob.getBukkitEntity().getType(), disguise.getEntityType()))
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

