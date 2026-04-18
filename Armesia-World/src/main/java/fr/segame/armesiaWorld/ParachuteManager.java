package fr.segame.armesiaWorld;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère le TP parachute :
 * - Téléporte 70 blocs au-dessus du sol réel
 * - SLOW_FALLING (icône cachée) + dérive horizontale vers la direction de regard
 * - Sneak = descente accélérée
 * - Particules toile d'araignée en dôme + sillage derrière
 * - Parachute retiré à 5 blocs du sol (chute libre finale)
 */
public class ParachuteManager {

    /** Vitesse horizontale de glissement (blocs/tick). */
    private static final double GLIDE_SPEED   = 1.1;
    /** Vitesse de descente forcée quand le joueur sneake (blocs/tick, négatif = bas). */
    private static final double SNEAK_Y_SPEED = -0.55;
    /** Distance au sol (blocs) à laquelle le parachute est retiré. */
    private static final double LAND_THRESHOLD = 5.0;

    private final MainWorld plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public ParachuteManager(MainWorld plugin) {
        this.plugin = plugin;
    }

    // ─── API publique ─────────────────────────────────────────────────────────

    /**
     * Lance le parachute pour un joueur.
     *
     * @param player      Le joueur à TP
     * @param destination La position en hauteur (déjà calculée par MapZone)
     * @param cityName    Nom affiché en gros titre
     */
    public void launch(Player player, Location destination, String cityName) {
        cancel(player.getUniqueId()); // annule un éventuel parachute actif

        // 1. Téléportation en hauteur
        player.teleport(destination);
        player.setFallDistance(0f);

        // 2. Effet SLOW_FALLING – icône cachée, sans particules visibles
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING, 6000, 0, false, false, false));

        // 3. Si le joueur vole (créatif), on coupe le vol pour qu'il descende
        if (player.isFlying()) player.setFlying(false);

        // 4. Titre initial
        showTitle(player, cityName);

        // final Particle.DustOptions whiteDust = new Particle.DustOptions(Color.WHITE, 1.3f);

        // Tâche toutes les 2 ticks (0.1 s) pour un glissement fluide
        BukkitTask task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                tick++;

                player.setFallDistance(0f);
                Location pLoc = player.getLocation();

                // ─── Retrait du parachute à 5 blocs du sol ────────────────────
                int groundY = player.getWorld().getHighestBlockYAt(pLoc);
                if (pLoc.getY() - groundY <= LAND_THRESHOLD) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    player.setFallDistance(0f);
                    activeTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // ─── Dérive horizontale (direction de regard) ─────────────────
                Vector horiz = pLoc.getDirection().clone();
                horiz.setY(0);
                if (horiz.lengthSquared() > 0.001) horiz.normalize().multiply(GLIDE_SPEED);

                Vector vel = player.getVelocity();
                vel.setX(horiz.getX());
                vel.setZ(horiz.getZ());

                // Forcer la descente : Y toujours ≤ 0 (on ne peut jamais monter)
                if (player.isSneaking()) {
                    vel.setY(SNEAK_Y_SPEED);          // descente rapide
                } else {
                    vel.setY(Math.min(vel.getY(), -0.08)); // descente douce forcée
                }
                player.setVelocity(vel);

                // Particules désactivées
                // Dôme circulaire au-dessus du joueur (8 points)
                // for (int i = 0; i < 8; i++) {
                //     double angle = (Math.PI * 2.0 / 8) * i;
                //     player.getWorld().spawnParticle(Particle.REDSTONE,
                //             pLoc.getX() + Math.cos(angle) * 2.0,
                //             pLoc.getY() + 3.0,
                //             pLoc.getZ() + Math.sin(angle) * 2.0,
                //             1, 0, 0, 0, 0, whiteDust);
                // }
                // Centre du dôme
                // player.getWorld().spawnParticle(Particle.REDSTONE,
                //         pLoc.getX(), pLoc.getY() + 3.5, pLoc.getZ(),
                //         2, 0.4, 0.1, 0.4, 0, whiteDust);

                // Sillage derrière le joueur
                // if (horiz.lengthSquared() > 0.001) {
                //     Vector behind = horiz.clone().normalize().multiply(-1.5);
                //     player.getWorld().spawnParticle(Particle.REDSTONE,
                //             pLoc.getX() + behind.getX(),
                //             pLoc.getY() + 1.2,
                //             pLoc.getZ() + behind.getZ(),
                //             3, 0.2, 0.15, 0.2, 0, whiteDust);
                // }
            }
        }.runTaskTimer(plugin, 10L, 2L);

        activeTasks.put(player.getUniqueId(), task);
    }

    /** Annule le parachute actif d'un joueur (déconnexion, commande, etc.). */
    public void cancel(UUID uuid) {
        BukkitTask t = activeTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public boolean isActive(UUID uuid) {
        return activeTasks.containsKey(uuid);
    }

    // ─── Interne ─────────────────────────────────────────────────────────────

    private void showTitle(Player player, String cityName) {
        player.sendTitle(
                "§a§l" + cityName,
                "§7Sneak pour descendre plus rapidement",
                5, 40, 5   // fadeIn=5t, stay=40t (2s), fadeOut=5t
        );
    }
}