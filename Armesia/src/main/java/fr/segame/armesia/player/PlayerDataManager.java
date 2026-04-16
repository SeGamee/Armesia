package fr.segame.armesia.player;

import fr.segame.armesia.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère la persistance des données joueur (players.yml) ainsi que
 * le chargement / la sauvegarde en mémoire de leurs informations.
 *
 * <p>À la connexion, les permissions du groupe Armesia sont injectées
 * dans le système de permissions Bukkit via un {@link PermissionAttachment}.
 * Cela permet d'utiliser {@code player.hasPermission("ma.permission")} nativement,
 * sans passer par {@code Main.hasGroupPermission()} ou {@code Main.checkPerm()}.
 */
public class PlayerDataManager {

    private final Main plugin;
    private File playersFile;
    private FileConfiguration playersConfig;

    /** Attachments Bukkit actifs, un par joueur connecté. */
    private static final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        setupPlayersFile();
    }

    // ─── players.yml ────────────────────────────────────────────────────────────

    private void setupPlayersFile() {
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            playersFile.getParentFile().mkdirs();
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    public FileConfiguration getPlayersConfig() {
        return playersConfig;
    }

    public void savePlayers() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── Chargement / Sauvegarde d'un joueur ────────────────────────────────────

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        String group = Main.groupManager.getValidGroupOrDefault(
                plugin.getConfig().getString("players." + uuid + ".group")
        );
        String job = plugin.getConfig().getString("players." + uuid + ".job", "Citoyen");

        Main.groups.put(uuid, group);
        Main.jobs.put(uuid, job);

        // Injecte les permissions du groupe dans Bukkit
        applyAttachment(player, group);
    }

    public void savePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getConfig().set("players." + uuid + ".group",  Main.groups.get(uuid));
        plugin.getConfig().set("players." + uuid + ".job",    Main.jobs.get(uuid));
        plugin.saveConfig();
    }

    /**
     * Appelé lors de la déconnexion pour libérer l'attachment Bukkit.
     * Doit être appelé dans {@code PlayerQuitEvent}.
     */
    public void unloadPlayer(Player player) {
        removeAttachment(player.getUniqueId());
        Main.groups.remove(player.getUniqueId());
        Main.jobs.remove(player.getUniqueId());
    }

    // ─── Permissions ────────────────────────────────────────────────────────────

    public boolean hasGroupPermission(Player player, String permission) {
        if (player.isOp()) return true;
        // Grâce à l'attachment, player.hasPermission() suffit désormais.
        // On conserve la vérification manuelle comme filet de sécurité.
        if (player.hasPermission(permission)) return true;

        String group = Main.groups.get(player.getUniqueId());
        group = Main.groupManager.getValidGroupOrDefault(group);

        List<String> permissions = plugin.getConfig()
                .getStringList("groups." + group + ".permissions");

        if (permissions.contains("*")) return true;
        return permissions.contains(permission);
    }

    // ─── Attachment Bukkit ───────────────────────────────────────────────────────

    /**
     * Crée (ou recrée) l'attachment Bukkit d'un joueur et y injecte
     * toutes les permissions de son groupe.
     *
     * <p>Si le groupe contient {@code "*"}, toutes les permissions connues
     * de Bukkit sont accordées ({@code isOp}-like via l'attachment).
     */
    public void applyAttachment(Player player, String group) {
        removeAttachment(player.getUniqueId());

        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        List<String> perms = plugin.getConfig()
                .getStringList("groups." + group + ".permissions");

        if (perms.contains("*")) {
            // Wildcard : accorde toutes les permissions enregistrées dans Bukkit
            org.bukkit.Bukkit.getPluginManager().getPermissions()
                    .forEach(p -> attachment.setPermission(p.getName(), true));
        } else {
            for (String perm : perms) {
                if (perm != null && !perm.isBlank()) {
                    attachment.setPermission(perm.trim(), true);
                }
            }
        }

        player.recalculatePermissions();
    }

    /**
     * Supprime l'attachment Bukkit d'un joueur (déconnexion ou changement de groupe).
     */
    private void removeAttachment(UUID uuid) {
        PermissionAttachment old = attachments.remove(uuid);
        if (old != null) {
            try { old.remove(); } catch (Exception ignored) { }
        }
    }
}

