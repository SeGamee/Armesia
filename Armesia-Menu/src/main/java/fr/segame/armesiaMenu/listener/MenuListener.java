package fr.segame.armesiaMenu.listener;

import fr.segame.armesiaMenu.menu.Menu;
import fr.segame.armesiaMenu.menu.MenuItem;
import fr.segame.armesiaMenu.menu.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {

    private final MenuManager manager;

    public MenuListener(MenuManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player player)) return;

        Menu menu = manager.getOpenMenu(player);
        if (menu == null) return;

        // 🔥 Toujours annuler
        e.setCancelled(true);

        // 🔥 Empêche shift-click / move
        if (e.getClick() == ClickType.SHIFT_LEFT ||
                e.getClick() == ClickType.SHIFT_RIGHT) {
            return;
        }

        // 🔥 Empêche clic hors GUI
        if (e.getClickedInventory() == null) return;

        // 🔥 Vérifie que c'est le menu
        if (!e.getView().getTopInventory().equals(e.getClickedInventory())) {
            return;
        }

        MenuItem item = menu.getItem(e.getSlot());

        if (item != null) {
            item.execute(player, e.getClick(), manager);
        }
    }

    // 🔥 IMPORTANT : désenregistre le menu à la fermeture
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        // Ne pas désenregistrer si c'est un refresh en cours
        if (manager.isRefreshing(player)) return;
        manager.closeMenu(player);
    }

    // 🔥 IMPORTANT : bloque drag
    @EventHandler
    public void onDrag(InventoryDragEvent e) {

        if (!(e.getWhoClicked() instanceof Player player)) return;

        Menu menu = manager.getOpenMenu(player);
        if (menu == null) return;

        e.setCancelled(true);
    }
}