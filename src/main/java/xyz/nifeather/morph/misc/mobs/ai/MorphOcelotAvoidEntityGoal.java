package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.MorphManager;

public class MorphOcelotAvoidEntityGoal extends MorphCommonAvoidPlayerGoal
{
    private static final Logger log = LoggerFactory.getLogger(MorphOcelotAvoidEntityGoal.class);
    private final Ocelot bindingOcelot;

    public MorphOcelotAvoidEntityGoal(MorphManager morphs, Ocelot bindingMob, float detectDistance, double walkSpeed, double sprintSpeed)
    {
        super(morphs, bindingMob, detectDistance, walkSpeed, sprintSpeed);

        this.bindingOcelot = bindingMob;
    }

    @Override
    public boolean canUse()
    {
        if (!this.bindingOcelot.isTrusting())
            return false;

        return super.canUse();
    }

    @Override
    public @Nullable AvoidEntityGoal<Player> getRecoverGoalOrNull()
    {
        return RecoverGoalGenerator.generateRecover(Ocelot.class, this.bindingOcelot, "OcelotAvoidEntityGoal", this.detectDistance, this.walkSpeed, this.sprintSpeed);
    }
}
