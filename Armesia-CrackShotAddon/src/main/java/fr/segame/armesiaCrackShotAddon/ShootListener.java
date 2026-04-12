package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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

        ConfigurationSection cp = section.getConfigurationSection("CustomParticles");
        if (cp != null && cp.getBoolean("Enabled", false)) {

            boolean followProjectile = cp.getBoolean("Follow_Projectile", false);

            if (followProjectile && e.getProjectile() != null) {
                if (e.getProjectile() instanceof org.bukkit.entity.Projectile projectile) {
                    Main.getInstance().getTrailManager()
                            .startProjectileTrail(projectile, cp);
                }
            } else {
                Location start = player.getEyeLocation();
                Vector direction = start.getDirection().normalize();

                Main.getInstance().getTrailManager()
                        .startEnergyTrail(player, start, direction, cp);
            }
        }

        // 🔥 IMPACT SYSTEM
        ConfigurationSection impact = section.getConfigurationSection("Impact");

        if (impact != null && impact.getBoolean("Enabled", false)
                && e.getProjectile() != null
                && e.getProjectile() instanceof org.bukkit.entity.Projectile projectile) {

            Main.getInstance().getImpactManager()
                    .trackProjectile(projectile, impact);
        }
    }
}