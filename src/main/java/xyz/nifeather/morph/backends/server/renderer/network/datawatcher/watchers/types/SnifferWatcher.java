package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sniffer;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntries;
import xyz.nifeather.morph.backends.server.renderer.network.registries.CustomEntry;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.misc.AnimationNames;

public class SnifferWatcher extends LivingEntityWatcher
{
    public SnifferWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.SNIFFER);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.SNIFFER);
    }

    @Override
    protected <X> void onEntryWrite(CustomEntry<X> entry, X oldVal, X newVal)
    {
        super.onEntryWrite(entry, oldVal, newVal);

        if (entry.equals(CustomEntries.ANIMATION))
        {
            var bindingPlayer = getBindingPlayer();
            var world = bindingPlayer.getWorld();
            var id = newVal.toString();

            switch (id)
            {
                case AnimationNames.SNIFF ->
                {
                    this.writePersistent(ValueIndex.SNIFFER.SNIFFER_STATE, Sniffer.State.SNIFFING);
                    world.playSound(bindingPlayer.getLocation(), Sound.ENTITY_SNIFFER_SNIFFING, SoundCategory.NEUTRAL, 1, 1);
                }
                case AnimationNames.RESET ->
                {
                    this.writePersistent(ValueIndex.SNIFFER.SNIFFER_STATE, Sniffer.State.IDLING);
                    this.remove(ValueIndex.SNIFFER.SNIFFER_STATE);
                }
                default -> logger.warn("Unknown animation sequence id '%s'".formatted(id));
            }
        }
    }
}
