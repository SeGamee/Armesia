package fr.segame.armesiaLevel.config;

import fr.segame.armesiaLevel.ArmesiaLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Lit et expose toutes les valeurs de config.yml d'Armesia-Level.
 */
public class LevelConfig {

    private final ArmesiaLevel plugin;

    public LevelConfig(ArmesiaLevel plugin) {
        this.plugin = plugin;
    }

    // ── Paramètres généraux ──────────────────────────────────────

    public int getMaxLevel() {
        return plugin.getConfig().getInt("settings.max-level", 100);
    }

    public int getXpMultiplier() {
        return plugin.getConfig().getInt("settings.xp-formula-multiplier", 1000);
    }

    // ── Message & affichage level-up ─────────────────────────────

    public String getLevelUpMessage() {
        return plugin.getConfig().getString("level-up.message", "&bLevel up ! &7Niveau %level%");
    }

    public String getLevelUpTitleMain() {
        return plugin.getConfig().getString("level-up.title.main", "&6&l⬆ LEVEL UP ⬆");
    }

    public String getLevelUpTitleSub() {
        return plugin.getConfig().getString("level-up.title.sub", "&eNiveau &6%level%");
    }

    public int getLevelUpTitleFadeIn()  { return plugin.getConfig().getInt("level-up.title.fade-in",  10); }
    public int getLevelUpTitleStay()    { return plugin.getConfig().getInt("level-up.title.stay",     60); }
    public int getLevelUpTitleFadeOut() { return plugin.getConfig().getInt("level-up.title.fade-out", 20); }

    public String getLevelUpSoundId() {
        return plugin.getConfig().getString("level-up.sound.id", "ENTITY_PLAYER_LEVELUP");
    }

    public float getLevelUpSoundVolume() {
        return (float) plugin.getConfig().getDouble("level-up.sound.volume", 1.0);
    }

    public float getLevelUpSoundPitch() {
        return (float) plugin.getConfig().getDouble("level-up.sound.pitch", 1.5);
    }

    // ── Paliers spéciaux ─────────────────────────────────────────

    /**
     * Retourne la liste des actions configurées pour un palier donné.
     * Retourne une liste vide si aucune action n'est définie pour ce niveau.
     */
    public List<MilestoneAction> getMilestoneActions(int level) {
        List<Map<?, ?>> raw = plugin.getConfig().getMapList("milestones." + level);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<MilestoneAction> actions = new ArrayList<>();
        for (Map<?, ?> map : raw) {
            MilestoneAction action = parseAction(map);
            if (action != null) actions.add(action);
        }
        return actions;
    }

    // ── Parsing interne ──────────────────────────────────────────

    private MilestoneAction parseAction(Map<?, ?> map) {
        String typeStr = getString(map, "type", "").toUpperCase();
        MilestoneAction.Type type;
        try {
            type = MilestoneAction.Type.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[LevelConfig] Type d'action inconnu : '" + typeStr + "' — ignoré.");
            return null;
        }

        String value    = getString(map, "value",    "");
        String subtitle = getString(map, "subtitle", "");
        String soundId  = getString(map, "id",       "UI_TOAST_CHALLENGE_COMPLETE");
        float  volume   = getFloat(map,  "volume",   1.0f);
        float  pitch    = getFloat(map,  "pitch",    1.0f);
        int    fadeIn   = getInt(map,    "fade-in",  10);
        int    stay     = getInt(map,    "stay",     60);
        int    fadeOut  = getInt(map,    "fade-out", 20);

        return new MilestoneAction(type, value, subtitle, soundId, volume, pitch, fadeIn, stay, fadeOut);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String getString(Map<?, ?> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String s ? s : def;
    }

    private float getFloat(Map<?, ?> map, String key, float def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.floatValue();
        return def;
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }
}

