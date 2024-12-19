package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.MorphManager;

public class MorphRabbitAvoidPlayerGoal extends MorphCommonAvoidPlayerGoal
{
    private static final Logger log = LoggerFactory.getLogger(MorphRabbitAvoidPlayerGoal.class);
    private final Rabbit rabbit;

    public MorphRabbitAvoidPlayerGoal(MorphManager morphs, Rabbit bindingMob, float detectDistance, double walkSpeed, double sprintSpeed)
    {
        super(morphs, bindingMob, detectDistance, walkSpeed, sprintSpeed);

        this.rabbit = bindingMob;
    }

    @Override
    public boolean canUse()
    {
        if (rabbit.getVariant() == Rabbit.Variant.EVIL)
            return false;

        return super.canUse();
    }

    @Override
    public @Nullable AvoidEntityGoal<Player> getRecoverGoalOrNull()
    {
        return RecoverGoalGenerator.generateRecover(Rabbit.class, this.rabbit, "RabbitAvoidEntityGoal", this.detectDistance, this.walkSpeed, this.sprintSpeed);
    }
}
