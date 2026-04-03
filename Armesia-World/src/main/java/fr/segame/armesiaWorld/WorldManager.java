package fr.segame.armesiaWorld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorldManager {

    private final MainWorld plugin;

    public WorldManager(MainWorld plugin) {
        this.plugin = plugin;
    }

    // ─── Auto-chargement au démarrage ─────────────────────────────────────────

    /**
     * Charge tous les mondes enregistrés dans config.yml → section "worlds".
     * Appelé une fois au onEnable pour que les mondes custom soient toujours présents.
     */
    public void loadAllSavedWorlds() {
        List<String> saved = plugin.getConfig().getStringList("worlds");
        for (String name : saved) {
            if (Bukkit.getWorld(name) != null) continue; // déjà chargé par Bukkit
            World w = new WorldCreator(name).createWorld();
            if (w != null) plugin.getLogger().info("[AutoLoad] Monde '" + name + "' chargé.");
            else           plugin.getLogger().warning("[AutoLoad] Impossible de charger '" + name + "'.");
        }
    }

    // ─── Création ─────────────────────────────────────────────────────────────

    public World createWorld(String worldName) {
        if (Bukkit.getWorld(worldName) != null) {
            plugin.getLogger().warning("Le monde '" + worldName + "' existe déjà.");
            return Bukkit.getWorld(worldName);
        }
        World world = new WorldCreator(worldName).createWorld();
        if (world != null) {
            plugin.getLogger().info("Monde '" + worldName + "' créé avec succès.");
            saveWorldToConfig(worldName);
        }
        return world;
    }

    /**
     * Enregistre un monde pour qu'il soit auto-chargé au démarrage.
     * Si le monde n'est pas encore chargé dans Bukkit, tente de le charger depuis le disque.
     * Retourne false si le dossier du monde est introuvable.
     */
    public boolean registerWorld(String worldName) {
        // Déjà chargé → juste enregistrer
        if (Bukkit.getWorld(worldName) != null) {
            saveWorldToConfig(worldName);
            return true;
        }
        // Vérifier que le dossier existe sur le disque
        java.io.File worldFolder = new java.io.File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !worldFolder.isDirectory()) return false;
        // Charger depuis le disque puis enregistrer
        World w = new WorldCreator(worldName).createWorld();
        if (w == null) return false;
        saveWorldToConfig(worldName);
        return true;
    }

    private void saveWorldToConfig(String worldName) {
        List<String> worlds = new ArrayList<>(plugin.getConfig().getStringList("worlds"));
        if (!worlds.contains(worldName)) {
            worlds.add(worldName);
            plugin.getConfig().set("worlds", worlds);
            plugin.saveConfig();
        }
    }

    // ─── Téléportation ────────────────────────────────────────────────────────

    /**
     * Téléporte un joueur dans un monde.
     * Utilise le spawn global si le monde correspond, sinon le centre du bloc de spawn du monde.
     */
    public boolean teleportToWorld(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return false;

        SpawnManager spawnManager = plugin.getSpawnManager();
        if (spawnManager != null && spawnManager.hasSpawn()
                && world.equals(spawnManager.getSpawn().getWorld())) {
            player.teleport(spawnManager.getSpawn());
        } else {
            player.teleport(centeredSpawn(world));
        }
        return true;
    }

    // ─── Liste ────────────────────────────────────────────────────────────────

    /**
     * Retourne uniquement les mondes de type NORMAL (exclut automatiquement
     * les dimensions nether _nether et end _the_end chargées par Bukkit).
     */
    public List<String> getLoadedWorldNames() {
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .map(World::getName)
                .collect(Collectors.toList());
    }

    // ─── Utilitaire ───────────────────────────────────────────────────────────

    /** Spawn du monde centré sur le bloc (évite le coin) avec pitch neutre. */
    private Location centeredSpawn(World world) {
        Location raw = world.getSpawnLocation();
        return new Location(world,
                Math.floor(raw.getX()) + 0.5,
                raw.getY(),
                Math.floor(raw.getZ()) + 0.5,
                raw.getYaw(), 0f);
    }
}





