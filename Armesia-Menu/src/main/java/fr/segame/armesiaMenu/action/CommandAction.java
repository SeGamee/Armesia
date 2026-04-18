package fr.segame.armesiaMenu.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class CommandAction extends AbstractAction {

    private final String command;
    private final boolean console;

    public CommandAction(String command, boolean console, Set<ClickType> clicks) {
        super(clicks);
        this.command = command;
        this.console = console;
    }

    @Override
    public void execute(Player player) {

        String cmd = command.replace("%player%", player.getName());

        if (console) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            player.performCommand(cmd);
        }
    }
}