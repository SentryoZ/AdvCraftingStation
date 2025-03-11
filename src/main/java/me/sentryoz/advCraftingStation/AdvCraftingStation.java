package me.sentryoz.advCraftingStation;

import mc.obliviate.inventory.InventoryAPI;
import me.sentryoz.advCraftingStation.commands.OpenStationCommand;
import me.sentryoz.advCraftingStation.commands.ReloadStationCommand;
import me.sentryoz.advCraftingStation.gui.GuiLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AdvCraftingStation extends JavaPlugin {

    public static AdvCraftingStation plugin;
    public Map<String, FileConfiguration> stationConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        saveDefaultConfig();

        new InventoryAPI(this).init();

        // Load stations
        GuiLoader guiLoader = new GuiLoader();
        guiLoader.loadStationConfigs();
        stationConfigs = guiLoader.getStationConfigs();
        Objects.requireNonNull(this.getCommand("open-station")).setExecutor(new OpenStationCommand());
        Objects.requireNonNull(this.getCommand("reload-station")).setExecutor(new ReloadStationCommand());
        //
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
