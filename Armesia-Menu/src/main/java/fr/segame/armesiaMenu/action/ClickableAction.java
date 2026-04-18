package fr.segame.armesiaMenu.action;

import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public interface ClickableAction {

    Set<ClickType> getClicks();

    default boolean matchesClick(ClickType click) {
        return getClicks() == null || getClicks().contains(click);
    }
}