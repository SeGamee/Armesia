package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import fr.segame.armesia.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LevelManager {

    private final PlayerManager playerManager;

    public LevelManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public int getXP(UUID uuid) {
        return playerManager.getPlayer(uuid).getXp();
    }

    public int getLevel(UUID uuid) {
        return playerManager.getPlayer(uuid).getLevel();
    }

    public void addXP(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        int xp = player.getXp() + amount;
        int level = player.getLevel();

        // Si déjà niveau 100, bloquer l'XP à 100000
        if (level >= 100) {
            level = 100;
            xp = Math.min(xp, 100000);
        } else {
            // Boucle de montée de niveau
            while (level < 100) {
                int xpNeeded = level * 1000;
                if (xp >= xpNeeded) {
                    xp -= xpNeeded;
                    level++;
                    Player bukkitPlayer = Bukkit.getPlayer(uuid);
                    if (bukkitPlayer != null) {
                        bukkitPlayer.sendMessage("§bLevel up ! §7Niveau " + level);
                        bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                        bukkitPlayer.sendTitle(
                                "§6§l⬆ LEVEL UP ⬆",
                                "§eNiveau §6" + level,
                                10,
                                60,
                                20
                        );
                    }
                } else {
                    break;
                }
            }
            // Si on atteint le niveau 100, bloquer l'XP à 100000
            if (level >= 100) {
                level = 100;
                xp = Math.min(xp, 100000);
            }
        }
        // Empêcher l'XP d'être négatif
        if (xp < 0) xp = 0;
        // Empêcher le niveau d'être négatif
        if (level < 1) level = 1;

        player.setXp(xp);
        player.setLevel(level);

        Main main = Main.getInstance();
        String path = "players." + uuid;
        main.getPlayersConfig().set(path + ".xp", xp);
        main.getPlayersConfig().set(path + ".level", level);
        main.savePlayers();
        // Mise à jour du tab pour tous les joueurs
        Main.updateAllTabs();
        // Mise à jour immédiate de la sidebar du joueur concerné
        Main.updatePlayerScoreboard(uuid);
    }

    public void removeXP(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        int xp = player.getXp() - amount;
        int level = player.getLevel();

        // Descente de niveau si besoin
        while (xp < 0 && level > 1) {
            level--;
            int xpNeeded = level * 1000;
            xp += xpNeeded;
        }
        // Empêcher l'XP d'être négatif
        if (xp < 0) xp = 0;
        // Empêcher le niveau d'être négatif
        if (level < 1) level = 1;
        // Si niveau 100, bloquer l'XP à 100000
        if (level >= 100 && xp > 100000) xp = 100000;

        player.setXp(xp);
        player.setLevel(level);

        Main main = Main.getInstance();
        String path = "players." + uuid;
        main.getPlayersConfig().set(path + ".xp", xp);
        main.getPlayersConfig().set(path + ".level", level);
        main.savePlayers();
        // Mise à jour du tab pour tous les joueurs
        Main.updateAllTabs();
        // Mise à jour immédiate de la sidebar du joueur concerné
        Main.updatePlayerScoreboard(uuid);
    }

    public void addLevel(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        int level = player.getLevel() + amount;
        int xp = player.getXp();
        if (level > 100) level = 100;
        if (level < 1) level = 1;
        // Si niveau 100, bloquer l'XP à 100000
        if (level == 100 && xp > 100000) xp = 100000;
        player.setLevel(level);
        player.setXp(xp);

        Main main = Main.getInstance();
        String path = "players." + uuid;
        main.getPlayersConfig().set(path + ".xp", xp);
        main.getPlayersConfig().set(path + ".level", level);
        main.savePlayers();
        // Mise à jour du tab pour tous les joueurs
        Main.updateAllTabs();
        // Mise à jour immédiate de la sidebar du joueur concerné
        Main.updatePlayerScoreboard(uuid);
    }

    public void removeLevel(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);
        int level = player.getLevel() - amount;
        int xp = player.getXp();
        if (level < 1) level = 1;
        // Si niveau 100, bloquer l'XP à 100000
        if (level == 100 && xp > 100000) xp = 100000;
        player.setLevel(level);
        player.setXp(xp);

        Main main = Main.getInstance();
        String path = "players." + uuid;
        main.getPlayersConfig().set(path + ".xp", xp);
        main.getPlayersConfig().set(path + ".level", level);
        main.savePlayers();
        // Mise à jour du tab pour tous les joueurs
        Main.updateAllTabs();
        // Mise à jour immédiate de la sidebar du joueur concerné
        Main.updatePlayerScoreboard(uuid);
    }

    public String getXPBar(UUID uuid, int sizeBar) {
        int xp = getXP(uuid);
        int level = getLevel(uuid);
        int xpNeeded = level * 1000;

        int filled = (int) ((xp / (double) xpNeeded) * sizeBar);

        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < sizeBar; i++) {
            if (i < filled) {
                bar.append("§b§m━");
            } else {
                bar.append("§7§m━");
            }
        }

        return bar.toString();
    }
}