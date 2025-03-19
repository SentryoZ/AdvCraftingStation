package me.sentryoz.advCraftingStation.gui;

import io.lumine.mythic.lib.api.item.NBTItem;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import mc.obliviate.inventory.advancedslot.AdvancedSlot;
import mc.obliviate.inventory.advancedslot.AdvancedSlotManager;
import mc.obliviate.inventory.pagination.PaginationManager;
import me.sentryoz.advCraftingStation.action.IconType;
import me.sentryoz.advCraftingStation.util.InventoryUtil;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.sentryoz.advCraftingStation.AdvCraftingStation.plugin;
import static org.bukkit.inventory.ItemFlag.*;

public class CraftingGUI extends Gui {

    protected String station;

    protected String selectedRecipeKey = null;

    protected List<Integer> ingredientSlots = new ArrayList<>();
    protected List<Integer> resultSlots = new ArrayList<>();
    protected HashMap<String, Double> bonusMMOStats = new HashMap<>();

    private final FileConfiguration config;
    private final PaginationManager recipeSlotManager = new PaginationManager(this);
    private final PaginationManager materialSlotManager = new PaginationManager(this);

    private final AdvancedSlotManager advancedSlotManager = new AdvancedSlotManager(this);
    private String errorMessage = null;
    MMOItems mmoItems = MMOItems.plugin;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CraftingGUI(Player player, String stationName) {
        super(player, "craftingStation", "Blank Station", 6);
        station = stationName;
        config = getConfig();

    }

    public String getStation() {
        return station;
    }

    public @Nullable FileConfiguration getConfig() {
        return plugin.stationConfigs.get(station);
    }

    public boolean canOpen() {
        if (config == null) {
            String message = station + " do not existed";
            TextComponent textComponent = Component.text(message);
            plugin.getLogger().warning(message);
            player.sendMessage(textComponent);
            return false;
        }
        return true;
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        super.onOpen(event);
        scheduler.scheduleAtFixedRate(() -> {
            errorMessage = null;
        }, 3, 3, TimeUnit.SECONDS);

        // Build contents
        @Nullable ConfigurationSection contents = config.getConfigurationSection("contents");
        if (contents == null) {
            String message = station + "do not have any contents.";
            plugin.getLogger().warning(message);
            return;
        }

        contents.getKeys(false).forEach(key -> {
            String configKey = "contents." + key;
            buildItemSlot(configKey);

        });


        // Create recipe list
        @Nullable ConfigurationSection recipes = config.getConfigurationSection("recipes");
        if (recipes == null) {
            String message = station + "do not have any recipes.";
            plugin.getLogger().warning(message);
        } else {
            recipes.getKeys(false).forEach(key -> {
                String configKey = "recipes." + key;
                ItemStack recipeItem = buildRecipeItem(configKey);
                if (recipeItem != null) {
                    Icon icon = new Icon(recipeItem);
                    icon.onClick(clickEvent -> {
                        selectedRecipeKey = key;
                        updateGuiData();
                    });

                    this.recipeSlotManager.addItem(icon);
                }
            });
        }
        recipeSlotManager.update();

    }

    private MMOItem getMMOItems(String type, String id) {
        type = type.toUpperCase();
        id = id.toUpperCase();
        return mmoItems.getMMOItem(MMOItems.plugin.getTypes().get(type), id);
    }

    private MMOItem getMMOItems(ItemStack item) {

        NBTItem nbtItem = NBTItem.get(item);

        String type = nbtItem.getType();
        String id = nbtItem.getString("MMOITEMS_ITEM_ID");

        if (type == null || id == null) {
            return null;
        }

        return getMMOItems(type, id);
    }

    private @Nullable ItemStack buildRecipeItem(String key) {
        String type = config.getString(key + ".type");
        String id = config.getString(key + ".id");

        MMOItem mmoItem = getMMOItems(type, id);
        if (mmoItem == null) {
            plugin.getLogger().warning("Could not find item " + type + " with id " + id);
            return null;
        }

        return mmoItem.newBuilder().build();
    }

    public void prepare() {
        String title = config.getString("title");
        //TODO: Implement page like "current page/total page"
        this.setTitle(title);

        int rows = config.getInt("rows");
        this.setSize(rows * 9);
    }

