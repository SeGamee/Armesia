package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.StatsManager;
import fr.segame.armesia.utils.APIProvider;
import fr.segame.armesiaLevel.api.LevelAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventPriority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class KillListener implements Listener {

    private final StatsManager statsManager;
    private final Main plugin;
    private final Random random = new Random();

    // Anti farm
    private final Map<UUID, Long> lastKills = new HashMap<>();

    public KillListener(Main plugin, StatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {

        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        UUID victimUUID = victim.getUniqueId();

        // Forcer le drop des items quelle que soit la gamerule keepInventory
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        // Death
        statsManager.addDeath(victimUUID);

        // Reset streak victime
        statsManager.setKillstreak(victimUUID, 0);

        // Refresh scoreboard victime
        Main.updatePlayerScoreboard(victimUUID);

        if (killer == null) return;

        UUID killerUUID = killer.getUniqueId();

        // ===== KILL CONFIG (chargée tôt pour l'antifarm aussi) =====
        ConfigurationSection killSection = plugin.getConfig().getConfigurationSection("kill");

        int minMoney = 100, maxMoney = 100, minXp = 100, maxXp = 100;
        String broadcastMsg = null, killerMsg = null, killedMsg = null;

        if (killSection != null) {
            ConfigurationSection rewardSection = killSection.getConfigurationSection("killerReward");
            if (rewardSection != null) {
                minMoney = rewardSection.getInt("minMoney", 100);
                maxMoney = rewardSection.getInt("maxMoney", 100);
                minXp    = rewardSection.getInt("minXp",    100);
                maxXp    = rewardSection.getInt("maxXp",    100);
            }
            broadcastMsg = killSection.getString("message");
            killerMsg    = killSection.getString("killerMessage");
            killedMsg    = killSection.getString("killedMessage");
        }

        int moneyReward = minMoney + (maxMoney > minMoney ? random.nextInt(maxMoney - minMoney + 1) : 0);
        int xpReward    = minXp    + (maxXp    > minXp    ? random.nextInt(maxXp    - minXp    + 1) : 0);

        // ===== ANTI FARM =====
        boolean antifarmEnabled = plugin.getConfig().getBoolean("antifarm.enabled", true);
        long cooldownMs = plugin.getConfig().getLong("antifarm.cooldown", 60) * 1000L;
        boolean isAntifarm = false;

        if (antifarmEnabled && lastKills.containsKey(victimUUID)) {
            long last = lastKills.get(victimUUID);
            if (System.currentTimeMillis() - last < cooldownMs) {
                isAntifarm = true;
                long remaining = (cooldownMs - (System.currentTimeMillis() - last)) / 1000;
                String afMsg = plugin.getConfig().getString("antifarm.message",
                        "§cKill non compté (anti-farm) — encore §e{remaining}s §c!");
                killer.sendMessage(afMsg
                        .replace("{remaining}", String.valueOf(remaining))
                        .replace("{killed}", victim.getName())
                        .replace("{player}", killer.getName()));
            }
        }

        if (!isAntifarm) {
            lastKills.put(victimUUID, System.currentTimeMillis());
        }

        // ===== KILL & STREAK (uniquement si pas antifarm) =====
        int streak = statsManager.getKillstreak(killerUUID);
        if (!isAntifarm) {
            statsManager.addKill(killerUUID);
            statsManager.addKillstreak(killerUUID);
            streak = statsManager.getKillstreak(killerUUID);
            if (streak > statsManager.getBestKillstreak(killerUUID)) {
                statsManager.setBestKillstreak(killerUUID, streak);
            }
            Main.updatePlayerScoreboard(killerUUID);
        }

        // ===== REWARD KILLER (uniquement si pas antifarm) =====
        var eco = APIProvider.getEconomy();
        if (!isAntifarm) {
            if (eco != null) eco.addMoney(killerUUID, moneyReward);
            LevelAPI.addXP(killerUUID, xpReward);
        }

        // ===== LOSS VICTIM (toujours) =====
        double victimBalance = eco != null ? eco.getMoney(victimUUID) : 0;
        double lostMoney = Math.min(victimBalance, moneyReward);
        if (lostMoney > 0 && eco != null) eco.removeMoney(victimUUID, lostMoney);

        // ===== MESSAGES (toujours) =====
        event.deathMessage(null);

        if (broadcastMsg != null) {
            Bukkit.broadcastMessage(broadcastMsg
                    .replace("{player}", killer.getName())
                    .replace("{killed}", victim.getName()));
        }

        if (killerMsg != null && !isAntifarm) {
            killer.sendMessage(killerMsg
                    .replace("{player}", killer.getName())
                    .replace("{killed}", victim.getName())
                    .replace("{money}", String.valueOf(moneyReward))
                    .replace("{xp}",   String.valueOf(xpReward))
                    .replace("{killstreak}", String.valueOf(streak)));
        }

        if (killedMsg != null) {
            victim.sendMessage(killedMsg
                    .replace("{player}", killer.getName())
                    .replace("{killed}", victim.getName())
                    .replace("{money}", String.format("%.0f", lostMoney))
                    .replace("{killstreak}", String.valueOf(streak)));
        }

        // ===== KILLSTREAK CONFIG (uniquement si pas antifarm) =====
        if (!isAntifarm) {
            ConfigurationSection ksSection = plugin.getConfig().getConfigurationSection("killstreak");
            if (ksSection != null) {
                for (String key : ksSection.getKeys(false)) {
                    int threshold;
                    try { threshold = Integer.parseInt(key); } catch (NumberFormatException e) { continue; }
                    if (streak != threshold) continue;
                    String message = ksSection.getString(key + ".message");
                    if (message != null) {
                        Bukkit.broadcastMessage(message
                                .replace("{player}", killer.getName())
                                .replace("{killed}", victim.getName())
                                .replace("{killstreak}", String.valueOf(streak)));
                    }
                    List<String> commands = ksSection.getStringList(key + ".commands");
                    for (String cmd : commands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd
                                .replace("{player}", killer.getName())
                                .replace("{killed}", victim.getName())
                                .replace("{killstreak}", String.valueOf(streak)));
                    }
                }
            }
        }
    }
}