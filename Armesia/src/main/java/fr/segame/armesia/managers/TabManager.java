package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import fr.segame.armesiaLevel.api.LevelAPI;
import net.kyori.adventure.text.format.NamedTextColor;
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

        String groupPrefix  = getGroupTabPrefix(group);
        int    level        = LevelAPI.getLevel(player.getUniqueId());
        String displayName  = player.getName();
        boolean vanished    = VanishManager.isVanished(player.getUniqueId());

        // ── TAB LIST ─────────────────────

        String tabName = vanished
                ? "§8§o[V] §7[" + level + "✫] " + groupPrefix + displayName
                : "§7[" + level + "✫] " + groupPrefix + displayName;

        player.setPlayerListName(tabName);

        // ── NAMETAG (team par joueur pour contrôle indépendant) ──────────────

        String nametagPrefix = vanished
                ? "§8§o[V] §7[" + level + "✫] " + groupPrefix
                : "§7[" + level + "✫] " + groupPrefix;

        ChatColor nameColor = vanished
                ? ChatColor.GRAY
                : getLastChatColor(nametagPrefix);

        // Teams par joueur : "p{XXXX}_{realName}"
        //  p{XXXX} = 9999-level → tri décroissant par niveau dans le TAB
        String entry    = player.getName();
        String teamName = String.format("p%04d_%s", 9999 - level, entry);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();

            // Retire l'entry de toute team ; libère les teams vides
            for (Team t : new java.util.ArrayList<>(board.getTeams())) {
                if (t.hasEntry(entry)) {
                    t.removeEntry(entry);
                    if (t.getEntries().isEmpty()) t.unregister();
                }
            }

            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            team.setPrefix(nametagPrefix);
            team.setSuffix("");
            team.color(chatColorToNamed(nameColor));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.addEntry(entry);
        }
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

    /** Convertit un ChatColor legacy en NamedTextColor Adventure (Paper 1.20+). */
    private static NamedTextColor chatColorToNamed(ChatColor c) {
        return switch (c) {
            case BLACK        -> NamedTextColor.BLACK;
            case DARK_BLUE    -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN   -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA    -> NamedTextColor.DARK_AQUA;
            case DARK_RED     -> NamedTextColor.DARK_RED;
            case DARK_PURPLE  -> NamedTextColor.DARK_PURPLE;
            case GOLD         -> NamedTextColor.GOLD;
            case GRAY         -> NamedTextColor.GRAY;
            case DARK_GRAY    -> NamedTextColor.DARK_GRAY;
            case BLUE         -> NamedTextColor.BLUE;
            case GREEN        -> NamedTextColor.GREEN;
            case AQUA         -> NamedTextColor.AQUA;
            case RED          -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW       -> NamedTextColor.YELLOW;
            default           -> NamedTextColor.WHITE;
        };
    }
}