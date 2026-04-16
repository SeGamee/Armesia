package fr.segame.armesiaScoreboard;
import fr.segame.armesia.Main;
import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import fr.segame.armesiaLevel.api.LevelAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class ScoreboardManager {
    private final Main core;
    // Scoreboard persistant par joueur - cree une seule fois a la connexion
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    public ScoreboardManager(Main core) {
        this.core = core;
    }
    public static String formatNumber(double number) {
        if (number >= 1_000_000_000) {
            return format(number, 1_000_000_000, "G");
        } else if (number >= 1_000_000) {
            return format(number, 1_000_000, "M");
        } else if (number >= 1_000) {
            return format(number, 1_000, "K");
        } else {
            return String.valueOf((int) number);
        }
    }
    private static String format(double number, double divisor, String suffix) {
        double value = number / divisor;
        if (value % 1 == 0) {
            return String.format("%.0f%s", value, suffix);
        } else {
            return String.format("%.1f%s", value, suffix);
        }
    }
    // Appele a la connexion
    public void init(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        board.registerNewObjective("sb", "dummy", "\u00a76\u00a7lARMESIA")
                .setDisplaySlot(DisplaySlot.SIDEBAR);
        scoreboards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        updateScores(player, board);
        // BELOW_NAME : vie sous le nom
        initBelowName(board);
        // Mettre a jour le score de ce joueur sur TOUS les boards existants
        updateBelowName(player);
        Main.updateAllTabs();
    }
    // Appele toutes les minutes (refresh periodique)
    public void update(Player player) {
        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) {
            init(player);
            return;
        }
        updateScores(player, board);
    }
    // Appele sur changement de niveau (mise a jour immediate)
    public void updatePlayer(Player player) {
        update(player);
    }
    // Appele a la deconnexion
    public void remove(Player player) {
        for (Scoreboard board : scoreboards.values()) {
            Objective obj = board.getObjective("hp");
            if (obj != null) board.resetScores(player.getName());
        }
        scoreboards.remove(player.getUniqueId());
    }
    // Cree l'objectif BELOW_NAME sur un board.
    // Critere "health" = Bukkit met a jour automatiquement la sante lors de chaque changement.
    // Le seed manuel est necessaire pour les joueurs DEJA en ligne au moment de la creation
    // du board : sans ca, leur sante apparait a 0 jusqu'au prochain changement de vie.
    private void initBelowName(Scoreboard board) {
        Objective old = board.getObjective("hp");
        if (old != null) old.unregister();
        Objective obj = board.registerNewObjective("hp", "health", "\u00a7c\u2764");
        obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        for (Player p : Bukkit.getOnlinePlayers()) {
            obj.getScore(p.getName()).setScore((int) Math.round(p.getHealth()));
        }
    }
    // Appelée quand un joueur vient de rejoindre : pousse sa sante initiale sur TOUS
    // les boards deja existants (sinon les autres voient 0 jusqu'au prochain damage).
    public void updateBelowName(Player player) {
        int hp = (int) Math.round(player.getHealth());
        for (Scoreboard board : scoreboards.values()) {
            Objective obj = board.getObjective("hp");
            if (obj != null) {
                obj.getScore(player.getName()).setScore(hp);
            }
        }
    }
    // Met a jour les lignes de la sidebar sur un scoreboard existant
    private void updateScores(Player player, Scoreboard board) {
        Objective old = board.getObjective("sb");
        if (old != null) old.unregister();
        Objective obj = board.registerNewObjective("sb", "dummy", "\u00a76\u00a7lARMESIA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        UUID uuid = player.getUniqueId();
        EconomyAPI eco = APIProvider.getEconomy();
        double money  = eco != null ? eco.getMoney(uuid)   : 0;
        int tokens    = eco != null ? eco.getTokens(uuid)  : 0;
        String moneyFormatted  = eco != null ? eco.formatMoney(money)    : "N/A";
        String tokensFormatted = eco != null ? eco.formatTokens(tokens)  : "N/A";
        int kills  = core.getStatsAPI().getKills(uuid);
        int deaths = core.getStatsAPI().getDeaths(uuid);
        int streak = core.getStatsAPI().getKillstreak(uuid);
        String ratio = core.getStatsAPI().getRatio(uuid);
        int level  = LevelAPI.getLevel(uuid);
        int xp     = LevelAPI.getXP(uuid);
        String xpBar = LevelAPI.getXPBar(uuid, 10);
        String maxXp = formatNumber(level * 1000);
        obj.getScore("\u00a74").setScore(10);
        obj.getScore("Niveau: \u00a77%d\u272b".formatted(level)).setScore(9);
        obj.getScore("\u00a7b%d %s \u00a7r \u00a7e%s".formatted(xp, xpBar, maxXp)).setScore(8);
        obj.getScore("\u00a73").setScore(7);
        obj.getScore("K/D: \u00a7a%d\u00a77/\u00a7a%d".formatted(kills, deaths)).setScore(6);
        obj.getScore("KS/Ratio: \u00a7a%d\u00a77/\u00a7a%s".formatted(streak, ratio)).setScore(5);
        obj.getScore("\u00a72").setScore(4);
        obj.getScore("Money: \u00a76" + moneyFormatted).setScore(3);
        obj.getScore("Tokens: \u00a7b" + tokensFormatted).setScore(2);
        obj.getScore("\u00a71").setScore(1);
    }
    public Scoreboard getScoreboard(UUID uuid) {
        return scoreboards.get(uuid);
    }
}