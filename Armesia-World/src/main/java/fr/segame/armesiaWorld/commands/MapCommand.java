package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.MapZone;
import fr.segame.armesiaWorld.MapZoneManager;
import fr.segame.armesiaWorld.ParachuteManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /map <nom> – Se téléporter en parachute dans la zone de map indiquée.
 */
public class MapCommand implements CommandExecutor, TabCompleter {

    private final MapZoneManager   mapZoneManager;
    private final ParachuteManager parachuteManager;

    public MapCommand(MapZoneManager mapZoneManager, ParachuteManager parachuteManager) {
        this.mapZoneManager   = mapZoneManager;
        this.parachuteManager = parachuteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        if (!player.hasPermission("armesia.world.map")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage : /map <nom>");
            // Afficher les zones disponibles s'il y en a
            List<String> zones = mapZoneManager.getZoneNames();
            if (!zones.isEmpty())
                player.sendMessage("§7Zones disponibles : §e" + String.join("§7, §e", zones));
            return true;
        }

        String name = args[0];
        MapZone zone = mapZoneManager.getZone(name);

        if (zone == null) {
            player.sendMessage("§c[Map] Zone '§6" + name + "§c' introuvable.");
            List<String> zones = mapZoneManager.getZoneNames();
            if (!zones.isEmpty())
                player.sendMessage("§7Zones disponibles : §e" + String.join("§7, §e", zones));
            return true;
        }

        Location spawn = zone.getParachuteSpawn();
        if (spawn == null) {
            player.sendMessage("§c[Map] Le monde '§6" + zone.getWorldName() + "§c' n'est pas chargé.");
            return true;
        }

        // Lancer le TP parachute
        parachuteManager.launch(player, spawn, zone.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1)
            return mapZoneManager.getZoneNames().stream()
                    .filter(n -> n.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        return List.of();
    }
}

