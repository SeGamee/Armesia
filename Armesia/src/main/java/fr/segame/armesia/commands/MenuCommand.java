package fr.segame.armesia.commands;

import fr.segame.armesia.gui.MenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {
            new MenuGUI().openGUI(player);
        } else {
            sender.sendMessage("§cVous devez être un joueur.");
        }

        return true;
    }
}
