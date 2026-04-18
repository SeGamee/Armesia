package fr.segame.armesiaMenu.action;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Set;

public class TakeTokensAction extends AbstractAction {

    private final int amount;

    public TakeTokensAction(int amount, Set<ClickType> clicks) {
        super(clicks);
        this.amount = amount;
    }

    @Override
    public void execute(Player player) {

        EconomyAPI eco = APIProvider.getEconomy();
        if (eco == null) return;

        int tokens = eco.getTokens(player.getUniqueId());

        if (tokens < amount) return;

        eco.removeTokens(player.getUniqueId(), amount);
    }
}