package fr.segame.armesiaWorld.commands;

import fr.segame.armesiaWorld.SpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;

    public SetSpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        if (!player.hasPermission("armesia.world.setspawn")) {
            player.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        spawnManager.setSpawn(player.getLocation());

        player.sendMessage("§aSpawn défini à :");
        player.sendMessage(String.format("  §7Monde : §f%s", player.getWorld().getName()));
        player.sendMessage(String.format("  §7Position : §f%.1f, %.1f, %.1f",
                Math.floor(player.getLocation().getX()) + 0.5,
                player.getLocation().getY(),
                Math.floor(player.getLocation().getZ()) + 0.5));
        player.sendMessage(String.format("  §7Vue : §fYaw=%.1f  Pitch=%.1f",
                player.getLocation().getYaw(),
                player.getLocation().getPitch()));
        return true;
    }
}

