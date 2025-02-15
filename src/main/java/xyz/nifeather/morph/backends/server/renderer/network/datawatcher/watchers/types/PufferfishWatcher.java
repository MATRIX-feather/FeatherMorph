package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.PufferfishValues;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.AnimationNames;

public class PufferfishWatcher extends LivingEntityWatcher
{
    public PufferfishWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.PUFFERFISH);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        this.register(ValueIndex.PUFFERFISH);
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.ANIMATION))
        {
            var animId = newVal.toString();
            var world = getBindingPlayer().getWorld();

            var lastState = this.readOr(ValueIndex.PUFFERFISH.PUFF_STATE, 0);
            switch (animId)
            {
                case AnimationNames.INFLATE ->
                {
                    this.writePersistent(ValueIndex.PUFFERFISH.PUFF_STATE, PufferfishValues.PuffStates.LARGE);

                    if (lastState != PufferfishValues.PuffStates.LARGE)
                        world.playSound(getBindingPlayer().getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.HOSTILE, 1, 1);
                }
                case AnimationNames.DEFLATE ->
                {
                    this.writePersistent(ValueIndex.PUFFERFISH.PUFF_STATE, PufferfishValues.PuffStates.SMALL);

                    if (lastState != PufferfishValues.PuffStates.SMALL)
                        world.playSound(getBindingPlayer().getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.HOSTILE, 1, 1);
                }
                case AnimationNames.RESET ->
                {
                    this.writePersistent(ValueIndex.PUFFERFISH.PUFF_STATE, PufferfishValues.PuffStates.SMALL);
                }
            }
        }
    }
}
