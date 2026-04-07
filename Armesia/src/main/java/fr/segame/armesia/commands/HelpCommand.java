package fr.segame.armesia.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * /help [page]
 *
 * Affiche la liste de toutes les commandes disponibles du plugin,
 * filtrées selon les permissions du joueur.
 */
public class HelpCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 8;

    /** Entrée de la liste d'aide : commande, description, permission requise (null = tout le monde). */
    private record HelpEntry(String usage, String description, String permission) {}

    private static final List<HelpEntry> ALL_ENTRIES = List.of(
            // ── Utilitaires ──────────────────────────────────────────────────────
            new HelpEntry("/help [page]",               "Afficher cette aide",                                  null),
            new HelpEntry("/ping [joueur]",             "Afficher votre ping (ou celui d'un joueur)",           "armesia.ping"),
            new HelpEntry("/near [rayon]",              "Voir les joueurs proches",                             "armesia.near"),
            new HelpEntry("/heal [joueur]",             "Se soigner",                                           "armesia.heal"),
            new HelpEntry("/feed [joueur]",             "Se rassasier",                                         "armesia.feed"),
            new HelpEntry("/suicide",                   "Se suicider",                                          "armesia.suicide"),
            new HelpEntry("/god [joueur]",              "Activer/désactiver l'invincibilité",                   "armesia.god"),
            new HelpEntry("/cleareffect",               "Supprimer les effets actifs",                          "armesia.cleareffect"),
            // ── Inventaire ───────────────────────────────────────────────────────
            new HelpEntry("/invsee <joueur>",           "Voir l'inventaire d'un joueur",                        "armesia.invsee"),
            new HelpEntry("/clearinventory [joueur]",   "Vider un inventaire",                                  "armesia.clearinventory"),
            new HelpEntry("/item <sous-cmd>",           "Modifier l'item en main",                              "armesia.item"),
            // ── Économie ────────────────────────────────────────────────────────
            new HelpEntry("/money [joueur]",            "Voir son argent",                                      "armesia.money"),
            new HelpEntry("/tokens [joueur]",           "Voir ses tokens",                                      "armesia.tokens"),
            new HelpEntry("/pay <joueur> <montant>",    "Donner de l'argent",                                   "armesia.pay"),
            new HelpEntry("/baltop [page]",             "Classement des joueurs les plus riches",               "armesia.baltop"),
            // ── Administration ───────────────────────────────────────────────────
            new HelpEntry("/kill <joueur>",             "Tuer un joueur",                                       "armesia.kill"),
            new HelpEntry("/broadcast <message>",       "Envoyer un message global",                            "armesia.broadcast"),
            new HelpEntry("/group <...>",               "Gérer les groupes",                                    "armesia.group"),
            new HelpEntry("/stats [joueur]",            "Consulter les statistiques",                           "armesia.stats"),
            new HelpEntry("/statsadmin <...>",          "Gérer les statistiques",                               "armesia.statsadmin"),
            new HelpEntry("/reloadconfig",              "Recharger la configuration",                           "armesia.reloadconfig")
    );

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Filtrage selon permissions
        List<HelpEntry> visible = new ArrayList<>();
        for (HelpEntry entry : ALL_ENTRIES) {
            if (entry.permission() == null || sender.hasPermission(entry.permission())) {
                visible.add(entry);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) visible.size() / PAGE_SIZE));
        int page = 1;

        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cUsage : §f/help [page]");
                return true;
            }
        }

        if (page < 1 || page > totalPages) {
            sender.sendMessage("§cPage invalide. Pages disponibles : §f1 §càj §f" + totalPages + "§c.");
            return true;
        }

        int from = (page - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, visible.size());

        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§a§lAide §8— §7Page §f" + page + "§7/§f" + totalPages
                + (sender instanceof Player ? " §8— §7" + visible.size() + " commande(s) disponible(s)" : ""));
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        for (int i = from; i < to; i++) {
            HelpEntry entry = visible.get(i);
            sender.sendMessage("§a" + entry.usage() + " §8— §7" + entry.description());
        }

        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        if (totalPages > 1) {
            sender.sendMessage("§7Tapez §f/help " + Math.min(page + 1, totalPages) + " §7pour la page suivante.");
        }

        return true;
    }
}

