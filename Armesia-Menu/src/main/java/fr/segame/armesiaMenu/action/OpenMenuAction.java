package fr.segame.armesiaMenu.action;

import fr.segame.armesiaMenu.menu.Menu;
import fr.segame.armesiaMenu.menu.MenuManager;
import org.bukkit.entity.Player;

public class OpenMenuAction implements MenuAction {

    private final MenuManager manager;
    private final String menuId;

    public OpenMenuAction(MenuManager manager, String menuId) {
        this.manager = manager;
        this.menuId = menuId;
    }

    @Override
    public void execute(Player player) {
        Menu menu = manager.getMenu(menuId);
        if (menu != null) {
            manager.openMenu(player, menu);
        }
    }
}