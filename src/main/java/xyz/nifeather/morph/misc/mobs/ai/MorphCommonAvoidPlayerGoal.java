package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.MorphManager;
import xyz.nifeather.morph.misc.DisguiseState;
import xyz.nifeather.morph.utilities.EntityTypeUtils;

public class MorphCommonAvoidPlayerGoal extends MorphBasicAvoidPlayerGoal
{
    public MorphCommonAvoidPlayerGoal(MorphManager morphs, PathfinderMob bindingMob, float detectDistance, double walkSpeed, double sprintSpeed)
    {
        super(morphs, bindingMob, detectDistance, walkSpeed, sprintSpeed);
    }

    @Override
    protected boolean panicFrom(Player nmsPlayer, DisguiseState disguiseState)
    {
        return EntityTypeUtils.panicsFrom(this.bindingMob.getBukkitEntity().getType(), disguiseState.getEntityType());
    }

    @Override
    @Nullable
    public AvoidEntityGoal<Player> getRecoverGoalOrNull()
    {
        return new AvoidEntityGoal<>(
                this.bindingMob, Player.class,
                this.detectDistance, this.walkSpeed, this.sprintSpeed
        );
    }
}
