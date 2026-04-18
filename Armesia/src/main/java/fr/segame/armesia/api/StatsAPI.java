package fr.segame.armesia.api;

import fr.segame.armesia.managers.StatsManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatsAPI {

    private final StatsManager statsManager;

    public StatsAPI(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    public int getKills(UUID uuid) { return statsManager.getKills(uuid); }
    public int getDeaths(UUID uuid) { return statsManager.getDeaths(uuid); }
    public int getKillstreak(UUID uuid) { return statsManager.getKillstreak(uuid); }
    public int getBestKillstreak(UUID uuid) { return statsManager.getBestKillstreak(uuid); }
    public String getRatio(UUID uuid) { return statsManager.getFormattedKillDeathRatio(uuid); }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) { return statsManager.getTopKills(limit); }
    public List<Map.Entry<UUID, Integer>> getTopDeaths(int limit) { return statsManager.getTopDeaths(limit); }
    public List<Map.Entry<UUID, Integer>> getTopKillstreak(int limit) { return statsManager.getTopKillstreak(limit); }
    public List<Map.Entry<UUID, Integer>> getTopBestKillstreak(int limit) { return statsManager.getTopBestKillstreak(limit); }
}

