package xyz.nifeather.morph.misc;

import net.minecraft.world.entity.animal.armadillo.Armadillo;

public enum ArmadilloState
{
    IDLE(Armadillo.ArmadilloState.IDLE),
    ROLLING(Armadillo.ArmadilloState.ROLLING),
    SCARED(Armadillo.ArmadilloState.SCARED),
    UNROLLING(Armadillo.ArmadilloState.UNROLLING);

    private final Armadillo.ArmadilloState state;
    public Armadillo.ArmadilloState nmsState()
    {
        return state;
    }

    ArmadilloState(Armadillo.ArmadilloState state)
    {
        this.state = state;
    }
}
