package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers.ArmadilloState;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.AnimationNames;

public class ArmadilloWatcher extends LivingEntityWatcher
{
    public ArmadilloWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.ARMADILLO);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        this.register(ValueIndex.ARMADILLO);
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.ANIMATION))
        {
            var animId = newVal.toString();
            var player = getBindingPlayer();
            var world = player.getWorld();
            switch (animId)
            {
                case AnimationNames.PANIC_ROLLING ->
                {
                    writePersistent(ValueIndex.ARMADILLO.STATE, ArmadilloState.ROLLING);
                    world.playSound(player.getLocation(), Sound.ENTITY_ARMADILLO_ROLL, 1, 1);
                }
                case AnimationNames.PANIC_SCARED ->
                {
                    writePersistent(ValueIndex.ARMADILLO.STATE, ArmadilloState.SCARED);
                    world.playSound(player.getLocation(), Sound.ENTITY_ARMADILLO_LAND, 1, 1);
                }
                case AnimationNames.PANIC_UNROLLING ->
                {
                    writePersistent(ValueIndex.ARMADILLO.STATE, ArmadilloState.UNROLLING);
                    world.playSound(player.getLocation(), Sound.ENTITY_ARMADILLO_UNROLL_START, 1, 1);
                }
                case AnimationNames.PANIC_IDLE, AnimationNames.RESET ->
                {
                    writePersistent(ValueIndex.ARMADILLO.STATE, ArmadilloState.IDLE);

                    if (animId.equals(AnimationNames.PANIC_IDLE))
                        world.playSound(player.getLocation(), Sound.ENTITY_ARMADILLO_UNROLL_FINISH, 1, 1);
                }
            }
        }
    }
}
