package fr.segame.armesiaMobs.commands;

import fr.segame.armesiaMobs.ArmesiaMobs;
import fr.segame.armesiaMobs.config.MessagesConfig;
import fr.segame.armesiaMobs.managers.StatsManager;
import fr.segame.armesiaMobs.mobs.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /mobstats — Statistiques de kills des mobs
 *
 *  /mobstats                              → ses propres stats            (armesia.mobstats)
 *  /mobstats <joueur>                     → stats d'un autre joueur      (armesia.mobstats.others)
 *  /mobstats reset <joueur> [mobId]       → réinitialise                 (armesia.mobstats.admin)
 *  /mobstats add <joueur> <mobId> <n>     → ajoute n kills               (armesia.mobstats.admin)
 *  /mobstats remove <joueur> <mobId> <n>  → retire n kills               (armesia.mobstats.admin)
 *  /mobstats top [mobId]                  → top 10                       (armesia.mobstats.admin)
 */
public class StatsCommand implements CommandExecutor, TabCompleter {

    private final StatsManager statsManager;
    private final MobManager   mobManager;

    private MessagesConfig msg() { return ArmesiaMobs.getInstance().getMessages(); }

    public StatsCommand(StatsManager statsManager, MobManager mobManager) {
        this.statsManager = statsManager;
        this.mobManager   = mobManager;
    }

    // ─── Exécution ────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /mobstats (sans argument) → ses propres stats
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg().get("common.player-only")); return true;
            }
            showStats(sender, p.getUniqueId(), p.getName());
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── reset ─────────────────────────────────────────────────────────
            case "reset" -> {
                if (!sender.hasPermission("armesia.mobstats.admin")) {
                    sender.sendMessage(msg().get("common.no-permission")); return true;
                }
                if (args.length < 2) { sender.sendMessage(msg().get("common.args-missing")); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage(msg().get("stats.never-joined", "player", args[1])); return true;
                }
                if (args.length >= 3) {
                    statsManager.resetStats(target.getUniqueId(), args[2]);
                    sender.sendMessage(msg().get("stats.reset-mob", "player", args[1], "mob", args[2]));
                } else {
                    statsManager.resetStats(target.getUniqueId());
                    sender.sendMessage(msg().get("stats.reset-all", "player", args[1]));
                }
                statsManager.save();
            }

            // ── add ───────────────────────────────────────────────────────────
            case "add" -> {
                if (!sender.hasPermission("armesia.mobstats.admin")) {
                    sender.sendMessage(msg().get("common.no-permission")); return true;
                }
                if (args.length < 4) { sender.sendMessage(msg().get("common.args-missing")); return true; }
                int amount = parsePositiveInt(args[3]);
                if (amount <= 0) { sender.sendMessage(msg().get("stats.amount-invalid")); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage(msg().get("stats.never-joined", "player", args[1])); return true;
                }
                statsManager.addKills(target.getUniqueId(), args[2], amount);
                statsManager.save();
                sender.sendMessage(msg().get("stats.add-kills",
                        "amount", amount, "mob", args[2], "player", args[1]));
            }

            // ── remove ────────────────────────────────────────────────────────
            case "remove" -> {
                if (!sender.hasPermission("armesia.mobstats.admin")) {
                    sender.sendMessage(msg().get("common.no-permission")); return true;
                }
                if (args.length < 4) { sender.sendMessage(msg().get("common.args-missing")); return true; }
                int amount = parsePositiveInt(args[3]);
                if (amount <= 0) { sender.sendMessage(msg().get("stats.amount-invalid")); return true; }
                OfflinePlayer target = getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage(msg().get("stats.never-joined", "player", args[1])); return true;
                }
                statsManager.removeKills(target.getUniqueId(), args[2], amount);
                statsManager.save();
                sender.sendMessage(msg().get("stats.remove-kills",
                        "amount", amount, "mob", args[2], "player", args[1]));
            }

            // ── top ───────────────────────────────────────────────────────────
            case "top" -> {
                if (!sender.hasPermission("armesia.mobstats.admin")) {
                    sender.sendMessage(msg().get("common.no-permission")); return true;
                }
                String mobId = args.length >= 2 ? args[1] : null;
                List<Map.Entry<UUID, Integer>> top = statsManager.getTop(mobId, 10);
                if (top.isEmpty()) { sender.sendMessage(msg().get("stats.top-empty")); return true; }
                String filter = mobId != null
                        ? msg().get("stats.top-filter", "mob", mobId)
                        : msg().get("stats.top-global");
                msg().getLines("stats.top-header", "filter", filter).forEach(sender::sendMessage);
                int rank = 1;
                for (Map.Entry<UUID, Integer> entry : top) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                    String name = op.getName() != null ? op.getName() : entry.getKey().toString().substring(0, 8) + "...";
                    msg().getLines("stats.top-entry", "rank", rank, "player", name, "kills", entry.getValue())
                         .forEach(sender::sendMessage);
                    rank++;
                }
            }

            // ── /mobstats <joueur> ────────────────────────────────────────────
            default -> {
                boolean isSelf = sender instanceof Player p && args[0].equalsIgnoreCase(p.getName());
                if (!isSelf && !sender.hasPermission("armesia.mobstats.others")) {
                    sender.sendMessage(msg().get("common.no-permission")); return true;
                }
                OfflinePlayer target = getOfflinePlayer(args[0]);
                if (!target.hasPlayedBefore() && !isSelf) {
                    sender.sendMessage(msg().get("stats.never-joined", "player", args[0])); return true;
                }
                showStats(sender, target.getUniqueId(), args[0]);
            }
        }
        return true;
    }

    // ─── Affichage ────────────────────────────────────────────────────────────

    private void showStats(CommandSender s, UUID uuid, String name) {
        Map<String, Integer> ps = statsManager.getStats(uuid);
        if (ps.isEmpty()) { s.sendMessage(msg().get("stats.no-stats", "player", name)); return; }
        msg().getLines("stats.header", "player", name).forEach(s::sendMessage);
        ps.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> msg().getLines("stats.entry",
                                "mob", getMobDisplayName(e.getKey()), "kills", e.getValue())
                                   .forEach(s::sendMessage));
        int total = ps.values().stream().mapToInt(Integer::intValue).sum();
        msg().getLines("stats.total", "total", total).forEach(s::sendMessage);
    }

    /** Retourne le nom d'affichage du mob (fallback sur l'ID si non trouvé). */
    private String getMobDisplayName(String mobId) {
        fr.segame.armesiaMobs.mobs.MobData mob = mobManager.getMob(mobId);
        return (mob != null) ? mob.getName() : mobId;
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(List.of("reset", "add", "remove", "top"));
            Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(suggestions::add);
            return suggestions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            return switch (sub) {
                case "reset", "add", "remove" -> Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "top" -> mobManager.getMobIds().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .sorted().collect(Collectors.toList());
                default -> List.of();
            };
        }
        if (args.length == 3 && (sub.equals("reset") || sub.equals("add") || sub.equals("remove")))
            return mobManager.getMobIds().stream().filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                    .sorted().collect(Collectors.toList());
        if (args.length == 4 && (sub.equals("add") || sub.equals("remove")))
            return List.of("1", "5", "10", "50", "100");
        return List.of();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private OfflinePlayer getOfflinePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }

    private int parsePositiveInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }
}
