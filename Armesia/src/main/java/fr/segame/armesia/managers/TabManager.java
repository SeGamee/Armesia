package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import fr.segame.armesia.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Gère l'affichage du TAB list, les nametags et la synchronisation
 * avec les scoreboards des joueurs (via un callback optionnel).
 */
public class TabManager {

    private final Main plugin;
    private static Consumer<Player> scoreboardUpdateCallback = null;

    public TabManager(Main plugin) {
        this.plugin = plugin;
    }

    // ─── Préfixes de groupe ──────────────────────────────────────────────────────

    public String getGroupChatPrefix(String group) {
        return plugin.getConfig().getString("groups." + group + ".chat-prefix", "");
    }

    public String getGroupTabPrefix(String group) {
        return plugin.getConfig().getString("groups." + group + ".tab-prefix", "");
    }

    // ─── Mise à jour du TAB ──────────────────────────────────────────────────────

    public void updateTab(Player player) {
        String group = Main.groups.get(player.getUniqueId());
        group = Main.groupManager.getValidGroupOrDefault(group);

        boolean showTabPrefix = Main.tabPrefixEnabled.getOrDefault(player.getUniqueId(), true);
        String prefix = "§7";
        if (showTabPrefix) {
            prefix = getGroupTabPrefix(group);
        }

        GamePlayer gp = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        int level = gp != null ? gp.getLevel() : 1;

        player.setPlayerListName("§7[" + level + "✫] " + prefix + player.getName());

        // ── Tri par niveau via teams de scoreboard ───────────────────────────────
        String teamName      = String.format("lvl_%04d", 9999 - level);
        String nametagPrefix = "§7[" + level + "✫] " + prefix;
        ChatColor nameColor  = getLastChatColor(nametagPrefix);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();

            for (Team t : board.getTeams()) {
                if (t.getName().startsWith("lvl_") && t.hasEntry(player.getName())) {
                    t.removeEntry(player.getName());
                }
            }

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            team.setPrefix(nametagPrefix);
            team.setColor(nameColor);
            team.addEntry(player.getName());
        }
    }

    public void updateAllTabs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTab(player);
        }
    }

    // ─── Callback scoreboard (enregistré par Armesia-Scoreboard) ────────────────

    public static void registerScoreboardUpdateCallback(Consumer<Player> callback) {
        scoreboardUpdateCallback = callback;
    }

    public static void updatePlayerScoreboard(UUID uuid) {
        if (scoreboardUpdateCallback == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        scoreboardUpdateCallback.accept(player);
    }

    // ─── Utilitaire couleur ──────────────────────────────────────────────────────

    private static ChatColor getLastChatColor(String text) {
        for (int i = text.length() - 2; i >= 0; i--) {
            if (text.charAt(i) == '§') {
                ChatColor c = ChatColor.getByChar(text.charAt(i + 1));
                if (c != null && c.isColor()) {
                    return c;
                }
            }
        }
        return ChatColor.WHITE;
    }
}

