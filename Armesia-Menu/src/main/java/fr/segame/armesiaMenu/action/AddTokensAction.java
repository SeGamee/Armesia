package fr.segame.armesiaMenu.action;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class AddTokensAction extends AbstractAction {

    private final int amount;

    public AddTokensAction(int amount, Set<ClickType> clicks) {
        super(clicks);
        this.amount = amount;
    }

    @Override
    public void execute(Player player) {

        EconomyAPI eco = APIProvider.getEconomy();
        if (eco == null) return;

        eco.addTokens(player.getUniqueId(), amount);
    }
}