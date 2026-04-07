package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        OfflinePlayer target;
        if (args.length < 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cCommande réservée aux joueurs connectés.");
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage("§cCe joueur n'a jamais joué sur le serveur.");
                return true;
            }
        }
        sender.sendMessage("§aStats de §e" + target.getName() + " §a:");
        sender.sendMessage("§a| Kills : §e" + Main.getStatsManager().getKills(target.getUniqueId()));
        sender.sendMessage("§a| Morts : §e" + Main.getStatsManager().getDeaths(target.getUniqueId()));
        sender.sendMessage("§a| K/D Ratio : §e" + Main.getStatsManager().getFormattedKillDeathRatio(target.getUniqueId()));
        sender.sendMessage("§a| Killstreak : §e" + Main.getStatsManager().getKillstreak(target.getUniqueId()));
        sender.sendMessage("§a| Killstreak max : §e" + Main.getStatsManager().getBestKillstreak(target.getUniqueId()));
        return true;
    }
}
