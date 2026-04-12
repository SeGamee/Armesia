package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.MainWorld;
import fr.segame.armesiaWorld.MapZone;
import fr.segame.armesiaWorld.MapZoneManager;
import fr.segame.armesiaWorld.Portal;
import fr.segame.armesiaWorld.PortalManager;
import fr.segame.armesiaWorld.listeners.MapWandListener;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /portal wand                    – Donne l'outil de sélection (même que /mapzone wand)
 * /portal set <nom> <zone>        – Crée un portail lié à une zone de map
 * /portal list                    – Liste les portails
 * /portal delete <nom>            – Supprime un portail
 * /portal info <nom>              – Détails d'un portail
 */
public class PortalCommand implements CommandExecutor, TabCompleter {

    private final MapZoneManager  mapZoneManager;
    private final PortalManager   portalManager;
    private final MapWandListener wandListener;

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("wand", "set", "list", "delete", "info");

    public PortalCommand(MapZoneManager mapZoneManager, PortalManager portalManager,
                         MapWandListener wandListener) {
        this.mapZoneManager = mapZoneManager;
        this.portalManager  = portalManager;
        this.wandListener   = wandListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("armesia.world.portal")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            // ── Wand ──────────────────────────────────────────────────────────
            case "wand" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande réservée aux joueurs."); return true;
                }
                player.getInventory().addItem(wandListener.createWand());
                player.sendMessage("§d[Portal] §fOutil de sélection obtenu !");
                player.sendMessage("§7• Clic §fgauche §7→ §aPosition 1");
                player.sendMessage("§7• Clic §fdroit  §7→ §aPosition 2");
                player.sendMessage("§7Puis : §e/portal set <nom> <zone>");
            }

            // ── Set ───────────────────────────────────────────────────────────
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande réservée aux joueurs."); return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§cUsage : /portal set <nom_portail> <nom_zone>");
                    return true;
                }
                Location pos1 = wandListener.getPos1(player.getUniqueId());
                Location pos2 = wandListener.getPos2(player.getUniqueId());
                if (pos1 == null || pos2 == null) {
                    player.sendMessage("§c[Portal] Définissez d'abord les deux positions avec §e/portal wand§c !");
                    return true;
                }
                if (!pos1.getWorld().equals(pos2.getWorld())) {
                    player.sendMessage("§c[Portal] Les deux positions doivent être dans le même monde.");
                    return true;
                }
                String portalName = args[1];
                String zoneName   = args[2];

                // Vérifier que la zone cible existe
                MapZone zone = mapZoneManager.getZone(zoneName);
                if (zone == null) {
                    player.sendMessage("§c[Portal] Zone '§6" + zoneName + "§c' introuvable.");
                    player.sendMessage("§7Zones disponibles : §e"
                            + String.join("§7, §e", mapZoneManager.getZoneNames()));
                    return true;
                }

                Portal portal = new Portal(portalName,
                        pos1.getWorld().getName(),
                        pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                        pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ(),
                        zoneName);
                portalManager.addPortal(portal);
                wandListener.clearPositions(player.getUniqueId());

                player.sendMessage("§a[Portal] §fPortail §d" + portalName
                        + "§f créé → lié à la zone §6" + zoneName + "§f !");
                player.sendMessage("§7Les joueurs entrant dans la zone seront téléportés en parachute.");
            }

            // ── List ──────────────────────────────────────────────────────────
            case "list" -> {
                List<String> names = portalManager.getPortalNames();
                if (names.isEmpty()) {
                    sender.sendMessage("§7Aucun portail enregistré.");
                } else {
                    sender.sendMessage("§d=== Portails (" + names.size() + ") ===");
                    for (String n : names) {
                        Portal p = portalManager.getPortal(n);
                        sender.sendMessage("  §7- §d" + n + " §7→ zone §6" + p.getTargetZone());
                    }
                }
            }

            // ── Delete ────────────────────────────────────────────────────────
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /portal delete <nom>"); return true;
                }
                if (portalManager.removePortal(args[1])) {
                    sender.sendMessage("§a[Portal] §fPortail §d" + args[1] + "§f supprimé.");
                } else {
                    sender.sendMessage("§c[Portal] Portail '§d" + args[1] + "§c' introuvable.");
                }
            }

            // ── Info ──────────────────────────────────────────────────────────
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /portal info <nom>"); return true;
                }
                Portal p = portalManager.getPortal(args[1]);
                if (p == null) {
                    sender.sendMessage("§c[Portal] Portail '§d" + args[1] + "§c' introuvable."); return true;
                }
                sender.sendMessage("§d=== Portail : " + p.getName() + " ===");
                sender.sendMessage("§7Monde : §f" + p.getWorldName());
                sender.sendMessage("§7Min : §f(" + p.getMinX() + ", " + p.getMinY() + ", " + p.getMinZ() + ")");
                sender.sendMessage("§7Max : §f(" + p.getMaxX() + ", " + p.getMaxY() + ", " + p.getMaxZ() + ")");
                sender.sendMessage("§7Zone cible : §6" + p.getTargetZone());
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("info")))
            return portalManager.getPortalNames().stream()
                    .filter(n -> n.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("set"))
            return mapZoneManager.getZoneNames().stream()
                    .filter(n -> n.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§d=== Portal ===");
        sender.sendMessage("§e/portal wand §f- Obtenir l'outil de sélection");
        sender.sendMessage("§e/portal set §7<nom> <zone> §f- Créer un portail lié à une zone");
        sender.sendMessage("§e/portal list §f- Lister tous les portails");
        sender.sendMessage("§e/portal info §7<nom> §f- Voir les détails d'un portail");
        sender.sendMessage("§e/portal delete §7<nom> §f- Supprimer un portail");
    }
}

