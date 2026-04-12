package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
import fr.segame.armesiaLevel.api.LevelAPI;
import fr.segame.armesia.commands.GodCommand;
import fr.segame.armesia.commands.SpeedCommand;
import fr.segame.armesia.managers.MessageManager;
import fr.segame.armesia.managers.TpaManager;
import fr.segame.armesia.managers.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayersListeners implements Listener {

    /** Timestamp du dernier message envoyé par UUID (thread-safe car AsyncPlayerChatEvent). */
    private static final Map<UUID, Long> chatCooldowns = new ConcurrentHashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        Main.loadPlayer(joueur);
        Main.updateAllTabs();

        // ── God mode OFF + mode Survie ───────────────────────────────────────
        GodCommand.remove(joueur.getUniqueId());
        joueur.setGameMode(org.bukkit.GameMode.SURVIVAL);

        // ── Vitesse réinitialisée ────────────────────────────────────────────
        if (Main.getInstance().getConfig().getBoolean("speed.reset-on-login", true)) {
            SpeedCommand.resetSpeed(joueur);
        }

        // ── Vanish réinitialisé ──────────────────────────────────────────────
        if (Main.getInstance().getConfig().getBoolean("vanish.reset-on-login", false)) {
            VanishManager.forceRemove(joueur.getUniqueId());
        }

        // ── Message de join (silencieux si vanished) ─────────────────────────
        if (VanishManager.isVanished(joueur.getUniqueId())) {
            event.joinMessage(null);
        } else {
            event.joinMessage(Component.text("§7[§a+§7] ").append(joueur.displayName()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        Main.savePlayer(joueur);

        TpaManager.cleanup(joueur.getUniqueId());
        MessageManager.cleanupPlayer(joueur.getUniqueId());
        chatCooldowns.remove(joueur.getUniqueId());

        // ── Vanish silent-quit ───────────────────────────────────────────────
        if (Main.getInstance().getConfig().getBoolean("vanish.silent-quit", true)
                && VanishManager.isVanished(joueur.getUniqueId())) {
            event.quitMessage(null);
            return;
        }

        event.quitMessage(Component.text("§7[§c-§7] ").append(joueur.displayName()));
    }

    @EventHandler
    public void onDeathMessage(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getLastDamageCause() != null
                && player.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.deathMessage(Component.text()
                    .append(player.displayName().color(NamedTextColor.GRAY))
                    .append(Component.text(" vient de mourir de chute.", NamedTextColor.YELLOW))
                    .build());
        } else {
            event.deathMessage(Component.text()
                    .append(player.displayName().color(NamedTextColor.GRAY))
                    .append(Component.text(" vient de mourir.", NamedTextColor.YELLOW))
                    .build());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player joueur = event.getPlayer();

        // ── Cooldown chat ────────────────────────────────────────────────────
        if (!Main.hasGroupPermission(joueur, Main.getInstance().getConfig()
                .getString("chat.cooldown-bypass", "armesia.chat.nocooldown"))) {
            long cooldownMs = (long) (Main.getInstance().getConfig().getDouble("chat.cooldown", 3.0) * 1000L);
            if (cooldownMs > 0) {
                Long last = chatCooldowns.get(joueur.getUniqueId());
                long now  = System.currentTimeMillis();
                if (last != null && now - last < cooldownMs) {
                    long remaining = (cooldownMs - (now - last) + 999) / 1000;
                    String msg = Main.getInstance().getConfig().getString(
                            "chat.message", "§cVous devez attendre §e{remaining}s §cavant de reparler.");
                    joueur.sendMessage(msg.replace("{remaining}", String.valueOf(remaining)));
                    event.setCancelled(true);
                    return;
                }
                chatCooldowns.put(joueur.getUniqueId(), now);
            }
        }

        String message = event.getMessage();
        String group   = Main.groups.get(joueur.getUniqueId());

        if (Main.hasGroupPermission(joueur, "chat.color")) {
            message = message.replace("&", "§");
        }
        if (group == null) group = "Citoyen";

        String chatPrefix  = Main.getGroupChatPrefix(group);
        int    level       = LevelAPI.getLevel(joueur.getUniqueId());
        String levelPrefix = "§7[" + level + "✫] ";
        String displayName = joueur.getName();

        event.setFormat(levelPrefix + chatPrefix + "§7" + displayName + ": §f" + message);
    }

    // ── Liste serveur : masquer l'infobulle joueurs + exclure les vanished ───
    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        // Vide le sample de joueurs → supprime l'infobulle au survol
        // (sans setHidePlayers qui afficherait "???")
        event.getPlayerSample().clear();

        // Décompter les vanished du compteur affiché (affiche X/max sans eux)
        long vanishedCount = VanishManager.getVanishedSet().stream()
                .filter(uuid -> org.bukkit.Bukkit.getPlayer(uuid) != null)
                .count();
        event.setNumPlayers(Math.max(0, event.getNumPlayers() - (int) vanishedCount));
    }

    @EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                fr.segame.armesia.Main.getInstance(), fr.segame.armesia.Main::updateAllTabs, 2L);
    }

    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        fr.segame.armesia.Main.updateAllTabs();
    }
}
