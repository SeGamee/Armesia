package fr.segame.armesia.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SuicideCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player player) {
            player.setHealth(0);
            player.sendMessage("§7Vous vous êtes suicidé.");
        } else {
            sender.sendMessage("§cVous devez être un joueur.");
        }
        return true;
    }
}
