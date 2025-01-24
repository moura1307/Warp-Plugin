package com.coffee.warpCommand;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getCommand("warp").setExecutor(new WarpCommand());
    }

}