package fr.segame.armesia.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class KillCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {        if(args.length != 1) {
            sender.sendMessage("§c/kill <joueur>");
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cCe joueur n'est pas connecté.");
            } else {
                target.setHealth(0);
                target.setLastDamageCause(new EntityDamageEvent(target.getPlayer(), EntityDamageEvent.DamageCause.SUICIDE, 0));
                sender.sendMessage("§7Le joueur a été tué.");
                target.sendMessage("§cVous avez été tué.");
            }
        }
        return true;
    }
}
