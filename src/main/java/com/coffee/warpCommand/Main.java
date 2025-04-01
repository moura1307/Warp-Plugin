package com.coffee.warpCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private WarpCommand warpCommand;
    @Override
    public void onEnable() {
        // Register events and commands
        warpCommand = new WarpCommand(this);
        getCommand("warp").setExecutor(warpCommand);

        // Load locations when the plugin starts
        warpCommand.loadLocations();
    }

    @Override
    public void onDisable() {
        // Save locations when the server shuts down
        warpCommand.saveLocations();
    }
}