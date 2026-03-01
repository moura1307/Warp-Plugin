package com.coffee.WarpPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {
    private WarpCommand warpCommand;
    private WarpConfig warpConfig;
    private WarpMenu warpMenu;
    private AutoUpdate updater;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        reloadConfig();

        this.warpCommand = new WarpCommand(this);
        this.warpConfig = new WarpConfig();
        this.warpMenu = new WarpMenu(this);
        this.updater = new AutoUpdate(this);
        CommandManager.initialize(this);

        getCommand("warp").setExecutor(warpCommand);
        getCommand("warpconfig").setExecutor(warpConfig);
        getCommand("warpmenu").setExecutor(warpMenu);

        Bukkit.getPluginManager().registerEvents(new WarpMenuListener(), this);

        warpCommand.loadLocations();

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            updater.checkUpdates("moura1307/Warp-Plugin", getDescription().getVersion());
        }, 0, 20 * 60 * 60 * 24);
    }

    @Override
    public void onDisable() {
        warpCommand.saveLocations();
        if (updater != null) {
            updater.applyUpdate();
        }
        getLogger().info("WarpPlugin disabled successfully.");
    }
}