package xyz.nifeather.morph.misc.mobs;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftVex;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;

public class MorphBukkitVexModifier
{
    private final Vex vex;
    private final Player owner;

    public MorphBukkitVexModifier(org.bukkit.entity.Vex bukkitVex, org.bukkit.entity.Player bukkitPlayer)
    {
        this(
                ((CraftVex)bukkitVex).getHandle(),
                ((CraftPlayer)bukkitPlayer).getHandle()
        );
    }

    public MorphBukkitVexModifier(Vex nmsVex, Player owner)
    {
        Objects.requireNonNull(nmsVex, "Null NMS Vex");
        Objects.requireNonNull(owner, "Null owner");

        this.vex = nmsVex;
        this.owner = owner;

        registerGoals();
    }

    protected void registerGoals()
    {
        var targetSelectors = vex.targetSelector.getAvailableGoals();
        for (var wrapped : targetSelectors) if (wrapped != null)
            vex.targetSelector.removeGoal(wrapped.getGoal());

        vex.targetSelector.addGoal(0, new MorphOwnerHurtTargetGoal(vex, this));
        vex.targetSelector.addGoal(1, new MorphOwnerHurtByTargetGoal(vex, this));
    }

    public Player getPlayerOwner()
    {
        return owner;
    }

    private static class MorphOwnerHurtByTargetGoal extends TargetGoal
    {
        @NotNull
        private LivingEntity owner()
        {
            return wrapper.getPlayerOwner();
        }

        private final Vex thisEntity;

        private final MorphBukkitVexModifier wrapper;

        public MorphOwnerHurtByTargetGoal(Vex thisEntity, MorphBukkitVexModifier wrapper)
        {
            super(thisEntity, false);

            this.thisEntity = thisEntity;
            this.wrapper = wrapper;
        }

        @Override
        public boolean canUse()
        {
            var owner = this.owner();

            var lastHurtBy = owner.getLastHurtByMob();
            this.ownerLastHurtBy = lastHurtBy;

            return lastHurtBy != null
                    && this.canAttack(lastHurtBy, TargetingConditions.DEFAULT)
                    && ownerLastHurtByMobTimestamp != owner.getLastHurtByMobTimestamp();
        }

        private int ownerLastHurtByMobTimestamp;
        private LivingEntity ownerLastHurtBy;

        @Override
        public void start()
        {
            super.start();

            var owner = this.owner();
            this.ownerLastHurtByMobTimestamp = owner.getLastHurtByMobTimestamp();

            this.thisEntity.setTarget(ownerLastHurtBy, EntityTargetEvent.TargetReason.CUSTOM, true);
        }
    }

    private static class MorphOwnerHurtTargetGoal extends TargetGoal
    {
        @NotNull
        private LivingEntity owner()
        {
            return wrapper.getPlayerOwner();
        }

        private final Vex thisEntity;

        private final MorphBukkitVexModifier wrapper;

        public MorphOwnerHurtTargetGoal(Vex thisEntity, MorphBukkitVexModifier wrapper)
        {
            super(thisEntity, false);

            Objects.requireNonNull(thisEntity, "Null Entity");
            Objects.requireNonNull(wrapper, "Null Wrapper");

            this.wrapper = wrapper;
            this.thisEntity = thisEntity;

            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse()
        {
            var lastHurt = owner().getLastHurtMob();
            this.ownerLastHurt = lastHurt;

            return lastHurt != null
                    && this.canAttack(lastHurt, TargetingConditions.DEFAULT)
                    && ownerLastHurtTimestamp != owner().getLastHurtMobTimestamp();
        }

        private LivingEntity ownerLastHurt;

        private int ownerLastHurtTimestamp;

        @Override
        public void start()
        {
            super.start();

            ownerLastHurtTimestamp = this.owner().getLastHurtMobTimestamp();
            this.thisEntity.setTarget(this.ownerLastHurt, EntityTargetEvent.TargetReason.CUSTOM, true);
        }
    }
}
