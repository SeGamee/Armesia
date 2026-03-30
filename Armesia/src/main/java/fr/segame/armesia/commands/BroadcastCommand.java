package fr.segame.armesia.commands;

import com.google.common.base.Joiner;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BroadcastCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 0) {
            sender.sendMessage("§c/broadcast <message>");
        } else {
            String message = Joiner.on(" ").join(args);
            for(Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("§8[§aBroadcast§8] §7%s".formatted(message));
            }
        }
        return false;
    }
}
