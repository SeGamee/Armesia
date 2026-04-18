package fr.segame.armesia.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;;

public class StatsManager {

    private final FileConfiguration config;

    public StatsManager(FileConfiguration config) {
        this.config = config;
    }

    private String getPath(UUID uuid) {
        return "players." + uuid.toString();
    }

    // ===== KILLS =====
    public void addKill(UUID uuid) {
        int kills = config.getInt(getPath(uuid) + ".kills");
        config.set(getPath(uuid) + ".kills", kills + 1);
    }

    public int getKills(UUID uuid) {
        return config.getInt(getPath(uuid) + ".kills");
    }

    public void setKills(UUID uuid, int value) {
        config.set(getPath(uuid) + ".kills", Math.max(0, value));
    }

    // ===== DEATHS =====
    public void addDeath(UUID uuid) {
        int deaths = config.getInt(getPath(uuid) + ".deaths");
        config.set(getPath(uuid) + ".deaths", deaths + 1);
    }

    public int getDeaths(UUID uuid) {
        return config.getInt(getPath(uuid) + ".deaths");
    }

    public void setDeaths(UUID uuid, int value) {
        config.set(getPath(uuid) + ".deaths", Math.max(0, value));
    }

    // ===== KILLSTREAK =====
    public void addKillstreak(UUID uuid) {
        int streak = config.getInt(getPath(uuid) + ".killstreak");
        config.set(getPath(uuid) + ".killstreak", streak + 1);
    }

    public void setKillstreak(UUID uuid, int value) {
        config.set(getPath(uuid) + ".killstreak", Math.max(0, value));
    }

    public int getKillstreak(UUID uuid) {
        return config.getInt(getPath(uuid) + ".killstreak");
    }

    // ===== BEST STREAK =====
    public int getBestKillstreak(UUID uuid) {
        return config.getInt(getPath(uuid) + ".bestkillstreak");
    }

    public void setBestKillstreak(UUID uuid, int value) {
        config.set(getPath(uuid) + ".bestkillstreak", Math.max(0, value));
    }
    
    // Ratio Kill/mort
    public double getKillDeathRatio(UUID uuid) {
        int kills = getKills(uuid);
        int deaths = getDeaths(uuid);
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    public String getFormattedKillDeathRatio(UUID uuid) {
        double ratio = getKillDeathRatio(uuid);
        String formatted = String.format("%.2f", ratio);
        if (formatted.charAt(formatted.length() - 1) == '0') {
            formatted = String.format("%.1f", ratio);
        }
        return formatted;
    }

    // ===== LEADERBOARDS =====

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        return getTop("kills", limit);
    }

    public List<Map.Entry<UUID, Integer>> getTopDeaths(int limit) {
        return getTop("deaths", limit);
    }

    public List<Map.Entry<UUID, Integer>> getTopKillstreak(int limit) {
        return getTop("killstreak", limit);
    }

    public List<Map.Entry<UUID, Integer>> getTopBestKillstreak(int limit) {
        return getTop("bestkillstreak", limit);
    }

    private List<Map.Entry<UUID, Integer>> getTop(String stat, int limit) {
        ConfigurationSection players = config.getConfigurationSection("players");
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int val = config.getInt("players." + key + "." + stat, 0);
                    if (val > 0) list.add(Map.entry(uuid, val));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

}