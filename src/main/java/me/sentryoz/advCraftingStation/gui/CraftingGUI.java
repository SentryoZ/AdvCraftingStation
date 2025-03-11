package me.sentryoz.advCraftingStation.gui;

import io.lumine.mythic.lib.api.item.NBTItem;
import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import mc.obliviate.inventory.advancedslot.AdvancedSlot;
import mc.obliviate.inventory.advancedslot.AdvancedSlotManager;
import mc.obliviate.inventory.pagination.PaginationManager;
import me.sentryoz.advCraftingStation.action.IconType;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.type.ItemStat;
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

import java.util.*;
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
    private final PaginationManager paginationManager = new PaginationManager(this);
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
        scheduler.scheduleAtFixedRate(() -> {
            errorMessage = null;
        }, 3,3, TimeUnit.SECONDS);

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

    private MMOItem getMMOItems(String type, String id) {
        type = type.toUpperCase();
        id = id.toUpperCase();
        return mmoItems.getMMOItem(MMOItems.plugin.getTypes().get(type), id);
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
            advancedSlot.onPrePutClick((inventoryClickEvent, eventItem) -> !checkIngredient(eventItem));
            advancedSlot.onPut((inventoryClickEvent, eventItem) -> {
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
        List<String> allowedStats = plugin.getConfig().getStringList("allowed_stats");

        for (int ingredientSlot : ingredientSlots) {
            Inventory inventory = getInventory();
            ItemStack ingredientItem = inventory.getItem(ingredientSlot);
            NBTItem nbtItem = NBTItem.get(ingredientItem);

            nbtItem.getStat("");
            for (String stat : allowedStats) {
                stat = stat.trim();
                double amount = nbtItem.getStat(stat);

                if (bonusMMOStats.containsKey(stat)) {
                    amount += bonusMMOStats.get(stat);
                }
                bonusMMOStats.put(stat, amount);
            }
        }

        // update preview item
        if (selectedRecipeKey != null) {
            String key = "recipes." + selectedRecipeKey;
            String type = config.getString(key + ".type");
            String id = config.getString(key + ".id");
            MMOItem mmoitem = getMMOItems(type, id);

            ItemStat itemStats = ItemStats.ITEM_COOLDOWN;

            ItemStack resultItem = mmoitem.newBuilder().build();
            if (resultItem == null) {
                return;
            }
            NBTItem nbtItem = NBTItem.get(resultItem);

            for (Integer slot : resultSlots) {
                Inventory inventory = getInventory();
                inventory.setItem(slot, resultItem);
            }

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

    private void sendErrorMessage(String message){
        if (errorMessage == null || !errorMessage.equals(message)) {
            player.sendMessage(message);
            errorMessage = message;
        }
    }
}
