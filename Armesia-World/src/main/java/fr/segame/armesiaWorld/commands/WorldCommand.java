package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.WorldManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WorldCommand implements CommandExecutor, TabCompleter {

    private final WorldManager worldManager;
    private static final List<String> SUB_COMMANDS = Arrays.asList("create", "register", "teleport", "list");

    public WorldCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("armesia.world.use")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /world create <nom>"); return true; }
                sender.sendMessage("§aGénération du monde §6" + args[1] + "§a en cours...");
                var world = worldManager.createWorld(args[1]);
                if (world != null) sender.sendMessage("§aLe monde §6" + args[1] + "§a a été créé et enregistré !");
                else               sender.sendMessage("§cÉchec de la création du monde §6" + args[1] + "§c.");
            }
            case "register" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /world register <nom>"); return true; }
                sender.sendMessage("§eChargement du monde §6" + args[1] + "§e...");
                if (worldManager.registerWorld(args[1]))
                    sender.sendMessage("§aLe monde §6" + args[1] + "§a est chargé et enregistré (auto-chargé au démarrage).");
                else
                    sender.sendMessage("§cAucun dossier de monde trouvé pour §6" + args[1] + "§c. Vérifiez le nom du dossier sur le serveur.");
            }
            case "teleport", "tp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /world teleport <nom>");
                    return true;
                }
                if (worldManager.teleportToWorld(player, args[1])) {
                    player.sendMessage("§aTéléportation vers §6" + args[1] + "§a !");
                } else {
                    player.sendMessage("§cLe monde §6" + args[1] + "§c n'est pas chargé.");
                }
            }
            case "list" -> {
                List<String> worlds = worldManager.getLoadedWorldNames();
                sender.sendMessage("§6Mondes chargés §7(" + worlds.size() + ")§6 :");
                worlds.forEach(w -> sender.sendMessage("  §7- §a" + w));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("teleport")
                || args[0].equalsIgnoreCase("tp")
                || args[0].equalsIgnoreCase("register")))
            return worldManager.getLoadedWorldNames().stream()
                    .filter(w -> w.startsWith(args[1])).collect(Collectors.toList());
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Armesia-World ===");
        sender.sendMessage("§e/world create §7<nom> §f- Créer un nouveau monde");
        sender.sendMessage("§e/world register §7<nom> §f- Enregistrer un monde existant (auto-chargé au démarrage)");
        sender.sendMessage("§e/world teleport §7<nom> §f- Se téléporter dans un monde");
        sender.sendMessage("§e/world list §f- Lister les mondes normaux chargés");
    }
}







