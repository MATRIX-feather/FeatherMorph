package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.MorphManager;

public class MorphPandaAvoidPlayerGoal extends MorphCommonAvoidPlayerGoal
{
    private static final Logger log = LoggerFactory.getLogger(MorphPandaAvoidPlayerGoal.class);
    private final Panda panda;

    public MorphPandaAvoidPlayerGoal(MorphManager morphs, Panda bindingMob, float detectDistance, double walkSpeed, double sprintSpeed)
    {
        super(morphs, bindingMob, detectDistance, walkSpeed, sprintSpeed);

        this.panda = bindingMob;
    }

    @Override
    public boolean canUse()
    {
        if (!panda.isWorried() || !panda.canPerformAction())
            return false;

        return super.canUse();
    }

    @Override
    @Nullable
    public AvoidEntityGoal<Player> getRecoverGoalOrNull()
    {
        return RecoverGoalGenerator.generateRecover(Panda.class, this.panda, "PandaAvoidGoal", this.detectDistance, this.walkSpeed, this.sprintSpeed);
    }
}
