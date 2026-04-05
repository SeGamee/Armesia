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

        // 🔥 Récup config de l’arme
        ConfigurationSection section = Main.getInstance()
                .getReloadListener()
                .getWeaponConfig(weapon);

        if (section == null) return;

        // 🔥 Vérifie CustomParticles
        if (!section.contains("CustomParticles")) return;

        ConfigurationSection cp = section.getConfigurationSection("CustomParticles");
        if (cp == null) return;

        if (!cp.getBoolean("Enabled", false)) return;

        // 🔥 Position + direction
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();

        // 🔥 Lance le trail avec la config directe
        Main.getInstance().getTrailManager()
                .startEnergyTrail(player, start, direction, cp);
    }
}