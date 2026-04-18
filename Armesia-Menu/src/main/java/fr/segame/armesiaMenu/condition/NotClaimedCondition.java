package fr.segame.armesiaMenu.condition;

import fr.segame.armesiaMenu.reward.RewardManager;
import org.bukkit.entity.Player;

public class NotClaimedCondition implements Condition {

    private final String key;
    private final RewardManager manager;

    public NotClaimedCondition(String key, RewardManager manager) {
        this.key = key;
        this.manager = manager;
    }

    @Override
    public boolean isValid(Player player) {
        return !manager.hasClaimed(player.getUniqueId(), key);
    }
}