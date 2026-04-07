package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les interactions du /invsee.
 *
 * Règles :
 *  - Sans armesia.invsee.take  → lecture seule, tous les clics annulés.
 *  - Avec armesia.invsee.take  → clics autorisés + sync GUI → cible,
 *    UNIQUEMENT si le viewer a effectivement modifié le GUI (flag dirty).
 *    Cela évite d'écraser les changements que la cible a faits entre-temps.
 */
public class InvSeeListener implements Listener {

    // ── Session ──────────────────────────────────────────────────────────────

    private static class Session {
        final Player target;
        final Inventory gui;
        final boolean canTake;
        /** true dès que le viewer a réellement modifié un slot du GUI. */
        boolean dirty = false;

        Session(Player target, Inventory gui, boolean canTake) {
            this.target = target;
            this.gui    = gui;
            this.canTake = canTake;
        }
    }

    private static final Map<UUID, Session> sessions = new HashMap<>();

    // ── API publique ─────────────────────────────────────────────────────────

    public static void open(Player viewer, Player target, Inventory gui, boolean canTake) {
        sessions.put(viewer.getUniqueId(), new Session(target, gui, canTake));
    }

    // ── Événements ───────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        Session session = sessions.get(viewer.getUniqueId());
        if (session == null) return;
        if (!event.getInventory().equals(session.gui)) return;

        if (!session.canTake) {
            event.setCancelled(true);
            return;
        }

        int raw    = event.getRawSlot();
        int guiSize = session.gui.getSize();
        InventoryAction action = event.getAction();

        boolean affectsGui =
                // Clic direct dans le GUI (panel haut)
                raw < guiSize
                // Ctrl+Click depuis le panel bas → l'item monte dans le GUI
                || (raw >= guiSize && action == InventoryAction.MOVE_TO_OTHER_INVENTORY);

        if (affectsGui) {
            session.dirty = true;
            org.bukkit.Bukkit.getScheduler().runTask(Main.getInstance(),
                    () -> syncToTarget(session));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        Session session = sessions.get(viewer.getUniqueId());
        if (session == null) return;
        if (!event.getInventory().equals(session.gui)) return;

        if (!session.canTake) {
            event.setCancelled(true);
            return;
        }

        // Si au moins un slot du drag est dans le GUI → sync
        boolean affectsGui = event.getRawSlots().stream().anyMatch(s -> s < session.gui.getSize());
        if (affectsGui) {
            session.dirty = true;
            org.bukkit.Bukkit.getScheduler().runTask(Main.getInstance(),
                    () -> syncToTarget(session));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;

        Session session = sessions.remove(viewer.getUniqueId());
        if (session == null) return;

        // On ne sync QUE si le viewer a réellement touché quelque chose.
        // Évite d'écraser les changements faits par la cible entre-temps.
        if (session.dirty) {
            syncToTarget(session);
        }
    }

    @EventHandler
    public void onViewerQuit(PlayerQuitEvent event) {
        Session session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null && session.dirty) {
            syncToTarget(session);
        }
    }

    @EventHandler
    public void onTargetQuit(PlayerQuitEvent event) {
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().target.equals(event.getPlayer())) {
                Player viewer = org.bukkit.Bukkit.getPlayer(entry.getKey());
                if (viewer != null) {
                    viewer.closeInventory();
                    viewer.sendMessage("§cLe joueur que vous observiez s'est déconnecté.");
                }
                return true;
            }
            return false;
        });
    }

    // ── Sync TARGET → GUI (cible modifie son propre inventaire) ─────────────

    /**
     * Quand la cible clique dans son propre inventaire, tous les viewers
     * qui ont son GUI ouvert reçoivent une mise à jour au tick suivant.
     */
    @EventHandler
    public void onTargetInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player target)) return;

        // Cherche les sessions dont ce joueur est la cible
        for (Session session : sessions.values()) {
            if (!session.target.equals(target)) continue;
            // Rafraîchit le GUI au tick suivant (après que Bukkit ait appliqué le clic)
            org.bukkit.Bukkit.getScheduler().runTask(Main.getInstance(),
                    () -> refreshGuiFromTarget(session));
        }
    }

    @EventHandler
    public void onTargetInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player target)) return;
        for (Session session : sessions.values()) {
            if (!session.target.equals(target)) continue;
            org.bukkit.Bukkit.getScheduler().runTask(Main.getInstance(),
                    () -> refreshGuiFromTarget(session));
        }
    }

    /** Copie l'inventaire actuel de la cible dans le GUI du viewer. */
    private void refreshGuiFromTarget(Session session) {
        Player target = session.target;
        if (!target.isOnline()) return;

        Inventory gui = session.gui;

        for (int i = 0; i < 36; i++) {
            gui.setItem(i, target.getInventory().getItem(i));
        }
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < 4; i++) gui.setItem(36 + i, armor[i]);
        gui.setItem(44, target.getInventory().getItemInOffHand());
    }

    private void syncToTarget(Session session) {
        Player target = session.target;
        if (!target.isOnline()) return;

        Inventory gui = session.gui;

        // Slots 0-35 : inventaire principal
        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, gui.getItem(i));
        }

        // Slots 36-39 : armure (boots, leggings, chestplate, helmet)
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = gui.getItem(36 + i);
        }
        target.getInventory().setArmorContents(armor);

        // Slot 44 : offhand
        ItemStack offhand = gui.getItem(44);
        target.getInventory().setItemInOffHand(offhand != null ? offhand : new ItemStack(org.bukkit.Material.AIR));
    }
}

