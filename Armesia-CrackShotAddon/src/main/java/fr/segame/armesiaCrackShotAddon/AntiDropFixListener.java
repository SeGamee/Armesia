package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponPrepareShootEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiDropFixListener implements Listener {

    private final CSUtility csUtility = new CSUtility();
    private final Map<UUID, Long> recentDrop = new HashMap<>();

    // 🔥 check config
    private boolean hasFixDrop(String weapon) {
        ConfigurationSection section = Main.getInstance()
                .getReloadListener()
                .getWeaponConfig(weapon);

        if (section == null) return false;
        if (!section.contains("Addon")) return false;

        return section.getConfigurationSection("Addon")
                .getBoolean("FixDrop", false);
    }

    private long getFixDelay(String weapon) {
        ConfigurationSection section = Main.getInstance()
                .getReloadListener()
                .getWeaponConfig(weapon);

        if (section == null) return 200;
        if (!section.contains("Addon")) return 200;

        return section.getConfigurationSection("Addon")
                .getLong("DropCancelShootDelay", 200);
    }

    // 🔥 ON LAISSE LE DROP NORMAL
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent e) {

        Player player = e.getPlayer();
        ItemStack item = e.getItemDrop().getItemStack();

        if (item == null) return;

        String weapon = csUtility.getWeaponTitle(item);

        if (weapon == null) return;
        if (!hasFixDrop(weapon)) return;

        // 🔥 on note le moment du drop
        recentDrop.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // 🔥 ON BLOQUE UNIQUEMENT LE TIR
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareShoot(WeaponPrepareShootEvent e) {

        Player player = e.getPlayer();
        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        String weapon = csUtility.getWeaponTitle(item);

        if (weapon == null) return;
        if (!hasFixDrop(weapon)) return;

        UUID uuid = player.getUniqueId();

        long last = recentDrop.getOrDefault(uuid, 0L);
        long now = System.currentTimeMillis();

        long delay = getFixDelay(weapon);

        // 🔥 SI drop récent → on bloque le tir
        if (now - last < delay) {
            e.setCancelled(true);
        }
    }
}