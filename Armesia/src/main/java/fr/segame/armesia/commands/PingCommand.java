package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.ping")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            // /ping → propre ping
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage : /ping <joueur>");
                return true;
            }
            int ping = player.getPing();
            sender.sendMessage("§7Votre ping : " + colorPing(ping) + ping + " ms");

        } else {
            // /ping <joueur>
            if (!Main.checkPerm(sender, "armesia.ping.others")) {
                sender.sendMessage("§cVous n'avez pas la permission de voir le ping d'un autre joueur.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cCe joueur n'est pas connecté.");
                return true;
            }
            int ping = target.getPing();
            sender.sendMessage("§7Ping de §f" + target.getName() + "§7 : " + colorPing(ping) + ping + " ms");
        }

        return true;
    }

    /** Retourne une couleur selon la valeur du ping. */
    private String colorPing(int ping) {
        if (ping < 50)  return "§a";   // Vert   — excellent
        if (ping < 100) return "§e";   // Jaune  — correct
        if (ping < 200) return "§6";   // Orange — moyen
        return "§c";                   // Rouge  — mauvais
    }
}

