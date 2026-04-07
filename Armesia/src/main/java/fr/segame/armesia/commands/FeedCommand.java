package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /feed          — Se rassasier
 * /feed <joueur> — Rassasier un autre joueur (armesia.feed.others)
 */
public class FeedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.feed")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage console : /feed <joueur>");
                return true;
            }
            feed(player);
            player.sendMessage("§aVous avez été rassasié.");
        } else {
            if (!Main.checkPerm(sender, "armesia.feed.others")) {
                sender.sendMessage("§cVous n'avez pas la permission de rassasier d'autres joueurs.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Main.getInstance().getConfig().getString("general.player-not-found", "§cJoueur introuvable."));
                return true;
            }
            feed(target);
            target.sendMessage("§aVous avez été rassasié par §f" + sender.getName() + "§a.");
            sender.sendMessage("§aVous avez rassasié §f" + target.getName() + "§a.");
        }
        return true;
    }

    private void feed(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);
    }
}
