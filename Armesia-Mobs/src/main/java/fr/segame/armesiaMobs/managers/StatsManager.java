package fr.segame.armesiaMobs.managers;

import fr.segame.armesiaMobs.ArmesiaMobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Gère les statistiques de kills des joueurs (par mob).
 * Persiste dans stats.yml au data folder du plugin.
 */
public class StatsManager {

    private final ArmesiaMobs plugin;
    private final File file;
    /** playerUUID → mobId → kills */
    private final Map<UUID, Map<String, Integer>> stats = new HashMap<>();

    public StatsManager(ArmesiaMobs plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    // ─── API ──────────────────────────────────────────────────────────────────

    /** Incrémente le compteur de kills pour ce joueur et ce mob. */
    public void addKill(UUID playerUUID, String mobId) {
        addKills(playerUUID, mobId, 1);
    }

    /** Ajoute {@code amount} kills pour ce joueur et ce mob. */
    public void addKills(UUID playerUUID, String mobId, int amount) {
        if (amount <= 0) return;
        stats.computeIfAbsent(playerUUID, k -> new HashMap<>())
             .merge(mobId, amount, Integer::sum);
    }

    /** Retire {@code amount} kills pour ce joueur et ce mob (plancher à 0, ne passe jamais négatif). */
    public void removeKills(UUID playerUUID, String mobId, int amount) {
        if (amount <= 0) return;
        Map<String, Integer> ps = stats.get(playerUUID);
        if (ps == null) return;
        int current = ps.getOrDefault(mobId, 0);
        int newVal  = Math.max(0, current - amount);
        if (newVal == 0) {
            ps.remove(mobId);
            if (ps.isEmpty()) stats.remove(playerUUID);
        } else {
            ps.put(mobId, newVal);
        }
    }

    /** Retourne la map mobId → kills pour un joueur (vide si aucun). */
    public Map<String, Integer> getStats(UUID playerUUID) {
        return Collections.unmodifiableMap(stats.getOrDefault(playerUUID, Collections.emptyMap()));
    }

    /** Retourne le nombre de kills d'un joueur pour un mob précis. */
    public int getKills(UUID playerUUID, String mobId) {
        Map<String, Integer> ps = stats.get(playerUUID);
        return ps == null ? 0 : ps.getOrDefault(mobId, 0);
    }

    /** Retourne le total de kills (tous mobs) pour un joueur. */
    public int getTotalKills(UUID playerUUID) {
        Map<String, Integer> ps = stats.get(playerUUID);
        return ps == null ? 0 : ps.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Réinitialise toutes les stats d'un joueur. */
    public void resetStats(UUID playerUUID) {
        stats.remove(playerUUID);
    }

    /** Réinitialise les stats d'un joueur pour un mob précis. */
    public void resetStats(UUID playerUUID, String mobId) {
        Map<String, Integer> ps = stats.get(playerUUID);
        if (ps == null) return;
        ps.remove(mobId);
        if (ps.isEmpty()) stats.remove(playerUUID);
    }

    /**
     * Retourne le top N des joueurs par kills.
     * @param mobId  filtre sur un mob précis ; null = toutes tués confondus
     * @param limit  nombre d'entrées max
     */
    public List<Map.Entry<UUID, Integer>> getTop(String mobId, int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Integer>> entry : stats.entrySet()) {
            int kills = (mobId == null)
                    ? entry.getValue().values().stream().mapToInt(Integer::intValue).sum()
                    : entry.getValue().getOrDefault(mobId, 0);
            if (kills > 0) list.add(Map.entry(entry.getKey(), kills));
        }
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

    // ─── Persistance ─────────────────────────────────────────────────────────

    /** Sauvegarde toutes les stats dans stats.yml. */
    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Integer>> entry : stats.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Integer> kill : entry.getValue().entrySet()) {
                cfg.set("stats." + uuidStr + "." + kill.getKey(), kill.getValue());
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[StatsManager] Impossible de sauvegarder stats.yml.", e);
        }
    }

    private void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        var section = cfg.getConfigurationSection("stats");
        if (section == null) return;
        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                var mobSection = section.getConfigurationSection(uuidStr);
                if (mobSection == null) continue;
                Map<String, Integer> ps = new HashMap<>();
                for (String mobId : mobSection.getKeys(false)) {
                    ps.put(mobId, mobSection.getInt(mobId, 0));
                }
                stats.put(uuid, ps);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}


