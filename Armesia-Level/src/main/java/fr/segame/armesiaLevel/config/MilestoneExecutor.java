package fr.segame.armesiaLevel.config;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Exécute une MilestoneAction sur un joueur.
 */
public final class MilestoneExecutor {

    private MilestoneExecutor() {}

    public static void execute(Player player, int level, MilestoneAction action) {
        String name     = player.getName();
        String levelStr = String.valueOf(level);

        switch (action.getType()) {

            case MESSAGE -> player.sendMessage(
                    colorize(apply(action.getValue(), name, levelStr)));

            case BROADCAST -> Bukkit.broadcastMessage(
                    colorize(apply(action.getValue(), name, levelStr)));

            case TITLE -> player.sendTitle(
                    colorize(apply(action.getValue(),    name, levelStr)),
                    colorize(apply(action.getSubtitle(), name, levelStr)),
                    action.getFadeIn(), action.getStay(), action.getFadeOut());

            case SOUND -> {
                Sound sound = parseSound(action.getSoundId());
                if (sound != null) {
                    player.playSound(player.getLocation(), sound,
                            action.getVolume(), action.getPitch());
                }
            }

            case COMMAND -> Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    apply(action.getValue(), name, levelStr));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static String apply(String s, String player, String level) {
        return s.replace("%player%", player).replace("%level%", level);
    }

    private static String colorize(String s) {
        return s.replace("&", "§");
    }

    private static Sound parseSound(String id) {
        try {
            return Sound.valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Armesia-Level] Son inconnu : '" + id + "'");
            return null;
        }
    }
}

