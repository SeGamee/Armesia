package fr.segame.armesiaMenu.action;

import fr.segame.armesiaMenu.reward.RewardManager;
import org.bukkit.entity.Player;

public class MarkClaimedAction implements MenuAction {

    private final String key;
    private final RewardManager manager;

    public MarkClaimedAction(String key, RewardManager manager) {
        this.key = key;
        this.manager = manager;
    }

    @Override
    public void execute(Player player) {
        manager.claim(player.getUniqueId(), key);
    }
}