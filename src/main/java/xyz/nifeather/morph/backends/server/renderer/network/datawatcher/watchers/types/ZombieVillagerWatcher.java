package xyz.nifeather.morph.backends.server.renderer.network.datawatcher.watchers.types;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import xyz.nifeather.morph.backends.server.renderer.network.registries.ValueIndex;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.DataWrappers;
import xyz.nifeather.morph.misc.disguiseProperty.DisguiseProperties;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;
import xyz.nifeather.morph.misc.disguiseProperty.values.VillagerProperties;
import xyz.nifeather.morph.utilities.MathUtils;

public class ZombieVillagerWatcher extends ZombieWatcher
{
    public ZombieVillagerWatcher(Player bindingPlayer)
    {
        super(bindingPlayer, EntityType.ZOMBIE_VILLAGER);
    }

    @Override
    protected void initRegistry()
    {
        super.initRegistry();

        register(ValueIndex.ZOMBIE_VILLAGER);
    }

    // region Cache

    private Villager.Profession profession;
    private Villager.Type type;
    private int lvl;

    private DataWrappers.VillagerData computeVillagerData()
    {
        var prof = this.profession == null ? Villager.Profession.NONE : this.profession;
        var type = this.type == null ? Villager.Type.PLAINS : this.type;

        return new DataWrappers.VillagerData(type, prof, lvl);
    }

    // endregion Cache

    @Override
    protected <X> void onPropertyWrite(SingleProperty<X> property, X value)
    {
        var properties = DisguiseProperties.INSTANCE.getOrThrow(VillagerProperties.class);

        if (property.equals(properties.LEVEL))
        {
            this.lvl = (Integer) value;

            writePersistent(ValueIndex.ZOMBIE_VILLAGER.VILLAGER_DATA, computeVillagerData());
        }

        if (property.equals(properties.TYPE))
        {
            this.type = (Villager.Type) value;

            writePersistent(ValueIndex.ZOMBIE_VILLAGER.VILLAGER_DATA, computeVillagerData());
        }

        if (property.equals(properties.PROFESSION))
        {
            this.profession = (Villager.Profession) value;

            writePersistent(ValueIndex.ZOMBIE_VILLAGER.VILLAGER_DATA, computeVillagerData());
        }

        super.onPropertyWrite(property, value);
    }

    private void mergeFromVillagerData(CompoundTag nbt)
    {
        int level = 0;
        Villager.Profession profession = Villager.Profession.NONE;
        Villager.Type type = Villager.Type.PLAINS;

        var profRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.VILLAGER_PROFESSION);
        var typeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.VILLAGER_TYPE);

        if (nbt.contains("level"))
            level = MathUtils.clamp(1, 5, nbt.getInt("level"));

        if (nbt.contains("profession"))
        {
            NamespacedKey rl = NamespacedKey.fromString(nbt.getString("profession"));

            if (rl == null)
            {
                logger.warn("Can't parse string '%s' to a NamespacedKey, using default".formatted(rl));
            }
            else
            {
                var prof = profRegistry.get(rl);

                if (prof == null)
                    logger.warn("No such profession '%s', using default".formatted(rl));
                else
                    profession = prof;
            }
        }

        if (nbt.contains("type"))
        {
            NamespacedKey rl = NamespacedKey.fromString(nbt.getString("type"));

            if (rl == null)
            {
                logger.warn("Can't parse string '%s' to a NamespacedKey, using default".formatted(rl));
            }
            else
            {
                var typeFromRegistry = typeRegistry.get(rl);

                if (typeFromRegistry == null)
                    logger.warn("No such type '%s', using default".formatted(rl));
                else
                    type = typeFromRegistry;
            }
        }

        writePersistent(ValueIndex.VILLAGER.VILLAGER_DATA, new DataWrappers.VillagerData(type, profession, level));
    }

    @Override
    public void mergeFromCompound(CompoundTag nbt)
    {
        super.mergeFromCompound(nbt);

        if (nbt.contains("VillagerData"))
            mergeFromVillagerData(nbt.getCompound("VillagerData"));
    }

    @Override
    public void writeToCompound(CompoundTag nbt)
    {
        super.writeToCompound(nbt);

        var villagerData = read(ValueIndex.ZOMBIE_VILLAGER.VILLAGER_DATA);
        var profession = villagerData.profession();
        var type = villagerData.type();
        var level = villagerData.level();

        var compound = new CompoundTag();
        compound.putInt("level", level);
        compound.putString("profession", profession.key().asString());
        compound.putString("type", type.key().asString());

        nbt.put("VillagerData", compound);
    }
}
