package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * /near  — Affiche les joueurs proches.
 *
 * Rayon déterminé par permission (sans argument) :
 *   armesia.near      → 100 blocs
 *   armesia.near.200  → 200 blocs
 *   armesia.near.300  → 300 blocs
 *   armesia.near.400  → 400 blocs
 *   armesia.near.500  → 500 blocs
 */
public class NearCommand implements CommandExecutor {

    private static final int[] LEVELS = {500, 400, 300, 200, 100};

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }

        if (!Main.hasGroupPermission(player, "armesia.near")) {
            player.sendMessage(Main.getInstance().getConfig().getString(
                    "general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        // Rayon = plus grand palier pour lequel le joueur a la permission
        int radius = 100;
        for (int lvl : LEVELS) {
            if (lvl == 100) break; // base déjà à 100
            if (Main.hasGroupPermission(player, "armesia.near." + lvl)) {
                radius = lvl;
                break;
            }
        }

        List<Player> nearby = new ArrayList<>();
        double rSq = (double) radius * radius;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (other.getLocation().distanceSquared(player.getLocation()) <= rSq) {
                nearby.add(other);
            }
        }

        nearby.sort(Comparator.comparingDouble(
                o -> o.getLocation().distanceSquared(player.getLocation())));

        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§aJoueurs proches §8(§7" + radius + " blocs§8) §8— §f" + nearby.size());
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        if (nearby.isEmpty()) {
            player.sendMessage("§7Aucun joueur à proximité.");
        } else {
            for (Player other : nearby) {
                int dist = (int) other.getLocation().distance(player.getLocation());
                player.sendMessage("§7• §f" + other.getDisplayName() + " §8— §a" + dist + " §7blocs");
            }
        }
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return true;
    }
}
