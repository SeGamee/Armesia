package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
import fr.segame.armesia.player.GamePlayer;
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

public class PlayersListeners implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joueur = event.getPlayer();
        Main.loadPlayer(joueur);
        Main.updateAllTabs();
        Component component = Component.text("§7[§a+§7] " + joueur.getName());
        event.joinMessage(component);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        Main.savePlayer(joueur);
        Component component = Component.text("§7[§c+§7] " + joueur.getName());
        event.quitMessage(component);
    }

    @EventHandler
    public void onDeathMessage(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if(player.getLastDamageCause() != null && player.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
            Component component = Component.text()
                    .append(player.displayName().color(NamedTextColor.GRAY))
                    .append(Component.text(" vient de mourir de chute.", NamedTextColor.YELLOW))
                    .build();
            event.deathMessage(component);
        } else {
            Component component = Component.text()
                    .append(player.displayName().color(NamedTextColor.GRAY))
                    .append(Component.text(" vient de mourir.", NamedTextColor.YELLOW))
                    .build();
            event.deathMessage(component);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player joueur = event.getPlayer();
        String message = event.getMessage();
        String group = Main.groups.get(joueur.getUniqueId());
        String job = Main.jobs.get(joueur.getUniqueId());
        if (Main.hasGroupPermission(joueur, "chat.color")) {
            message = message.replace("&", "§");
        }

        if (group == null) group = "Joueur";
        if (job == null) job = "Citoyen";

        boolean showChatPrefix = Main.chatPrefixEnabled.getOrDefault(joueur.getUniqueId(), true);

        String chatPrefix = "";
        if (showChatPrefix) {
            chatPrefix = Main.getGroupChatPrefix(group);
        }

        // Récupération du niveau du joueur
        GamePlayer gp = Main.getInstance().getPlayerManager().getPlayer(joueur.getUniqueId());
        int level = gp != null ? gp.getLevel() : 1;
        String levelPrefix = "§7[" + level + "✫] ";

        event.setFormat(levelPrefix + chatPrefix + "§7" + joueur.getName() + ": §f" + message);
    }

    @org.bukkit.event.EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        // Mise à jour du tab après respawn
        org.bukkit.Bukkit.getScheduler().runTaskLater(fr.segame.armesia.Main.getInstance(), fr.segame.armesia.Main::updateAllTabs, 2L);
    }

    @org.bukkit.event.EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        // Mise à jour du tab après changement de monde
        fr.segame.armesia.Main.updateAllTabs();
    }
}
