package fr.segame.armesiaCasino.gui;

import fr.segame.armesiaCasino.MainCasino;
import fr.segame.armesiaCasino.Prize;
import fr.segame.armesiaCasino.managers.PrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouletteAnimation {

    // ── Constantes roulette ───────────────────────────────────────────────────
    /** Nombre de slots dans la fenêtre roulette (slots 9–17). */
    private static final int WINDOW_SIZE   = 9;
    /** Index dans la fenêtre du lot gagnant (centre de 9 = index 4 → slot 13). */
    private static final int WINNER_OFFSET = 4;

    // ── Constantes vitres scrollantes ─────────────────────────────────────────
    /** Nombre de vitres colorées visibles par ligne (hors slot 4 / slot 22). */
    private static final int COLOR_SLOTS = 8;
    /** Slots de la ligne 1 recevant les vitres colorées (skip slot 4 = verre noir). */
    private static final int[] TOP_COLOR_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    /** Slots de la ligne 3 recevant les vitres colorées (skip slot 22 = verre noir). */
    private static final int[] BOT_COLOR_SLOTS = {18, 19, 20, 21, 23, 24, 25, 26};

    /** Palette de vitres colorées (toutes sauf le noir réservé aux slots fixes). */
    private static final Material[] COLOR_POOL = {
        Material.WHITE_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.GRAY_STAINED_GLASS_PANE,
        Material.LIGHT_GRAY_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.BROWN_STAINED_GLASS_PANE,
        Material.GREEN_STAINED_GLASS_PANE,
        Material.RED_STAINED_GLASS_PANE
    };

    // ── Champs ────────────────────────────────────────────────────────────────
    private final MainCasino      plugin;
    private final Player          player;
    private final Inventory       inventory;
    private final Prize           winner;
    private final List<ItemStack> sequence;
    private final List<Material>  colorSequence;
    private final List<Long>      delays;
    private       int             currentStep = 0;
    private       BukkitTask      currentTask;
    private       boolean         finished    = false;

    private Runnable onComplete;

    // ── Constructeur ──────────────────────────────────────────────────────────

    public RouletteAnimation(MainCasino plugin, Player player,
                             Inventory inventory, PrizeManager prizeManager) {
        this.plugin    = plugin;
        this.player    = player;
        this.inventory = inventory;

        FileConfiguration cfg = plugin.getConfig();
        int     totalSteps = cfg.getInt("roulette.total-steps", 40);
        boolean slowDown   = cfg.getBoolean("roulette.slow-down", true);
        int     maxDelay   = cfg.getInt("roulette.max-delay", 8);
        int     minDelay   = cfg.getInt("roulette.min-delay", 1);

        Random rng = new Random();
        this.winner        = prizeManager.getRandomPrize();
        this.sequence      = buildSequence(prizeManager, totalSteps);
        this.colorSequence = buildColorSequence(totalSteps, rng);
        this.delays        = buildDelays(totalSteps, slowDown, maxDelay, minDelay);
    }

    // ── API publique ──────────────────────────────────────────────────────────

    public void setOnComplete(Runnable callback) { this.onComplete = callback; }

    public void start() {
        updateDisplay(0);
        scheduleNext(0);
    }

    /** Annule l'animation (jeton perdu, pas de récompense). */
    public void cancel() {
        if (currentTask != null) { currentTask.cancel(); currentTask = null; }
        if (onComplete != null) onComplete.run();
    }

    public boolean isFinished() { return finished; }
    public Prize   getWinner()  { return winner; }

    // ── Construction de la séquence de lots ───────────────────────────────────

    private List<ItemStack> buildSequence(PrizeManager prizeManager, int totalSteps) {
        int size = totalSteps + WINDOW_SIZE;
        List<ItemStack> seq = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            seq.add(prizeManager.createItemStack(
                    i == totalSteps + WINNER_OFFSET ? winner : prizeManager.getRandomPrize()));
        }
        return seq;
    }

    // ── Construction de la séquence de couleurs ───────────────────────────────

    /**
     * Génère {@code totalSteps + COLOR_SLOTS} couleurs aléatoires.
     * À l'offset {@code k}, les slots de couleur affichent
     * {@code colorSequence[k..k+7]} (identiques haut et bas).
     */
    private List<Material> buildColorSequence(int totalSteps, Random rng) {
        int size = totalSteps + COLOR_SLOTS;
        List<Material> seq = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            seq.add(COLOR_POOL[rng.nextInt(COLOR_POOL.length)]);
        }
        return seq;
    }

    // ── Construction du calendrier de délais ──────────────────────────────────

    private List<Long> buildDelays(int totalSteps, boolean slowDown, int maxDelay, int minDelay) {
        List<Long> d = new ArrayList<>(totalSteps);
        for (int i = 0; i < totalSteps; i++) {
            double progress = totalSteps <= 1 ? 1.0 : (double) i / (totalSteps - 1);
            double delay;
            if (slowDown) {
                delay = progress < 0.5
                        ? maxDelay - (maxDelay - minDelay) * (progress * 2.0)
                        : minDelay + (maxDelay - minDelay) * ((progress - 0.5) * 2.0);
            } else {
                delay = maxDelay - (maxDelay - minDelay) * progress;
            }
            d.add(Math.max(1L, Math.round(delay)));
        }
        return d;
    }

    // ── Boucle d'animation ────────────────────────────────────────────────────

    private void scheduleNext(int step) {
        if (step >= delays.size()) { finish(); return; }

        currentTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { cancel(); return; }
            currentStep = step + 1;
            updateDisplay(currentStep);
            playStepSound(step);
            scheduleNext(step + 1);
        }, delays.get(step));
    }

    // ── Mise à jour de l'affichage ────────────────────────────────────────────

    private void updateDisplay(int offset) {
        // Fenêtre roulette : slots 9-17
        for (int i = 0; i < WINDOW_SIZE; i++) {
            int idx = offset + i;
            inventory.setItem(9 + i, idx < sequence.size() ? sequence.get(idx) : null);
        }

        // Vitres colorées scrollantes (même couleur haut et bas)
        for (int i = 0; i < COLOR_SLOTS; i++) {
            int colorIdx = offset + i;
            Material mat = colorIdx < colorSequence.size()
                    ? colorSequence.get(colorIdx)
                    : COLOR_POOL[0];
            ItemStack glass = createColoredGlass(mat);
            inventory.setItem(TOP_COLOR_SLOTS[i], glass);
            inventory.setItem(BOT_COLOR_SLOTS[i], glass.clone());
        }
    }

    // ── Son ───────────────────────────────────────────────────────────────────

    private void playStepSound(int step) {
        FileConfiguration cfg = plugin.getConfig();
        String soundName = cfg.getString("roulette.sound", "BLOCK_NOTE_BLOCK_PLING");
        float  volume    = (float) cfg.getDouble("roulette.sound-volume", 0.7);
        float  pitchMin  = (float) cfg.getDouble("roulette.sound-pitch-min", 0.5);
        float  pitchMax  = (float) cfg.getDouble("roulette.sound-pitch-max", 2.0);

        Sound sound;
        try { sound = Sound.valueOf(soundName); }
        catch (IllegalArgumentException e) { sound = Sound.BLOCK_NOTE_BLOCK_PLING; }

        float progress = delays.isEmpty() ? 1f : (float) step / delays.size();
        float pitch = pitchMin + (pitchMax - pitchMin) * progress;
        player.playSound(player.getLocation(), sound, volume, Math.min(pitch, 2.0f));
    }

    // ── Fin de l'animation ────────────────────────────────────────────────────

    private void finish() {
        if (finished) return;
        finished = true;

        String finishSoundName = plugin.getConfig().getString(
                "roulette.finish-sound", "ENTITY_PLAYER_LEVELUP");
        Sound finishSound;
        try { finishSound = Sound.valueOf(finishSoundName); }
        catch (IllegalArgumentException e) { finishSound = Sound.ENTITY_PLAYER_LEVELUP; }
        player.playSound(player.getLocation(), finishSound, 1.0f, 1.0f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && winner != null) giveReward();
            if (onComplete != null) onComplete.run();
        }, 20L);
    }

    private void giveReward() {
        String cmd = winner.getConsoleCommand();
        if (cmd != null && !cmd.isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }

        String broadcast = winner.getBroadcastMessage();
        if (broadcast != null && !broadcast.isBlank()) {
            Bukkit.broadcastMessage(color(broadcast.replace("%player%", player.getName())));
        }

        String pm = winner.getPlayerMessage();
        if (pm != null && !pm.isBlank()) {
            player.sendMessage(color(pm.replace("%player%", player.getName())));
        }

        String winTitle = color(plugin.getConfig().getString("messages.win-title", "&6✦ GAGNÉ ! ✦"));
        player.sendTitle(winTitle, winner.getDisplayName(), 10, 60, 20);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static ItemStack createColoredGlass(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
