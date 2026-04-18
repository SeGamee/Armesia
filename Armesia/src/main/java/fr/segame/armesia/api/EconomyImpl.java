package fr.segame.armesia.api;

import fr.segame.armesia.managers.EconomyManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation concrète de {@link EconomyAPI} qui délègue à {@link EconomyManager}.
 * Enregistrée dans le ServicesManager au démarrage du plugin Armesia.
 */
public class EconomyImpl implements EconomyAPI {

    private final EconomyManager manager;
    private final DecimalFormat format;

    public EconomyImpl(EconomyManager manager) {
        this.manager = manager;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        this.format = new DecimalFormat("#,##0.##", symbols);
    }

    // ---------- MONEY ----------

    @Override
    public double getMoney(UUID uuid) {
        return manager.getMoney(uuid);
    }

    @Override
    public void setMoney(UUID uuid, double amount) {
        manager.setMoney(uuid, amount);
    }

    @Override
    public void addMoney(UUID uuid, double amount) {
        manager.addMoney(uuid, amount);
    }

    @Override
    public boolean removeMoney(UUID uuid, double amount) {
        return manager.removeMoney(uuid, amount);
    }

    @Override
    public boolean hasMoney(UUID uuid, double amount) {
        return manager.hasMoney(uuid, amount);
    }

    @Override
    public double getMaxMoney() {
        return manager.getMaxMoney();
    }

    // ---------- TOKENS ----------

    @Override
    public int getTokens(UUID uuid) {
        return manager.getTokens(uuid);
    }

    @Override
    public void setTokens(UUID uuid, int amount) {
        manager.setTokens(uuid, amount);
    }

    @Override
    public void addTokens(UUID uuid, int amount) {
        manager.addTokens(uuid, amount);
    }

    @Override
    public boolean removeTokens(UUID uuid, int amount) {
        return manager.removeTokens(uuid, amount);
    }

    @Override
    public int getMaxTokens() {
        return manager.getMaxTokens();
    }

    // ---------- FORMAT ----------

    @Override
    public String formatMoney(double amount) {
        double floored = Math.floor(amount * 100.0) / 100.0;
        return format.format(floored) + " $";
    }

    @Override
    public String formatTokens(int amount) {
        // Les tokens sont des entiers, mais on applique le même format pour cohérence
        return format.format(amount) + " 🪙";
    }

    // ---------- LEADERBOARDS ----------

    @Override
    public List<Map.Entry<UUID, Double>> getTopMoney(int limit) {
        return manager.getTopMoney(limit);
    }

    @Override
    public List<Map.Entry<UUID, Integer>> getTopTokens(int limit) {
        return manager.getTopTokens(limit);
    }
}
