package com.coffee.WarpPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {
    private WarpCommand warpCommand;
    private com.coffee.WarpPlugin.WarpConfig warpConfig;

    public File getPluginFile() {
        return super.getFile();
    }

    private String currentVersion;
    private String githubRepo;

    AutoUpdate updater = new AutoUpdate(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        CommandManager.initialize(this);

        this.warpCommand = new WarpCommand(this);
        this.warpConfig = new WarpConfig();

        getCommand("warp").setExecutor(warpCommand);
        getCommand("warpconfig").setExecutor(warpConfig);

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