package com.coffee.WarpPlugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class CommandManager {
    // Constants and configuration groups
    public static final Map<String, List<String>> COMMAND_GROUPS = new HashMap<>();
    static {
        COMMAND_GROUPS.put("warpoponly", Arrays.asList("set", "remove", "rename"));
    }

    // State management
    private static final Map<String, Boolean> commandStates = new HashMap<>();
    private static JavaPlugin plugin;

    // Mode flags
    private static boolean publicOnlyMode = false;
    private static boolean warpOpOnlyMode = false;

    // Initialization
    public static void initialize(JavaPlugin plugin) {
        CommandManager.plugin = plugin;
        loadStates();
        loadConfig();
    }

    // Command state accessors
    public static boolean isCommandEnabled(String command) {
        return commandStates.getOrDefault(command.toLowerCase(), true);
    }

    public static void setCommandState(String command, boolean enabled) {
        commandStates.put(command.toLowerCase(), enabled);
        saveStates();
    }

    // Group state management
    public static void setGroupState(String groupName, boolean enabled) {
        List<String> subcommands = COMMAND_GROUPS.getOrDefault(
                groupName.toLowerCase(),
                Collections.emptyList()
        );
        subcommands.forEach(subcmd -> setCommandState("warp." + subcmd, enabled));
    }

    // Mode accessors
    public static boolean isPublicOnlyMode() {
        return publicOnlyMode;
    }

    public static boolean isWarpOpOnlyMode() {
        return warpOpOnlyMode;
    }

    // Mode modifiers
    public static void setPublicOnlyMode(boolean enabled) {
        publicOnlyMode = enabled;
        plugin.getConfig().set("publiconly-mode", enabled);
        plugin.saveConfig();

        if (enabled) {
            setGroupState("warpoponly", false);
        }
    }

    public static void setWarpOpOnlyMode(boolean enabled) {
        if (enabled) {
            setPublicOnlyMode(false);
        }

        warpOpOnlyMode = enabled;
        plugin.getConfig().set("warpoponly-mode", enabled);
        plugin.saveConfig();
        setGroupState("warpoponly", !enabled);
    }

    // Config loading utilities
    private static void loadConfig() {
        publicOnlyMode = plugin.getConfig().getBoolean("publiconly-mode", false);
        warpOpOnlyMode = plugin.getConfig().getBoolean("warpoponly-mode", false);
    }

    private static void loadStates() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("command-states");

        // Initialize warp group commands
        COMMAND_GROUPS.get("warpoponly").forEach(subcmd -> {
            String key = "warp." + subcmd;
            commandStates.put(key, section != null ? section.getBoolean(key, true) : true);
        });

        // Load all other commands
        if (section != null) {
            section.getKeys(false).forEach(key -> {
                if (!commandStates.containsKey(key)) {
                    commandStates.put(key, section.getBoolean(key));
                }
            });
        }
    }

    // State persistence
    private static void saveStates() {
        try {
            FileConfiguration config = plugin.getConfig();
            config.set("command-states", null);
            ConfigurationSection section = config.createSection("command-states");
            commandStates.forEach(section::set);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving command states: " + e.getMessage());
        }
    }
}