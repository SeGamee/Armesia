package fr.segame.armesiaWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Random;

/**
 * Représente une zone rectangulaire sur la map (deux coins opposés).
 */
public class MapZone {

    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public MapZone(String name, String worldName,
                   int x1, int y1, int z1,
                   int x2, int y2, int z2) {
        this.name      = name;
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getName()      { return name;      }
    public String getWorldName() { return worldName; }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    /** Retourne true si la localisation est à l'intérieur de la zone. */
    public boolean isInside(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    /**
     * Retourne un point de spawn aléatoire pour le parachute :
     * X et Z aléatoires dans les bornes de la zone,
     * Y = hauteur réelle du sol en ce point + 70 (plafonné à 319).
     */
    public Location getParachuteSpawn() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        Random rng = new Random();
        double rx = minX + rng.nextDouble() * (maxX - minX + 1);
        double rz = minZ + rng.nextDouble() * (maxZ - minZ + 1);
        int groundY = w.getHighestBlockYAt((int) rx, (int) rz);
        double sy = Math.min(groundY + 70, 319);
        return new Location(w, rx, sy, rz);
    }

    @Override
    public String toString() {
        return name + " [" + worldName + " | ("
            + minX + "," + minY + "," + minZ + ") → ("
            + maxX + "," + maxY + "," + maxZ + ")]";
    }
}




