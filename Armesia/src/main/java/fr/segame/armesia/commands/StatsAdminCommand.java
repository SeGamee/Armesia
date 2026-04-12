package fr.segame.armesia.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StatsAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        OfflinePlayer target;
        if (args.length < 4) {
            sender.sendMessage("§cUsage : /statsadmin <joueur> <add/remove> <kill/death/killstreak/bestkillstreak> <valeur>");
            return true;
        } else {
            target = sender.getServer().getOfflinePlayer(args[0]);
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage("§cCe joueur n'a jamais joué sur le serveur.");
                return true;
            }
        }

        String action = args[1].toLowerCase();
        String stat = args[2].toLowerCase();
        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cLa valeur doit être un nombre entier.");
            return true;
        }

        // Récupérer le StatsManager
        fr.segame.armesia.managers.StatsManager statsManager = fr.segame.armesia.Main.getStatsManager();
        if (statsManager == null) {
            sender.sendMessage("§cStatsManager introuvable.");
            return true;
        }

        boolean success = false;
        String statName = "";
        switch (stat) {
            case "kill":
                statName = "Kills";
                if (action.equals("add")) {
                    for (int i = 0; i < value; i++) statsManager.addKill(target.getUniqueId());
                    success = true;
                } else if (action.equals("remove")) {
                    int current = statsManager.getKills(target.getUniqueId());
                    statsManager.setKills(target.getUniqueId(), Math.max(0, current - value));
                    success = true;
                }
                break;
            case "death":
                statName = "Morts";
                if (action.equals("add")) {
                    for (int i = 0; i < value; i++) statsManager.addDeath(target.getUniqueId());
                    success = true;
                } else if (action.equals("remove")) {
                    int current = statsManager.getDeaths(target.getUniqueId());
                    statsManager.setDeaths(target.getUniqueId(), Math.max(0, current - value));
                    success = true;
                }
                break;
            case "killstreak":
                statName = "Killstreak";
                int newKillstreak;
                if (action.equals("add")) {
                    newKillstreak = statsManager.getKillstreak(target.getUniqueId()) + value;
                    statsManager.setKillstreak(target.getUniqueId(), newKillstreak);
                    success = true;
                } else if (action.equals("remove")) {
                    int current = statsManager.getKillstreak(target.getUniqueId());
                    newKillstreak = Math.max(0, current - value);
                    statsManager.setKillstreak(target.getUniqueId(), newKillstreak);
                    success = true;
                } else {
                    break;
                }
                // Mettre à jour le bestkillstreak si besoin
                int best = statsManager.getBestKillstreak(target.getUniqueId());
                if (newKillstreak > best) {
                    statsManager.setBestKillstreak(target.getUniqueId(), newKillstreak);
                }
                break;
            case "bestkillstreak":
                statName = "Killstreak max";
                if (action.equals("add")) {
                    statsManager.setBestKillstreak(target.getUniqueId(), statsManager.getBestKillstreak(target.getUniqueId()) + value);
                    success = true;
                } else if (action.equals("remove")) {
                    int current = statsManager.getBestKillstreak(target.getUniqueId());
                    statsManager.setBestKillstreak(target.getUniqueId(), Math.max(0, current - value));
                    success = true;
                }
                break;
            default:
                sender.sendMessage("§cStatistique inconnue. Utilisez kill, death, killstreak ou bestkillstreak.");
                return true;
        }

        if (!success) {
            sender.sendMessage("§cAction inconnue. Utilisez add ou remove.");
            return true;
        }

        // Refresh immédiat du scoreboard si le joueur est connecté
        fr.segame.armesia.Main.updatePlayerScoreboard(target.getUniqueId());

        sender.sendMessage("§a" + statName + " de §e" + target.getName() + " §amodifié avec succès.");
        return true;
    }
}
