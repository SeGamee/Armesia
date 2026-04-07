package fr.segame.armesia.api;

import fr.segame.armesia.managers.StatsManager;

import java.util.UUID;

public class StatsAPI {

    private final StatsManager statsManager;

    public StatsAPI(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    public int getKills(UUID uuid) {
        return statsManager.getKills(uuid);
    }

    public int getDeaths(UUID uuid) {
        return statsManager.getDeaths(uuid);
    }

    public int getKillstreak(UUID uuid) {
        return statsManager.getKillstreak(uuid);
    }

    public String getRatio(UUID uuid) { return statsManager.getFormattedKillDeathRatio(uuid); }
}