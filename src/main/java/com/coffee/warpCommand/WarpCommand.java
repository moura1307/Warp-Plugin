package com.coffee.warpCommand;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final Map<String, Location> savedLocations = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This is a player-only command.");
            return true;
        }

        Player player = (Player) commandSender;

        if (args.length == 0) {
            // If no arguments are provided, show the list of available subcommands
            player.sendMessage("/warp set <name> - Save your current location with the given name");
            player.sendMessage("/warp remove <name> - Remove a saved location");
            player.sendMessage("/warp list - List all your saved locations");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /warp set <name>");
                return true;
            }
            String locationName = args[1];
            savedLocations.put(player.getName() + ":" + locationName, player.getLocation());
            player.sendMessage("Location '" + locationName + "' has been saved!");
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /warp remove <name>");
                return true;
            }

            String locationName = args[1];
            String key = player.getName() + ":" + locationName;

            if (savedLocations.containsKey(key)) {
                savedLocations.remove(key);
                player.sendMessage("Location '" + locationName + "' has been removed!");
            } else {
                player.sendMessage("Location '" + locationName + "' does not exist!");
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            List<String> playerLocations = new ArrayList<>();
            for (String key : savedLocations.keySet()) {
                if (key.startsWith(player.getName() + ":")) {
                    playerLocations.add(key.split(":")[1]);
                }
            }

            if (playerLocations.isEmpty()) {
                player.sendMessage("You have no saved locations!");
            } else {
                player.sendMessage("Your saved locations:");
                for (String location : playerLocations) {
                    player.sendMessage("- " + location);
                }
            }
        } else {
            String locationName = args[0];
            Location location = savedLocations.get(player.getName() + ":" + locationName);
            if (location != null) {
                player.teleport(location);
                player.sendMessage("Teleported to '" + locationName + "'!");
            } else {
                player.sendMessage("No location found with the name '" + locationName + "'.");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length == 1) {
                return Arrays.asList("set", "list", "remove");
            } else if ((args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) && args.length == 2) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<String> playerLocations = new ArrayList<>();
                    for (String key : savedLocations.keySet()) {
                        if (key.startsWith(player.getName() + ":")) {
                            playerLocations.add(key.split(":")[1]);
                        }
                    }
                    return playerLocations;
                }
            }
        }
        return null; // Let Minecraft handle default completions
    }
}
