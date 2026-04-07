package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Gère les demandes de téléportation TPA / TPAHere en mémoire. */
public class TpaManager {

    public enum Type { TPA, TPAHERE }

    public static class Request {
        public final UUID     sender;
        public final UUID     receiver;
        public final Type     type;
        public BukkitTask     timeoutTask;

        public Request(UUID sender, UUID receiver, Type type) {
            this.sender   = sender;
            this.receiver = receiver;
            this.type     = type;
        }
    }

    /** Keyed by receiver UUID — une seule demande active par receveur */
    private static final Map<UUID, Request> pendingByReceiver = new HashMap<>();

    /** Timestamp (ms) du dernier envoi de demande acceptée par sender */
    private static final Map<UUID, Long> lastSendTime = new HashMap<>();

    // ── Cooldown ──────────────────────────────────────────────────────────────

    /** @return secondes restantes avant de pouvoir renvoyer (0 si cooldown expiré) */
    public static long getSendCooldownRemaining(UUID senderUuid, int cooldownSecs) {
        long last = lastSendTime.getOrDefault(senderUuid, 0L);
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        long remaining = cooldownSecs - elapsed;
        return remaining > 0 ? remaining : 0;
    }

    /** Enregistre le timestamp d'un envoi (appelé quand la demande est acceptée). */
    public static void recordSend(UUID senderUuid) {
        lastSendTime.put(senderUuid, System.currentTimeMillis());
    }

    // ── Envoi ─────────────────────────────────────────────────────────────────

    /** @return false si une demande est déjà en cours depuis sender vers receiver */
    public static boolean sendRequest(Player sender, Player receiver, Type type) {
        UUID senderUuid   = sender.getUniqueId();
        UUID receiverUuid = receiver.getUniqueId();

        Request existing = pendingByReceiver.get(receiverUuid);
        if (existing != null && existing.sender.equals(senderUuid)) return false;

        removeRequest(receiverUuid);

        int timeout = Main.getInstance().getConfig().getInt("tpa.timeout", 60);

        Request req = new Request(senderUuid, receiverUuid, type);
        pendingByReceiver.put(receiverUuid, req);

        req.timeoutTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Request r = pendingByReceiver.remove(receiverUuid);
            if (r == null || !r.sender.equals(senderUuid)) return;

            Player s  = Bukkit.getPlayer(senderUuid);
            Player rv = Bukkit.getPlayer(receiverUuid);
            String expMsg   = Main.getInstance().getConfig().getString("tpa.messages.expired",          "§cDemande expirée.");
            String expInMsg = Main.getInstance().getConfig().getString("tpa.messages.expired-incoming", "§cDemande expirée.");
            if (s  != null) s.sendMessage(expMsg.replace("{player}", receiver.getName()));
            if (rv != null) rv.sendMessage(expInMsg.replace("{player}", sender.getName()));
        }, timeout * 20L);

        return true;
    }

    // ── Récupération ──────────────────────────────────────────────────────────

    public static Request getRequest(UUID receiverUuid) {
        return pendingByReceiver.get(receiverUuid);
    }

    public static void removeRequest(UUID receiverUuid) {
        Request r = pendingByReceiver.remove(receiverUuid);
        if (r != null && r.timeoutTask != null) r.timeoutTask.cancel();
    }

    /**
     * Supprime la demande envoyée PAR senderUuid (pour /tpacancel).
     * @return la demande supprimée, ou null si aucune
     */
    public static Request removeRequestBySender(UUID senderUuid) {
        for (Map.Entry<UUID, Request> entry : pendingByReceiver.entrySet()) {
            if (entry.getValue().sender.equals(senderUuid)) {
                UUID receiverUuid = entry.getKey();
                Request r = pendingByReceiver.remove(receiverUuid);
                if (r != null && r.timeoutTask != null) r.timeoutTask.cancel();
                return r;
            }
        }
        return null;
    }

    /** Supprime toutes les demandes impliquant ce joueur (déconnexion). */
    public static void cleanup(UUID uuid) {
        pendingByReceiver.entrySet().removeIf(e -> {
            Request r = e.getValue();
            if (r.sender.equals(uuid) || r.receiver.equals(uuid)) {
                if (r.timeoutTask != null) r.timeoutTask.cancel();
                return true;
            }
            return false;
        });
        lastSendTime.remove(uuid);
    }
}
