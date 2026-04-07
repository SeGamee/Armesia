package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyCommand implements CommandExecutor {

    private final Main core;

    public EconomyCommand(Main core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // =========================
        // /money
        // =========================
        if (cmd.getName().equalsIgnoreCase("money")) {

            // /money
            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande joueur uniquement.");
                    return true;
                }

                double money = core.getEconomyAPI().getMoney(player.getUniqueId());
                sender.sendMessage("§aArgent: §f" + core.getEconomyAPI().formatMoney(money));
                return true;
            }

            // /money <player>
            if (args.length == 1) {
                if (!Main.checkPerm(sender, "armesia.money.admin")) {
                    sender.sendMessage("§cPas la permission.");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

                if (!target.hasPlayedBefore()) {
                    sender.sendMessage("§cJoueur inconnu.");
                    return true;
                }

                double money = core.getEconomyAPI().getMoney(target.getUniqueId());
                sender.sendMessage("§aArgent de " + target.getName() + ": §f" + core.getEconomyAPI().formatMoney(money));
                return true;
            }

            // /money add/remove/set
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /money <add|remove|set> <joueur> <montant>");
                return true;
            }

            if (!Main.checkPerm(sender, "armesia.money.admin")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            String action = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJoueur inconnu.");
                return true;
            }

            double amount;

            try {
                amount = Double.parseDouble(args[2]);
            } catch (Exception e) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            UUID uuid = target.getUniqueId();

            switch (action.toLowerCase()) {

                case "add":
                    core.getEconomyAPI().addMoney(uuid, amount);
                    sender.sendMessage("§aAjouté.");
                    break;

                case "remove":
                    if (!core.getEconomyAPI().removeMoney(uuid, amount)) {
                        sender.sendMessage("§cPas assez d'argent.");
                        return true;
                    }
                    sender.sendMessage("§aRetiré.");
                    break;

                case "set":
                    core.getEconomyAPI().addMoney(uuid, -core.getEconomyAPI().getMoney(uuid));
                    core.getEconomyAPI().addMoney(uuid, amount);
                    sender.sendMessage("§aDéfini.");
                    break;

                default:
                    sender.sendMessage("§cAction inconnue.");
                    break;
            }

            return true;
        }

        // =========================
        // /tokens
        // =========================
        if (cmd.getName().equalsIgnoreCase("tokens")) {

            // /tokens
            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande joueur uniquement.");
                    return true;
                }

                int tokens = core.getEconomyAPI().getTokens(player.getUniqueId());
                sender.sendMessage("§bTokens: §f" + core.getEconomyAPI().formatTokens(tokens));
                return true;
            }

            // /tokens <player>
            if (args.length == 1) {
                if (!Main.checkPerm(sender, "armesia.tokens.admin")) {
                    sender.sendMessage("§cPas la permission.");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

                if (!target.hasPlayedBefore()) {
                    sender.sendMessage("§cJoueur inconnu.");
                    return true;
                }

                int tokens = core.getEconomyAPI().getTokens(target.getUniqueId());
                sender.sendMessage("§bTokens de " + target.getName() + ": §f" + core.getEconomyAPI().formatTokens(tokens));
                return true;
            }

            // /tokens add/remove/set
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tokens <add|remove|set> <joueur> <montant>");
                return true;
            }

            if (!Main.checkPerm(sender, "armesia.tokens.admin")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            String action = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJoueur inconnu.");
                return true;
            }

            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (Exception e) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            UUID uuid = target.getUniqueId();

            switch (action.toLowerCase()) {

                case "add":
                    core.getEconomyAPI().addTokens(uuid, amount);
                    sender.sendMessage("§aAjouté.");
                    break;

                case "remove":
                    if (!core.getEconomyAPI().removeTokens(uuid, amount)) {
                        sender.sendMessage("§cPas assez de tokens.");
                        return true;
                    }
                    sender.sendMessage("§aRetiré.");
                    break;

                case "set":
                    core.getEconomyAPI().addTokens(uuid, -core.getEconomyAPI().getTokens(uuid));
                    core.getEconomyAPI().addTokens(uuid, amount);
                    sender.sendMessage("§aDéfini.");
                    break;
            }

            return true;
        }

        // =========================
        // /pay
        // =========================
        if (cmd.getName().equalsIgnoreCase("pay")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cCommande joueur uniquement.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage("§cUsage: /pay <joueur> <montant>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);

            if (target == null) {
                sender.sendMessage("§cJoueur hors ligne.");
                return true;
            }

            double amount;

            try {
                amount = Double.parseDouble(args[1]);
            } catch (Exception e) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            if (amount <= 0) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            UUID senderUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();

            if (!core.getEconomyAPI().hasMoney(senderUUID, amount)) {
                sender.sendMessage("§cPas assez d'argent.");
                return true;
            }

            core.getEconomyAPI().removeMoney(senderUUID, amount);
            core.getEconomyAPI().addMoney(targetUUID, amount);

            player.sendMessage("§aTu as envoyé §f" + core.getEconomyAPI().formatMoney(amount) + " §aà " + target.getName());
            target.sendMessage("§aTu as reçu §f" + core.getEconomyAPI().formatMoney(amount) + " §ade " + player.getName());

            return true;
        }

        return true;
    }
}