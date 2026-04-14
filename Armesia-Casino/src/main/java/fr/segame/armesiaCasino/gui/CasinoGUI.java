package fr.segame.armesiaCasino.gui;

import fr.segame.armesiaCasino.MainCasino;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CasinoGUI {

    /**
     * Crée l'inventaire casino (3 lignes = 27 slots).
     *
     * Structure :
     *   Ligne 1  (slots  0–8)  : vitres colorées scrollantes | slot 4 = verre noir fixe
     *   Ligne 2  (slots  9–17) : fenêtre roulette (9 emplacements, milieu = slot 13)
     *   Ligne 3  (slots 18–26) : vitres colorées scrollantes | slot 22 = verre noir fixe
     *
     * Les vitres scrollantes sont initialisées en placeholder ; RouletteAnimation
     * les remplace dès le premier tick.
     */
    public static Inventory create(MainCasino plugin) {
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&8&l[ &6&lCASINO &8&l]"));

        Inventory inv = Bukkit.createInventory(null, 27, title);

        String borderName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.border-name", " "));

        // Placeholder scrollant (remplacé par RouletteAnimation immédiatement)
        ItemStack placeholder = createGlass(Material.PURPLE_STAINED_GLASS_PANE, borderName);

        // Ligne 1 : slots 0-3 et 5-8 → placeholder scrollant
        for (int i = 0; i <= 8; i++) {
            if (i != 4) inv.setItem(i, placeholder.clone());
        }
        // Ligne 3 : slots 18-21 et 23-26 → placeholder scrollant
        for (int i = 18; i <= 26; i++) {
            if (i != 22) inv.setItem(i, placeholder.clone());
        }

        // Slots centraux fixes : verre noir (bloc, pas vitre)
        ItemStack blackGlass = createGlass(Material.BLACK_STAINED_GLASS, borderName);
        inv.setItem(4,  blackGlass);
        inv.setItem(22, blackGlass.clone());

        // Slots 9-17 : fenêtre roulette, laissés vides (remplis par RouletteAnimation)
        return inv;
    }

    public static ItemStack createGlass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}

