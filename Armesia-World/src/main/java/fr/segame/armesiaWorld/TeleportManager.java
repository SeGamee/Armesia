package fr.segame.armesiaWorld;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private static final int COUNTDOWN_SECONDS = 10;

    private final MainWorld plugin;
    private final Map<UUID, BukkitTask> pendingTasks      = new HashMap<>();
    private final Map<UUID, Location>   startLocations    = new HashMap<>();
    private final Map<UUID, Location>   destinations      = new HashMap<>();

    public TeleportManager(MainWorld plugin) {
        this.plugin = plugin;
    }

    // ─── API publique ─────────────────────────────────────────────────────────

    public void requestTeleport(Player player, Location destination) {
        silentCancel(player.getUniqueId()); // annule un éventuel tp déjà en cours

        startLocations.put(player.getUniqueId(), player.getLocation().clone());
        destinations.put(player.getUniqueId(), destination);

        player.sendMessage("§aTéléportation au spawn dans §6" + COUNTDOWN_SECONDS + " §asecondes. §eNe bougez pas !");
        sendCountdownTitle(player, COUNTDOWN_SECONDS);

        final int[] remaining = {COUNTDOWN_SECONDS};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { silentCancel(player.getUniqueId()); cancel(); return; }
                remaining[0]--;
                if (remaining[0] <= 0) {
                    player.teleport(destination);
                    player.sendTitle("§a✔ Spawn", "§7Bienvenue !", 5, 50, 10);
                    silentCancel(player.getUniqueId());
                    cancel();
                } else {
                    sendCountdownTitle(player, remaining[0]);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        pendingTasks.put(player.getUniqueId(), task);
    }

    /** Retourne true si un tp est en attente pour ce joueur. */
    public boolean hasPending(UUID uuid) {
        return pendingTasks.containsKey(uuid);
    }

    /** Position de départ sauvegardée au moment de la demande de tp. */
    public Location getStartLocation(UUID uuid) {
        return startLocations.get(uuid);
    }

    /** Annule le tp et prévient le joueur qu'il a bougé. */
    public void cancelDueToMovement(Player player) {
        if (!hasPending(player.getUniqueId())) return;
        silentCancel(player.getUniqueId());
        player.sendMessage("§cTéléportation annulée : vous avez bougé !");
        player.sendTitle("§c✘ Annulé", "§7Vous avez bougé", 0, 50, 10);
    }

    /** Nettoyage silencieux à la déconnexion. */
    public void cleanupOnQuit(UUID uuid) {
        silentCancel(uuid);
    }

    // ─── Interne ─────────────────────────────────────────────────────────────

    private void silentCancel(UUID uuid) {
        BukkitTask t = pendingTasks.remove(uuid);
        if (t != null) t.cancel();
        startLocations.remove(uuid);
        destinations.remove(uuid);
    }

    private void sendCountdownTitle(Player player, int seconds) {
        String color = seconds <= 3 ? "§c" : seconds <= 6 ? "§e" : "§a";
        player.sendTitle("§6Spawn", color + seconds + "s §7— Ne bougez pas !", 0, 25, 5);
    }
}

