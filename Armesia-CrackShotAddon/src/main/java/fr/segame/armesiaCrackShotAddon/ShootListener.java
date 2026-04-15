package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ShootListener implements Listener {

    private final CSUtility csUtility = new CSUtility();

    @EventHandler
    public void onShoot(WeaponShootEvent e) {

        Player player = e.getPlayer();
        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        String weapon = csUtility.getWeaponTitle(item);
        if (weapon == null) return;

        ConfigurationSection section = Main.getInstance()
                .getReloadListener()
                .getWeaponConfig(weapon);

        if (section == null) return;

        // 🔥 ZONES AU TIR (Trigger: SHOOT)
        ConfigurationSection zones = section.getConfigurationSection("Zones");

        if (zones != null) {
            for (String key : zones.getKeys(false)) {

                ConfigurationSection zone = zones.getConfigurationSection(key);
                if (zone == null || !zone.getBoolean("Enabled", false)) continue;

                if (zone.getString("Trigger", "IMPACT").equalsIgnoreCase("SHOOT")) {
                    Location loc = ZoneUtils.resolveLocation(zone, player, null);
                    Main.getInstance().getZoneManager().createZone(loc, zone);
                }
            }
        }

        // 🔥 TRAIL + IMPACT SUR LE PROJECTILE
        if (e.getProjectile() != null
                && e.getProjectile() instanceof Projectile projectile) {

            // ── CustomParticles → Trail ──────────────────────────────
            ConfigurationSection particles = section.getConfigurationSection("CustomParticles");
            if (particles != null && particles.getBoolean("Enabled", false)) {
                Main.getInstance().getTrailManager()
                        .startProjectileTrail(projectile, particles);
            }

            // ── Impact particles + Zones IMPACT ─────────────────────
            boolean hasImpactParticles = section.getConfigurationSection("Impact") != null
                    && section.getBoolean("Impact.Enabled", false);
            boolean hasImpactZones = zones != null;

            if (hasImpactParticles || hasImpactZones) {
                Main.getInstance().getImpactManager()
                        .trackProjectile(projectile, section, player);
            }
        }
    }
}