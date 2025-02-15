package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;

public class HoglinWatcher extends EHasAttackAnimationWatcher
{
    public HoglinWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.HOGLIN);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.AGEABLE_MOB);
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("IsBaby"))
            writePersistent(ValueIndex.AGEABLE_MOB.IS_BABY, nbt.getBoolean("IsBaby"));
    }
}
