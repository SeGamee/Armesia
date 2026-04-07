package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private static final Set<UUID> vanished = new HashSet<>();

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public static Set<UUID> getVanishedSet() {
        return Collections.unmodifiableSet(vanished);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    public static void vanish(Player player) {
        vanished.add(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!Main.hasGroupPermission(other, "armesia.vanish")) {
                other.hidePlayer(Main.getInstance(), player);
            }
        }
        Main.updateTab(player);
    }

    public static void unvanish(Player player) {
        vanished.remove(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(Main.getInstance(), player);
        }
        Main.updateTab(player);
    }

    public static void toggle(Player player) {
        if (isVanished(player.getUniqueId())) unvanish(player);
        else vanish(player);
    }

    /** Retire le vanish sans re-montrer au monde (ex : déconnexion ou reset-on-login). */
    public static void forceRemove(UUID uuid) {
        vanished.remove(uuid);
    }

    /**
     * Réévalue la visibilité de TOUS les joueurs vanished pour TOUS les joueurs en ligne.
     * Appelé après un changement de statut OP ou de groupe.
     */
    public static void reapplyAll() {
        for (UUID uid : vanished) {
            Player vp = Bukkit.getPlayer(uid);
            if (vp == null) continue;
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(vp)) continue;
                if (Main.hasGroupPermission(other, "armesia.vanish")) {
                    other.showPlayer(Main.getInstance(), vp);
                } else {
                    other.hidePlayer(Main.getInstance(), vp);
                }
            }
        }
    }

    /**
     * Appelé quand un joueur rejoint : masque les joueurs vanished
     * qui ne doivent pas être vus par ce joueur.
     */
    public static void applyForNewPlayer(Player newPlayer) {
        for (UUID uid : vanished) {
            Player vp = Bukkit.getPlayer(uid);
            if (vp == null || vp.equals(newPlayer)) continue;
            if (!Main.hasGroupPermission(newPlayer, "armesia.vanish")) {
                newPlayer.hidePlayer(Main.getInstance(), vp);
            }
        }
    }

    /**
     * Appelé quand un joueur qui était vanished se reconnecte :
     * le cache pour tous ceux qui n'ont pas la permission.
     */
    public static void reapplyVanishFor(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!Main.hasGroupPermission(other, "armesia.vanish")) {
                other.hidePlayer(Main.getInstance(), player);
            }
        }
    }
}
