package fr.segame.armesia.api;

import java.util.UUID;

/**
 * API publique d'économie exposée via le ServicesManager de Bukkit.
 * Utilisez {@link fr.segame.armesia.utils.APIProvider#getEconomy()} pour y accéder.
 */
public interface EconomyAPI {

    // ---------- MONEY ----------
    double getMoney(UUID uuid);
    void setMoney(UUID uuid, double amount);
    void addMoney(UUID uuid, double amount);
    boolean removeMoney(UUID uuid, double amount);
    boolean hasMoney(UUID uuid, double amount);
    double getMaxMoney();

    // ---------- TOKENS ----------
    int getTokens(UUID uuid);
    void setTokens(UUID uuid, int amount);
    void addTokens(UUID uuid, int amount);
    boolean removeTokens(UUID uuid, int amount);
    int getMaxTokens();

    // ---------- FORMAT ----------
    String formatMoney(double amount);
    String formatTokens(int amount);
}