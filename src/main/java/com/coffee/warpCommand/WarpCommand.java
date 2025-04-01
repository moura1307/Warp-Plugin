package com.coffee.warpCommand;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final Map<String, Location> savedLocations = new HashMap<>();
    private final JavaPlugin plugin;

    // Constants for messages
    private static final String USAGE_MESSAGE =
            "§6Usage:§f /warp <§e list | rename | remove | set | to> [name] §f>";

    private static final String PLAYER_ONLY_MESSAGE = "§cThis command is for players only!";

    public WarpCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadLocations();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PLAYER_ONLY_MESSAGE);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "set": handleSet(player, args); break;
            case "remove": handleRemove(player, args); break;
            case "to": handleTeleport(player, args); break;
            case "list": handleList(player); break;
            case "rename": handleRename(player, args); break;
            default: sendUsage(player);
        }
        return true;
    }

    // ========== COMMAND HANDLERS ==========
    private void handleSet(Player player, String[] args) {
        if (!validateArgs(player, args, 2, "§6Usage:§f /warp set <§ename§f>")) return;

        String key = buildKey(player, args[1]);
        if (savedLocations.containsKey(key)) {
            player.sendMessage("§cLocation '§e" + args[1] + "§c' already exists!");
            return;
        }

        Location loc = getBlockCenterLocation(player.getLocation());
        savedLocations.put(key, loc);
        player.sendMessage("§aLocation '§e" + args[1] + "§a' saved!");
        saveLocations();
    }
    private void handleRemove(Player player, String[] args) {
        if (!validateArgs(player, args, 2, "§6Usage:§f /warp remove <§ename§f>")) return;

        String key = buildKey(player, args[1]);
        if (!savedLocations.containsKey(key)) {
            player.sendMessage("§cLocation '§e" + args[1] + "§c' not found!");
            return;
        }

        savedLocations.remove(key);
        player.sendMessage("§aLocation '§e" + args[1] + "§a' removed!");
        saveLocations();
    }

    private void handleTeleport(Player player, String[] args) {
        if (!validateArgs(player, args, 2, "§6Usage:§f /warp to <§ename§f>")) return;

        String key = buildKey(player, args[1]);
        Location loc = savedLocations.get(key);
        if (loc == null || loc.getWorld() == null) {
            player.sendMessage("§cLocation '§e" + args[1] + "§c' is invalid!");
            return;
        }

        player.teleport(loc);
        player.sendMessage("§aTeleported to '§e" + args[1] + "§a'!");
    }

    private void handleList(Player player) {
        List<String> locations = getPlayerLocations(player);
        if (locations.isEmpty()) {
            player.sendMessage("§cYou have no saved locations!");
            return;
        }

        player.sendMessage("§6Your saved locations:");
        locations.forEach(loc -> player.sendMessage("§7- §e" + loc));
    }

    private void handleRename(Player player, String[] args) {
        if (!validateArgs(player, args, 3, "§6Usage:§f /warp rename <§eold§f> <§enew§f>")) return;

        String oldKey = buildKey(player, args[1]);
        String newKey = buildKey(player, args[2]);

        if (!savedLocations.containsKey(oldKey)) {
            player.sendMessage("§cLocation '§e" + args[1] + "§c' not found!");
            return;
        }

        if (savedLocations.containsKey(newKey)) {
            player.sendMessage("§cLocation '§e" + args[2] + "§c' already exists!");
            return;
        }

        Location loc = savedLocations.get(oldKey);
        savedLocations.remove(oldKey);
        savedLocations.put(newKey, loc);
        player.sendMessage("§aRenamed '§e" + args[1] + "§a' to '§e" + args[2] + "§a'!");
        saveLocations();
    }

    private Location getBlockCenterLocation(Location original) {
        Location blockCenter = original.getBlock().getLocation()
                .add(0.5, 0, 0.5); // Center of block
        blockCenter.setYaw(calculateSnappedYaw(original.getYaw()));
        blockCenter.setPitch(1);
        return blockCenter;
    }

    private float calculateSnappedYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360; // Normalize
        return (Math.round(yaw / 45) * 45) % 360; // Snap to 45-degree increments
    }

    // [Keep all other handlers (remove, teleport, list, rename) unchanged from previous version]

    // ========== PERSISTENCE ==========
    public void saveLocations() {
        ConfigurationSection section = plugin.getConfig().createSection("warps");
        savedLocations.forEach((key, loc) ->
                section.set(key, loc.serialize())
        );
        plugin.saveConfig();
    }

    public void loadLocations() {
        savedLocations.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("warps");
        if (section == null) return;

        section.getKeys(false).forEach(key -> {
            Location loc = Location.deserialize(section.getConfigurationSection(key).getValues(false));
            if (loc.getWorld() != null) {
                savedLocations.put(key, loc);
            }
        });
    }

    // ========== TAB COMPLETION ==========
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;

        if (args.length == 1) {
            return filterCompletions(args[0], Arrays.asList("set", "remove", "rename", "to", "list"));
        }

        if (args.length == 2) {
            List<String> locations = getPlayerLocations(player);
            return filterCompletions(args[1], locations);
        }
        return Collections.emptyList();
    }

    // ========== HELPERS ==========
    private List<String> getPlayerLocations(Player player) {
        List<String> locations = new ArrayList<>();
        String prefix = player.getName().toLowerCase() + ":";
        savedLocations.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> key.substring(prefix.length()))
                .forEach(locations::add);
        return locations;
    }

    private String buildKey(Player player, String locationName) {
        return player.getName().toLowerCase() + ":" + locationName.toLowerCase();
    }

    private boolean validateArgs(Player player, String[] args, int required, String errorMessage) {
        if (args.length >= required) return true;
        player.sendMessage(errorMessage);
        return false;
    }

    private void sendUsage(Player player) {
        player.sendMessage(USAGE_MESSAGE);
    }

    private List<String> filterCompletions(String input, List<String> options) {
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(input.toLowerCase()))
                .sorted()
                .toList();
    }
}