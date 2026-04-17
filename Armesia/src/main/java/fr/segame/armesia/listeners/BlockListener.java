package fr.segame.armesia.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (!event.getPlayer().isOp() && block.getType() == Material.TNT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVous ne pouvez pas poser de TNT.");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if(event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
        }
    }

    /** Bloque le craft dans la grille de l'inventaire joueur (4 slots). */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (player.isOp()) return;
        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    /** Bloque la récupération de l'item crafté (shift-clic inclus). */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.isOp()) return;
        event.setCancelled(true);
        player.sendMessage("§cVous ne pouvez pas crafter.");
    }

    /** Désactive l'ouverture du craft inventaire, établi, hopper, distributeur, four, enclume. */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (player.isOp()) return;

        InventoryType type = event.getInventory().getType();
        switch (type) {
            case CRAFTING, WORKBENCH, HOPPER, DROPPER, FURNACE, BLAST_FURNACE, SMOKER, ANVIL -> {
                event.setCancelled(true);
                player.sendMessage("§cVous ne pouvez pas utiliser cet équipement.");
            }
            default -> {}
        }
    }

    /** Désactive l'utilisation des feux d'artifice. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true);
        }
    }
}
