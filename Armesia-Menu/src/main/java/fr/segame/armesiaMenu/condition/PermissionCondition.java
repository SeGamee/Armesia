package fr.segame.armesiaMenu.condition;

import org.bukkit.entity.Player;

public class PermissionCondition implements Condition {

    private final String permission;

    public PermissionCondition(String permission) {
        this.permission = permission;
    }

    @Override
    public boolean isValid(Player player) {
        return player.hasPermission(permission);
    }
}