    private void buildItemSlot(String key) {

        String slotsString = config.getString(key + ".slots");
        ArrayList<Integer> slots = calculateSlots(slotsString);

        ItemStack item = buildItem(key);
        for (int slot : slots) {
            if (isAdvancedSlot(key)) {
                buildAdvancedSlot(slot, key, item);
            } else if (isPreviewItemSlot(key)) {
                appendRecipeSlot(slot);
            } else if (getType(key) == IconType.PREVIEW_MATERIAL) {
                appendMaterialSlot(slot);
            } else {
                if (getType(key) == IconType.PREVIEW_RESULT) {
                    resultSlots.add(slot);
                }
                Icon icon = buildGuiIcon(key, item);
                addItem(slot, icon);
            }
        }
    }

    private Icon buildGuiIcon(String key, ItemStack item) {

        IconType type = getType(key);

        Icon icon = new Icon(item);
        switch (type) {
            case CLOSE -> icon.onClick(event -> {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            });
            case NEXT -> icon.onClick(event -> {
                recipeSlotManager.goNextPage();
                recipeSlotManager.update();
            });
            case PREV -> icon.onClick(event -> {
                recipeSlotManager.goPreviousPage();
                recipeSlotManager.update();
            });
            case CRAFT -> icon.onClick(event -> {
                craftItem();
            });
        }

        return icon;
    }

    private boolean isAdvancedSlot(String key) {
        return getType(key) == IconType.INGREDIENT;
    }

    private boolean isPreviewItemSlot(String key) {
        return getType(key) == IconType.PREVIEW_ITEM;
    }

    private void buildAdvancedSlot(int slot, String key, ItemStack itemStack) {
        IconType type = getType(key);
        AdvancedSlot advancedSlot = advancedSlotManager.addAdvancedIcon(slot, new Icon(itemStack));
        if (type == IconType.INGREDIENT) {
            ingredientSlots.add(slot);
            advancedSlot.onPrePutClick((inventoryClickEvent, eventItem) -> !checkIngredient(eventItem));
            advancedSlot.onPut((inventoryClickEvent, eventItem) -> updateGuiData());
        }
    }

    private void appendRecipeSlot(int slot) {
        recipeSlotManager.registerPageSlots(slot);
    }

    private void appendMaterialSlot(int slot) {
        materialSlotManager.registerPageSlots(slot);
    }

