package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BaltopCommand implements CommandExecutor {

    private static final int PAGE_SIZE  = 10;
    private static final int MAX_PAGES  = 10;

    private final Main plugin;

    public BaltopCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.baltop")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        int page = 1;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cUsage : §f/baltop [page]");
                return true;
            }
        }

        // ── Récupération et tri de tous les joueurs ──────────────────────────
        List<Map.Entry<String, Double>> entries = new ArrayList<>();

        if (plugin.getPlayersConfig().contains("players")) {
            for (String uuidStr : plugin.getPlayersConfig().getConfigurationSection("players").getKeys(false)) {
                double money = plugin.getPlayersConfig().getDouble("players." + uuidStr + ".money", 0.0);
                if (money <= 0) continue;

                String name;
                try {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                    name = op.getName() != null ? op.getName() : uuidStr;
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                entries.add(new AbstractMap.SimpleEntry<>(name, money));
            }
        }

        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // ── Pagination ───────────────────────────────────────────────────────
        int totalEntries = Math.min(entries.size(), PAGE_SIZE * MAX_PAGES);
        int totalPages   = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));

        if (page < 1 || page > totalPages) {
            sender.sendMessage("§cPage invalide. Pages disponibles : §f1 §càj §f" + totalPages + "§c.");
            return true;
        }

        int from  = (page - 1) * PAGE_SIZE;
        int to    = Math.min(from + PAGE_SIZE, totalEntries);

        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§6§lClassement §e§largent §8— §7Page §f" + page + "§7/§f" + totalPages);
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (entries.isEmpty()) {
            sender.sendMessage("§7Aucune donnée disponible.");
        } else {
            for (int i = from; i < to; i++) {
                Map.Entry<String, Double> entry = entries.get(i);
                String rank = rankPrefix(i + 1);
                sender.sendMessage(rank + " §f" + entry.getKey()
                        + " §8— §6" + plugin.getEconomyAPI().formatMoney(entry.getValue()));
            }
        }

        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (totalPages > 1) {
            sender.sendMessage("§7Utilisez §f/baltop <page> §7pour naviguer.");
        }
        return true;
    }

    private String rankPrefix(int rank) {
        return switch (rank) {
            case 1 -> "§6§l#1";
            case 2 -> "§7§l#2";
            case 3 -> "§c§l#3";
            default -> "§8#" + rank;
        };
    }
}


