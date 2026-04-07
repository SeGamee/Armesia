package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;

/**
 * Gère la persistance du vanish, la réapplication au login
 * et la mise à jour en temps réel lors des /op et /deop.
 */
public class VanishListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Cache les joueurs vanished au nouveau joueur (si il n'a pas la perm)
        VanishManager.applyForNewPlayer(player);

        // Si le joueur rejoint et était déjà en vanish, le cacher pour les autres
        if (VanishManager.isVanished(player.getUniqueId())) {
            // runTaskLater pour laisser le temps au joueur d'être pleinement chargé
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (player.isOnline()) {
                    VanishManager.reapplyVanishFor(player);
                    Main.updateTab(player);
                }
            }, 2L);
            // Supprimer le message de join pour le joueur vanished
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Ne supprimer l'état vanish au logout QUE si reset-on-login est activé
        if (Main.getInstance().getConfig().getBoolean("vanish.reset-on-login", false)) {
            VanishManager.forceRemove(player.getUniqueId());
        }
        // Message de quit silencieux si vanished
        if (VanishManager.isVanished(player.getUniqueId())) {
            event.quitMessage(null);
        }
    }

    // ── Détection /op et /deop depuis la console ──────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String cmd = event.getCommand().trim().toLowerCase();
        if (cmd.startsWith("op ") || cmd.startsWith("deop ")) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    VanishManager::reapplyAll, 1L);
        }
    }

    // ── Détection /op et /deop depuis un joueur ───────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().substring(1).trim().toLowerCase();
        if (cmd.startsWith("op ") || cmd.startsWith("deop ")) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    VanishManager::reapplyAll, 1L);
        }
    }
}
