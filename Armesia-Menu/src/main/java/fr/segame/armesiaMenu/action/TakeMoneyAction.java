package fr.segame.armesiaMenu.action;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.entity.Player;
import java.util.Set;
import org.bukkit.event.inventory.ClickType;

public class TakeMoneyAction extends AbstractAction {

    private final double amount;

    public TakeMoneyAction(double amount, Set<ClickType> clicks) {
        super(clicks);
        this.amount = amount;
    }

    @Override
    public void execute(Player player) {

        EconomyAPI eco = APIProvider.getEconomy();
        if (eco == null) return;

        double money = eco.getMoney(player.getUniqueId());

        if (money < amount) return;

        eco.removeMoney(player.getUniqueId(), amount);
    }
}