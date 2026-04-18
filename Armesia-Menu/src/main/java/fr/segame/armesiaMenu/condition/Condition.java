package fr.segame.armesiaMenu.condition;

import org.bukkit.entity.Player;

public interface Condition {
    boolean isValid(Player player);
}