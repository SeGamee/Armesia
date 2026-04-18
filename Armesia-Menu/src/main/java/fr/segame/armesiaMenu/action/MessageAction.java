package fr.segame.armesiaMenu.action;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class MessageAction implements MenuAction {

    private final String message;
    private final Set<ClickType> clicks;

    public MessageAction(String message, Set<ClickType> clicks) {
        this.message = ChatColor.translateAlternateColorCodes('&', message);
        this.clicks = clicks;
    }

    @Override
    public void execute(Player player) {
        player.sendMessage(message);
    }

    @Override
    public boolean matches(ClickType click) {
        return clicks == null || clicks.contains(click);
    }
}