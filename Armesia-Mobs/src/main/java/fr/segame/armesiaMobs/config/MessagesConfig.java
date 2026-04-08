package fr.segame.armesiaMobs.config;

import fr.segame.armesiaMobs.ArmesiaMobs;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Charge messages.yml et fournit les messages colorés avec remplacement de placeholders.
 *
 * Utilisation :
 *   msg.get("mob.not-found", "id", mobId)
 *   msg.getLines("kill.notification", "mob", name, "money", "100", "xp", "50")
 *
 * Les préfixes sont directement intégrés dans chaque message du YAML.
 * Pour les messages multi-lignes (liste YAML), utiliser getLines() qui retourne
 * une List<String> — ajoutez ou supprimez des lignes librement dans messages.yml.
 */
public class MessagesConfig {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public MessagesConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    // ─── Rechargement ─────────────────────────────────────────────────────────

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        config = YamlConfiguration.loadConfiguration(file);

        // Fusion avec les valeurs par défaut embarquées dans le JAR
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
    }

    // ─── API publique ──────────────────────────────────────────────────────────

    /**
     * Retourne le message coloré (& → §) correspondant au chemin.
     * Si absent → message de fallback rouge.
     */
    public String get(String path) {
        String raw = config.getString(path, null);
        if (raw == null) {
            ArmesiaMobs.getInstance().getLogger()
                    .warning("[MessagesConfig] Clé manquante : " + path);
            raw = "&c[msg manquant: " + path + "]";
        }
        return color(raw);
    }

    /**
     * Retourne le message avec substitution de placeholders.
     * Les arguments sont des paires clé/valeur :
     *   get("loot.not-found", "table", "drops_boss")
     *   get("loot.entry-added", "table", "drops", "item", "DIAMOND", "min", "1", "max", "3", "chance", "5%")
     */
    public String get(String path, Object... replacements) {
        return applyReplacements(get(path), replacements);
    }

    /**
     * Retourne la liste de lignes colorées pour un message multi-lignes (liste YAML).
     * Si le chemin est une liste → chaque item devient une ligne.
     * Si le chemin est une chaîne simple → liste d'un seul élément.
     * Vous pouvez ajouter ou supprimer des lignes librement dans messages.yml.
     *
     *   getLines("kill.notification", "mob", name, "money", "100", "xp", "50")
     *   getLines("stats.header", "player", name)
     */
    public List<String> getLines(String path, Object... replacements) {
        List<String> raw = config.getStringList(path);
        if (!raw.isEmpty()) {
            return raw.stream()
                    .map(line -> applyReplacements(color(line), replacements))
                    .collect(Collectors.toList());
        }
        // Fallback chaîne simple
        return List.of(get(path, replacements));
    }

    // ─── Utilitaire interne ───────────────────────────────────────────────────

    private String applyReplacements(String msg, Object[] replacements) {
        for (int i = 0; i + 1 < replacements.length; i += 2)
            msg = msg.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        return msg;
    }


    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

