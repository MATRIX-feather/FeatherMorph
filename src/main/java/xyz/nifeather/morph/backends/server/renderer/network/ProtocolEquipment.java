package xyz.nifeather.morph.backends.server.renderer.network;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftInventoryPlayer;
import org.bukkit.inventory.EntityEquipment;
import xyz.nifeather.morph.FeatherMorphMain;
import xyz.nifeather.morph.utilities.ItemUtils;

public class ProtocolEquipment
{
    public static ObjectArrayList<Pair<EquipmentSlot, ItemStack>> toPairs(EntityEquipment equipment)
    {
        var list = new ObjectArrayList<Pair<EquipmentSlot, ItemStack>>();

        for (org.bukkit.inventory.EquipmentSlot bukkitSlot : org.bukkit.inventory.EquipmentSlot.values())
            list.add(toEquipmentPair(equipment, bukkitSlot));

        return list;
    }

    private static EquipmentSlot toNMSSlot(org.bukkit.inventory.EquipmentSlot bukkitSlot)
    {
        return switch (bukkitSlot)
        {
            case HAND -> EquipmentSlot.MAINHAND;
            case OFF_HAND -> EquipmentSlot.OFFHAND;

            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;

            case BODY -> EquipmentSlot.BODY;
        };
    }

    private static Pair<EquipmentSlot, ItemStack> toEquipmentPair(EntityEquipment equipment, org.bukkit.inventory.EquipmentSlot bukkitSlot)
    {
        try
        {
            if (equipment instanceof CraftInventoryPlayer && bukkitSlot == org.bukkit.inventory.EquipmentSlot.BODY)
                return Pair.of(toNMSSlot(bukkitSlot), ItemUtils.nmsAir);

            var bukkitItem = equipment.getItem(bukkitSlot);
            var nmsItem = ItemStack.fromBukkitCopy(bukkitItem);

            return Pair.of(toNMSSlot(bukkitSlot), nmsItem);
        }
        catch (Throwable t)
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            logger.warn("Can't generate equipment pair: " + t.getMessage());
        }

        return Pair.of(EquipmentSlot.FEET, ItemStack.fromBukkitCopy(new org.bukkit.inventory.ItemStack(Material.AIR)));
    }
}
