package fr.segame.armesiaMenu.action;

import fr.segame.armesiaMenu.ArmesiaMenu;
import fr.segame.armesiaMenu.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class RefreshMenuAction extends AbstractAction {

    private final MenuManager manager;

    public RefreshMenuAction(MenuManager manager, Set<ClickType> clicks) {
        super(clicks);
        this.manager = manager;
    }

    @Override
    public void execute(Player player) {

        // 🔥 delay 1 tick pour éviter les conflits Bukkit
        Bukkit.getScheduler().runTaskLater(
                ArmesiaMenu.getInstance(),
                () -> manager.refreshMenu(player),
                1L
        );
    }

    @Override
    public boolean shouldRefresh() {
        return false; // 🔥 évite double refresh
    }
}