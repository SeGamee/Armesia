package fr.segame.armesiaMenu.action;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class AddMoneyAction extends AbstractAction {

    private final double amount;

    public AddMoneyAction(double amount, Set<ClickType> clicks) {
        super(clicks);
        this.amount = amount;
    }

    @Override
    public void execute(Player player) {

        if (amount <= 0) return;

        EconomyAPI eco = APIProvider.getEconomy();
        if (eco == null) return;

        eco.addMoney(player.getUniqueId(), amount);
    }
}