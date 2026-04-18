package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;;

public class EconomyManager {

    private final Main plugin;

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
    }

    // ---------- MONEY ----------
    public double getMoney(UUID uuid) {
        return plugin.getPlayersConfig().getDouble("players." + uuid + ".money", 0.0);
    }

    public void setMoney(UUID uuid, double amount) {
        double max = plugin.getConfig().getDouble("economy.money.max-balance", 1_000_000.0);
        plugin.getPlayersConfig().set("players." + uuid + ".money", Math.max(0.0, Math.min(amount, max)));
        plugin.savePlayers();
        Main.updatePlayerScoreboard(uuid);
    }

    public double getMaxMoney() {
        return plugin.getConfig().getDouble("economy.money.max-balance", 1_000_000.0);
    }

    public void addMoney(UUID uuid, double amount) {
        if (amount <= 0) return;
        setMoney(uuid, getMoney(uuid) + amount);
    }

    public boolean removeMoney(UUID uuid, double amount) {
        double money = getMoney(uuid);
        if (money < amount) return false;

        setMoney(uuid, money - amount);
        return true;
    }

    public boolean hasMoney(UUID uuid, double amount) {
        return getMoney(uuid) >= amount;
    }

    // ---------- TOKENS ----------
    public int getTokens(UUID uuid) {
        return plugin.getPlayersConfig().getInt("players." + uuid + ".tokens", 0);
    }

    public void setTokens(UUID uuid, int amount) {
        int max = plugin.getConfig().getInt("economy.tokens.max-balance", 100_000);
        plugin.getPlayersConfig().set("players." + uuid + ".tokens", Math.max(0, Math.min(amount, max)));
        plugin.savePlayers();
        Main.updatePlayerScoreboard(uuid);
    }

    public int getMaxTokens() {
        return plugin.getConfig().getInt("economy.tokens.max-balance", 100_000);
    }

    public void addTokens(UUID uuid, int amount) {
        if (amount <= 0) return;
        setTokens(uuid, getTokens(uuid) + amount);
    }

    public boolean removeTokens(UUID uuid, int amount) {
        int tokens = getTokens(uuid);
        if (tokens < amount) return false;

        setTokens(uuid, tokens - amount);
        return true;
    }

    public boolean hasTokens(UUID uuid, int amount) {
        return getTokens(uuid) >= amount;
    }

    // ---------- LEADERBOARDS ----------

    public List<Map.Entry<UUID, Double>> getTopMoney(int limit) {
        ConfigurationSection players = plugin.getPlayersConfig().getConfigurationSection("players");
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double val = plugin.getPlayersConfig().getDouble("players." + key + ".money", 0.0);
                    if (val > 0) list.add(Map.entry(uuid, val));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<UUID, Integer>> getTopTokens(int limit) {
        ConfigurationSection players = plugin.getPlayersConfig().getConfigurationSection("players");
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>();
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int val = plugin.getPlayersConfig().getInt("players." + key + ".tokens", 0);
                    if (val > 0) list.add(Map.entry(uuid, val));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }
}