package fr.segame.armesia.loot;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LootManager {

    private final Map<String, List<LootData>> tables = new HashMap<>();

    public void register(String id, List<LootData> loots) {
        tables.put(id, loots);
    }

    public void applyLoot(Player player, String tableId) {

        List<LootData> loots = tables.get(tableId);
        if (loots == null) return;

        for (LootData loot : loots) {
            if (Math.random() <= loot.getChance()) {
                int amount = new Random().nextInt(loot.getMax() - loot.getMin() + 1) + loot.getMin();
                player.getInventory().addItem(new ItemStack(loot.getMaterial(), amount));
            }
        }
    }
}