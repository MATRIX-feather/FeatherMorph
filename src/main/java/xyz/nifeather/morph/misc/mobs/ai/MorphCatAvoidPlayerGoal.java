package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.MorphManager;

public class MorphCatAvoidPlayerGoal extends MorphCommonAvoidPlayerGoal
{
    private static final Logger log = LoggerFactory.getLogger(MorphCatAvoidPlayerGoal.class);
    private final Cat cat;

    public MorphCatAvoidPlayerGoal(MorphManager morphs, Cat bindingMob, float detectDistance, double walkSpeed, double sprintSpeed)
    {
        super(morphs, bindingMob, detectDistance, walkSpeed, sprintSpeed);

        this.cat = bindingMob;
    }

    @Override
    public boolean canUse()
    {
        if (cat.isTame())
            return false;

        return super.canUse();
    }

    @Override
    @Nullable
    public AvoidEntityGoal<Player> getRecoverGoalOrNull()
    {
        return RecoverGoalGenerator.generateRecover(Cat.class, this.cat, "CatAvoidEntityGoal", this.detectDistance, this.walkSpeed, this.sprintSpeed);
    }
}
