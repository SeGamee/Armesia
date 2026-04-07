package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /heal          — Se soigner
 * /heal <joueur> — Soigner un autre joueur (armesia.heal.others)
 */
public class HealCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.heal")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            // soi-même
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage console : /heal <joueur>");
                return true;
            }
            heal(player);
            player.sendMessage("§aVous avez été soigné.");
        } else {
            // autre joueur
            if (!Main.checkPerm(sender, "armesia.heal.others")) {
                sender.sendMessage("§cVous n'avez pas la permission de soigner d'autres joueurs.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Main.getInstance().getConfig().getString("general.player-not-found", "§cJoueur introuvable."));
                return true;
            }
            heal(target);
            target.sendMessage("§aVous avez été soigné par §f" + sender.getName() + "§a.");
            sender.sendMessage("§aVous avez soigné §f" + target.getName() + "§a.");
        }
        return true;
    }

    private void heal(Player player) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) player.setHealth(attr.getValue());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
    }
}
