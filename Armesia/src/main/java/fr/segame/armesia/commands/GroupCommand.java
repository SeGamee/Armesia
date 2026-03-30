package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GroupCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /group <create|delete|set|addperm|delperm|list|info|setchatprefix|clearchatprefix|settabprefix|cleartabprefix|setpriority>");
            return true;
        }

        String sub = args[0].toLowerCase();

        // -------- CREATE --------
        if (sub.equals("create")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /group create <nom>");
                return true;
            }

            if (Main.groupManager.createGroup(args[1])) {
                sender.sendMessage("§aGroupe créé.");
            } else {
                sender.sendMessage("§cImpossible de créer ce groupe.");
            }
            return true;
        }

        // -------- DELETE --------
        if (sub.equals("delete")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /group delete <nom>");
                return true;
            }

            if (Main.groupManager.deleteGroup(args[1])) {
                sender.sendMessage("§aGroupe supprimé et joueurs réassignés à Citoyen.");
            } else {
                sender.sendMessage("§cImpossible de supprimer ce groupe.");
            }
            return true;
        }

        // -------- LIST --------
        if (sub.equals("list")) {
            Set<String> groups = Main.groupManager.getGroups();

            if (groups.isEmpty()) {
                sender.sendMessage("§cAucun groupe.");
                return true;
            }

            sender.sendMessage("§aGroupes:");
            for (String group : groups) {
                sender.sendMessage("§7- " + group + " (§fprio: " + Main.groupManager.getPriority(group) + "§7)");
            }
            return true;
        }

        // -------- INFO --------
        if (sub.equals("info")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /group info <groupe>");
                return true;
            }

            String group = args[1];

            if (!Main.groupManager.exists(group)) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            String chatPrefix = Main.groupManager.getChatPrefix(group);
            String tabPrefix = Main.groupManager.getTabPrefix(group);
            int priority = Main.groupManager.getPriority(group);
            List<String> permissions = Main.groupManager.getPermissions(group);

            sender.sendMessage("§6=== Infos du groupe: §e" + group + " §6===");

            if (chatPrefix.isBlank()) {
                sender.sendMessage("§7Prefix chat: §cAucun");
            } else {
                sender.sendMessage("§7Prefix chat: §f" + chatPrefix + "§r§8(aperçu)");
            }

            if (tabPrefix.isBlank()) {
                sender.sendMessage("§7Prefix tab: §cAucun");
            } else {
                sender.sendMessage("§7Prefix tab: §f" + tabPrefix + "§r§8(aperçu)");
            }

            sender.sendMessage("§7Priorité: §f" + priority);

            if (permissions.isEmpty()) {
                sender.sendMessage("§7Permissions: §cAucune");
            } else {
                sender.sendMessage("§7Permissions:");
                for (String perm : permissions) {
                    sender.sendMessage("§8- §f" + perm);
                }
            }

            return true;
        }

        // -------- SET --------
        if (sub.equals("set")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /group set <joueur> <groupe>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            String group = args[2];

            if (!Main.groupManager.exists(group)) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            if (!target.isOnline() && !target.hasPlayedBefore()) {
                sender.sendMessage("§cCe joueur n'a jamais joué.");
                return true;
            }

            if (Main.groupManager.setPlayerGroup(target, group)) {
                sender.sendMessage("§aGroupe défini pour " + target.getName() + ".");
            } else {
                sender.sendMessage("§cImpossible de définir ce groupe.");
            }

            return true;
        }

        // -------- ADDPERM --------
        if (sub.equals("addperm")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /group addperm <groupe> <permission>");
                return true;
            }

            if (!Main.groupManager.exists(args[1])) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            if (Main.groupManager.addPermission(args[1], args[2])) {
                sender.sendMessage("§aPermission ajoutée.");
            } else {
                sender.sendMessage("§cImpossible d'ajouter cette permission.");
            }
            return true;
        }

        // -------- DELPERM --------
        if (sub.equals("delperm")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /group delperm <groupe> <permission>");
                return true;
            }

            if (!Main.groupManager.exists(args[1])) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            if (Main.groupManager.removePermission(args[1], args[2])) {
                sender.sendMessage("§aPermission supprimée.");
            } else {
                sender.sendMessage("§cImpossible de supprimer cette permission.");
            }
            return true;
        }

        // -------- SET CHAT PREFIX --------
        if (sub.equals("setchatprefix")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /group setchatprefix <groupe> <prefix>");
                return true;
            }

            if (!Main.groupManager.exists(args[1])) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            String prefix = Main.groupManager.formatPrefix(
                    String.join(" ", Arrays.copyOfRange(args, 2, args.length))
            );

            if (Main.groupManager.setChatPrefix(args[1], prefix)) {
                sender.sendMessage("§aPréfixe chat changé.");
            } else {
                sender.sendMessage("§cImpossible de changer le préfixe chat.");
            }
            return true;
        }

        // -------- CLEAR CHAT PREFIX --------
        if (sub.equals("clearchatprefix")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /group clearchatprefix <groupe>");
                return true;
            }

            if (Main.groupManager.clearChatPrefix(args[1])) {
                sender.sendMessage("§aPréfixe chat supprimé.");
            } else {
                sender.sendMessage("§cImpossible de supprimer le préfixe chat.");
            }
            return true;
        }

        // -------- SET TAB PREFIX --------
        if (sub.equals("settabprefix")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /group settabprefix <groupe> <prefix>");
                return true;
            }

            if (!Main.groupManager.exists(args[1])) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            String prefix = Main.groupManager.formatPrefix(
                    String.join(" ", Arrays.copyOfRange(args, 2, args.length))
            );

            if (Main.groupManager.setTabPrefix(args[1], prefix)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String playerGroup = Main.groups.get(p.getUniqueId());
                    playerGroup = Main.groupManager.getValidGroupOrDefault(playerGroup);

                    if (args[1].equalsIgnoreCase(playerGroup)) {
                        Main.updateTab(p);
                    }
                }

                sender.sendMessage("§aPréfixe tab changé.");
            } else {
                sender.sendMessage("§cImpossible de changer le préfixe tab.");
            }

            return true;
        }

        // -------- CLEAR TAB PREFIX --------
        if (sub.equals("cleartabprefix")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /group cleartabprefix <groupe>");
                return true;
            }

            if (Main.groupManager.clearTabPrefix(args[1])) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String playerGroup = Main.groups.get(p.getUniqueId());
                    playerGroup = Main.groupManager.getValidGroupOrDefault(playerGroup);

                    if (args[1].equalsIgnoreCase(playerGroup)) {
                        Main.updateTab(p);
                    }
                }

                sender.sendMessage("§aPréfixe tab supprimé.");
            } else {
                sender.sendMessage("§cImpossible de supprimer le préfixe tab.");
            }
            return true;
        }

        // -------- PRIORITY --------
        if (sub.equals("setpriority")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /group setpriority <groupe> <nombre>");
                return true;
            }

            if (!Main.groupManager.exists(args[1])) {
                sender.sendMessage("§cGroupe inexistant.");
                return true;
            }

            int priority;
            try {
                priority = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre invalide.");
                return true;
            }

            if (Main.groupManager.setPriority(args[1], priority)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String playerGroup = Main.groups.get(p.getUniqueId());
                    playerGroup = Main.groupManager.getValidGroupOrDefault(playerGroup);

                    if (args[1].equalsIgnoreCase(playerGroup)) {
                        Main.updateTab(p);
                    }
                }

                sender.sendMessage("§aPriorité définie.");
            } else {
                sender.sendMessage("§cImpossible de définir la priorité.");
            }

            return true;
        }

        sender.sendMessage("§cSous-commande inconnue.");
        return true;
    }
}