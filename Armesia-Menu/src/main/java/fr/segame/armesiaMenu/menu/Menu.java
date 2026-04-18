package fr.segame.armesiaMenu.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Menu {

    private final String id;
    private final String title;
    private final int size;
    private final Map<Integer, MenuItem> items = new HashMap<>();

    private final List<String> allowedWorlds;
    private final List<String> blockedWorlds;

    public Menu(String id, String title, int size,
                List<String> allowedWorlds,
                List<String> blockedWorlds) {

        this.id = id;
        this.title = title;
        this.size = size;
        this.allowedWorlds = allowedWorlds;
        this.blockedWorlds = blockedWorlds;
    }

    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    public List<String> getBlockedWorlds() {
        return blockedWorlds;
    }

    public Inventory createInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, size, title);

        for (MenuItem item : items.values()) {
            // 🔥 TRÈS IMPORTANT
            inv.setItem(item.getSlot(), item.build(player));
        }

        return inv;
    }

    public void addItem(MenuItem item) {
        items.put(item.getSlot(), item);
    }

    public MenuItem getItem(int slot) {
        return items.get(slot);
    }
}