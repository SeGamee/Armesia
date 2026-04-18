package fr.segame.armesiaMenu.action;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.ClickType;

import java.util.Set; // 🔥 IMPORTANT

public class GiveItemAction extends AbstractAction {

    private final Material material;
    private final int amount;

    public GiveItemAction(Material material, int amount, Set<ClickType> clicks) {
        super(clicks);
        this.material = material;
        this.amount = amount;
    }

    @Override
    public void execute(Player player) {
        player.getInventory().addItem(new ItemStack(material, amount));
    }
}