package me.sentryoz.advCraftingStation.util;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtil {
    public static boolean canTakeMMOItems(Player player, String type, String id, int amount) {
        PlayerInventory inventory = player.getInventory();
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack inventoryItem = inventory.getItem(slotIndex);
            NBTItem nbtItem = NBTItem.get(inventoryItem);
            String itemType = nbtItem.getType();
            if (itemType == null) {
                continue;
            }
            String itemId = nbtItem.getString("MMOITEMS_ITEM_ID");
            if (
                    itemType.equalsIgnoreCase(type) &&
                            itemId.equalsIgnoreCase(id) &&
                            inventoryItem != null
            ) {
                int itemAmount = inventoryItem.getAmount();
                if (itemAmount >= amount) {
                    return true;
                } else {
                    amount -= inventoryItem.getAmount();
                }
            }
        }
        return false;
    }

    public static void takeMMOItems(Player player, String type, String id, int amount) {
        PlayerInventory inventory = player.getInventory();
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack inventoryItem = inventory.getItem(slotIndex);
            NBTItem nbtItem = NBTItem.get(inventoryItem);
            String itemType = nbtItem.getType();
            if (itemType == null) {
                continue;
            }
            String itemId = nbtItem.getString("MMOITEMS_ITEM_ID");
            if (
                    itemType.equalsIgnoreCase(type) &&
                            itemId.equalsIgnoreCase(id) &&
                            inventoryItem != null
            ) {
                int itemAmount = inventoryItem.getAmount();
                if (itemAmount >= amount) {
                    inventoryItem.setAmount(itemAmount - amount);
                    inventory.setItem(slotIndex, inventoryItem);
                    return;
                } else {
                    inventoryItem.setAmount(0);
                    amount -= itemAmount;
                    inventory.setItem(slotIndex, inventoryItem);
                }
            }
        }
    }

    public static void takeVanillaItems(Player player, String id, int amount) {
        PlayerInventory inventory = player.getInventory();
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack inventoryItem = inventory.getItem(slotIndex);
            if (inventoryItem == null) {
                continue;
            }

            if (id.equalsIgnoreCase(inventoryItem.getType().toString())) {
                int itemAmount = inventoryItem.getAmount();
                if (itemAmount >= amount) {
                    inventoryItem.setAmount(itemAmount - amount);
                    return;
                } else {
                    amount -= inventoryItem.getAmount();
                    inventoryItem.setAmount(0);
                }
            }
        }
    }

    public static boolean canTakeVanillaItems(Player player, String id, int amount) {
        PlayerInventory inventory = player.getInventory();
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack inventoryItem = inventory.getItem(slotIndex);
            if (inventoryItem == null) {
                continue;
            }

            if (id.equalsIgnoreCase(inventoryItem.getType().toString())) {
                int itemAmount = inventoryItem.getAmount();
                if (itemAmount >= amount) {
                    return true;
                } else {
                    amount -= inventoryItem.getAmount();
                }
            };
        }

        return false;
    }
}
