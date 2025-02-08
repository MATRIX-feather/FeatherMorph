package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values;

import org.bukkit.entity.Villager;
import xyz.nifeather.morph.backends.server.renderer.network.CustomSerializeMethods;

import static xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers.*;

public class ZombieVillagerValues extends ZombieValues
{
    public final SingleValue<Boolean> CONVERTING = createSingle("zVillager_converting", false);
    public final SingleValue<VillagerData> VILLAGER_DATA = createSingle("zVillager_data", new VillagerData(Villager.Type.PLAINS, Villager.Profession.NONE, 1));

    public ZombieVillagerValues()
    {
        super();

        VILLAGER_DATA.setSerializeMethod(CustomSerializeMethods.VILLAGER_DATA);

        registerSingle(CONVERTING, VILLAGER_DATA);
    }
}
