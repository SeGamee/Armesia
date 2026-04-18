package fr.segame.armesiaMenu.action;

import fr.segame.armesiaMenu.menu.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class CloseAction extends AbstractAction {

    private final MenuManager manager;

    public CloseAction(MenuManager manager, Set<ClickType> clicks) {
        super(clicks);
        this.manager = manager;
    }

    @Override
    public void execute(Player player) {

        // 🔥 ferme l'inventaire
        player.closeInventory();

        // 🔥 IMPORTANT : enlève le menu du tracking
        manager.closeMenu(player);
    }

    @Override
    public boolean shouldRefresh() {
        return false; // 🔥 empêche le refresh après close
    }
}