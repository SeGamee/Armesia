package fr.segame.armesiaCasino.managers;

import fr.segame.armesiaCasino.MainCasino;
import fr.segame.armesiaCasino.Prize;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PrizeManager {

    private final MainCasino  plugin;
    private final List<Prize> prizes      = new ArrayList<>();
    private       int         totalWeight = 0;
    private final Random      random      = new Random();

    public PrizeManager(MainCasino plugin) {
        this.plugin = plugin;
        loadPrizes();
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    public void loadPrizes() {
        prizes.clear();
        totalWeight = 0;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("prizes");
        if (section == null) {
            plugin.getLogger().warning("Aucun lot défini dans config.yml (section 'prizes' manquante).");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            String matName = s.getString("display-material", "STONE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) {
                plugin.getLogger().warning("Matériau inconnu '" + matName + "' pour le lot '" + key + "'. Utilisation de STONE.");
                mat = Material.STONE;
            }

            String name = color(s.getString("display-name", key));

            List<String> lore = new ArrayList<>();
            for (String l : s.getStringList("display-lore")) {
                lore.add(color(l));
            }

            int    chance    = Math.max(1, s.getInt("chance", 1));
            String cmd       = s.getString("console-command", "");
            String broadcast = s.getString("broadcast-message", "");
            String playerMsg = s.getString("player-message", "");

            prizes.add(new Prize(key, mat, name, lore, chance, cmd, broadcast, playerMsg));
            totalWeight += chance;
        }

        plugin.getLogger().info(prizes.size() + " lot(s) chargé(s) (poids total : " + totalWeight + ").");
    }

    // ── Sélection aléatoire pondérée ─────────────────────────────────────────

    public Prize getRandomPrize() {
        if (prizes.isEmpty()) return null;
        int roll       = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Prize p : prizes) {
            cumulative += p.getChance();
            if (roll < cumulative) return p;
        }
        return prizes.get(prizes.size() - 1);
    }

    // ── Création d'un ItemStack depuis un lot ─────────────────────────────────

    public ItemStack createItemStack(Prize prize) {
        if (prize == null) return new ItemStack(Material.STONE);

        ItemStack item = new ItemStack(prize.getDisplayMaterial());
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(prize.getDisplayName());
            meta.setLore(prize.getDisplayLore());
            item.setItemMeta(meta);
        }
        return item;
    }

    public List<Prize> getPrizes() {
        return Collections.unmodifiableList(prizes);
    }


    // ── Utilitaire ────────────────────────────────────────────────────────────

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}


