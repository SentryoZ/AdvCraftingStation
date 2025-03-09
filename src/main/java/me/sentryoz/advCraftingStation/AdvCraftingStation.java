package me.sentryoz.advCraftingStation;

import mc.obliviate.inventory.InventoryAPI;
import me.sentryoz.advCraftingStation.commands.OpenStationCommand;
import me.sentryoz.advCraftingStation.commands.ReloadStationCommand;
import me.sentryoz.advCraftingStation.gui.GuiLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AdvCraftingStation extends JavaPlugin {

    public static AdvCraftingStation plugin;
    public boolean mmoItemsEnabled = false;
    public boolean mythicMobEnabled = false;
    public Map<String, FileConfiguration> stationConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        saveDefaultConfig();
        mmoItemsEnabled = checkSoftDependenciesPlugin("MMOItems");
        mythicMobEnabled = checkSoftDependenciesPlugin("MythicMobs");
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


    private boolean checkSoftDependenciesPlugin(String name) {
        // Check if server have MMOItems
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        if (plugin != null) {
            this.getLogger().info(name + " plugin found.");
            return true;
        } else {
            this.getLogger().info(name + " plugin not found.");
            return false;
        }
    }
}
