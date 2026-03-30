package fr.segame.armesia.api;

import fr.segame.armesia.managers.EconomyManager;

import java.text.DecimalFormat;
import java.util.UUID;

public class EconomyAPI {

    private final EconomyManager manager;
    private final DecimalFormat format = new DecimalFormat("#,###");

    public EconomyAPI(EconomyManager manager) {
        this.manager = manager;
    }

    // ---------- MONEY ----------
    public double getMoney(UUID uuid) {
        return manager.getMoney(uuid);
    }

    public void addMoney(UUID uuid, double amount) {
        manager.addMoney(uuid, amount);
    }

    public boolean removeMoney(UUID uuid, double amount) {
        return manager.removeMoney(uuid, amount);
    }

    public boolean hasMoney(UUID uuid, double amount) {
        return manager.hasMoney(uuid, amount);
    }

    // ---------- TOKENS ----------
    public int getTokens(UUID uuid) {
        return manager.getTokens(uuid);
    }

    public void addTokens(UUID uuid, int amount) {
        manager.addTokens(uuid, amount);
    }

    public boolean removeTokens(UUID uuid, int amount) {
        return manager.removeTokens(uuid, amount);
    }

    // ---------- FORMAT ----------
    public String formatMoney(double amount) {
        return format.format(amount) + " $";
    }

    public String formatTokens(int amount) {
        return format.format(amount) + " ⛃";
    }
}