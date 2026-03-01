package com.coffee.WarpPlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class WarpMenuListener implements Listener {

    private Main Main;
    WarpMenu warpMenu = new WarpMenu(Main);

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory().getHolder() instanceof WarpMenuHolder) {
            e.setCancelled(true);

            Player player = (Player) e.getWhoClicked();

            switch (e.getRawSlot()) {
                case 49:
                    player.closeInventory();
                    break;
                default:
                    break;
            }
        }
    }
}
