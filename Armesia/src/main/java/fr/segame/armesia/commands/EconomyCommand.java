package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

            // /money — propre solde
            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande joueur uniquement.");
                    return true;
                }
                sender.sendMessage("§aArgent: §f" + eco.formatMoney(eco.getMoney(player.getUniqueId())));
                return true;
            }

            // /money <joueur> — consulter le solde d'un autre (permission légère)
            if (!Main.checkPerm(sender, "armesia.money.see")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJoueur inconnu.");
                return true;
            }

            sender.sendMessage("§aArgent de §f" + target.getName() + "§a : §f"
                    + eco.formatMoney(eco.getMoney(target.getUniqueId())));
            return true;
        }

        // =========================
        // /moneyadmin
        // =========================
        if (cmd.getName().equalsIgnoreCase("moneyadmin")) {

            if (!Main.checkPerm(sender, "armesia.money.admin")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§cUsage: /moneyadmin <add|remove|set|reset> <joueur> [montant]");
                return true;
            }

            String action = args[0].toLowerCase();

            // /moneyadmin reset <joueur>
            if (action.equals("reset")) {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /moneyadmin reset <joueur>");
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

            // /moneyadmin add|remove|set <joueur> <montant>
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /moneyadmin <add|remove|set|reset> <joueur> [montant]");
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
            } catch (NumberFormatException e) {
                sender.sendMessage("§cMontant invalide.");
                return true;
            }

            if (Double.isInfinite(amount) || Double.isNaN(amount) || amount < 0) {
                sender.sendMessage("§cLe montant doit être un nombre positif.");
                return true;
            }

            UUID uuid = target.getUniqueId();
            double max = eco.getMaxMoney();

            switch (action) {
                case "add" -> {
                    if (eco.getMoney(uuid) + amount > max)
                        sender.sendMessage("§cPlafond atteint (§f" + eco.formatMoney(max) + "§c). Solde plafonné.");
                    eco.addMoney(uuid, amount);
                    sender.sendMessage("§aAjouté §f" + eco.formatMoney(amount) + " §aà §f" + target.getName()
                            + "§a. Nouveau solde : §f" + eco.formatMoney(eco.getMoney(uuid)));
                }
                case "remove" -> {
                    if (!eco.removeMoney(uuid, amount)) {
                        sender.sendMessage("§cPas assez d'argent sur le compte de §f" + target.getName() + "§c.");
                        return true;
                    }
                    sender.sendMessage("§aRetiré §f" + eco.formatMoney(amount) + " §aà §f" + target.getName()
                            + "§a. Nouveau solde : §f" + eco.formatMoney(eco.getMoney(uuid)));
                }
                case "set" -> {
                    if (amount > max)
                        sender.sendMessage("§cMontant supérieur au plafond (§f" + eco.formatMoney(max) + "§c). Valeur plafonnée.");
                    eco.setMoney(uuid, amount);
                    sender.sendMessage("§aSolde de §f" + target.getName() + " §adéfini à §f"
                            + eco.formatMoney(eco.getMoney(uuid)));
                }
                default -> sender.sendMessage("§cAction inconnue. Usage: /moneyadmin <add|remove|set|reset> <joueur> [montant]");
            }

            return true;
        }

        // =========================
        // /tokens
        // =========================
        if (cmd.getName().equalsIgnoreCase("tokens")) {

            // /tokens — propre solde
            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande joueur uniquement.");
                    return true;
                }
                sender.sendMessage("§bTokens: §f" + eco.formatTokens(eco.getTokens(player.getUniqueId())));
                return true;
            }

            // /tokens <joueur> — consulter le solde d'un autre
            if (!Main.checkPerm(sender, "armesia.tokens.see")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJoueur inconnu.");
                return true;
            }

            sender.sendMessage("§bTokens de §f" + target.getName() + "§b : §f"
                    + eco.formatTokens(eco.getTokens(target.getUniqueId())));
            return true;
        }

        // =========================
        // /tokensadmin
        // =========================
        if (cmd.getName().equalsIgnoreCase("tokensadmin")) {

            if (!Main.checkPerm(sender, "armesia.tokens.admin")) {
                sender.sendMessage("§cPas la permission.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§cUsage: /tokensadmin <add|remove|set|reset> <joueur> [montant]");
                return true;
            }

            String action = args[0].toLowerCase();

            // /tokensadmin reset <joueur>
            if (action.equals("reset")) {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /tokensadmin reset <joueur>");
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

            // /tokensadmin add|remove|set <joueur> <montant>
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tokensadmin <add|remove|set|reset> <joueur> [montant]");
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

            switch (action) {
                case "add" -> {
                    if (eco.getTokens(uuid) + amount > maxTokens)
                        sender.sendMessage("§cPlafond atteint (§f" + eco.formatTokens(maxTokens) + "§c). Solde plafonné.");
                    eco.addTokens(uuid, amount);
                    sender.sendMessage("§bAjouté §f" + eco.formatTokens(amount) + " §bà §f" + target.getName()
                            + "§b. Nouveau solde : §f" + eco.formatTokens(eco.getTokens(uuid)));
                }
                case "remove" -> {
                    if (!eco.removeTokens(uuid, amount)) {
                        sender.sendMessage("§cPas assez de tokens sur le compte de §f" + target.getName() + "§c.");
                        return true;
                    }
                    sender.sendMessage("§bRetiré §f" + eco.formatTokens(amount) + " §bà §f" + target.getName()
                            + "§b. Nouveau solde : §f" + eco.formatTokens(eco.getTokens(uuid)));
                }
                case "set" -> {
                    if (amount > maxTokens)
                        sender.sendMessage("§cMontant supérieur au plafond (§f" + eco.formatTokens(maxTokens) + "§c). Valeur plafonnée.");
                    eco.setTokens(uuid, amount);
                    sender.sendMessage("§bTokens de §f" + target.getName() + " §bdéfinis à §f"
                            + eco.formatTokens(eco.getTokens(uuid)));
                }
                default -> sender.sendMessage("§cAction inconnue. Usage: /tokensadmin <add|remove|set|reset> <joueur> [montant]");
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

            if (Double.isInfinite(amount) || Double.isNaN(amount)) {
                sender.sendMessage("§cLe montant doit être un nombre positif.");
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

