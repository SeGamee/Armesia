package fr.segame.armesia.utils;

import fr.segame.armesia.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Téléportation avec countdown titre + vérification de mouvement chaque seconde.
 * Style identique à TeleportManager d'Armesia-World, mais configurable.
 */
public class TeleportUtil {

    private static final Map<UUID, BukkitTask>  tasks      = new HashMap<>();
    private static final Map<UUID, Location>    startLocs  = new HashMap<>();

    /**
     * Lance une téléportation avec délai, titre décompte et annulation si mouvement.
     *
     * @param player     Joueur à téléporter
     * @param dest       Destination
     * @param delaySecs  Délai (0 = instantané)
     * @param titleLine2 Sous-titre affiché pendant le décompte (ex: "Ne bougez pas !")
     * @param cancelMsg  Message chat en cas d'annulation
     * @param onSuccess  Callback après téléportation réussie
     */
    public static void schedule(Player player, Location dest, int delaySecs,
                                String titleLine2, String cancelMsg, Runnable onSuccess) {
        UUID uuid = player.getUniqueId();
        cancel(uuid);

        if (delaySecs <= 0) {
            player.teleport(dest);
            player.sendTitle("§a✔", "§7Téléporté !", 5, 40, 10);
            if (onSuccess != null) onSuccess.run();
            return;
        }

        double threshold = Main.getInstance().getConfig().getDouble("homes.move-threshold", 0.5);
        double threshSq  = threshold * threshold;

        startLocs.put(uuid, player.getLocation().clone());
        sendTitle(player, delaySecs, titleLine2 != null ? titleLine2 : "Ne bougez pas !");

        int[] remaining = {delaySecs};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { silentCancel(uuid); cancel(); return; }

                // Vérification de mouvement
                Location start = startLocs.get(uuid);
                if (start != null && player.getLocation().distanceSquared(start) > threshSq) {
                    silentCancel(uuid);
                    player.sendMessage(cancelMsg != null ? cancelMsg : "§cTéléportation annulée.");
                    player.sendTitle("§c✘ Annulé", "§7Vous avez bougé.", 0, 40, 10);
                    cancel();
                    return;
                }

                remaining[0]--;
                if (remaining[0] <= 0) {
                    silentCancel(uuid);
                    player.teleport(dest);
                    player.sendTitle("§a✔", "§7Téléporté !", 5, 40, 10);
                    if (onSuccess != null) onSuccess.run();
                    cancel();
                } else {
                    sendTitle(player, remaining[0], titleLine2 != null ? titleLine2 : "Ne bougez pas !");
                }
            }
        }.runTaskTimer(Main.getInstance(), 20L, 20L);

        tasks.put(uuid, task);
    }

    /** Surcharge de compatibilité (anciens appels avec 4 paramètres). */
    public static void schedule(Player player, Location dest, int delaySecs,
                                String cancelMsg, Runnable onSuccess) {
        schedule(player, dest, delaySecs, null, cancelMsg, onSuccess);
    }

    public static void cancel(UUID uuid) {
        silentCancel(uuid);
    }

    public static boolean hasPending(UUID uuid) {
        return tasks.containsKey(uuid);
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    private static void silentCancel(UUID uuid) {
        BukkitTask t = tasks.remove(uuid);
        if (t != null) t.cancel();
        startLocs.remove(uuid);
    }

    private static void sendTitle(Player player, int seconds, String subtitle) {
        String color = seconds <= 2 ? "§c" : seconds <= 3 ? "§e" : "§a";
        player.sendTitle("§6Téléportation", color + seconds + "s §7— " + subtitle, 0, 25, 5);
    }
}
