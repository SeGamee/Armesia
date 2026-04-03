package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.SpawnManager;
import fr.segame.armesiaWorld.TeleportManager;
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
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        if (!spawnManager.hasSpawn()) {
            player.sendMessage("§cAucun spawn n'a encore été défini. Utilisez §e/setspawn§c.");
            return true;
        }

        if (teleportManager.hasPending(player.getUniqueId())) {
            player.sendMessage("§eUne téléportation est déjà en cours...");
            return true;
        }

        teleportManager.requestTeleport(player, spawnManager.getSpawn());
        return true;
    }
}
