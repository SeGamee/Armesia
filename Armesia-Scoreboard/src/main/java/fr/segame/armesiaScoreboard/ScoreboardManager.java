package fr.segame.armesiaScoreboard;

import fr.segame.armesia.Main;
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

    // Scoreboard persistant par joueur — créé une seule fois à la connexion
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardManager(Main core) {
        this.core = core;
    }

    // ── Appelé à la connexion ─────────────────────────────────────────────────
    public void init(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        board.registerNewObjective("sb", "dummy", "§6§lARMESIA")
                .setDisplaySlot(DisplaySlot.SIDEBAR);

        scoreboards.put(player.getUniqueId(), board);
        player.setScoreboard(board);

        // Remplir la sidebar immédiatement
        updateScores(player, board);

        // Re-appliquer les préfixes/couleurs de groupe sur le nouveau scoreboard.
        // updateAllTabs() boucle sur tous les viewers en ligne et écrit la team
        // de chaque joueur sur le scoreboard actif du viewer → ce scoreboard (board)
        // reçoit automatiquement toutes les teams avec prefix/suffix corrects.
        Main.updateAllTabs();
    }

    // ── Appelé toutes les minutes (refresh périodique) ────────────────────────
    public void update(Player player) {
        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) {
            init(player);
            return;
        }
        updateScores(player, board);
    }

    // ── Appelé sur changement de niveau (mise à jour immédiate) ──────────────
    public void updatePlayer(Player player) {
        update(player);
    }

    // ── Appelé à la déconnexion ───────────────────────────────────────────────
    public void remove(Player player) {
        scoreboards.remove(player.getUniqueId());
    }

    // ── Met à jour les lignes de la sidebar sur un scoreboard existant ────────
    private void updateScores(Player player, Scoreboard board) {
        // On supprime et recrée l'objective à chaque update pour éviter les doublons :
        // dans Bukkit, le texte de la ligne EST la clé → si le texte change (ex: niveau),
        // l'ancienne entrée reste affichée en plus de la nouvelle.
        Objective old = board.getObjective("sb");
        if (old != null) old.unregister();

        Objective obj = board.registerNewObjective("sb", "dummy", "§6§lARMESIA");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        UUID uuid = player.getUniqueId();

        // 💰 Économie
        double money = core.getEconomyAPI().getMoney(uuid);
        int tokens = core.getEconomyAPI().getTokens(uuid);
        String moneyFormatted = core.getEconomyAPI().formatMoney(money);
        String tokensFormatted = core.getEconomyAPI().formatTokens(tokens);

        // 📊 Stats
        int kills  = core.getStatsAPI().getKills(uuid);
        int deaths = core.getStatsAPI().getDeaths(uuid);
        int streak = core.getStatsAPI().getKillstreak(uuid);
        String ratio = core.getStatsAPI().getRatio(uuid);

        // 📈 Level
        int level  = LevelAPI.getLevel(uuid);
        int xp     = LevelAPI.getXP(uuid);
        String xpBar = LevelAPI.getXPBar(uuid, 10);

        obj.getScore("§4").setScore(10);
        obj.getScore("Niveau: §7%d✫".formatted(level)).setScore(9);
        obj.getScore("§b%d %s §e%d".formatted(xp, xpBar, level * 1000)).setScore(8);
        obj.getScore("§3").setScore(7);
        obj.getScore("K/D: §a%d§7/§a%d".formatted(kills, deaths)).setScore(6);
        obj.getScore("KS/Ratio: §a%d§7/§a%s".formatted(streak, ratio)).setScore(5);
        obj.getScore("§2").setScore(4);
        obj.getScore("Money: §6"  + moneyFormatted).setScore(3);
        obj.getScore("Tokens: §b" + tokensFormatted).setScore(2);
        obj.getScore("§1").setScore(1);
    }


    public Scoreboard getScoreboard(UUID uuid) {
        return scoreboards.get(uuid);
    }
}