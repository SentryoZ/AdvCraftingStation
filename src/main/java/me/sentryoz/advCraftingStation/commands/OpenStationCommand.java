package me.sentryoz.advCraftingStation.commands;

import me.sentryoz.advCraftingStation.gui.CraftingGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OpenStationCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        String stationName = null;
        String playerName = null;
        if (args.length >= 1) {
            stationName = args[0];
        }
        if (args.length >= 2) {
            playerName = args[1];
        }

        if (stationName == null) {
            sender.sendMessage("You need provide station name");
            return false;
        }

        if (!(sender instanceof Player) && playerName == null) {
            sender.sendMessage("You need provide player name");
            return false;
        }
        Player target = null;

        if (playerName != null) {
            target = Bukkit.getPlayer(playerName);
            if (target == null) {
                sender.sendMessage("Player not found");
            }
        } else {
            target = ((Player) sender).getPlayer();
        }

        if (target == null) {
            return false;
        }

        CraftingGUI stationGUI = new CraftingGUI(target, stationName);

        if (stationGUI.canOpen()) {
            stationGUI.prepare();
            stationGUI.open();
        } else {
            return false;
        }

        return true;
    }
}
