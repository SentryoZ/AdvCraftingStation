package me.sentryoz.advCraftingStation.gui;

import me.sentryoz.advCraftingStation.AdvCraftingStation;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static me.sentryoz.advCraftingStation.AdvCraftingStation.plugin;

public class GuiLoader {
    private Map<String, FileConfiguration> stationConfigs = new HashMap<>();

    public void loadStationConfigs() {
        File stationsDir = new File(plugin.getDataFolder(), "stations");

        if (!stationsDir.exists()) {
            plugin.getLogger().warning("No stations found.");
        }

        File[] configFiles = stationsDir.listFiles((dir, name) -> name.endsWith(".yml"));

        if (configFiles != null) {
            for (File configFile : configFiles) {
                String fileName = configFile.getName();
                String stationName = fileName.substring(0, fileName.length() - 4); // Remove ".yml"

                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                stationConfigs.put(stationName, config);

                plugin.getLogger().info("Loaded station config: " + stationName);
            }
        } else {
            plugin.getLogger().warning("No station configuration files found.");
        }
    }
}
