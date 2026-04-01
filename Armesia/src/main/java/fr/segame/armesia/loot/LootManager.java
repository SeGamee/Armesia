package fr.segame.armesia.loot;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.*;

public class LootManager {

    private final Map<String, List<LootData>> tables = new HashMap<>();

    public void register(String id, List<LootData> loots) {
        tables.put(id, loots);
    }

    /** Crée une table vide si elle n'existe pas encore */
    public void createTable(String id) {
        tables.putIfAbsent(id, new ArrayList<>());
    }

    public void removeTable(String id) {
        tables.remove(id);
    }

    public List<LootData> getTable(String id) {
        return tables.get(id);
    }

    public Map<String, List<LootData>> getAllTables() {
        return tables;
    }

    public Set<String> getTableIds() {
        return tables.keySet();
    }

    /** Ajoute une entrée à une table (crée la table si besoin) */
    public void addEntry(String tableId, LootData loot) {
        tables.computeIfAbsent(tableId, k -> new ArrayList<>()).add(loot);
    }

    /** Supprime l'entrée à l'index (0-based) */
    public boolean removeEntry(String tableId, int index) {
        List<LootData> list = tables.get(tableId);
        if (list == null || index < 0 || index >= list.size()) return false;
        list.remove(index);
        return true;
    }

    /** Drop les items au sol à la position donnée (mort du mob) */
    public void dropLoot(Location location, String tableId) {
        List<LootData> loots = tables.get(tableId);
        if (loots == null) return;

        Random rnd = new Random();
        for (LootData loot : loots) {
            if (Math.random() <= loot.getChance()) {
                int amount = rnd.nextInt(loot.getMax() - loot.getMin() + 1) + loot.getMin();
                ItemStack item = loot.getCustomItem();
                item.setAmount(amount);
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }

    /** @deprecated Préférer dropLoot(Location, tableId) — drop au sol */
    public void applyLoot(Player player, String tableId) {
        List<LootData> loots = tables.get(tableId);
        if (loots == null) return;

        Random rnd = new Random();
        for (LootData loot : loots) {
            if (Math.random() <= loot.getChance()) {
                int amount = rnd.nextInt(loot.getMax() - loot.getMin() + 1) + loot.getMin();
                ItemStack item = loot.getCustomItem();
                item.setAmount(amount);
                player.getInventory().addItem(item);
            }
        }
    }
}