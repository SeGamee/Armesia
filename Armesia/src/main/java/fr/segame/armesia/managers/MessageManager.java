package fr.segame.armesia.managers;

import java.util.*;

/** Gère les derniers interlocuteurs (pour /r) et le spy mode. */
public class MessageManager {

    /** uuid joueur → uuid dernier destinataire */
    private static final Map<UUID, UUID> lastRecipient = new HashMap<>();

    /** Joueurs en mode social spy */
    private static final Set<UUID> spyPlayers = new HashSet<>();

    // ── Réponse ───────────────────────────────────────────────────────────────

    public static void setLastRecipient(UUID sender, UUID recipient) {
        lastRecipient.put(sender,    recipient);
        lastRecipient.put(recipient, sender);    // les deux peuvent se répondre
    }

    public static UUID getLastRecipient(UUID player) {
        return lastRecipient.get(player);
    }

    public static void cleanupPlayer(UUID uuid) {
        lastRecipient.remove(uuid);
        spyPlayers.remove(uuid);
    }

    // ── Social Spy ────────────────────────────────────────────────────────────

    public static boolean isSpy(UUID uuid) {
        return spyPlayers.contains(uuid);
    }

    public static boolean toggleSpy(UUID uuid) {
        if (spyPlayers.contains(uuid)) { spyPlayers.remove(uuid); return false; }
        else { spyPlayers.add(uuid); return true; }
    }

    public static Set<UUID> getSpyPlayers() {
        return Collections.unmodifiableSet(spyPlayers);
    }
}

