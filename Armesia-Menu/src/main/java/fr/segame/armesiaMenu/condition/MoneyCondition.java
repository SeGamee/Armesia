package fr.segame.armesiaMenu.condition;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.entity.Player;

public class MoneyCondition implements Condition {

    private final double min;

    public MoneyCondition(double min) {
        this.min = min;
    }

    @Override
    public boolean isValid(Player player) {

        EconomyAPI eco = APIProvider.getEconomy();
        if (eco == null) return false;

        double money = eco.getMoney(player.getUniqueId());

        return money >= min;
    }
}