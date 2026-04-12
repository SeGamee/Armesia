package fr.segame.armesiaWorld.listeners;

import fr.segame.armesiaWorld.MainWorld;
import fr.segame.armesiaWorld.MapZone;
import fr.segame.armesiaWorld.MapZoneManager;
import fr.segame.armesiaWorld.ParachuteManager;
import fr.segame.armesiaWorld.Portal;
import fr.segame.armesiaWorld.PortalManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Détecte quand un joueur entre dans la région d'un portail et déclenche
 * le TP en parachute vers la MapZone liée.
 * Un cooldown de 3 secondes évite les déclenchements multiples.
 */
public class PortalListener implements Listener {

    /** Délai en ticks avant qu'un joueur puisse être retéléporté (3 s). */
    private static final long COOLDOWN_TICKS = 60L;

    private final MainWorld        plugin;
    private final PortalManager    portalManager;
    private final MapZoneManager   mapZoneManager;
    private final ParachuteManager parachuteManager;

    /** Joueurs actuellement en cooldown (déjà téléportés, en attente). */
    private final Set<UUID> cooldowns = new HashSet<>();

    public PortalListener(MainWorld plugin, PortalManager portalManager,
                          MapZoneManager mapZoneManager, ParachuteManager parachuteManager) {
        this.plugin           = plugin;
        this.portalManager    = portalManager;
        this.mapZoneManager   = mapZoneManager;
        this.parachuteManager = parachuteManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimisation : ne vérifier que si le joueur a changé de bloc
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        // Ignorer si cooldown actif ou parachute déjà en cours
        if (cooldowns.contains(uuid))             return;
        if (parachuteManager.isActive(uuid))       return;

        // Chercher le premier portail dont la région contient la nouvelle position
        for (Portal portal : portalManager.getAllPortals()) {
            if (!portal.isInside(to)) continue;

            // Récupérer la zone cible
            MapZone zone = mapZoneManager.getZone(portal.getTargetZone());
            if (zone == null) {
                plugin.getLogger().warning("[Portal] Zone '" + portal.getTargetZone()
                        + "' introuvable pour le portail '" + portal.getName() + "'.");
                return;
            }

            Location spawn = zone.getParachuteSpawn();
            if (spawn == null) {
                plugin.getLogger().warning("[Portal] Monde de la zone '"
                        + zone.getWorldName() + "' non chargé.");
                return;
            }

            // Démarrer le cooldown AVANT le TP pour éviter tout re-déclenchement
            cooldowns.add(uuid);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> cooldowns.remove(uuid), COOLDOWN_TICKS);

            // Son de portail
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.2f);

            // Lancer le parachute (le TP a lieu à l'intérieur)
            parachuteManager.launch(player, spawn, zone.getName());
            break;
        }
    }

    /** Nettoyage à la déconnexion. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Intercepte TOUTE modification de vélocité pendant le parachute.
     * Priorité HIGH pour écraser les jetpacks et autres plugins.
     * La composante Y est toujours forcée à ≤ -0.08 (descente obligatoire).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVelocity(PlayerVelocityEvent event) {
        if (!parachuteManager.isActive(event.getPlayer().getUniqueId())) return;
        Vector vel = event.getVelocity();
        if (vel.getY() > -0.08) {
            vel.setY(-0.08);
            event.setVelocity(vel);
        }
    }
}



