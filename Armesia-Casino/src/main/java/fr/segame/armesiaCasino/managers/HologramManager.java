package fr.segame.armesiaCasino.managers;

import fr.segame.armesiaCasino.MainCasino;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.Map;

public class HologramManager {

    private final MainCasino              plugin;
    /** Clé : "world,x,y,z" → entité TextDisplay */
    private final Map<String, TextDisplay> holograms = new HashMap<>();

    public HologramManager(MainCasino plugin) {
        this.plugin = plugin;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    public void createHologram(Location blockLoc) {
        String key = locationKey(blockLoc);
        removeHologram(blockLoc); // Supprime l'ancien s'il existe

        double offsetY = plugin.getConfig().getDouble("casino-block.hologram.offset-y", 2.5);
        String text    = plugin.getConfig().getString("casino-block.hologram.text", "&6&l✦ CASINO ✦");

        // Centre le hologramme au-dessus du bloc
        Location holoLoc = blockLoc.clone().add(0.5, offsetY, 0.5);

        if (holoLoc.getWorld() == null) return;

        TextDisplay display = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, td -> {
            td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false); // Ne pas sauvegarder dans le monde
            td.setVisibleByDefault(true);
        });

        holograms.put(key, display);
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    public void removeHologram(Location blockLoc) {
        TextDisplay existing = holograms.remove(locationKey(blockLoc));
        if (existing != null && !existing.isDead()) {
            existing.remove();
        }
    }

    public void removeAll() {
        holograms.values().stream()
                .filter(td -> td != null && !td.isDead())
                .forEach(TextDisplay::remove);
        holograms.clear();
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    private String locationKey(Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ();
    }
}

