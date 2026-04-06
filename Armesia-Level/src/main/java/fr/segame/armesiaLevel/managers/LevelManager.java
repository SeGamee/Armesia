package fr.segame.armesiaLevel.managers;

import fr.segame.armesiaLevel.ArmesiaLevel;
import fr.segame.armesiaLevel.config.LevelConfig;
import fr.segame.armesiaLevel.config.MilestoneAction;
import fr.segame.armesiaLevel.config.MilestoneExecutor;
import fr.segame.armesiaLevel.player.GamePlayer;
import fr.segame.armesiaLevel.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class LevelManager {

    private final PlayerManager playerManager;
    private final ArmesiaLevel  plugin;

    public LevelManager(PlayerManager playerManager, ArmesiaLevel plugin) {
        this.playerManager = playerManager;
        this.plugin        = plugin;
    }

    // ─── Lecture ────────────────────────────────────────────────────────────────

    public int getXP(UUID uuid) {
        return playerManager.getPlayer(uuid).getXp();
    }

    public int getLevel(UUID uuid) {
        return playerManager.getPlayer(uuid).getLevel();
    }

    // ─── Modification ───────────────────────────────────────────────────────────

    public void addXP(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        LevelConfig cfg   = plugin.getLevelConfig();
        int maxLevel      = cfg.getMaxLevel();
        int xpMult        = cfg.getXpMultiplier();

        int xp    = player.getXp() + amount;
        int level = player.getLevel();

        if (level >= maxLevel) {
            level = maxLevel;
            xp    = Math.min(xp, maxLevel * xpMult);
        } else {
            while (level < maxLevel) {
                int xpNeeded = level * xpMult;
                if (xp >= xpNeeded) {
                    xp -= xpNeeded;
                    level++;
                    sendLevelUpFeedback(uuid, level);
                } else {
                    break;
                }
            }
            if (level >= maxLevel) {
                level = maxLevel;
                xp    = Math.min(xp, maxLevel * xpMult);
            }
        }

        if (xp    < 0) xp    = 0;
        if (level < 1) level = 1;

        player.setXp(xp);
        player.setLevel(level);
        persist(uuid, xp, level);
        notifyChange(uuid);
    }

    public void removeXP(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        LevelConfig cfg   = plugin.getLevelConfig();
        int xpMult        = cfg.getXpMultiplier();

        int xp    = player.getXp() - amount;
        int level = player.getLevel();

        while (xp < 0 && level > 1) {
            level--;
            xp += level * xpMult;
        }
        if (xp    < 0) xp    = 0;
        if (level < 1) level = 1;

        int maxLevel = cfg.getMaxLevel();
        if (level >= maxLevel && xp > maxLevel * xpMult) xp = maxLevel * xpMult;

        player.setXp(xp);
        player.setLevel(level);
        persist(uuid, xp, level);
        notifyChange(uuid);
    }

    public void addLevel(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        LevelConfig cfg   = plugin.getLevelConfig();
        int maxLevel      = cfg.getMaxLevel();

        int level = Math.min(maxLevel, Math.max(1, player.getLevel() + amount));
        int xp    = player.getXp();
        if (level == maxLevel && xp > maxLevel * cfg.getXpMultiplier())
            xp = maxLevel * cfg.getXpMultiplier();

        player.setLevel(level);
        player.setXp(xp);
        persist(uuid, xp, level);
        notifyChange(uuid);
    }

    public void removeLevel(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        LevelConfig cfg   = plugin.getLevelConfig();
        int maxLevel      = cfg.getMaxLevel();

        int level = Math.max(1, player.getLevel() - amount);
        int xp    = player.getXp();
        if (level == maxLevel && xp > maxLevel * cfg.getXpMultiplier())
            xp = maxLevel * cfg.getXpMultiplier();

        player.setLevel(level);
        player.setXp(xp);
        persist(uuid, xp, level);
        notifyChange(uuid);
    }

    // ─── Barre d'XP ─────────────────────────────────────────────────────────────

    public String getXPBar(UUID uuid, int sizeBar) {
        int xp       = getXP(uuid);
        int level    = getLevel(uuid);
        int xpNeeded = level * plugin.getLevelConfig().getXpMultiplier();
        int filled   = (int) ((xp / (double) xpNeeded) * sizeBar);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < sizeBar; i++) {
            bar.append(i < filled ? "§b§m━" : "§7§m━");
        }
        return bar.toString();
    }

    // ─── Feedback level-up (messages, son, titre, paliers) ──────────────────────

    private void sendLevelUpFeedback(UUID uuid, int level) {
        Player bp = Bukkit.getPlayer(uuid);
        if (bp == null) return;

        LevelConfig cfg = plugin.getLevelConfig();

        // Message chat
        String msg = colorize(cfg.getLevelUpMessage().replace("%level%", String.valueOf(level)));
        bp.sendMessage(msg);

        // Son
        Sound sound = parseSound(cfg.getLevelUpSoundId());
        if (sound != null)
            bp.playSound(bp.getLocation(), sound,
                    cfg.getLevelUpSoundVolume(), cfg.getLevelUpSoundPitch());

        // Titre
        String titleMain = colorize(cfg.getLevelUpTitleMain().replace("%level%", String.valueOf(level)));
        String titleSub  = colorize(cfg.getLevelUpTitleSub() .replace("%level%", String.valueOf(level)));
        bp.sendTitle(titleMain, titleSub,
                cfg.getLevelUpTitleFadeIn(), cfg.getLevelUpTitleStay(), cfg.getLevelUpTitleFadeOut());

        // Actions de palier
        List<MilestoneAction> milestones = cfg.getMilestoneActions(level);
        for (MilestoneAction action : milestones) {
            MilestoneExecutor.execute(bp, level, action);
        }
    }

    // ─── Helpers privés ─────────────────────────────────────────────────────────

    private void persist(UUID uuid, int xp, int level) {
        String path = "players." + uuid;
        plugin.getPlayerDataManager().getPlayersConfig().set(path + ".xp",    xp);
        plugin.getPlayerDataManager().getPlayersConfig().set(path + ".level", level);
        plugin.getPlayerDataManager().save();
    }

    private void notifyChange(UUID uuid) {
        java.util.function.Consumer<UUID> cb = ArmesiaLevel.getLevelChangeCallback();
        if (cb != null) cb.accept(uuid);
    }

    private static String colorize(String s) {
        return s.replace("&", "§");
    }

    private Sound parseSound(String id) {
        try {
            return Sound.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[LevelManager] Son inconnu : '" + id + "' — son par défaut utilisé.");
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }
}
