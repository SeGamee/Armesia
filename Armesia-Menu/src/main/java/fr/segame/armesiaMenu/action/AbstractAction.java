package fr.segame.armesiaMenu.action;

import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public abstract class AbstractAction implements MenuAction {

    protected final Set<ClickType> clicks;

    public AbstractAction(Set<ClickType> clicks) {
        this.clicks = clicks;
    }

    @Override
    public boolean matches(ClickType click) {
        return clicks != null && clicks.contains(click);
    }
}
