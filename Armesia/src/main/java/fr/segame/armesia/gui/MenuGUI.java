package fr.segame.armesia.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MenuGUI {

    private final Inventory inventory;

    public MenuGUI() {
        this.inventory = Bukkit.createInventory(null, 27, Component.translatable("Mon Menu"));
    }

    public void openGUI(Player player) {
        player.openInventory(this.inventory);

        ItemStack bonjour = new ItemStack(Material.DIAMOND, 2);
        ItemMeta bonjourMeta = bonjour.getItemMeta();
        bonjourMeta.displayName(Component.text("§aDire bonjour !"));
        bonjourMeta.lore(List.of(
                Component.text("§7Cliquez pour dire bonjour")
        ));
        bonjour.setItemMeta(bonjourMeta);
        this.inventory.setItem(12, bonjour);

        ItemStack kick = new ItemStack(Material.SPRUCE_WOOD);
        ItemMeta kickMeta = kick.getItemMeta();
        kickMeta.displayName(Component.text("§cSe déconnecter."));
        kick.setItemMeta(kickMeta);
        this.inventory.setItem(14, kick);

    }
}
