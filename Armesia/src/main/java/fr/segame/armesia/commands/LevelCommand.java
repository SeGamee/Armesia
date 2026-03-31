package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LevelCommand implements CommandExecutor {

    private final Main core;
    public LevelCommand(Main core) {this.core = core;}

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("level")) {
            if (!sender.hasPermission("armesia.level.admin")) {
                sender.sendMessage("§cVous n'avez pas la permission.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /level addxp | removexp | addlevel | removelevel <joueur> <montant>");
                return true;
            }

            String action = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (Exception e) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJoueur inconnu.");
                return true;
            }

            UUID uuid = target.getUniqueId();

            switch (action.toLowerCase()) {

                case "addxp":
                    core.getLevelManager().addXP(uuid, amount);
                    sender.sendMessage("§7Tu viens de give §b%sxp §7 à §b%s§7.".formatted(amount, target.getName()));
                    // si le joueur est en ligne, lui envoyer un message
                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage("§7Tu viens de recevoir §b%sxp§7.".formatted(amount));
                        }
                    }
                    break;

                case "removexp":
                    core.getLevelManager().removeXP(uuid, amount);
                    sender.sendMessage("§7Tu viens de retirer §b%sxp §7 à §b%s§7.".formatted(amount, target.getName()));
                    // si le joueur est en ligne, lui envoyer un message
                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage("§7On vient de te retirer §b%sxp§7.".formatted(amount));
                        }
                    }
                    break;

                case "addlevel":
                    core.getLevelManager().addLevel(uuid, amount);
                    sender.sendMessage("§7Tu viens de give §b%s level(s) §7 à §b%s§7.".formatted(amount, target.getName()));
                    // si le joueur est en ligne, lui envoyer un message
                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage("§7Tu viens de recevoir §b%s level(s)§7.".formatted(amount));
                        }
                    }
                    break;

                case "removelevel":
                    core.getLevelManager().removeLevel(uuid, amount);
                    sender.sendMessage("§7Tu viens de retirer §b%s level(s) §7 à §b%s§7.".formatted(amount, target.getName()));
                    // si le joueur est en ligne, lui envoyer un message
                    if (target.isOnline()) {
                        Player onlinePlayer = target.getPlayer();
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage("§7On vient de te retirer §b%s level(s)§7.".formatted(amount));
                        }
                    }
                    break;

                default:
                    sender.sendMessage("§cAction inconnue.");
                    break;
            }

            return true;
        }
        return true;
    }
}
