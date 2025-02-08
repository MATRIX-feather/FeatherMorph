package xyz.nifeather.morph.backends.server.renderer.network.datawatcher;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.bukkit.entity.Villager;

public class DataWrappers
{
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

    public enum ShulkerDirection
    {
        DOWN(Direction.DOWN),
        UP(Direction.UP),
        NORTH(Direction.NORTH),
        SOUTH(Direction.SOUTH),
        WEST(Direction.WEST),
        EAST(Direction.EAST);

        private final Direction nmsDirection;
        public Direction nmsDirection()
        {
            return nmsDirection;
        }

        ShulkerDirection(Direction nmsDirection)
        {
            this.nmsDirection = nmsDirection;
        }
    }

    public record VillagerData(Villager.Type type, Villager.Profession profession, int level)
    {
    }
}
