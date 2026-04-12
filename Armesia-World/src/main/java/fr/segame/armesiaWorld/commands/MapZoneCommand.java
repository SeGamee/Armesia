package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.MainWorld;
import fr.segame.armesiaWorld.MapZone;
import fr.segame.armesiaWorld.MapZoneManager;
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
 * /mapzone wand              – Donne l'outil de sélection
 * /mapzone set <nom>         – Crée une zone avec les positions sélectionnées
 * /mapzone list              – Liste les zones enregistrées
 * /mapzone delete <nom>      – Supprime une zone
 * /mapzone info <nom>        – Affiche les détails d'une zone
 */
public class MapZoneCommand implements CommandExecutor, TabCompleter {

    private final MainWorld      plugin;
    private final MapZoneManager mapZoneManager;
    private final MapWandListener wandListener;

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("wand", "set", "list", "delete", "info");

    public MapZoneCommand(MainWorld plugin, MapZoneManager mapZoneManager,
                          MapWandListener wandListener) {
        this.plugin         = plugin;
        this.mapZoneManager = mapZoneManager;
        this.wandListener   = wandListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("armesia.world.mapzone")) {
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
                player.sendMessage("§a[MapZone] §fOutil de sélection obtenu !");
                player.sendMessage("§7• Clic §fgauche §7→ §aPosition 1");
                player.sendMessage("§7• Clic §fdroit  §7→ §aPosition 2");
            }

            // ── Set ───────────────────────────────────────────────────────────
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCommande réservée aux joueurs."); return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage : /mapzone set <nom>"); return true;
                }
                Location pos1 = wandListener.getPos1(player.getUniqueId());
                Location pos2 = wandListener.getPos2(player.getUniqueId());
                if (pos1 == null || pos2 == null) {
                    player.sendMessage("§c[MapZone] Définissez d'abord les deux positions avec §e/mapzone wand§c !");
                    return true;
                }
                if (!pos1.getWorld().equals(pos2.getWorld())) {
                    player.sendMessage("§c[MapZone] Les deux positions doivent être dans le même monde.");
                    return true;
                }
                String name = args[1];
                MapZone zone = new MapZone(name,
                        pos1.getWorld().getName(),
                        pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                        pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
                mapZoneManager.addZone(zone);
                wandListener.clearPositions(player.getUniqueId());
                player.sendMessage("§a[MapZone] §fZone §6" + name + "§f créée avec succès !");
                player.sendMessage("§7Parachute spawn → §e" + fmtLoc(zone.getParachuteSpawn()));
            }

            // ── List ──────────────────────────────────────────────────────────
            case "list" -> {
                List<String> names = mapZoneManager.getZoneNames();
                if (names.isEmpty()) {
                    sender.sendMessage("§7Aucune zone de map enregistrée.");
                } else {
                    sender.sendMessage("§6=== Zones de map (" + names.size() + ") ===");
                    names.forEach(n -> sender.sendMessage("  §7- §a" + n));
                    sender.sendMessage("§7Utilisez §e/map <nom> §7pour vous téléporter.");
                }
            }

            // ── Delete ────────────────────────────────────────────────────────
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /mapzone delete <nom>"); return true;
                }
                if (mapZoneManager.removeZone(args[1])) {
                    sender.sendMessage("§a[MapZone] §fZone §6" + args[1] + "§f supprimée.");
                } else {
                    sender.sendMessage("§c[MapZone] Zone '§6" + args[1] + "§c' introuvable.");
                }
            }

            // ── Info ──────────────────────────────────────────────────────────
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /mapzone info <nom>"); return true;
                }
                MapZone z = mapZoneManager.getZone(args[1]);
                if (z == null) {
                    sender.sendMessage("§c[MapZone] Zone '§6" + args[1] + "§c' introuvable."); return true;
                }
                sender.sendMessage("§6=== Zone : " + z.getName() + " ===");
                sender.sendMessage("§7Monde : §f" + z.getWorldName());
                sender.sendMessage("§7Min : §f(" + z.getMinX() + ", " + z.getMinY() + ", " + z.getMinZ() + ")");
                sender.sendMessage("§7Max : §f(" + z.getMaxX() + ", " + z.getMaxY() + ", " + z.getMaxZ() + ")");
                sender.sendMessage("§7Spawn parachute : §e" + fmtLoc(z.getParachuteSpawn()));
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
            return mapZoneManager.getZoneNames().stream()
                    .filter(n -> n.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== MapZone ===");
        sender.sendMessage("§e/mapzone wand §f- Obtenir l'outil de sélection (Blaze Rod)");
        sender.sendMessage("§e/mapzone set §7<nom> §f- Créer une zone avec les positions sélectionnées");
        sender.sendMessage("§e/mapzone list §f- Lister toutes les zones");
        sender.sendMessage("§e/mapzone info §7<nom> §f- Voir les détails d'une zone");
        sender.sendMessage("§e/mapzone delete §7<nom> §f- Supprimer une zone");
    }

    private String fmtLoc(Location l) {
        if (l == null) return "§cnon disponible";
        return l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
    }
}

