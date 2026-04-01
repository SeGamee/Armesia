package fr.segame.armesia.zones;

import fr.segame.armesia.managers.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Envoie des messages de debug lors des changements de zone et de palier de despawn.
 * Les messages ne sont envoyés qu'aux joueurs ayant le debug actif (NORMAL ou VERBOSE).
 */
public class ZoneListener implements Listener {

    private static final String PRE = "§8[§eDBG§8] §7";

    private final ZoneManager  zoneManager;
    private final DebugManager debug;

    /** Zone actuelle de chaque joueur (null = hors zone) */
    private final Map<UUID, String>  playerZone = new HashMap<>();
    /** Palier de despawn actuel : 0=close 1=mid 2=far 3=outer (-1 = inconnu) */
    private final Map<UUID, Integer> playerTier = new HashMap<>();

    public ZoneListener(ZoneManager zoneManager, DebugManager debug) {
        this.zoneManager = zoneManager;
        this.debug       = debug;
    }

    // ─── Events ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Ignorer les simples rotations de caméra
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();
        if (debug.getLevel(uuid) == DebugManager.Level.NONE) return;

        ZoneData newZone   = zoneManager.getZoneAt(event.getTo());
        String   newZoneId = newZone != null ? newZone.getId() : null;
        String   oldZoneId = playerZone.get(uuid);

        // ── Changement de zone ────────────────────────────────────────────
        if (!Objects.equals(oldZoneId, newZoneId)) {
            playerZone.put(uuid, newZoneId);
            playerTier.put(uuid, -1); // reset pour forcer un log du tier à l'entrée

            if (newZoneId == null) {
                // Sortie de zone
                player.sendMessage(PRE + "§7[ZONE§8-§7] Sortie de §f'" + oldZoneId + "'");

            } else if (oldZoneId == null) {
                // Entrée dans une zone depuis le monde ouvert
                player.sendMessage(PRE + "§a[ZONE§8+§a] §7Entrée §f'" + newZoneId + "'"
                        + "  §7prio=§f" + newZone.getPriority()
                        + "  §7max=§f" + newZone.getMax()
                        + "  §7spawn=[§f" + fmt(newZone.getSpawnRadiusMin())
                        + "§7-§f" + fmt(newZone.getSpawnRadiusMax()) + "§7blocs]"
                        + "  §7despawn=§f" + fmt(newZone.getDespawnDistance()) + "§7blocs");

            } else {
                // Transition zone → zone (sous-zone ou changement de priorité)
                player.sendMessage(PRE + "§e[ZONE§8→§e] §f'" + oldZoneId
                        + "' §7→ §f'" + newZoneId + "'"
                        + "  §7prio=§f" + newZone.getPriority());
            }
        }

        // ── Changement de palier de despawn ───────────────────────────────
        if (newZone == null) return;
        Location center = zoneManager.getCenter(newZone);
        if (center == null) return;

        double dist = event.getTo().distance(center);
        double d    = newZone.getDespawnDistance();

        int tier;
        if      (dist < d * newZone.getDespawnRatio1()) tier = 0;
        else if (dist < d * newZone.getDespawnRatio2()) tier = 1;
        else if (dist < d * newZone.getDespawnRatio3()) tier = 2;
        else                                            tier = 3;

        int oldTier = playerTier.getOrDefault(uuid, -1);
        if (oldTier == tier) return;

        playerTier.put(uuid, tier);

        String[] names   = { "§aclose", "§emid", "§6far", "§couter" };
        double[] limits  = { d * newZone.getDespawnRatio1(),
                             d * newZone.getDespawnRatio2(),
                             d * newZone.getDespawnRatio3() };
        double[] chances = { newZone.getDespawnChanceClose(), newZone.getDespawnChanceMid(),
                             newZone.getDespawnChanceFar(),   newZone.getDespawnChanceOuter() };

        String distInfo = tier < 3
                ? "§7< §f" + fmt(limits[tier]) + " §7blocs"
                : "§7> §f" + fmt(limits[2])   + " §7blocs";

        player.sendMessage(PRE + "§e[SEUIL] §7zone=§f'" + newZone.getId()
                + "' §7palier=" + names[tier]
                + "  §8(" + distInfo
                + " §7chance=§f" + String.format("%.0f%%", chances[tier] * 100) + "§8)");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerZone.remove(uuid);
        playerTier.remove(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerZone.remove(uuid);
        playerTier.remove(uuid);
    }

    /**
     * Quand un chunk (re)charge ses entités, on tente de ré-enregistrer
     * les mobs custom qui y étaient sauvegardés (persistent=true).
     * On diffère d'un tick pour être sûr que getEntities() est complet.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return; // un nouveau chunk ne peut pas contenir nos mobs
        Bukkit.getScheduler().runTask(zoneManager.getPlugin(), () -> {
            for (Entity entity : event.getChunk().getEntities()) {
                zoneManager.tryRecoverEntity(entity);
            }
        });
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private String fmt(double v) { return String.format("%.0f", v); }
}
