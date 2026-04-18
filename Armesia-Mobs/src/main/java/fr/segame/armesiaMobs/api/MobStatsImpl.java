package fr.segame.armesiaMobs.api;

import fr.segame.armesiaMobs.managers.StatsManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobStatsImpl implements MobStatsAPI {

    private final StatsManager statsManager;

    public MobStatsImpl(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public int getKills(UUID playerUUID, String mobId) {
        return statsManager.getKills(playerUUID, mobId);
    }

    @Override
    public int getTotalKills(UUID playerUUID) {
        return statsManager.getTotalKills(playerUUID);
    }

    @Override
    public Map<String, Integer> getMobKills(UUID playerUUID) {
        return statsManager.getStats(playerUUID);
    }

    @Override
    public List<Map.Entry<UUID, Integer>> getTop(String mobId, int limit) {
        return statsManager.getTop(mobId, limit);
    }

    @Override
    public List<Map.Entry<UUID, Integer>> getTopTotal(int limit) {
        return statsManager.getTop(null, limit);
    }
}

