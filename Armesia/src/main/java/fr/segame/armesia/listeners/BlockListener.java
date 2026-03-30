package fr.segame.armesia.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!player.isOp() && block.getType() == Material.TNT) {
            event.setCancelled(true);
            player.sendMessage("§cVous ne pouvez pas poser de TNT.");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if(event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
        }
    }
}
