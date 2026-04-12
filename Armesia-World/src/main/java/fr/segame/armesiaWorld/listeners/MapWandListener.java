package fr.segame.armesiaWorld.listeners;

import fr.segame.armesiaWorld.MainWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener pour l'outil de sélection de zone (Blaze Rod « MapZone Wand »).
 * <ul>
 *   <li>Clic gauche sur un bloc → Position 1</li>
 *   <li>Clic droit sur un bloc  → Position 2</li>
 * </ul>
 */
public class MapWandListener implements Listener {

    /** Valeur stockée dans le PDC pour identifier l'outil. */
    private static final String WAND_VALUE = "mapzone_wand";

    private final MainWorld plugin;
    private final NamespacedKey wandKey;

    /** Positions sélectionnées par joueur. */
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public MapWandListener(MainWorld plugin) {
        this.plugin  = plugin;
        this.wandKey = new NamespacedKey(plugin, "wand_type");
    }

    // ─── Événement ───────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !isWand(item)) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true); // ne pas placer/casser le bloc

        Location clicked = event.getClickedBlock().getLocation().clone();
        Action action    = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            pos1Map.put(player.getUniqueId(), clicked);
            player.sendMessage("§a[MapZone] §fPosition §61 §fdéfinie : §e"
                    + fmtLoc(clicked));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            pos2Map.put(player.getUniqueId(), clicked);
            player.sendMessage("§a[MapZone] §fPosition §62 §fdéfinie : §e"
                    + fmtLoc(clicked));
        }

        // Afficher l'état des deux positions
        if (pos1Map.containsKey(player.getUniqueId()) && pos2Map.containsKey(player.getUniqueId())) {
            player.sendMessage("§a[MapZone] §fDeux positions définies ! Faites §e/mapzone set <nom> §fpour créer la zone.");
        }
    }

    // ─── API ─────────────────────────────────────────────────────────────────

    /** Crée l'item Blaze Rod avec le PDC wand. */
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta  = wand.getItemMeta();
        meta.setDisplayName("§6§lOutil de Sélection de Zone");
        meta.setLore(Arrays.asList(
                "§7Clic gauche §f→ §aPosition 1",
                "§7Clic droit  §f→ §aPosition 2",
                "",
                "§8Armesia-World MapZone"
        ));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, WAND_VALUE);
        wand.setItemMeta(meta);
        return wand;
    }

    /** Vérifie si un item est le wand. */
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return WAND_VALUE.equals(
                meta.getPersistentDataContainer().get(wandKey, PersistentDataType.STRING));
    }

    public Location getPos1(UUID uuid) { return pos1Map.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2Map.get(uuid); }

    /** Efface les positions d'un joueur (après création de zone). */
    public void clearPositions(UUID uuid) {
        pos1Map.remove(uuid);
        pos2Map.remove(uuid);
    }

    // ─── Utilitaire ──────────────────────────────────────────────────────────

    private String fmtLoc(Location l) {
        return l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
    }
}

