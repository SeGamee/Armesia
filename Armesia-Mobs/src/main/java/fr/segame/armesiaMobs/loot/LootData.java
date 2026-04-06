package fr.segame.armesiaMobs.loot;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LootData {

    private Material material;
    private ItemStack customItem; // null = drop basé sur Material
    private int min;
    private int max;
    private double chance;

    /** Drop de base : material simple */
    public LootData(Material material, int min, int max, double chance) {
        this.material = material;
        this.customItem = null;
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    /** Drop personnalisé : item depuis la main (avec nom/lore/enchants) */
    public LootData(ItemStack customItem, int min, int max, double chance) {
        this.material = customItem.getType();
        this.customItem = customItem.clone();
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    public boolean isCustomItem() { return customItem != null; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; this.customItem = null; }

    public ItemStack getCustomItem() {
        if (customItem != null) return customItem.clone();
        return new ItemStack(material);
    }
    public void setCustomItem(ItemStack item) {
        this.customItem = item != null ? item.clone() : null;
        if (item != null) this.material = item.getType();
    }

    public int getMin() { return min; }
    public void setMin(int min) { this.min = min; }

    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }

    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = chance; }

    /** Nom lisible pour le /loot info */
    public String getDisplayName() {
        if (customItem != null && customItem.getItemMeta() != null && customItem.getItemMeta().hasDisplayName()) {
            return customItem.getItemMeta().getDisplayName();
        }
        return material.name();
    }
}