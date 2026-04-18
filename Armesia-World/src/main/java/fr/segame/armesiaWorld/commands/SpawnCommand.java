package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.SpawnManager;
import fr.segame.armesiaWorld.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;
    private final TeleportManager teleportManager;

    public SpawnCommand(SpawnManager spawnManager, TeleportManager teleportManager) {
        this.spawnManager    = spawnManager;
        this.teleportManager = teleportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        if (!spawnManager.hasSpawn()) {
            player.sendMessage("§cAucun spawn n'a encore été défini. Utilisez §e/setspawn§c.");
            return true;
        }

        // /spawn <joueur> — TP immédiat d'un autre joueur au spawn
        if (args.length >= 1) {
            if (!sender.hasPermission("armesia.world.spawn.other")) {
                sender.sendMessage("§cVous n'avez pas la permission de téléporter un autre joueur.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("§cJoueur §f" + args[0] + " §cintrouvable ou hors ligne.");
                return true;
            }

            target.teleport(spawnManager.getSpawn());
            target.sendTitle("§a✔ Spawn", "§7Téléporté par §e" + sender.getName(), 5, 50, 10);
            target.sendMessage("§aVous avez été téléporté au spawn par §e" + sender.getName() + "§a.");
            sender.sendMessage("§a§f" + target.getName() + " §aa été téléporté au spawn.");
            return true;
        }

        // /spawn — TP du joueur lui-même (countdown)
        if (teleportManager.hasPending(player.getUniqueId())) {
            player.sendMessage("§eUne téléportation est déjà en cours...");
            return true;
        }

        teleportManager.requestTeleport(player, spawnManager.getSpawn());
        return true;
    }
}
