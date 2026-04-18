package fr.segame.armesiaMenu.placeholder;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.api.StatsAPI;
import fr.segame.armesia.api.GroupAPI;
import fr.segame.armesia.utils.APIProvider;
import fr.segame.armesiaMobs.api.MobStatsAPI;
import fr.segame.armesiaLevel.api.LevelAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderManager {

    // Patterns pour les placeholders dynamiques
    private static final Pattern TOP_MONEY_NAME    = Pattern.compile("%top_money_name_(\\d+)%");
    private static final Pattern TOP_MONEY_VALUE   = Pattern.compile("%top_money_(\\d+)%");
    private static final Pattern TOP_TOKENS_NAME   = Pattern.compile("%top_tokens_name_(\\d+)%");
    private static final Pattern TOP_TOKENS_VALUE  = Pattern.compile("%top_tokens_(\\d+)%");
    private static final Pattern TOP_KILLS_NAME    = Pattern.compile("%top_kills_name_(\\d+)%");
    private static final Pattern TOP_KILLS_VALUE   = Pattern.compile("%top_kills_(\\d+)%");
    private static final Pattern TOP_DEATHS_NAME   = Pattern.compile("%top_deaths_name_(\\d+)%");
    private static final Pattern TOP_DEATHS_VALUE  = Pattern.compile("%top_deaths_(\\d+)%");
    private static final Pattern TOP_KS_NAME       = Pattern.compile("%top_killstreak_name_(\\d+)%");
    private static final Pattern TOP_KS_VALUE      = Pattern.compile("%top_killstreak_(\\d+)%");
    private static final Pattern TOP_BKS_NAME      = Pattern.compile("%top_bestkillstreak_name_(\\d+)%");
    private static final Pattern TOP_BKS_VALUE     = Pattern.compile("%top_bestkillstreak_(\\d+)%");
    private static final Pattern MOB_KILLS         = Pattern.compile("%mob_kills_([^%]+)%");
    private static final Pattern TOP_MOB_KILLS_NAME  = Pattern.compile("%top_mob_kills_name_(\\d+)%");
    private static final Pattern TOP_MOB_KILLS_VALUE = Pattern.compile("%top_mob_kills_(\\d+)%");
    private static final Pattern TOP_MOB_ID_KILLS_NAME  = Pattern.compile("%top_mob_kills_([^_%]+)_name_(\\d+)%");
    private static final Pattern TOP_MOB_ID_KILLS_VALUE = Pattern.compile("%top_mob_kills_([^_%]+)_(\\d+)%");

    public static String parse(Player player, String text) {
        if (text == null) return null;

        // ─── Groupe et préfixes ───────────────────────────────────────────────
        GroupAPI groups = APIProvider.getGroups();
        if (groups != null) {
            String group = groups.getPlayerGroup(player.getUniqueId());
            text = text.replace("%group%", group != null ? group : "—");
            String chatPrefix = groups.getChatPrefix(group);
            text = text.replace("%group_chat_prefix%", chatPrefix != null ? chatPrefix : "");
            String tabPrefix = groups.getTabPrefix(group);
            text = text.replace("%group_tab_prefix%", tabPrefix != null ? tabPrefix : "");
        } else {
            text = text.replace("%group%", "—");
            text = text.replace("%group_chat_prefix%", "");
            text = text.replace("%group_tab_prefix%", "");
        }
        // ─── Placeholders simples ────────────────────────────────────────────────
        text = text.replace("%player%", player.getName());
        text = text.replace("%money%", getMoney(player));
        text = text.replace("%tokens%", String.valueOf(getTokens(player)));
        text = text.replace("%level%", String.valueOf(getLevel(player)));
        text = text.replace("%xp%", String.valueOf(getXP(player)));
        text = text.replace("%xp_bar%", getXPBar(player));

        // ─── Stats PvP joueur ────────────────────────────────────────────────────
        StatsAPI stats = APIProvider.getStats();
        if (stats != null) {
            text = text.replace("%kills%", String.valueOf(stats.getKills(player.getUniqueId())));
            text = text.replace("%deaths%", String.valueOf(stats.getDeaths(player.getUniqueId())));
            text = text.replace("%killstreak%", String.valueOf(stats.getKillstreak(player.getUniqueId())));
            text = text.replace("%bestkillstreak%", String.valueOf(stats.getBestKillstreak(player.getUniqueId())));
            text = text.replace("%ratio%", stats.getRatio(player.getUniqueId()));
        }

        // ─── Stats mobs joueur ───────────────────────────────────────────────────
        MobStatsAPI mobStats = getMobStats();
        if (mobStats != null) {
            text = text.replace("%mob_kills_total%", String.valueOf(mobStats.getTotalKills(player.getUniqueId())));

            Matcher m = MOB_KILLS.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String mobId = m.group(1);
                if (!mobId.equals("total")) {
                    m.appendReplacement(sb, String.valueOf(mobStats.getKills(player.getUniqueId(), mobId)));
                } else {
                    m.appendReplacement(sb, String.valueOf(mobStats.getTotalKills(player.getUniqueId())));
                }
            }
            m.appendTail(sb);
            text = sb.toString();
        }

        // ─── Classements économie ────────────────────────────────────────────────
        EconomyAPI eco = APIProvider.getEconomy();
        if (eco != null) {
            text = resolveTopDouble(text, TOP_MONEY_NAME, TOP_MONEY_VALUE, eco.getTopMoney(10));
            text = resolveTopInt(text, TOP_TOKENS_NAME, TOP_TOKENS_VALUE, eco.getTopTokens(10));
        }

        // ─── Classements stats PvP ───────────────────────────────────────────────
        if (stats != null) {
            text = resolveTopInt(text, TOP_KILLS_NAME, TOP_KILLS_VALUE, stats.getTopKills(10));
            text = resolveTopInt(text, TOP_DEATHS_NAME, TOP_DEATHS_VALUE, stats.getTopDeaths(10));
            text = resolveTopInt(text, TOP_KS_NAME, TOP_KS_VALUE, stats.getTopKillstreak(10));
            text = resolveTopInt(text, TOP_BKS_NAME, TOP_BKS_VALUE, stats.getTopBestKillstreak(10));
        }

        // ─── Classements mobs (top kills total) ──────────────────────────────────
        if (mobStats != null) {
            text = resolveTopInt(text, TOP_MOB_KILLS_NAME, TOP_MOB_KILLS_VALUE, mobStats.getTopTotal(10));

            // top_mob_kills_<mobId>_name_N et top_mob_kills_<mobId>_N
            text = resolveTopMobById(text, mobStats);
        }

        // ─── Suppression des lignes avec un placeholder top non résolu ──────────
        if (text.contains("\0")) return null;

        return text;
    }

    // Marqueur interne : indique que la ligne doit être supprimée du lore
    private static final String HIDE = "\0";

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static <T extends Number> String resolveTopDouble(String text,
            Pattern namePattern, Pattern valuePattern,
            List<Map.Entry<UUID, Double>> top) {

        Matcher m = namePattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int rank = Integer.parseInt(m.group(1));
            if (rank >= 1 && rank <= top.size()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(getTopName(top, rank)));
            } else {
                m.appendReplacement(sb, HIDE);
            }
        }
        m.appendTail(sb);
        text = sb.toString();

        m = valuePattern.matcher(text);
        sb = new StringBuffer();
        while (m.find()) {
            int rank = Integer.parseInt(m.group(1));
            if (rank >= 1 && rank <= top.size()) {
                m.appendReplacement(sb, String.valueOf(top.get(rank - 1).getValue().intValue()));
            } else {
                m.appendReplacement(sb, HIDE);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolveTopInt(String text,
            Pattern namePattern, Pattern valuePattern,
            List<Map.Entry<UUID, Integer>> top) {

        Matcher m = namePattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int rank = Integer.parseInt(m.group(1));
            if (rank >= 1 && rank <= top.size()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(getTopName(top, rank)));
            } else {
                m.appendReplacement(sb, HIDE);
            }
        }
        m.appendTail(sb);
        text = sb.toString();

        m = valuePattern.matcher(text);
        sb = new StringBuffer();
        while (m.find()) {
            int rank = Integer.parseInt(m.group(1));
            if (rank >= 1 && rank <= top.size()) {
                m.appendReplacement(sb, String.valueOf(top.get(rank - 1).getValue()));
            } else {
                m.appendReplacement(sb, HIDE);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String resolveTopMobById(String text, MobStatsAPI mobStats) {
        // %top_mob_kills_<mobId>_name_N%
        Matcher m = TOP_MOB_ID_KILLS_NAME.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String mobId = m.group(1);
            int rank = Integer.parseInt(m.group(2));
            List<Map.Entry<UUID, Integer>> top = mobStats.getTop(mobId, 10);
            if (rank >= 1 && rank <= top.size()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(getTopName(top, rank)));
            } else {
                m.appendReplacement(sb, HIDE);
            }
        }
        m.appendTail(sb);
        text = sb.toString();

        // %top_mob_kills_<mobId>_N%
        m = TOP_MOB_ID_KILLS_VALUE.matcher(text);
        sb = new StringBuffer();
        while (m.find()) {
            String mobId = m.group(1);
            int rank = Integer.parseInt(m.group(2));
            List<Map.Entry<UUID, Integer>> top = mobStats.getTop(mobId, 10);
            if (rank >= 1 && rank <= top.size()) {
                m.appendReplacement(sb, String.valueOf(top.get(rank - 1).getValue()));
            } else {
                m.appendReplacement(sb, HIDE);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static <V> String getTopName(List<Map.Entry<UUID, V>> top, int rank) {
        if (rank < 1 || rank > top.size()) return "—";
        OfflinePlayer op = Bukkit.getOfflinePlayer(top.get(rank - 1).getKey());
        String name = op.getName();
        return name != null ? name : "—";
    }

    // ─── Delegates ───────────────────────────────────────────────────────────────

    private static String getMoney(Player player) {
        EconomyAPI eco = APIProvider.getEconomy();
        double money = eco == null ? 0 : eco.getMoney(player.getUniqueId());
        return eco != null ? eco.formatMoney(money) : "0";
    }

    private static String getTokens(Player player) {
        EconomyAPI eco = APIProvider.getEconomy();
        int tokens = eco == null ? 0 : eco.getTokens(player.getUniqueId());
        return eco != null ? eco.formatTokens(tokens) : "0";
    }

    private static int getLevel(Player player) {
        return LevelAPI.getLevel(player.getUniqueId());
    }

    private static int getXP(Player player) {
        return LevelAPI.getXP(player.getUniqueId());
    }

    private static String getXPBar(Player player) {
        return LevelAPI.getXPBar(player.getUniqueId(), 10);
    }

    private static MobStatsAPI getMobStats() {
        try {
            return org.bukkit.Bukkit.getServicesManager().load(MobStatsAPI.class);
        } catch (Exception e) {
            return null;
        }
    }

}