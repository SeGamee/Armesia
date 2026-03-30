package fr.segame.armesia.listeners;

import fr.segame.armesia.Main;
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
        Main.updateTab(joueur);
        Component component = Component.text("[+] " + joueur.getName());
        event.joinMessage(component);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player joueur = event.getPlayer();
        Main.savePlayer(joueur);
        Component component = Component.text("[-] " + joueur.getName());
        event.quitMessage(component);
    }

    @EventHandler
    public void onDeathMessage(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if(player.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
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

        event.setFormat(chatPrefix + "§7" + joueur.getName() + ": §f" + message);
    }
}
