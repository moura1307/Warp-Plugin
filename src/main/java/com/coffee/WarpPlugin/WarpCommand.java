package com.coffee.WarpPlugin;

import com.coffee.WarpPlugin.CommandManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class WarpCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Map<String, Location> publicWarps = new HashMap<>();
    private final Map<String, Location> privateWarps = new HashMap<>();

    private static final String MAIN_USAGE = "§6Usage:§f /warp <list | set | remove | rename | to> [name] [public]";
    private static final String PLAYER_ONLY = "§cThis command is for players only!";

    public WarpCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (CommandManager.isWarpOpOnlyMode() && !sender.isOp()) {
            sender.sendMessage("§cThis command is restricted to server operators!");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(PLAYER_ONLY);
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (!isSubcommandAllowed(player, sub)) {
            player.sendMessage("§cThis command is disabled in public-only mode!");
            return true;
        }

        switch (sub) {
            case "set": handleSet(player, args); break;
            case "remove": handleRemove(player, args); break;
            case "to": handleTeleport(player, args); break;
            case "list": handleList(player); break;
            case "rename": handleRename(player, args); break;
            default: sendUsage(player);
        }
        return true;
    }

    private void handleSet(Player player, String[] args) {
        if (!validateArgs(player, args, 2, "§6Usage:§f /warp set <name> [public]")) return;

        boolean isPublic = args.length > 2 && args[2].equalsIgnoreCase("public");
        if (isPublic && !player.isOp() && !player.hasPermission("warp.admin")) {
            player.sendMessage("§cOnly staff can create public warps!");
            return;
        }

        String warpName = args[1].toLowerCase();
        String publicCheckKey = buildKey(player, warpName, true);
        if (publicWarps.containsKey(publicCheckKey)) {
            player.sendMessage("§cA public warp named '§e" + warpName + "§c' already exists!");
            return;
        }

        String key = buildKey(player, warpName, isPublic);
        Map<String, Location> targetMap = isPublic ? publicWarps : privateWarps;

        if (targetMap.containsKey(key)) {
            String warpType = isPublic ? "public" : "private";
            player.sendMessage("§cYou already have a " + warpType + " warp named '§e" + warpName + "§c'!");
            return;
        }

        targetMap.put(key, getBlockCenterLocation(player.getLocation()));
        player.sendMessage("§a" + (isPublic ? "Public" : "Private") + " warp '§e" + warpName + "§a' set!");
        saveLocations();
    }

    private void handleRemove(Player player, String[] args) {
        if (!validateArgs(player, args, 2, "§6Usage:§f /warp remove <name>")) return;

        String publicKey = buildKey(player, args[1], true);
        String privateKey = buildKey(player, args[1], false);

        if (publicWarps.containsKey(publicKey) && canModify(player, publicKey)) {
            publicWarps.remove(publicKey);
            player.sendMessage("§aPublic warp '§e" + args[1] + "§a' removed!");
            saveLocations();
            return;
        }

        if (privateWarps.containsKey(privateKey) && canModify(player, privateKey)) {
            privateWarps.remove(privateKey);
            player.sendMessage("§aPrivate warp '§e" + args[1] + "§a' removed!");
            saveLocations();
            return;
        }

        player.sendMessage("§cWarp '§e" + args[1] + "§c' not found!");
    }

    private void handleTeleport(Player player, String[] args) {
        if (!validateArgs(player, args, 2, "§6Usage:§f /warp to <name>")) return;

        String publicKey = buildKey(player, args[1], true);
        String privateKey = buildKey(player, args[1], false);

        Location target = publicWarps.get(publicKey);
        if (target == null && (!CommandManager.isPublicOnlyMode() || player.isOp())) {
            target = privateWarps.get(privateKey);
        }

        if (target == null || target.getWorld() == null) {
            player.sendMessage("§cWarp '§e" + args[1] + "§c' not found!");
            return;
        }

        player.teleport(target);
        player.sendMessage("§aTeleported to '§e" + args[1] + "§a'!");
    }

    private void handleList(Player player) {
        List<String> warps = new ArrayList<>();

        publicWarps.keySet().stream()
                .map(k -> "§b[Public] §f" + k.replace("public:", ""))
                .forEach(warps::add);

        if (!CommandManager.isPublicOnlyMode() || player.isOp()) {
            privateWarps.keySet().stream()
                    .filter(k -> k.startsWith(player.getUniqueId().toString()))
                    .map(k -> "§7[Private] §f" + k.split(":")[1])
                    .forEach(warps::add);
        }

        if (warps.isEmpty()) {
            player.sendMessage("§cNo warps available!");
            return;
        }

        player.sendMessage("§6=== Available Warps ===");
        warps.forEach(player::sendMessage);
    }

    private void handleRename(Player player, String[] args) {
        if (!validateArgs(player, args, 3, "§6Usage:§f /warp rename <old> <new>")) return;

        String oldName = args[1].toLowerCase();
        String newName = args[2].toLowerCase();

        String oldPublicKey = "public:" + oldName;
        String newPublicKey = "public:" + newName;
        if (publicWarps.containsKey(oldPublicKey)) {
            if (!canModify(player, oldPublicKey)) {
                player.sendMessage("§cYou don't have permission to rename this public warp!");
                return;
            }
            if (publicWarps.containsKey(newPublicKey)) {
                player.sendMessage("§cPublic warp '§e" + newName + "§c' already exists!");
                return;
            }
            Location loc = publicWarps.remove(oldPublicKey);
            publicWarps.put(newPublicKey, loc);
            player.sendMessage("§aRenamed public warp to '§e" + newName + "§a'!");
            saveLocations();
            return;
        }

        String oldPrivateKey = player.getUniqueId().toString().toLowerCase() + ":" + oldName;
        String newPrivateKey = player.getUniqueId().toString().toLowerCase() + ":" + newName;
        if (privateWarps.containsKey(oldPrivateKey)) {
            if (privateWarps.containsKey(newPrivateKey)) {
                player.sendMessage("§cPrivate warp '§e" + newName + "§c' already exists!");
                return;
            }
            Location loc = privateWarps.remove(oldPrivateKey);
            privateWarps.put(newPrivateKey, loc);
            player.sendMessage("§aRenamed private warp to '§e" + newName + "§a'!");
            saveLocations();
            return;
        }

        player.sendMessage("§cWarp '§e" + oldName + "§c' not found!");
    }

    private Location getBlockCenterLocation(Location original) {
        Location blockCenter = original.getBlock().getLocation().add(0.5, 0, 0.5);
        blockCenter.setYaw(calculateSnappedYaw(original.getYaw()));
        blockCenter.setPitch(1);
        return blockCenter;
    }

    private float calculateSnappedYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        return (Math.round(yaw / 45) * 45) % 360;
    }

    private String buildKey(Player player, String name, boolean isPublic) {
        return isPublic
                ? "public:" + name.toLowerCase()
                : player.getUniqueId().toString().toLowerCase() + ":" + name.toLowerCase();
    }

    private boolean canModify(Player player, String warpKey) {
        return player.isOp() || warpKey.startsWith(player.getUniqueId().toString());
    }

    public void saveLocations() {
        ConfigurationSection section = plugin.getConfig().createSection("warps");
        ConfigurationSection publicSection = section.createSection("public");
        ConfigurationSection privateSection = section.createSection("private");

        publicWarps.forEach((k, v) -> publicSection.set(k.replace("public:", ""), v.serialize()));
        privateWarps.forEach((k, v) -> privateSection.set(k, v.serialize()));

        plugin.saveConfig();
    }

    public void loadLocations() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("warps");
        if (section != null) {
            loadWarps(section.getConfigurationSection("public"), publicWarps, "public:");
            loadWarps(section.getConfigurationSection("private"), privateWarps, "");
        }
    }

    private void loadWarps(ConfigurationSection section, Map<String, Location> target, String prefix) {
        if (section != null) {
            section.getKeys(false).forEach(key -> {
                Location loc = Location.deserialize(section.getConfigurationSection(key).getValues(false));
                if (loc.getWorld() != null) {
                    target.put(prefix + key.toLowerCase(), loc);
                }
            });
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        Player player = (Player) sender;

        if (CommandManager.isWarpOpOnlyMode() && !player.isOp()) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Arrays.asList("set", "remove", "rename", "to", "list").stream()
                    .filter(subcmd -> isSubcommandAllowed(player, subcmd))
                    .forEach(completions::add);
        } else if (args.length == 2) {
            String subcmd = args[0].toLowerCase();
            if (isSubcommandAllowed(player, subcmd)) {
                switch (subcmd) {
                    case "remove":
                    case "rename":
                    case "to":
                        publicWarps.keySet().stream()
                                .map(k -> k.replace("public:", ""))
                                .forEach(completions::add);
                        if (!CommandManager.isPublicOnlyMode() || player.isOp()) {
                            privateWarps.keySet().stream()
                                    .filter(k -> k.startsWith(player.getUniqueId().toString()))
                                    .map(k -> k.split(":")[1])
                                    .forEach(completions::add);
                        }
                        break;
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            if (player.isOp() || player.hasPermission("warp.admin")) {
                completions.add("public");
            }
        }

        return filterCompletions(args[args.length - 1], completions);
    }

    private boolean isSubcommandAllowed(Player player, String subcommand) {
        if (player.isOp()) return true;
        if (subcommand.equals("to") || subcommand.equals("list")) return true;
        if (CommandManager.isPublicOnlyMode()) return false;

        List<String> restricted = CommandManager.COMMAND_GROUPS.getOrDefault("warpoponly", Collections.emptyList());
        return !restricted.contains(subcommand) || CommandManager.isCommandEnabled("warp." + subcommand);
    }

    private boolean validateArgs(Player player, String[] args, int required, String errorMessage) {
        if (args.length >= required) return true;
        player.sendMessage(errorMessage);
        return false;
    }

    private void sendUsage(Player player) {
        player.sendMessage(MAIN_USAGE);
    }

    private List<String> filterCompletions(String input, List<String> options) {
        String search = input.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(search))
                .sorted()
                .toList();
    }
}