    private ItemStack buildItem(String key) {
        IconType type = getType(key);

        if (type == IconType.PREVIEW_RESULT || type == IconType.PREVIEW_ITEM || type == IconType.PREVIEW_MATERIAL) {
            return new ItemStack(Material.AIR);
        }

        ItemStack item;

        // Material
        Material material = Material.matchMaterial(config.getString(key + ".material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }
        item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Amount
        int id = config.getInt(key + ".amount", 1);
        item.setAmount(id);

        // Display name
        String name = config.getString(key + ".name");
        if (name != null) {
            TextComponent nameComponent = Component.text(name);
            meta.customName(nameComponent);
        }

        // Lore
        if (config.isList(key + ".lore")) {
            List<String> lore = config.getStringList(key + ".lore");
            List<TextComponent> loreComponents = new ArrayList<>();
            for (String line : lore) {
                TextComponent lineComponent = Component.text(line);
                loreComponents.add(lineComponent);
            }
            meta.lore(loreComponents);
        }


        // Custom model data
        int customModelData = config.getInt(key + ".customModelData");
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        meta.setHideTooltip(true);
        item.addItemFlags(
                HIDE_ENCHANTS,
//                HIDE_ATTRIBUTES,
                HIDE_UNBREAKABLE,
                HIDE_DESTROYS,
                HIDE_PLACED_ON,
                HIDE_ADDITIONAL_TOOLTIP,
                HIDE_DYE,
                HIDE_ARMOR_TRIM,
                HIDE_STORED_ENCHANTS
        );

        item.setItemMeta(meta);
        return item;
    }

    private ArrayList<Integer> calculateSlots(String slotString) {
        ArrayList<Integer> slots = new ArrayList<>();
        if (slotString == null || slotString.isEmpty()) {
            return slots;
        }

        String[] ranges = slotString.split(",");
        for (String range : ranges) {
            if (range.contains("-")) {
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    try {
                        int start = Integer.parseInt(parts[0].trim());
                        int end = Integer.parseInt(parts[1].trim());
                        if (start <= end) {
                            for (int i = start; i <= end; i++) {
                                slots.add(i);
                            }
                        }
                    } catch (NumberFormatException e) {
                        String message = "Invalid slot range format: " + range;
                        TextComponent textComponent = Component.text(message);
                        plugin.getLogger().warning(message);
                        player.sendMessage(textComponent);
                    }
                } else {
                    String message = "Invalid slot range format: " + range;
                    TextComponent textComponent = Component.text(message);
                    plugin.getLogger().warning(message);
                    player.sendMessage(textComponent);
                }
            } else {
                try {
                    int slot = Integer.parseInt(range.trim());
                    slots.add(slot);
                } catch (NumberFormatException e) {
                    String message = "Invalid slot number format: " + range;
                    TextComponent textComponent = Component.text(message);
                    plugin.getLogger().warning(message);
                    player.sendMessage(textComponent);
                }
            }
        }
        return slots;
    }


    private IconType getType(String key) {
        return IconType.fromString(
                config.getString(key + ".type")
        );
    }

    private MMOItem createMMOItem() {
        List<String> allowedStats = plugin.getConfig().getStringList("allowed_stats");

        String key = "recipes." + selectedRecipeKey;
        String type = config.getString(key + ".type");
        String id = config.getString(key + ".id");

        MMOItem mmoItem = getMMOItems(type, id);
        Type mmoItemType = mmoItem.getType();
        Inventory inventory = getInventory();

        List<String> appliedItems = new ArrayList<>();
        for (int ingredientSlot : ingredientSlots) {
            ItemStack ingredientItem = inventory.getItem(ingredientSlot);
            if (ingredientItem == null) {
                continue;
            }
            MMOItem mmoIngredientItem = getMMOItems(ingredientItem);
            if (mmoIngredientItem == null) {
                continue;
            }
            String ingredientItemType = mmoIngredientItem.getType().getId();
            String ingredientItemId = mmoIngredientItem.getId();

            String arrayKey = String.join("_", ingredientItemType, ingredientItemId);

            boolean allowedDuplicate = plugin.getConfig().getBoolean("allowed_duplicates", false);
            if (appliedItems.contains(arrayKey) && !allowedDuplicate) {
                continue;
            }
            mmoIngredientItem.getStats().forEach(itemStat -> {
                if (!allowedStats.contains(itemStat.getId())) {
                    return;
                }
                if (!itemStat.isCompatible(mmoItemType)) {
                    return;
                }

                StatData statData = mmoIngredientItem.getData(itemStat);
                plugin.getLogger().info(itemStat.getId() + ": " + statData.toString());
                if (mmoItem.hasData(itemStat)) {
                    mmoItem.mergeData(itemStat, statData, null);
                } else {
                    mmoItem.setData(itemStat, statData);
                }
            });
            appliedItems.add(arrayKey);
        }

        return mmoItem;
    }

    private void updateGuiData() {
        if (selectedRecipeKey == null) {
            return;
        }

        Inventory inventory = getInventory();
        ItemStack resultItem = createMMOItem().newBuilder().build();
        for (Integer slot : resultSlots) {
            inventory.setItem(slot, resultItem);
        }


        //build material list
        List<String> materials = plugin.getConfig().getStringList("recipes." + selectedRecipeKey + ".materials");
        for (String materialString : materials) {
            HashMap<String, String> materialData = implodeRecipes(materialString);
            if (materialData == null) continue;

            String id = materialData.get("id");
            String type = materialData.get("type");
            int amount = Integer.parseInt(materialData.get("amount"));

            ItemStack materialResultItem = null;
            if (type.equalsIgnoreCase("vanilla")) {
                Material material = Material.getMaterial(id);
                if (material == null) continue;

                materialResultItem = new ItemStack(material, amount);
            } else {
                MMOItem mmoItem = getMMOItems(type, id);
                if (mmoItem == null) continue;
                materialResultItem = mmoItem.newBuilder().build();
                materialResultItem.setAmount(amount);
            }

            Icon icon = new Icon(materialResultItem);
            materialSlotManager.addItem(icon);
        }
    }

    private boolean checkIngredient(ItemStack ingredient) {
        if (ingredient == null) {
            return false;
        }
        NBTItem nbtItem = NBTItem.get(ingredient);
        String type = nbtItem.getType();

        List<String> allowedTypes = plugin.getConfig().getStringList("allowed_types");

        if (type == null) {
            return false;
        }

        if (allowedTypes.contains(type)) {
            return true;
        }
        String message = "Allowed ingredient types: " + String.join(", ", allowedTypes).toLowerCase();
        sendErrorMessage(message);
        return false;
    }

    private void sendErrorMessage(String message) {
        if (errorMessage == null || !errorMessage.equals(message)) {
            player.sendMessage(message);
            errorMessage = message;
        }
    }

    private void craftItem() {
        if (selectedRecipeKey == null) {
            return;
        }
        String key = "recipes." + selectedRecipeKey;

        boolean canCraft = true;
        List<String> materials = config.getStringList(key + ".materials");
        for (String material : materials) {
            String[] materialData = material.split(",");
            if (materialData.length < 2) {
                return;
            }
            String type = materialData[0];
            String id = materialData[1];
            int amount = 1;
            if (materialData.length == 3) {
                try {
                    amount = Integer.parseInt(materialData[2]);
                } catch (NumberFormatException ignored) {
                }
            }
            boolean haveItem;
            if (type.equalsIgnoreCase("vanilla")) {
                haveItem = InventoryUtil.canTakeVanillaItems(player, id, amount);
            } else {
                haveItem = InventoryUtil.canTakeMMOItems(player, type, id, amount);
            }
            if (!haveItem) {
                canCraft = false;
                player.sendMessage("You don't have enough items in your inventory.");
                player.sendMessage(type, id);
            }
        }
        if (!canCraft) {
            return;
        }
        for (String material : materials) {
            HashMap<String, String> materialData = implodeRecipes(material);
            if (materialData == null) return;

            String id = materialData.get("id");
            String type = materialData.get("type");
            int amount = Integer.parseInt(materialData.get("amount"));

            if (type.equalsIgnoreCase("vanilla")) {
                InventoryUtil.takeVanillaItems(player, id, amount);
            } else {
                InventoryUtil.takeMMOItems(player, type, id, amount);
            }
        }
        Inventory inventory = getInventory();
        List<String> reducedMaterials = new ArrayList<>();
        for (int ingredientSlot : ingredientSlots) {
            ItemStack ingredientItem = inventory.getItem(ingredientSlot);
            if (ingredientItem == null) {
                continue;
            }
            NBTItem nbtItem = NBTItem.get(ingredientItem);
            String type = nbtItem.getType();
            if (type == null) {
                continue;
            }

            boolean allowedDuplicate = plugin.getConfig().getBoolean("allowed_duplicates", false);

            String id = nbtItem.getString("MMOITEMS_ITEM_ID");
            String arrayKey = String.join("_", type, id);

            if (reducedMaterials.contains(arrayKey) && !allowedDuplicate) {
                continue;
            }
            int currentAmount = ingredientItem.getAmount();
            ingredientItem.setAmount(currentAmount - 1);
            inventory.setItem(ingredientSlot, ingredientItem);

            reducedMaterials.add(arrayKey);
        }
        MMOItem mmoItem = createMMOItem();
        ItemStack resultItem = mmoItem.newBuilder().build();

        int amount = config.getInt(key + ".amount", 1);
        assert resultItem != null;
        resultItem.setAmount(amount);
        player.give(resultItem);

        updateGuiData();
    }

    private HashMap<String, String> implodeRecipes(String recipeString) {
        String[] materialData = recipeString.split(",");
        if (materialData.length < 2) {
            return null;
        }
        String type = materialData[0];
        String id = materialData[1];
        int amount = 1;
        if (materialData.length == 3) {
            try {
                amount = Integer.parseInt(materialData[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        HashMap<String, String> ingredients = new HashMap<>();
        ingredients.put("id", id);
        ingredients.put("type", type);
        ingredients.put("amount", String.valueOf(amount));

        return ingredients;
    }
}
