package fr.segame.armesiaWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Représente un portail rectangulaire lié à une {@link MapZone}.
 * Quand un joueur entre dans la région du portail, il est téléporté
 * en parachute dans la zone cible.
 */
public class Portal {

    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    /** Nom de la MapZone de destination. */
    private final String targetZone;

    public Portal(String name, String worldName,
                  int x1, int y1, int z1,
                  int x2, int y2, int z2,
                  String targetZone) {
        this.name       = name;
        this.worldName  = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.targetZone = targetZone;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getName()       { return name;       }
    public String getWorldName()  { return worldName;  }
    public String getTargetZone() { return targetZone; }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    /**
     * Vérifie si la position du joueur est à l'intérieur de la région du portail.
     * On utilise les coordonnées flottantes pour une détection fluide.
     */
    public boolean isInside(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x < maxX + 1
            && y >= minY && y < maxY + 2   // +2 pour couvrir la hauteur du joueur
            && z >= minZ && z < maxZ + 1;
    }

    /** Centre géométrique du portail (pour les particules). */
    public Location getCenter() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w,
                (minX + maxX) / 2.0 + 0.5,
                (minY + maxY) / 2.0 + 0.5,
                (minZ + maxZ) / 2.0 + 0.5);
    }

    @Override
    public String toString() {
        return name + " → " + targetZone + " [" + worldName
                + " | (" + minX + "," + minY + "," + minZ
                + ") → (" + maxX + "," + maxY + "," + maxZ + ")]";
    }
}

