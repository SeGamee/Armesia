package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /vanish [joueur]  — Activer / désactiver le mode invisible
 */
public class VanishCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.vanish")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        Player target;
        boolean isSelf;

        if (args.length >= 1) {
            if (!Main.checkPerm(sender, "armesia.vanish.others")) {
                sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable.")); return true; }
            isSelf = false;
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
                return true;
            }
            target = p;
            isSelf = true;
        }

        boolean wasVanished = VanishManager.isVanished(target.getUniqueId());
        VanishManager.toggle(target);
        boolean nowVanished = VanishManager.isVanished(target.getUniqueId());

        if (isSelf) {
            target.sendMessage(nowVanished
                    ? cfg("vanish.messages.on",  "§aVous êtes maintenant invisible.")
                    : cfg("vanish.messages.off", "§aVous êtes de nouveau visible."));
        } else {
            target.sendMessage(nowVanished
                    ? cfg("vanish.messages.on",  "§aVous êtes maintenant invisible.")
                    : cfg("vanish.messages.off", "§aVous êtes de nouveau visible."));
            sender.sendMessage((nowVanished
                    ? cfg("vanish.messages.on-other",  "§a{player} §aest maintenant invisible.")
                    : cfg("vanish.messages.off-other", "§a{player} §aest de nouveau visible."))
                    .replace("{player}", target.getName()));
        }
        return true;
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}

