package com.coffee.warpCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WarpConfig implements CommandExecutor, TabCompleter {

    private final Main main;
    private static final String USAGE_MESSAGE = ChatColor.GOLD + "Usage: " + ChatColor.WHITE
            + "/warpconfig " + ChatColor.YELLOW + "autoupdate <enable|disable>";

    public WarpConfig(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cThis is an &eOperator-Only &ccommand!"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        if (!args[0].equalsIgnoreCase("autoupdate")) {
            sendUsage(player);
            return true;
        }

        handleAutoUpdate(player, args);
        return true;
    }

    private void handleAutoUpdate(Player player, String[] args) {
        if (!validateArgs(player, args, 2,
                "&6Usage: &f/warpconfig autoupdate <&eenable|disable&f>")) {
            return;
        }

        String state = args[1].toLowerCase();
        if (!state.equals("enable") && !state.equals("disable")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cInvalid option! Use &eenable &cor &edisable"));
            return;
        }

        boolean enable = state.equals("enable");
        boolean currentState = main.getConfig().getBoolean("auto-update.enabled", true);

        if (enable == currentState) {
            String message = enable ?
                    "&aAuto-update is already &2enabled" :
                    "&aAuto-update is already &4disabled";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        main.getConfig().set("auto-update.enabled", enable);
        main.saveConfig();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAuto-update " + (enable ? "&2enabled" : "&4disabled")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player) || !((Player) sender).isOp()) {
            return completions;
        }

        if (args.length == 1) {
            if ("autoupdate".startsWith(args[0].toLowerCase())) {
                completions.add("autoupdate");
            }
        }
        else if (args.length == 2 && args[0].equalsIgnoreCase("autoupdate")) {
            for (String option : Arrays.asList("enable", "disable")) {
                if (option.startsWith(args[1].toLowerCase())) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }

    private boolean validateArgs(Player player, String[] args, int required, String errorMessage) {
        if (args.length >= required) {
            return true;
        }
        // Send the error message with proper color formatting
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMessage));
        return false;
    }
    private void sendUsage(Player player) {
        player.sendMessage(USAGE_MESSAGE);
    }
}