package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import fr.segame.armesiaLevel.api.LevelAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.function.Consumer;

public class TabManager {

    private final Main plugin;
    private static Consumer<Player> scoreboardUpdateCallback = null;

    public TabManager(Main plugin) {
        this.plugin = plugin;
    }

    // ─── Préfixes ─────────────────────────────────────────

    public String getGroupTabPrefix(String group) {
        return plugin.getConfig().getString("groups." + group + ".tab-prefix", "");
    }

    public String getGroupChatPrefix(String group) {
        return plugin.getConfig().getString("groups." + group + ".chat-prefix", "");
    }

    // ─── Update TAB + Nametag ────────────────────────────

    public void updateTab(Player player) {

        String group = Main.groups.get(player.getUniqueId());
        group = Main.groupManager.getValidGroupOrDefault(group);

        String groupPrefix = getGroupTabPrefix(group);
        int level = LevelAPI.getLevel(player.getUniqueId());

        String displayName = player.getName();

        boolean vanished = VanishManager.isVanished(player.getUniqueId());

        // ── TAB LIST ─────────────────────

        String tabName = vanished
                ? "§8§o[V] §7[" + level + "✫] " + groupPrefix + displayName
                : "§7[" + level + "✫] " + groupPrefix + displayName;

        player.setPlayerListName(tabName);

        // ── NAMETAG ─────────────────────

        String nametagPrefix = vanished
                ? "§8§o[V] §7[" + level + "✫] " + groupPrefix
                : "§7[" + level + "✫] " + groupPrefix;

        ChatColor nameColor = vanished
                ? ChatColor.GRAY
                : getLastChatColor(nametagPrefix);

        String teamName = String.format("lvl_%04d", 9999 - level);

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        String entry = player.getName();

        // ❌ nettoyage ancienne team
        for (Team t : board.getTeams()) {
            if (t.getName().startsWith("lvl_") && t.hasEntry(entry)) {
                t.removeEntry(entry);
            }
        }

        // ✅ créer / récupérer team
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        team.setPrefix(nametagPrefix);
        team.setSuffix("");
        team.setColor(nameColor);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        team.addEntry(entry);
    }

    public void updateAllTabs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateTab(player);
        }
    }

    // ─── Callback scoreboard ────────────────────────────

    public static void registerScoreboardUpdateCallback(Consumer<Player> callback) {
        scoreboardUpdateCallback = callback;
    }

    public static void updatePlayerScoreboard(UUID uuid) {
        if (scoreboardUpdateCallback == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        scoreboardUpdateCallback.accept(player);
    }

    // ─── Util couleur ───────────────────────────────────

    private static ChatColor getLastChatColor(String text) {
        for (int i = text.length() - 2; i >= 0; i--) {
            if (text.charAt(i) == '§') {
                ChatColor c = ChatColor.getByChar(text.charAt(i + 1));
                if (c != null && c.isColor()) return c;
            }
        }
        return ChatColor.WHITE;
    }
}