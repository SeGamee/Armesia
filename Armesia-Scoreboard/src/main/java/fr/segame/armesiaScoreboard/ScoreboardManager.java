package fr.segame.armesiaScoreboard;

import fr.segame.armesia.Main;
import fr.segame.armesiaLevel.api.LevelAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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

        // Injecter les teams de tri pour tous les joueurs déjà connectés
        for (Player online : Bukkit.getOnlinePlayers()) {
            injectLevelTeam(board, online);
        }

        // Remplir la sidebar immédiatement
        updateScores(player, board);
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

        // 📈 Level
        int level  = LevelAPI.getLevel(uuid);
        int xp     = LevelAPI.getXP(uuid);
        String xpBar = LevelAPI.getXPBar(uuid, 10);

        obj.getScore("§7 ").setScore(10);
        obj.getScore("Niveau: §7%d✫".formatted(level)).setScore(9);
        obj.getScore("§b%d %s §e%d".formatted(xp, xpBar, level * 1000)).setScore(8);
        obj.getScore("§aMoney: §f"  + moneyFormatted).setScore(7);
        obj.getScore("§bTokens: §f" + tokensFormatted).setScore(6);
        obj.getScore("§8 ").setScore(5);
        obj.getScore("§cKills: §f"  + kills).setScore(4);
        obj.getScore("§7Morts: §f"  + deaths).setScore(3);
        obj.getScore("§eStreak: §f" + streak).setScore(2);
        obj.getScore("§0 ").setScore(1);
    }

    // ── Injecte la team de tri lvl_ d'un joueur sur un scoreboard donné ──────
    public void injectLevelTeam(Scoreboard board, Player target) {
        int level = LevelAPI.getLevel(target.getUniqueId());
        String teamName = String.format("lvl_%04d", 9999 - level);

        for (Team t : board.getTeams()) {
            if (t.getName().startsWith("lvl_") && t.hasEntry(target.getName())) {
                t.removeEntry(target.getName());
            }
        }

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        team.addEntry(target.getName());
    }

    public Scoreboard getScoreboard(UUID uuid) {
        return scoreboards.get(uuid);
    }
}