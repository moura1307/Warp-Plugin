package com.coffee.warpCommand;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final Map<String, Location> savedLocations = new HashMap<>();
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public WarpCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();  // Use the plugin's config file
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This is a player-only command.");
            return true;
        }

        Player player = (Player) commandSender;

        if (args.length == 0) {
            // If no arguments are provided, show the list of available subcommands
            player.sendMessage("/warp list - List all your saved locations");
            player.sendMessage("/warp remove <name> - Remove a saved location");
            player.sendMessage("/warp rename <oldName> <newName> - Rename a saved location");
            player.sendMessage("/warp set <name> - Save your current location with the given name");
            player.sendMessage("/warp to <name> - Teleports you to given location");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /warp set <name>");
                return true;
            }

            String locationName = args[1];
            String key = player.getName() + ":" + locationName;

            if (savedLocations.containsKey(key)) {
                player.sendMessage("A location with the name '" + locationName + "' already exists! Please choose a different name.");
                return true;
            }

            savedLocations.put(key, player.getLocation());
            player.sendMessage("Location '" + locationName + "' has been saved!");
            saveLocations();  // Save locations after setting new one
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /warp remove <name>");
                return true;
            }

            String locationName = args[1];
            String key = player.getName() + ":" + locationName;

            if (savedLocations.containsKey(key)) {
                savedLocations.remove(key);
                config.set(key, null); // Remove location from the config
                player.sendMessage("Location '" + locationName + "' has been removed!");
                saveLocations();  // Save the updated locations to the config
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
        } else if (args[0].equalsIgnoreCase("to")) {
            String locationName = args[1];
            Location location = savedLocations.get(player.getName() + ":" + locationName);
            if (location != null) {
                player.teleport(location);
                player.sendMessage("Teleported to '" + locationName + "'!");
            } else {
                player.sendMessage("No location found with the name '" + locationName + "'.");
            }
        } else if (args[0].equalsIgnoreCase("rename")) {
            if (args.length < 3) {
                player.sendMessage("Usage: /warp rename <oldName> <newName>");
                return true;
            }

            String oldName = args[1];
            String newName = args[2];

            String oldKey = player.getName() + ":" + oldName;
            String newKey = player.getName() + ":" + newName;

            // Check if the old location exists
            if (!savedLocations.containsKey(oldKey)) {
                player.sendMessage("Location '" + oldName + "' does not exist!");
                return true;
            }

            // Check if the new name is already taken
            if (savedLocations.containsKey(newKey)) {
                player.sendMessage("Location with the name '" + newName + "' already exists!");
                return true;
            }

            // Rename the location
            Location location = savedLocations.get(oldKey);
            savedLocations.remove(oldKey);  // Remove the old location
            savedLocations.put(newKey, location);  // Save with the new name
            player.sendMessage("Location '" + oldName + "' has been renamed to '" + newName + "'!");
            saveLocations();  // Save the updated locations to the config
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length == 1) {
                // Show subcommands for the first argument
                return Arrays.asList("list", "remove", "rename", "set", "to");
            } else if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("rename")) {
                if (args.length == 2) {
                    // Suggest saved location names for /warp set <name>, /warp remove <name>, or /warp rename <oldName>
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
                } else if (args[0].equalsIgnoreCase("rename") && args.length == 3) {
                    // Suggest saved location names for the second argument of /warp rename <oldName> <newName>, excluding the first argument
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        List<String> playerLocations = new ArrayList<>();
                        String oldName = args[1]; // The first argument is the old location name

                        // Add all saved locations except the one provided in the first argument (oldName)
                        for (String key : savedLocations.keySet()) {
                            if (key.startsWith(player.getName() + ":")) {
                                String locationName = key.split(":")[1];
                                if (!locationName.equalsIgnoreCase(oldName)) {
                                    playerLocations.add(locationName);
                                }
                            }
                        }
                        return playerLocations; // Return locations excluding the oldName
                    }
                }
            } else if (args[0].equalsIgnoreCase("to") && args.length == 2) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<String> playerLocations = new ArrayList<>();
                    for (String key : savedLocations.keySet()) {
                        if (key.startsWith(player.getName() + ":")) {
                            playerLocations.add(key.split(":")[1]);
                        }
                    }
                    return playerLocations;  // Suggest saved locations for /warp to <name>
                }
            }
        }
        return null; // Let Minecraft handle default completions
    }
    public void saveLocations() {
        for (String key : savedLocations.keySet()) {
            Location location = savedLocations.get(key);
            config.set(key + ".world", location.getWorld().getName());
            config.set(key + ".x", location.getX());
            config.set(key + ".y", location.getY());
            config.set(key + ".z", location.getZ());
            config.set(key + ".yaw", location.getYaw());
            config.set(key + ".pitch", location.getPitch());
        }
        plugin.saveConfig();
    }

    // Load the locations from the config file on server startup
    public void loadLocations() {
        savedLocations.clear();  // Clear existing locations before loading
        FileConfiguration config = plugin.getConfig();
        Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            if (key.contains(":")) {
                String worldName = config.getString(key + ".world");
                double x = config.getDouble(key + ".x");
                double y = config.getDouble(key + ".y");
                double z = config.getDouble(key + ".z");
                float yaw = (float) config.getDouble(key + ".yaw");
                float pitch = (float) config.getDouble(key + ".pitch");
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                savedLocations.put(key, location);
            }
        }
    }
}