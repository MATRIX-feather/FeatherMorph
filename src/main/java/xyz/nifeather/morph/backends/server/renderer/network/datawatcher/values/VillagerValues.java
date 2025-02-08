package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import org.bukkit.entity.Villager;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.basetypes.AbstractVillagerValues;

import static xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers.*;

public class VillagerValues extends AbstractVillagerValues
{
    public final SingleValue<VillagerData> VILLAGER_DATA = createSingle("villager_data", new VillagerData(Villager.Type.PLAINS, Villager.Profession.NONE, 0));

    public VillagerValues()
    {
        super();

        VILLAGER_DATA.setSerializeMethod(CustomSerializeMethods.VILLAGER_DATA);

        registerSingle(VILLAGER_DATA);
    }
}
