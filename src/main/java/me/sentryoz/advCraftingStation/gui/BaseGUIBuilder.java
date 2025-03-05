package me.sentryoz.advCraftingStation.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class BaseGUIBuilder implements InventoryHolder {

    protected final Player player;

    public BaseGUIBuilder(Player player) {
        this.player = player;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }


    public Player getPlayer() {
        return player;
    }

    public void open() {
        player.openInventory(getInventory());
    }
}
