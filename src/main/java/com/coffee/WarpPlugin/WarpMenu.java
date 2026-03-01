package com.coffee.WarpPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class WarpMenu implements CommandExecutor {

    private Inventory warpMenu;
    private final Main plugin;

    public WarpMenu(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            this.warpMenu = Bukkit.createInventory(new WarpMenuHolder(), 54, "Warp Menu");

            ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            setItemName(border, "", null);
            for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53}) {
                warpMenu.setItem(i, border);
            }

            ItemStack close = new ItemStack(Material.BARRIER);
            setItemName(close, "Close", "&c");
            warpMenu.setItem(49, close);

            loadWarpsFromConfig(plugin, warpMenu);
            player.openInventory(warpMenu);
        }
        return true;
    }

    public void loadWarpsFromConfig(Main plugin, Inventory warpMenu) {
        ConfigurationSection privateWarps = plugin.getConfig().getConfigurationSection("warps.private");
        if (privateWarps == null) { return; }

        int slot = 10;
        for (String key : privateWarps.getKeys(false)) {
            String[] parts = key.split(":");

            String rawName = (parts.length > 1) ? parts[1] : "Unknown";
            String displayName = rawName.substring(0,1).toUpperCase() + rawName.substring(1).toLowerCase().replace("_", " ");

            while (slot < warpMenu.getSize() && warpMenu.getItem(slot) != null) {
                slot++;
            }

            if (slot >= warpMenu.getSize()) break;

            ItemStack warpItem = new ItemStack(Material.PAPER);
            setItemName(warpItem, displayName, "&b");
            warpMenu.setItem(slot, warpItem);

            slot++;
        }
    }

    private ItemMeta setItemName(@NotNull ItemStack item, @NotNull String name, String color) {
        ItemMeta meta = item.getItemMeta();

        if (name.equals("") || name.isEmpty()) {
            meta.setDisplayName(ChatColor.RESET.toString());
            item.setItemMeta(meta);
            return (meta);
        }

        if (color != null) {
            String fullPath = (color != null) ? color + name : name;
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', fullPath));
        } else {
            meta.setDisplayName(name);
        }
        item.setItemMeta(meta);
        return (meta);
    }

    public Inventory getInventory() {
        return warpMenu;
    }

}
