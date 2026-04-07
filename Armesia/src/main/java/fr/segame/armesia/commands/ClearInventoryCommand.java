package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClearInventoryCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.clearinventory")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            // /ci → soi-même
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage : /ci <joueur>");
                return true;
            }
            clearInventory(player);
            player.sendMessage("§aVotre inventaire a été vidé.");

        } else {
            // /ci <joueur>
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cCe joueur n'est pas connecté.");
                return true;
            }
            clearInventory(target);
            target.sendMessage("§cVotre inventaire a été vidé par §f" + sender.getName() + "§c.");
            sender.sendMessage("§aL'inventaire de §f" + target.getName() + " §aa été vidé.");
        }

        return true;
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }
}

