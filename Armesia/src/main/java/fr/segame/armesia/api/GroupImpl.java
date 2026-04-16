package fr.segame.armesia.api;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.GroupManager;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implémentation de {@link GroupAPI} qui délègue au {@link GroupManager}.
 */
public class GroupImpl implements GroupAPI {

    private final GroupManager manager;

    public GroupImpl(GroupManager manager) {
        this.manager = manager;
    }

    // ─── Gestion des groupes ────────────────────────────────────────────────────

    @Override
    public boolean exists(String group) {
        return manager.exists(group);
    }

    @Override
    public boolean createGroup(String group) {
        return manager.createGroup(group);
    }

    @Override
    public boolean deleteGroup(String group) {
        return manager.deleteGroup(group);
    }

    @Override
    public Set<String> getGroups() {
        return manager.getGroups();
    }

    // ─── Permissions par groupe ─────────────────────────────────────────────────

    @Override
    public boolean addPermission(String group, String permission) {
        return manager.addPermission(group, permission);
    }

    @Override
    public boolean removePermission(String group, String permission) {
        return manager.removePermission(group, permission);
    }

    @Override
    public List<String> getPermissions(String group) {
        return manager.getPermissions(group);
    }

    @Override
    public boolean groupHasPermission(String group, String permission) {
        return manager.getPermissions(group).contains(permission);
    }

    // ─── Vérification joueur ────────────────────────────────────────────────────

    @Override
    public boolean playerHasPermission(UUID uuid, String permission) {
        String group = getPlayerGroup(uuid);
        return groupHasPermission(group, permission);
    }

    // ─── Groupe du joueur ───────────────────────────────────────────────────────

    @Override
    public String getPlayerGroup(UUID uuid) {
        String group = Main.groups.get(uuid);
        return manager.getValidGroupOrDefault(group);
    }

    @Override
    public boolean setPlayerGroup(OfflinePlayer player, String group) {
        return manager.setPlayerGroup(player, group);
    }

    // ─── Préfixes ───────────────────────────────────────────────────────────────

    @Override
    public String getChatPrefix(String group) {
        return manager.formatPrefix(manager.getChatPrefix(group));
    }

    @Override
    public String getTabPrefix(String group) {
        return manager.formatPrefix(manager.getTabPrefix(group));
    }

    @Override
    public boolean setChatPrefix(String group, String prefix) {
        return manager.setChatPrefix(group, prefix);
    }

    @Override
    public boolean setTabPrefix(String group, String prefix) {
        return manager.setTabPrefix(group, prefix);
    }

    @Override
    public boolean clearChatPrefix(String group) {
        return manager.clearChatPrefix(group);
    }

    @Override
    public boolean clearTabPrefix(String group) {
        return manager.clearTabPrefix(group);
    }

    // ─── Priorité ───────────────────────────────────────────────────────────────

    @Override
    public int getPriority(String group) {
        return manager.getPriority(group);
    }

    @Override
    public boolean setPriority(String group, int priority) {
        return manager.setPriority(group, priority);
    }
}

