package me.sentryoz.advCraftingStation.gui;

import mc.obliviate.inventory.Gui;
import mc.obliviate.inventory.Icon;
import mc.obliviate.inventory.pagination.PaginationManager;
import me.sentryoz.advCraftingStation.action.ContainerAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.sentryoz.advCraftingStation.AdvCraftingStation.plugin;

public class CraftingGUI extends Gui {

    protected String station;

    protected HashMap<Integer, String> resultItems;
    protected HashMap<Integer, String> recipeItems;

    private FileConfiguration config;
    private final PaginationManager pagination = new PaginationManager(this);

    public CraftingGUI(Player player, String stationName) {
        super(player, "craftingStation", "Blank Station", 6);
        station = stationName;
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


    public void onOpen() {
        config = getConfig();

        assert config != null;

        String title = config.getString("title");
        //TODO: Implement page like "current page/total page"
        this.setTitle(title);

        int rows = config.getInt("rows");
        this.setSize(rows * 9);

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

    }

    private void buildItemSlot(String key) {
        ContainerAction action = ContainerAction.fromString(
                config.getString(key + ".action")
        );

        String slotsString = config.getString(key + ".slots");
        ArrayList<Integer> slots = calculateSlots(slotsString);

        ItemStack item = null;
        Icon icon;
        switch (action) {
            case PREVIEW_RESULT -> {
                item = new ItemStack(Material.PAPER);
            }
            case PREVIEW_ITEM -> {
                item = new ItemStack(Material.PAPER);
            }
            case CLOSE ->{
                item = buildItem(key);
                icon = new Icon(item);
                icon.onClick(event -> {
                    player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                });
            }
            case NEXT, PREV, NONE, INGREDIENT, CRAFT -> {
                item = buildItem(key);
            }
        }
        for (int slot : slots) {
            this.addItem(slot, item);
        }
    }

    private ItemStack buildItem(String key) {
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
}
