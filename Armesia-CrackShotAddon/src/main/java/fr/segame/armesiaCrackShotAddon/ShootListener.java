package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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

        // 🔥 ZONES AU TIR
        ConfigurationSection zones = section.getConfigurationSection("Zones");

        if (zones != null) {
            for (String key : zones.getKeys(false)) {

                ConfigurationSection zone = zones.getConfigurationSection(key);
                if (zone == null || !zone.getBoolean("Enabled", false)) continue;

                String trigger = zone.getString("Trigger", "IMPACT");

                if (trigger.equalsIgnoreCase("SHOOT")) {

                    Location loc = ZoneUtils.resolveLocation(zone, player, null);

                    Main.getInstance().getZoneManager()
                            .createZone(loc, zone);
                }
            }
        }

        // 🔥 ZONES À L'IMPACT
        if (zones != null
                && e.getProjectile() != null
                && e.getProjectile() instanceof org.bukkit.entity.Projectile projectile) {

            Main.getInstance().getImpactManager()
                    .trackProjectile(projectile, section, player);
        }
    }
}