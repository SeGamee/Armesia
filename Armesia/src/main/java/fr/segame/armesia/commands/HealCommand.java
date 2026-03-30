package fr.segame.armesia.commands;

import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HealCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(commandSender instanceof Player player) {
            if(player.hasPermission("armesia.heal")) {
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                commandSender.sendMessage("§aVous avez été soigné.");
            } else {
                commandSender.sendMessage("§cVous n'avez pas la permission.");
            }
        } else {
            commandSender.sendMessage("§cVous devez être un joueur.");
        }

         return true;
    }
}
