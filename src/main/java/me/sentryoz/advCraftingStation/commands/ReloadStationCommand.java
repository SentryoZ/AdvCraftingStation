package me.sentryoz.advCraftingStation.commands;

import me.sentryoz.advCraftingStation.AdvCraftingStation;
import me.sentryoz.advCraftingStation.gui.GuiLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadStationCommand implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        GuiLoader guiLoader = new GuiLoader();
        guiLoader.loadStationConfigs();
        AdvCraftingStation.plugin.stationConfigs = guiLoader.getStationConfigs();
        return true;
    }
}
