package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /ec [joueur]  — Ouvrir son/un enderchest
 */
public class EnderChestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
            return true;
        }

        if (!Main.hasGroupPermission(player, "armesia.enderchest")) {
            player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        if (args.length == 0) {
            player.openInventory(player.getEnderChest());
            return true;
        }

        // /ec <joueur> — admin
        if (!Main.hasGroupPermission(player, "armesia.enderchest.others")) {
            player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable."));
            return true;
        }
        player.openInventory(target.getEnderChest());
        player.sendMessage("§aVous consultez l'enderchest de §f" + target.getName() + "§a.");
        return true;
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}

