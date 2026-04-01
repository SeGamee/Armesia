package fr.segame.armesia.managers;

import fr.segame.armesia.Main;

import java.util.UUID;

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
        plugin.getPlayersConfig().set("players." + uuid + ".money", amount);
        plugin.savePlayers();
        Main.updatePlayerScoreboard(uuid);
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
        plugin.getPlayersConfig().set("players." + uuid + ".tokens", amount);
        plugin.savePlayers();
        Main.updatePlayerScoreboard(uuid);
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
}