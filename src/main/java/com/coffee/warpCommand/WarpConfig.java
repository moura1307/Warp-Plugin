package com.coffee.warpCommand;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class WarpConfig implements CommandExecutor, TabCompleter {
    private static final String USAGE = "§6Usage:§f /warpconfig <WarpOpOnly|WarpPublicOnly> <enable|disable>";
    private static final String PLAYER_ONLY = "§cThis command is for players only!";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(USAGE);
            return true;
        }

        String mode = args[0].toLowerCase();
        boolean enable = args[1].equalsIgnoreCase("enable");

        switch (mode) {
            case "warpoponly":
                CommandManager.setWarpOpOnlyMode(enable);
                sender.sendMessage("§aOP-only mode " + (enable ? "enabled" : "disabled"));
                break;
            case "warppubliconly":
                CommandManager.setPublicOnlyMode(enable);
                sender.sendMessage("§aPublic-only mode " + (enable ? "enabled" : "disabled"));
                break;
            default:
                sender.sendMessage(USAGE);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (!(sender instanceof Player) || !sender.isOp()) return options;

        if (args.length == 1) {
            options.addAll(Arrays.asList("WarpOpOnly", "WarpPublicOnly"));
        } else if (args.length == 2) {
            options.addAll(Arrays.asList("enable", "disable"));
        }

        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(args[args.length-1].toLowerCase()))
                .toList();
    }
}