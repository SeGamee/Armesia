package fr.segame.armesiaLevel.commands;

import fr.segame.armesiaLevel.managers.LevelManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LevelCommand implements CommandExecutor {

    private final LevelManager levelManager;

    public LevelCommand(LevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

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
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide.");
            return true;
        }

        if (!target.hasPlayedBefore()) {
            sender.sendMessage("§cJoueur inconnu.");
            return true;
        }

        UUID uuid = target.getUniqueId();

        switch (action.toLowerCase()) {
            case "addxp" -> {
                levelManager.addXP(uuid, amount);
                sender.sendMessage("§7Tu viens de give §b%sxp §7à §b%s§7.".formatted(amount, target.getName()));
                notify(target, "§7Tu viens de recevoir §b%sxp§7.".formatted(amount));
            }
            case "removexp" -> {
                levelManager.removeXP(uuid, amount);
                sender.sendMessage("§7Tu viens de retirer §b%sxp §7à §b%s§7.".formatted(amount, target.getName()));
                notify(target, "§7On vient de te retirer §b%sxp§7.".formatted(amount));
            }
            case "addlevel" -> {
                levelManager.addLevel(uuid, amount);
                sender.sendMessage("§7Tu viens de give §b%s level(s) §7à §b%s§7.".formatted(amount, target.getName()));
                notify(target, "§7Tu viens de recevoir §b%s level(s)§7.".formatted(amount));
            }
            case "removelevel" -> {
                levelManager.removeLevel(uuid, amount);
                sender.sendMessage("§7Tu viens de retirer §b%s level(s) §7à §b%s§7.".formatted(amount, target.getName()));
                notify(target, "§7On vient de te retirer §b%s level(s)§7.".formatted(amount));
            }
            default -> sender.sendMessage("§cAction inconnue.");
        }

        return true;
    }

    private void notify(OfflinePlayer target, String message) {
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) online.sendMessage(message);
        }
    }
}

