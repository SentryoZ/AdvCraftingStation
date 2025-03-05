package me.sentryoz.advCraftingStation;

import me.sentryoz.advCraftingStation.gui.GuiLoader;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdvCraftingStation extends JavaPlugin {

    public static AdvCraftingStation plugin;
    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        // Load stations
        GuiLoader guiLoader = new GuiLoader();
        guiLoader.loadStationConfigs();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
