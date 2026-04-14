package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyCommand implements CommandExecutor {

    public EconomyCommand() { }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        EconomyAPI eco = APIProvider.getEconomy();
        if (eco == null) {
            sender.sendMessage("§cLe service d'économie n'est pas disponible.");
            return true;
        }

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

                double money = eco.getMoney(player.getUniqueId());
                sender.sendMessage("§aArgent: §f" + eco.formatMoney(money));
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

                double money = eco.getMoney(target.getUniqueId());
                sender.sendMessage("§aArgent de " + target.getName() + ": §f" + eco.formatMoney(money));
                return true;
            }

            // /money add/remove/set/reset
            if (!Main.checkPerm(sender, "armesia.money.admin")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            String action = args[0];

            // /money reset <joueur> — ne requiert pas de montant
            if (action.equalsIgnoreCase("reset")) {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /money reset <joueur>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage("§cJoueur inconnu.");
                    return true;
                }
                eco.setMoney(target.getUniqueId(), 0);
                sender.sendMessage("§aArgent de §f" + target.getName() + " §aremis à zéro.");
                return true;
            }

            // /money add/remove/set <joueur> <montant>
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /money <add|remove|set|reset> <joueur> [montant]");
                return true;
            }

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

            if (amount != Math.floor(amount) || Double.isInfinite(amount)) {
                sender.sendMessage("§cLe montant doit être un nombre entier.");
                return true;
            }

            UUID uuid = target.getUniqueId();
            double max = eco.getMaxMoney();

            switch (action.toLowerCase()) {

                case "add":
                    double newBalance = eco.getMoney(uuid) + amount;
                    if (newBalance > max) {
                        sender.sendMessage("§cPlafond atteint (§f" + eco.formatMoney(max) + "§c). Solde de §f"
                                + target.getName() + " §cplafonné.");
                    }
                    eco.addMoney(uuid, amount);
                    sender.sendMessage("§aAjouté §f" + eco.formatMoney(amount) + " §aà §f" + target.getName()
                            + "§a. Nouveau solde : §f" + eco.formatMoney(eco.getMoney(uuid)));
                    break;

                case "remove":
                    if (!eco.removeMoney(uuid, amount)) {
                        sender.sendMessage("§cPas assez d'argent.");
                        return true;
                    }
                    sender.sendMessage("§aRetiré §f" + eco.formatMoney(amount) + " §aà §f" + target.getName()
                            + "§a. Nouveau solde : §f" + eco.formatMoney(eco.getMoney(uuid)));
                    break;

                case "set":
                    if (amount > max) {
                        sender.sendMessage("§cMontant supérieur au plafond (§f" + eco.formatMoney(max) + "§c). Valeur plafonnée.");
                    }
                    eco.setMoney(uuid, amount);
                    sender.sendMessage("§aSolde de §f" + target.getName() + " §adéfini à §f" + eco.formatMoney(eco.getMoney(uuid)));
                    break;

                default:
                    sender.sendMessage("§cAction inconnue. Usage: /money <add|remove|set|reset> <joueur> [montant]");
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

                int tokens = eco.getTokens(player.getUniqueId());
                sender.sendMessage("§bTokens: §f" + eco.formatTokens(tokens));
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

                int tokens = eco.getTokens(target.getUniqueId());
                sender.sendMessage("§bTokens de " + target.getName() + ": §f" + eco.formatTokens(tokens));
                return true;
            }

            // /tokens add/remove/set/reset
            if (!Main.checkPerm(sender, "armesia.tokens.admin")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            String action = args[0];

            // /tokens reset <joueur> — ne requiert pas de montant
            if (action.equalsIgnoreCase("reset")) {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /tokens reset <joueur>");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage("§cJoueur inconnu.");
                    return true;
                }
                eco.setTokens(target.getUniqueId(), 0);
                sender.sendMessage("§bTokens de §f" + target.getName() + " §bremis à zéro.");
                return true;
            }

            // /tokens add/remove/set <joueur> <montant>
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tokens <add|remove|set|reset> <joueur> [montant]");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJoueur inconnu.");
                return true;
            }

            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (Exception e) {
                sender.sendMessage("§cMontant invalide (entier requis).");
                return true;
            }

            UUID uuid = target.getUniqueId();
            int maxTokens = eco.getMaxTokens();

            switch (action.toLowerCase()) {

                case "add":
                    int newTotal = eco.getTokens(uuid) + amount;
                    if (newTotal > maxTokens) {
                        sender.sendMessage("§cPlafond atteint (§f" + eco.formatTokens(maxTokens) + "§c). Solde de §f"
                                + target.getName() + " §cplafonné.");
                    }
                    eco.addTokens(uuid, amount);
                    sender.sendMessage("§bAjouté §f" + eco.formatTokens(amount) + " §bà §f" + target.getName()
                            + "§b. Nouveau solde : §f" + eco.formatTokens(eco.getTokens(uuid)));
                    break;

                case "remove":
                    if (!eco.removeTokens(uuid, amount)) {
                        sender.sendMessage("§cPas assez de tokens.");
                        return true;
                    }
                    sender.sendMessage("§bRetiré §f" + eco.formatTokens(amount) + " §bà §f" + target.getName()
                            + "§b. Nouveau solde : §f" + eco.formatTokens(eco.getTokens(uuid)));
                    break;

                case "set":
                    if (amount > maxTokens) {
                        sender.sendMessage("§cMontant supérieur au plafond (§f" + eco.formatTokens(maxTokens) + "§c). Valeur plafonnée.");
                    }
                    eco.setTokens(uuid, amount);
                    sender.sendMessage("§bTokens de §f" + target.getName() + " §bdéfinis à §f" + eco.formatTokens(eco.getTokens(uuid)));
                    break;

                default:
                    sender.sendMessage("§cAction inconnue. Usage: /tokens <add|remove|set|reset> <joueur> [montant]");
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

            if (amount != Math.floor(amount) || Double.isInfinite(amount)) {
                sender.sendMessage("§cLe montant doit être un nombre entier.");
                return true;
            }

            if (amount <= 0) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            UUID senderUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();

            if (!eco.hasMoney(senderUUID, amount)) {
                sender.sendMessage("§cPas assez d'argent.");
                return true;
            }

            eco.removeMoney(senderUUID, amount);
            eco.addMoney(targetUUID, amount);

            player.sendMessage("§aTu as envoyé §f" + eco.formatMoney(amount) + " §aà " + target.getName());
            target.sendMessage("§aTu as reçu §f" + eco.formatMoney(amount) + " §ade " + player.getName());

            return true;
        }

        return true;
    }
}