package fr.segame.armesiaScoreboard;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

public class ScoreboardManager {

    private final Main core;

    public ScoreboardManager(Main core) {
        this.core = core;
    }

    public void update(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("sb", "dummy", "§6§lARMESIA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        UUID uuid = player.getUniqueId();

        // 💰 ECONOMIE
        double money = core.getEconomyAPI().getMoney(uuid);
        int tokens = core.getEconomyAPI().getTokens(uuid);

        String moneyFormatted = core.getEconomyAPI().formatMoney(money);
        String tokensFormatted = core.getEconomyAPI().formatTokens(tokens);

        // 📊 STATS (API propre)
        int kills = core.getStatsAPI().getKills(uuid);
        int deaths = core.getStatsAPI().getDeaths(uuid);
        int streak = core.getStatsAPI().getKillstreak(uuid);

        // 🧱 LIGNES (uniques !)
        obj.getScore("§7 ").setScore(8);

        obj.getScore("§aMoney: §f" + moneyFormatted).setScore(7);
        obj.getScore("§bTokens: §f" + tokensFormatted).setScore(6);

        obj.getScore("§8 ").setScore(5);

        obj.getScore("§cKills: §f" + kills).setScore(4);
        obj.getScore("§7Morts: §f" + deaths).setScore(3);
        obj.getScore("§eStreak: §f" + streak).setScore(2);

        obj.getScore("§0 ").setScore(1);

        player.setScoreboard(board);
    }
}