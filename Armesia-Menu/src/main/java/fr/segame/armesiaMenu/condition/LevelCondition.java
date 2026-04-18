package fr.segame.armesiaMenu.condition;

import fr.segame.armesiaLevel.api.LevelAPI;
import org.bukkit.entity.Player;

public class LevelCondition implements Condition {

    private final int min;

    public LevelCondition(int min) {
        this.min = min;
    }

    @Override
    public boolean isValid(Player player) {
        try {
            int level = LevelAPI.getLevel(player.getUniqueId());
            return level >= min;
        } catch (Exception e) {
            return false;
        }
    }
}