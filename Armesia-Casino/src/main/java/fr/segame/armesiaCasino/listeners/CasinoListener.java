package fr.segame.armesiaCasino.listeners;

import fr.segame.armesiaCasino.MainCasino;
import fr.segame.armesiaCasino.gui.CasinoGUI;
import fr.segame.armesiaCasino.gui.PrizesGUI;
import fr.segame.armesiaCasino.gui.RouletteAnimation;
import fr.segame.armesiaCasino.managers.CasinoManager;
import fr.segame.armesiaCasino.managers.PrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CasinoListener implements Listener {

    private final MainCasino    plugin;
    private final CasinoManager casinoManager;
    private final PrizeManager  prizeManager;

    /** Animations en cours par UUID du joueur */
    private final Map<UUID, RouletteAnimation> activeAnimations    = new HashMap<>();
    /** Inventaires casino (roulette) ouverts par UUID du joueur */
    private final Map<UUID, Inventory>         openInventories     = new HashMap<>();
    /** Inventaires de la liste des lots ouverts par UUID du joueur */
    private final Map<UUID, Inventory>         openPrizesInventories = new HashMap<>();
    /**
     * UUIDs autorisés à fermer leur inventaire casino via ESC.
     * Peuplé par onComplete (animation + récompense terminées) ou lors du nettoyage.
     */
    private final Set<UUID> canClose = new HashSet<>();

    public CasinoListener(MainCasino plugin, CasinoManager casinoManager, PrizeManager prizeManager) {
        this.plugin        = plugin;
        this.casinoManager = casinoManager;
        this.prizeManager  = prizeManager;
    }

    // ── Clic sur un bloc de casino ────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        if (!casinoManager.isCasinoBlock(event.getClickedBlock().getLocation())) return;

        Player player = event.getPlayer();

        // Annuler TOUTES les interactions avec un bloc de casino
        event.setCancelled(true);

        // Clic gauche → afficher les lots disponibles
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Inventory prizesInv = PrizesGUI.create(plugin, prizeManager);
            player.openInventory(prizesInv);
            openPrizesInventories.put(player.getUniqueId(), prizesInv);
            return;
        }

        // Seul le clic droit est traité pour jouer
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack inHand = event.getItem();

        if (!plugin.isCasinoToken(inHand)) {
            player.sendMessage(msg("need-token",
                    "&6✦ &eIl vous faut &6{token} &epour jouer !")
                    .replace("{token}", plugin.getTokenDisplayName()));
            // Poussée vers l'arrière
            org.bukkit.Location blockCenter =
                    event.getClickedBlock().getLocation().add(0.5, 0, 0.5);
            double dx = player.getLocation().getX() - blockCenter.getX();
            double dz = player.getLocation().getZ() - blockCenter.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) { dx /= len; dz /= len; }
            player.setVelocity(new Vector(dx * 0.35, 0.2, dz * 0.35));
            return;
        }

        if (activeAnimations.containsKey(player.getUniqueId())) {
            player.sendMessage(msg("animation-in-progress",
                    "&cUne animation de casino est déjà en cours !"));
            return;
        }

        // Consommer un jeton
        if (inHand.getAmount() > 1) {
            inHand.setAmount(inHand.getAmount() - 1);
        } else {
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        }

        // Ouvrir le GUI et lancer l'animation
        Inventory inv = CasinoGUI.create(plugin);
        player.openInventory(inv);

        UUID uuid = player.getUniqueId();
        openInventories.put(uuid, inv);

        RouletteAnimation anim = new RouletteAnimation(plugin, player, inv, prizeManager);

        anim.setOnComplete(() -> {
            // Animation + récompense terminées → fermeture automatique
            activeAnimations.remove(uuid);
            canClose.add(uuid);
            if (player.isOnline()) {
                player.closeInventory();
            }
        });

        activeAnimations.put(uuid, anim);
        anim.start();
    }

    // ── Clics dans le GUI casino ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // Menu des lots : annuler tous les clics
        Inventory prizesInv = openPrizesInventories.get(uuid);
        if (prizesInv != null && event.getView().getTopInventory() == prizesInv) {
            event.setCancelled(true);
            return;
        }

        // GUI casino (roulette) : annuler tous les clics
        Inventory casinoInv = openInventories.get(uuid);
        if (casinoInv == null) return;
        if (event.getView().getTopInventory() != casinoInv) return;
        event.setCancelled(true);
    }

    // ── Drag dans le GUI casino ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory prizesInv = openPrizesInventories.get(uuid);
        if (prizesInv != null && event.getView().getTopInventory() == prizesInv) {
            event.setCancelled(true);
            return;
        }

        Inventory casinoInv = openInventories.get(uuid);
        if (casinoInv != null && event.getView().getTopInventory() == casinoInv) {
            event.setCancelled(true);
        }
    }

    // ── Fermeture de l'inventaire ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // Fermeture du menu des lots : simple nettoyage, toujours autorisé
        Inventory prizesInv = openPrizesInventories.get(uuid);
        if (prizesInv != null && event.getInventory() == prizesInv) {
            openPrizesInventories.remove(uuid);
            return;
        }

        // Fermeture du GUI casino (roulette)
        Inventory casinoInv = openInventories.get(uuid);
        if (casinoInv == null || event.getInventory() != casinoInv) return;

        if (canClose.remove(uuid)) {
            // Fermeture autorisée (animation terminée, ou déconnexion/nettoyage)
            openInventories.remove(uuid);
            return;
        }

        if (!player.isOnline()) {
            openInventories.remove(uuid);
            return;
        }

        // Animation encore en cours : réouvrir l'inventaire au tick suivant
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!openInventories.containsKey(uuid)) return; // Déjà nettoyé
            if (!player.isOnline()) { openInventories.remove(uuid); return; }
            player.openInventory(casinoInv);
        }, 1L);
    }

    // ── Déconnexion du joueur ─────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        RouletteAnimation anim = activeAnimations.remove(uuid);
        if (anim != null && !anim.isFinished()) {
            anim.cancel(); // Jeton perdu
        }

        // Permettre la fermeture propre de l'inventaire sans tentative de réouverture
        canClose.add(uuid);
        openInventories.remove(uuid);
        openPrizesInventories.remove(uuid);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String msg(String key, String def) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages." + key, def));
    }
}








