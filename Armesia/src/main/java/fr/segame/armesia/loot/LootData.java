package fr.segame.armesia.loot;

import org.bukkit.Material;

public class LootData {

    private final Material material;
    private final int min;
    private final int max;
    private final double chance;

    public LootData(Material material, int min, int max, double chance) {
        this.material = material;
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    public Material getMaterial() { return material; }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public double getChance() { return chance; }
}