package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.LevelManager;
import fr.segame.armesia.managers.StatsManager;
import fr.segame.armesia.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillListener implements Listener {

    private final StatsManager statsManager;
    private final EconomyManager economyManager;
    private final Main plugin;
    
    // Anti farm
    private final Map<UUID, Long> lastKills = new HashMap<>();

    // Streak rewards
    private final Map<Integer, Integer> streakRewards = new HashMap<>();

    public KillListener(Main plugin, StatsManager statsManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.economyManager = economyManager;

        // Configurable
        streakRewards.put(3, 100);
        streakRewards.put(5, 200);
        streakRewards.put(10, 500);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        UUID victimUUID = victim.getUniqueId();

        // Death
        statsManager.addDeath(victimUUID);

        // Reset streak victime
        statsManager.setKillstreak(victimUUID, 0);

        if (killer == null) return;

        UUID killerUUID = killer.getUniqueId();

        // ===== ANTI FARM =====
        if (lastKills.containsKey(victimUUID)) {
            long last = lastKills.get(victimUUID);

            if (System.currentTimeMillis() - last < 60000) {
                killer.sendMessage("§cKill non compté (anti-farm)");
                return;
            }
        }

        lastKills.put(victimUUID, System.currentTimeMillis());

        // ===== KILL =====
        statsManager.addKill(killerUUID);

        plugin.getLevelManager().addXP(killer.getUniqueId(), 100);

        killer.sendMessage("§aKill enregistré ! +100 XP");

        // ===== STREAK =====
        statsManager.addKillstreak(killerUUID);
        int streak = statsManager.getKillstreak(killerUUID);

        // Record
        if (streak > statsManager.getBestKillstreak(killerUUID)) {
            statsManager.setBestKillstreak(killerUUID, streak);
        }

        // ===== REWARD =====
        int reward = 100 + (streak * 10);
        economyManager.addMoney(killerUUID, reward);

        // ===== BONUS STREAK =====
        if (streakRewards.containsKey(streak)) {
            int bonus = streakRewards.get(streak);
            economyManager.addMoney(killerUUID, bonus);

            killer.sendMessage("§6Bonus streak: +" + bonus + "$");
        }

        // ===== ANNOUNCES =====
        if (streak == 3) {
            Bukkit.broadcastMessage("§e" + killer.getName() + " est en série de 3 kills !");
        }

        if (streak == 5) {
            Bukkit.broadcastMessage("§6" + killer.getName() + " est en feu !");
        }

        if (streak == 10) {
            Bukkit.broadcastMessage("§c⚠ " + killer.getName() + " DOMINE LE SERVEUR !");
        }

        // ===== MESSAGE =====
        killer.sendMessage("§6[Kill] §f+" + reward + "$ §7(Streak: " + streak + ")");
    }
}