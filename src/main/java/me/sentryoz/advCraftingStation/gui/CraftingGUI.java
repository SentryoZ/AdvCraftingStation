package me.sentryoz.advCraftingStation.gui;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import mc.obliviate.inventory.advancedslot.AdvancedSlot;
import mc.obliviate.inventory.advancedslot.AdvancedSlotManager;
import mc.obliviate.inventory.pagination.PaginationManager;
import me.sentryoz.advCraftingStation.action.IconType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.sentryoz.advCraftingStation.AdvCraftingStation.plugin;
import static org.bukkit.inventory.ItemFlag.*;

public class CraftingGUI extends Gui {

    protected String station;

    protected HashMap<Integer, String> resultItems;
    protected HashMap<Integer, String> recipeItems;

    protected String selectedRecipeKey = null;

    protected List<Integer> ingredientSlots = new ArrayList<>();
    protected List<Integer> resultSlots = new ArrayList<>();
    protected Multimap<Attribute, AttributeModifier> bonusVanillaStats =  ArrayListMultimap.create();
    protected HashMap<String, Integer> bonusMMOStats;
    protected HashMap<String, Integer> bonusMythicStats;

    private final FileConfiguration config;
    private final PaginationManager paginationManager = new PaginationManager(this);
    private final AdvancedSlotManager advancedSlotManager = new AdvancedSlotManager(this);

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
        FileConfiguration config = getConfig();
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
                        updateBonusStats();
                    });

                    this.paginationManager.addItem(icon);
                }
            });
        }
        paginationManager.update();

    }

    private @Nullable ItemStack buildRecipeItem(String key) {
        String type = config.getString(key + ".type");
        String id = config.getString(key + ".id");

        ItemStack item = null;

        if (type == null || type.equalsIgnoreCase("VANILLA")) {
            Material material;
            try {
                material = Material.valueOf(id);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().info("Recipe " + key + " in station" + station + " have empty unknown item id");
                return null;
            }
            item = new ItemStack(material);
        } else if (Objects.equals(type, "MYTHIC_MOBS")) {
            // TODO: Check mythic mob items
        } else {
            // TODO: Check mmo items
        }

        return item;
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
                appendPaginateSlot(slot);
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
            case PREVIEW_RESULT -> {
                icon.onClick(event -> {
                    player.sendMessage("Clicked preview result");
                });
            }
            case PREVIEW_ITEM -> {
                icon.onClick(event -> {
                    player.sendMessage("Clicked preview item");
                });
            }
            case CLOSE -> {
                icon.onClick(event -> {
                    player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                });
            }
            case NEXT -> {
                icon.onClick(event -> {
                    paginationManager.goNextPage();
                    paginationManager.update();
                });
            }
            case PREV -> {
                icon.onClick(event -> {
                    paginationManager.goPreviousPage();
                    paginationManager.update();
                });
            }
            case CRAFT -> {
                icon.onClick(event -> {
                    player.sendMessage("Clicked craft");
                });
            }
            case INGREDIENT -> {
                icon.onClick(event -> {
                    player.sendMessage("Clicked ingredient");
                });
            }
            case NONE -> {
                icon.onClick(event -> {
                    player.sendMessage("Clicked none");
                });
            }
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
            advancedSlot.onPut((inventoryClickEvent, eventItem) -> {
                if (plugin.mmoItemsEnabled) {
                    // TODO: Check allowed ingredient
                }
                if (plugin.mythicMobEnabled) {
                    // TODO: Check allowed ingredient
                }
                updateBonusStats();
            });
        }
    }

    private void appendPaginateSlot(int slot) {
        paginationManager.registerPageSlots(slot);
    }

    private ItemStack buildItem(String key) {
        IconType type = getType(key);

        if (type == IconType.PREVIEW_RESULT || type == IconType.PREVIEW_ITEM) {
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

    private void updateBonusStats() {
        bonusVanillaStats = ArrayListMultimap.create();
        for (int ingredientSlot : ingredientSlots) {
            updateVanillaStats(ingredientSlot);
        }

        // update preview item
        if (selectedRecipeKey != null) {
            String key = "recipes." + selectedRecipeKey;
            ItemStack resultItem = buildRecipeItem(key);

            if (resultItem == null) {
                return;
            }
            ItemMeta meta = resultItem.getItemMeta();
            meta.setAttributeModifiers(bonusVanillaStats);

            resultItem.setItemMeta(meta);

            for (Integer slot : resultSlots) {
                Inventory inventory = getInventory();
                inventory.setItem(slot, resultItem);
            }

        }

    }

    private void updateVanillaStats(int ingredientSlot) {
        Inventory inventory = getInventory();
        ItemStack ingredientItem = inventory.getItem(ingredientSlot);
        if (ingredientItem != null) {
            @Nullable Multimap<Attribute, AttributeModifier> attributeModifiers = ingredientItem.getItemMeta().getAttributeModifiers();

            if (attributeModifiers != null) {
                for (Map.Entry<Attribute, AttributeModifier> entry : attributeModifiers.entries()) {
                    Attribute key = entry.getKey();
                    AttributeModifier value = entry.getValue();
                    plugin.getLogger().info("Key: " + key.getKey());
                    plugin.getLogger().info("Value: " + value.getKey());
                    bonusVanillaStats.put(key, value);
                }
            }
        }
    }
}
