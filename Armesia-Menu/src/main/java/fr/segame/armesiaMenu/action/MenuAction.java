package fr.segame.armesiaMenu.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public interface MenuAction {

    void execute(Player player);

    default boolean matches(ClickType click) {
        return true;
    }

    // 🔥 AJOUT IMPORTANT
    default boolean shouldRefresh() {
        return true;
    }
}