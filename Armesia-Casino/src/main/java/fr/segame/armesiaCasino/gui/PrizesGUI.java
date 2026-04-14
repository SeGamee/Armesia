package fr.segame.armesiaCasino.gui;

import fr.segame.armesiaCasino.MainCasino;
import fr.segame.armesiaCasino.Prize;
import fr.segame.armesiaCasino.managers.PrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventaire d'affichage des lots disponibles au casino.
 *
 * <ul>
 *   <li>Taille dynamique : 1 ligne pour ≤9 lots, 2 lignes pour ≤18, etc. (max 6 lignes).</li>
 *   <li>Tri : les lots les plus fréquents (chance élevée) en premier,
 *       les plus rares (chance faible) en dernier.</li>
 * </ul>
 */
public class PrizesGUI {

    private PrizesGUI() {}

    public static Inventory create(MainCasino plugin, PrizeManager prizeManager) {
        // Copie et tri décroissant par chance (rarest last)
        List<Prize> sorted = new ArrayList<>(prizeManager.getPrizes());
        sorted.sort((a, b) -> Integer.compare(b.getChance(), a.getChance()));

        int prizeCount = sorted.size();
        int rows = prizeCount == 0 ? 1 : (int) Math.ceil(prizeCount / 9.0);
        int size = Math.min(54, rows * 9); // max 6 lignes

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.prizes-title",
                        "&8&l[ &6&lLOTS DISPONIBLES &8&l]"));

        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < prizeCount && i < size; i++) {
            inv.setItem(i, prizeManager.createItemStack(sorted.get(i)));
        }

        return inv;
    }
}

