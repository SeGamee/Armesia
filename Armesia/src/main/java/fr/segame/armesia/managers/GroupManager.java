package fr.segame.armesia.managers;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class GroupManager {

    private final Main plugin;
    private static final String DEFAULT_GROUP = "Citoyen";

    public GroupManager(Main plugin) {
        this.plugin = plugin;
    }

    // ---------------- EXISTS ----------------
    public boolean exists(String group) {
        return group != null
                && !group.isBlank()
                && plugin.getConfig().contains("groups." + group);
    }

    // ---------------- CREATE ----------------
    public boolean createGroup(String group) {
        if (group == null || group.isBlank()) {
            return false;
        }

        if (exists(group)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".permissions", new ArrayList<String>());
        plugin.getConfig().set("groups." + group + ".chat-prefix", "");
        plugin.getConfig().set("groups." + group + ".tab-prefix", "");
        plugin.getConfig().set("groups." + group + ".priority", 0);
        plugin.saveConfig();

        return true;
    }

    // ---------------- DELETE ----------------
    public boolean deleteGroup(String group) {
        if (!exists(group)) {
            return false;
        }

        if (group.equalsIgnoreCase(DEFAULT_GROUP)) {
            return false;
        }

        ensureDefaultGroupExists();

        // Corrige immédiatement tous les joueurs qui avaient ce groupe
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (!offlinePlayer.hasPlayedBefore()) {
                continue;
            }

            UUID uuid = offlinePlayer.getUniqueId();
            String playerGroup = plugin.getConfig().getString("players." + uuid + ".group");

            if (playerGroup != null && playerGroup.equalsIgnoreCase(group)) {
                plugin.getConfig().set("players." + uuid + ".group", DEFAULT_GROUP);

                // cache mémoire
                Main.groups.put(uuid, DEFAULT_GROUP);

                // si le joueur est en ligne, on refresh le tab tout de suite
                if (offlinePlayer.isOnline()) {
                    Player onlinePlayer = offlinePlayer.getPlayer();
                    if (onlinePlayer != null) {
                        Main.updateTab(onlinePlayer);
                    }
                }
            }
        }

        // Supprime le groupe
        plugin.getConfig().set("groups." + group, null);
        plugin.saveConfig();

        return true;
    }

    // ---------------- GET GROUPS ----------------
    public Set<String> getGroups() {
        if (!plugin.getConfig().contains("groups")) {
            return new HashSet<>();
        }

        if (plugin.getConfig().getConfigurationSection("groups") == null) {
            return new HashSet<>();
        }

        return plugin.getConfig().getConfigurationSection("groups").getKeys(false);
    }

    // ---------------- PERMISSIONS ----------------
    public boolean addPermission(String group, String perm) {
        if (!exists(group) || perm == null || perm.isBlank()) {
            return false;
        }

        List<String> perms = plugin.getConfig().getStringList("groups." + group + ".permissions");

        if (perms.contains(perm)) {
            return false;
        }

        perms.add(perm);
        plugin.getConfig().set("groups." + group + ".permissions", perms);
        plugin.saveConfig();

        return true;
    }

    public boolean removePermission(String group, String perm) {
        if (!exists(group) || perm == null || perm.isBlank()) {
            return false;
        }

        List<String> perms = plugin.getConfig().getStringList("groups." + group + ".permissions");

        if (!perms.remove(perm)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".permissions", perms);
        plugin.saveConfig();

        return true;
    }

    public List<String> getPermissions(String group) {
        if (!exists(group)) {
            return new ArrayList<>();
        }

        return plugin.getConfig().getStringList("groups." + group + ".permissions");
    }

    // ---------------- PREFIX ----------------
    public String formatPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        String formatted = prefix.replace("&", "§").strip();

        // Espace de séparation uniquement s'il y a du texte visible (pas que des codes couleur)
        String visibleText = formatted.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        if (!visibleText.isBlank()) {
            return formatted + " ";
        }

        return formatted; // Prefix couleur seule : pas d'espace ajouté
    }

    public boolean setChatPrefix(String group, String prefix) {
        if (!exists(group)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".chat-prefix", prefix);
        plugin.saveConfig();

        return true;
    }

    public boolean setTabPrefix(String group, String prefix) {
        if (!exists(group)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".tab-prefix", prefix);
        plugin.saveConfig();

        return true;
    }

    public String getChatPrefix(String group) {
        group = getValidGroupOrDefault(group);
        return plugin.getConfig().getString("groups." + group + ".chat-prefix", "");
    }

    public String getTabPrefix(String group) {
        group = getValidGroupOrDefault(group);
        return plugin.getConfig().getString("groups." + group + ".tab-prefix", "");
    }

    // ---------------- PRIORITY ----------------
    public boolean setPriority(String group, int priority) {
        if (!exists(group)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".priority", priority);
        plugin.saveConfig();

        return true;
    }

    public int getPriority(String group) {
        if (!exists(group)) {
            return 0;
        }

        return plugin.getConfig().getInt("groups." + group + ".priority", 0);
    }

    // ---------------- PLAYER GROUP ----------------
    public boolean setPlayerGroup(OfflinePlayer player, String group) {
        if (player == null) {
            return false;
        }

        if (!exists(group)) {
            return false;
        }

        if (!player.isOnline() && !player.hasPlayedBefore()) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        plugin.getConfig().set("players." + uuid + ".group", group);
        plugin.saveConfig();

        Main.groups.put(uuid, group);

        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null) {
                Main.updateTab(onlinePlayer);
            }
        }

        return true;
    }

    public boolean clearChatPrefix(String group) {
        if (!exists(group)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".chat-prefix", "");
        plugin.saveConfig();

        return true;
    }

    public boolean clearTabPrefix(String group) {
        if (!exists(group)) {
            return false;
        }

        plugin.getConfig().set("groups." + group + ".tab-prefix", "");
        plugin.saveConfig();

        return true;
    }

    // ---------------- DEFAULT ----------------
    public String getValidGroupOrDefault(String group) {
        if (group == null || !exists(group)) {
            ensureDefaultGroupExists();
            return DEFAULT_GROUP;
        }

        return group;
    }

    public void ensureDefaultGroupExists() {
        if (exists(DEFAULT_GROUP)) {
            return;
        }

        plugin.getConfig().set("groups." + DEFAULT_GROUP + ".permissions", new ArrayList<String>());
        plugin.getConfig().set("groups." + DEFAULT_GROUP + ".chat-prefix", "");
        plugin.getConfig().set("groups." + DEFAULT_GROUP + ".tab-prefix", "");
        plugin.getConfig().set("groups." + DEFAULT_GROUP + ".priority", 0);
        plugin.saveConfig();
    }
}