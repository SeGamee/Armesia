package fr.segame.armesia.managers;

import fr.segame.armesia.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LevelManager {

    private final PlayerManager playerManager;

    public LevelManager(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    public void addXP(UUID uuid, int amount) {
        GamePlayer player = playerManager.getPlayer(uuid);

        int newXP = player.getXp() + amount;

        while (newXP >= 1000) {
            newXP -= 1000;

            if (player.getLevel() < 100) {
                player.setLevel(player.getLevel() + 1);

                Player bukkitPlayer = Bukkit.getPlayer(uuid);
                if (bukkitPlayer != null) {
                    bukkitPlayer.sendMessage("§bLevel up ! §7Niveau " + player.getLevel());
                }
            } else {
                newXP = 1000;
                break;
            }
        }

        player.setXp(newXP);
    }
}