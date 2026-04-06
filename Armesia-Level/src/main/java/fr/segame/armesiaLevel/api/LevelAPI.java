package fr.segame.armesiaLevel.api;

import fr.segame.armesiaLevel.managers.LevelManager;

import java.util.UUID;

/**
 * API statique exposant le système de niveau aux plugins tiers
 * (Armesia, Armesia-Scoreboard…).
 */
public final class LevelAPI {

    private static LevelManager levelManager;

    private LevelAPI() {}

    /** Appelé par ArmesiaLevel au démarrage. */
    public static void init(LevelManager lm) {
        levelManager = lm;
    }

    public static boolean isAvailable() {
        return levelManager != null;
    }

    public static int getLevel(UUID uuid) {
        return levelManager != null ? levelManager.getLevel(uuid) : 1;
    }

    public static int getXP(UUID uuid) {
        return levelManager != null ? levelManager.getXP(uuid) : 0;
    }

    public static void addXP(UUID uuid, int amount) {
        if (levelManager != null) levelManager.addXP(uuid, amount);
    }

    public static void removeXP(UUID uuid, int amount) {
        if (levelManager != null) levelManager.removeXP(uuid, amount);
    }

    public static void addLevel(UUID uuid, int amount) {
        if (levelManager != null) levelManager.addLevel(uuid, amount);
    }

    public static void removeLevel(UUID uuid, int amount) {
        if (levelManager != null) levelManager.removeLevel(uuid, amount);
    }

    public static String getXPBar(UUID uuid, int sizeBar) {
        return levelManager != null ? levelManager.getXPBar(uuid, sizeBar) : "";
    }
}

