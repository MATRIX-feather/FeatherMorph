package xyz.nifeather.morph.misc.recipe;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.utilities.ItemUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RecipeManager extends MorphPluginObject
{
    private final StandaloneYamlConfigManager configManager = new RecipeYamlConfigManager(new File(plugin.getDataFolder(), "recipes.yml"), "recipes.yml");

    private boolean allowCrafting = false;
    private boolean unShaped = false;
    private List<String> shape = new ObjectArrayList<>();
    private Map<String, String> materials = new Object2ObjectOpenHashMap<>();
    private String resultMaterialId = "~UNSET";
    private String resultName = "~UNSET";
    private List<String> resultLore = new ObjectArrayList<>();

    @Initializer
    private void load(MorphConfigManager configManager)
    {
        reload();
    }

    public void reload()
    {
        this.configManager.reload();

        readValuesFromConfig(this.configManager);
        prepareRecipe();
    }

    private void readValuesFromConfig(StandaloneYamlConfigManager configManager)
    {
        allowCrafting = configManager.getOrDefault(RecipeOptions.ALLOW_DISGUISE_TOOL_CRAFTING);
        unShaped = configManager.getOrDefault(RecipeOptions.DISGUISE_TOOL_CRAFTING_UNSHAPED);
        shape = configManager.getList(RecipeOptions.DISGUISE_TOOL_CRAFTING_SHAPE);
        resultMaterialId = configManager.getOrDefault(RecipeOptions.DISGUISE_TOOL_RESULT_MATERIAL);
        resultName = configManager.getOrDefault(RecipeOptions.DISGUISE_TOOL_RESULT_NAME);
        resultLore = configManager.getList(RecipeOptions.DISGUISE_TOOL_RESULT_LORE);
        var material = configManager.getMap(RecipeOptions.DISGUISE_TOOL_CRAFTING_MATERIALS);
        this.materials.clear();

        if (material != null)
            this.materials.putAll(material);
    }

    @Nullable
    private Material getMaterialFrom(String str)
    {
        var key = NamespacedKey.fromString(str);

        return Arrays.stream(Material.values())
                .parallel()
                .filter(m -> !m.isLegacy() && m.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public static final NamespacedKey SKILLITEM_CRAFTING_KEY = NamespacedKey.fromString("feathermorph:disguise_tool_crafting");

    private void prepareRecipe()
    {
        if (!allowCrafting)
        {
            Bukkit.removeRecipe(SKILLITEM_CRAFTING_KEY);
            return;
        }

        var minimessage = MiniMessage.miniMessage();

        Component name = this.resultName.equals("~UNSET") ? null : minimessage.deserialize(this.resultName);
        List<Component> loreComponents = this.resultLore.isEmpty() ? null : this.resultLore.parallelStream().map(minimessage::deserialize).toList();

        var resultMaterial = this.getMaterialFrom(this.resultMaterialId);
        if (resultMaterial == null)
        {
            logger.error("Invalid result material ID: '%s', skipping...".formatted(resultMaterialId));
            return;
        }

        Map<String, Material> materialsReal = new Object2ObjectOpenHashMap<>();
        this.materials.forEach((str, id) ->
        {
            var material = Arrays.stream(Material.values())
                    .filter(m -> m.key().equals(NamespacedKey.fromString(id)))
                    .findFirst()
                    .orElse(null);

            if (material == null)
            {
                logger.warn("Invalid material '%s', skipping...".formatted(id));
                return;
            }

            materialsReal.put(str, material);
        });

        var recipeProperty = new RecipeProperty(SKILLITEM_CRAFTING_KEY,
                !this.unShaped, this.shape,
                materialsReal,
                resultMaterial,
                name,
                loreComponents);

        buildAndAddRecipe(recipeProperty);
    }

    private void buildAndAddRecipe(RecipeProperty recipeProperty)
    {
        var resultItem = ItemUtils.buildDisguiseToolFrom(ItemStack.of(recipeProperty.resultMaterial()));
        resultItem.editMeta(meta ->
        {
            meta.setRarity(ItemRarity.UNCOMMON);
            meta.setEnchantmentGlintOverride(true);

            var name = recipeProperty.resultName();
            if (name != null)
                meta.itemName(name);

            var lore = recipeProperty.lore();
            if (lore != null && !lore.isEmpty())
                meta.lore(lore);
        });

        var key = recipeProperty.key();

        CraftingRecipe recipe;

        if (recipeProperty.shaped())
        {
            var shaped = new ShapedRecipe(key, resultItem);
            shaped.shape(recipeProperty.shape().toArray(new String[]{}));
            recipeProperty.materials().forEach((ch, material) -> shaped.setIngredient(ch.charAt(0), material));

            recipe = shaped;
        }
        else
        {
            var shapeless = new ShapelessRecipe(key, resultItem);
            recipeProperty.materials().forEach((ignored, material) -> shapeless.addIngredient(material));

            recipe = shapeless;
        }

        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe, true);
    }

    private void test_dumpExsampleConfig()
    {
        allowCrafting = true;
        unShaped = true;
        shape = List.of(
                "ABC",
                "DEF",
                "GHI"
        );
        resultMaterialId = Material.BEDROCK.key().asString();
        resultName = "<reset>技能物品";
        resultLore = List.of(
                "技能测试1", "技能测试2"
        );
        materials = new Object2ObjectOpenHashMap<>();
        materials.put("A", Material.BEDROCK.key().asString());
        materials.put("B", Material.ACACIA_BOAT.key().asString());
    }
}
