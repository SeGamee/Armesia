package fr.segame.armesiaMobs.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gère le mode debug pour le système de mobs/zones.
 *
 * NORMAL  → [SPAWN] + [DESPAWN]
 * VERBOSE → tout : NORMAL + [SKIP] + [CLEANUP] + [BLOCKED]
 */
public class DebugManager {

    public enum Level { NONE, NORMAL, VERBOSE }

    private final Map<UUID, Level> debuggers = new HashMap<>();

    // ─── API ──────────────────────────────────────────────────────────────────

    public Level getLevel(UUID uuid) {
        return debuggers.getOrDefault(uuid, Level.NONE);
    }

    public void setLevel(UUID uuid, Level level) {
        if (level == Level.NONE) debuggers.remove(uuid);
        else                     debuggers.put(uuid, level);
    }

    /** Alterne : NONE → NORMAL → VERBOSE → NONE. Retourne le nouvel état. */
    public Level cycle(UUID uuid) {
        Level next = switch (getLevel(uuid)) {
            case NONE    -> Level.NORMAL;
            case NORMAL  -> Level.VERBOSE;
            case VERBOSE -> Level.NONE;
        };
        setLevel(uuid, next);
        return next;
    }

    public boolean hasAny() { return !debuggers.isEmpty(); }

    /** Exécute une action pour chaque joueur connecté ayant le debug actif. */
    public void forEachDebugPlayer(Consumer<Player> action) {
        if (debuggers.isEmpty()) return;
        for (UUID uuid : new ArrayList<>(debuggers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) action.accept(p);
        }
    }

    // ─── Envoi ────────────────────────────────────────────────────────────────

    /** Événement important : [SPAWN], [DESPAWN]. */
    public void log(String message) {
        broadcast(Level.NORMAL, message);
    }

    /** Événement verbeux : [SKIP], [CLEANUP], [BLOCKED]. */
    public void logVerbose(String message) {
        broadcast(Level.VERBOSE, message);
    }

    private void broadcast(Level minLevel, String message) {
        if (debuggers.isEmpty()) return;
        String fmt = "§8[§eDBG§8] §7" + message;
        for (Map.Entry<UUID, Level> e : debuggers.entrySet()) {
            if (e.getValue().ordinal() >= minLevel.ordinal()) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p != null) p.sendMessage(fmt);
            }
        }
    }
}



