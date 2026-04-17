package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.events.WeaponExplodeEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ExplosionListener implements Listener {

    @EventHandler
    public void onWeaponExplode(WeaponExplodeEvent e) {

        Player player = e.getPlayer();
        if (player == null) return;

        String weaponTitle = e.getWeaponTitle();
        if (weaponTitle == null) return;

        Location explosionLoc = e.getLocation();
        if (explosionLoc == null) return;

        ConfigurationSection weaponSection = Main.getInstance()
                .getReloadListener()
                .getWeaponConfig(weaponTitle);

        if (weaponSection == null) return;

        ConfigurationSection zones = weaponSection.getConfigurationSection("Zones");
        if (zones == null) return;

        for (String key : zones.getKeys(false)) {

            ConfigurationSection zone = zones.getConfigurationSection(key);
            if (zone == null || !zone.getBoolean("Enabled", false)) continue;

            String trigger = zone.getString("Trigger", "IMPACT");
            if (!trigger.equalsIgnoreCase("EXPLODE")) continue;

            // Résolution de la position de la zone
            Location loc = resolveLocation(zone, player, explosionLoc);

            // Son de déclenchement (optionnel)
            String soundName = zone.getString("Sound");
            if (soundName != null) {
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    loc.getWorld().playSound(loc, sound, 1f, 1f);
                } catch (Exception ignored) {}
            }

            Main.getInstance().getZoneManager().createZone(loc, zone);
        }
    }

    /**
     * Résout la position de la zone selon la clé "Location" de la config.
     * EXPLODE / IMPACT → position de l'explosion
     * PLAYER           → position du joueur
     */
    private Location resolveLocation(ConfigurationSection zone, Player player, Location explosionLoc) {
        String locType = zone.getString("Location", "EXPLODE");
        return switch (locType.toUpperCase()) {
            case "PLAYER" -> player.getLocation();
            default -> explosionLoc;
        };
    }
}

