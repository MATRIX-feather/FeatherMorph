package xyz.nifeather.morph.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.nifeather.morph.FeatherMorphMain;

public class ItemUtils
{
    public static final ItemStack air = new ItemStack(Material.AIR, 1);

    public static final net.minecraft.world.item.ItemStack nmsAir = net.minecraft.world.item.ItemStack.fromBukkitCopy(air);

    public static ItemStack itemOrAir(ItemStack stack)
    {
        return stack == null ? air.clone() : stack;
    }

    public static ItemStack asCopy(@NotNull ItemStack stack)
    {
        return stack.clone();
    }

    public static ItemStack[] asCopy(ItemStack... stacks)
    {
        var array = new ItemStack[stacks.length];

        for (int i = 0; i < stacks.length; i++)
        {
            var stack = stacks[i];
            array[i] = stack != null ? stack.clone() : null;
        }

        return array;
    }

    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public static String itemToStr(ItemStack stack)
    {
        var item = ItemUtils.itemOrAir(stack);

        if (stack.getType().isAir()) return "{\"id\":\"minecraft:air\",\"Count\":3}";

        //NOTE: 1.20.6需要访问世界注册表来获取JsonOps的序列化上下文来完整地反序列化ItemStack
        var registry = ((CraftWorld)Bukkit.getWorlds().stream().findFirst().get()).getHandle().registryAccess();

        //CODEC
        var nmsCodec = net.minecraft.world.item.ItemStack.CODEC;

        var json = nmsCodec.encodeStart(registry.createSerializationContext(JsonOps.INSTANCE), CraftItemStack.asNMSCopy(item))
                .result();

        if (json.isPresent())
        {
            return gson.toJson(json.get());
        }
        else
        {
            var logger = FeatherMorphMain.getInstance().getSLF4JLogger();

            logger.warn("Can't encode item '%s'! Providing without component data...".formatted(stack));

            return "{\"id\": \"%s\", \"Count\": 1}".formatted(stack.getType().getKey().asString());
        }
    }

    public static final String SKILL_ACTIVATE_ITEM_KEY = "feathermorph:is_disguise_tool";

    public static ItemStack buildDisguiseToolFrom(ItemStack stack)
    {
        var nms = net.minecraft.world.item.ItemStack.fromBukkitCopy(stack);
        var customData = nms.getComponents().get(DataComponents.CUSTOM_DATA);
        if (customData == null) customData = CustomData.EMPTY;

        customData = customData.update(tag -> tag.putBoolean(SKILL_ACTIVATE_ITEM_KEY, true));
        nms.set(DataComponents.CUSTOM_DATA, customData);

        return nms.asBukkitMirror();
    }

    public static boolean isDisguiseTool(ItemStack stack)
    {
        var nms = net.minecraft.world.item.ItemStack.fromBukkitCopy(stack);
        var customData = nms.getComponents().get(DataComponents.CUSTOM_DATA);

        if (customData == null || !customData.contains(SKILL_ACTIVATE_ITEM_KEY)) return false;

        return customData.copyTag().getBoolean(SKILL_ACTIVATE_ITEM_KEY);
    }

    /**
     * Check if the given {@link Material} is a continuous usable type (have consuming animation).
     * @param type {@link Material}
     * @return True if this type is continuous usable.
     */
    public static boolean isContinuousUsable(Material type)
    {
        return type == Material.BOW
                || type == Material.CROSSBOW
                || type == Material.TRIDENT
                || type == Material.SHIELD
                || type == Material.POTION
                || type == Material.MILK_BUCKET
                || type == Material.SPYGLASS
                || type.isEdible();
    }
}
