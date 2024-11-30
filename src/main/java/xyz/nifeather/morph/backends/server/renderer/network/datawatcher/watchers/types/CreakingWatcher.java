package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.AnimationNames;

public class CreakingWatcher extends LivingEntityWatcher
{
    public CreakingWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.CREAKING);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.CREAKING);
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.ANIMATION))
        {
            var id = newVal.toString();
            var bindingPlayer = getBindingPlayer();
            var world = bindingPlayer.getWorld();

            switch (id)
            {
                case AnimationNames.MAKE_ACTIVE ->
                {
                    this.writePersistent(ValueIndex.CREAKING.IS_ACTIVE, true);
                    world.playSound(bindingPlayer.getLocation(), Sound.ENTITY_CREAKING_ACTIVATE, SoundCategory.HOSTILE, 1, 1);
                }

                case AnimationNames.MAKE_INACTIVE ->
                {
                    this.writePersistent(ValueIndex.CREAKING.IS_ACTIVE, false);
                    world.playSound(bindingPlayer.getLocation(), Sound.ENTITY_CREAKING_FREEZE, SoundCategory.HOSTILE, 1, 1);
                }
            }
        }
    }
}
