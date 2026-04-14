package fr.segame.armesiaCasino.managers;

import fr.segame.armesiaCasino.MainCasino;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CasinoManager {

    private final MainCasino       plugin;
    private final HologramManager  hologramManager;
    /** Ensemble des blocs enregistrés comme blocs de casino */
    private final Set<Location>    casinoBlocks = new HashSet<>();

    private final File             dataFile;
    private       FileConfiguration dataConfig;

    public CasinoManager(MainCasino plugin, HologramManager hologramManager) {
        this.plugin          = plugin;
        this.hologramManager = hologramManager;

        dataFile = new File(plugin.getDataFolder(), "casinos.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("Impossible de créer casinos.yml : " + e.getMessage()); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadBlocks();
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    public void loadBlocks() {
        casinoBlocks.clear();
        hologramManager.removeAll();

        ConfigurationSection section = dataConfig.getConfigurationSection("blocks");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String worldName = dataConfig.getString("blocks." + key + ".world");
            int    x         = dataConfig.getInt("blocks." + key + ".x");
            int    y         = dataConfig.getInt("blocks." + key + ".y");
            int    z         = dataConfig.getInt("blocks." + key + ".z");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Monde '" + worldName + "' introuvable pour le bloc casino '" + key + "'.");
                continue;
            }

            Location loc = new Location(world, x, y, z);
            casinoBlocks.add(loc);
            hologramManager.createHologram(loc);
        }

        plugin.getLogger().info(casinoBlocks.size() + " bloc(s) de casino chargé(s).");
    }

    // ── Ajout / suppression ───────────────────────────────────────────────────

    public void addBlock(Location loc) {
        Location blockLoc = loc.getBlock().getLocation();
        casinoBlocks.add(blockLoc);
        hologramManager.createHologram(blockLoc);
        saveBlock(blockLoc);
    }

    public void removeBlock(Location loc) {
        Location blockLoc = loc.getBlock().getLocation();
        casinoBlocks.remove(blockLoc);
        hologramManager.removeHologram(blockLoc);
        deleteBlock(blockLoc);
    }

    // ── Vérification ──────────────────────────────────────────────────────────

    public boolean isCasinoBlock(Location loc) {
        return casinoBlocks.contains(loc.getBlock().getLocation());
    }

    public Set<Location> getCasinoBlocks() {
        return Collections.unmodifiableSet(casinoBlocks);
    }

    // ── Rechargement ──────────────────────────────────────────────────────────

    public void reloadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadBlocks();
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    private String blockKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    private void saveBlock(Location loc) {
        String path = "blocks." + blockKey(loc);
        dataConfig.set(path + ".world", loc.getWorld().getName());
        dataConfig.set(path + ".x", loc.getBlockX());
        dataConfig.set(path + ".y", loc.getBlockY());
        dataConfig.set(path + ".z", loc.getBlockZ());
        saveDataFile();
    }

    private void deleteBlock(Location loc) {
        dataConfig.set("blocks." + blockKey(loc), null);
        saveDataFile();
    }

    private void saveDataFile() {
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("Impossible de sauvegarder casinos.yml : " + e.getMessage()); }
    }

    // ── Jeton personnalisé ────────────────────────────────────────────────────

    /** Sauvegarde un jeton personnalisé (matériau + nom + lore + custom-model-data). */
    public void saveCustomToken(Material material, String name, List<String> lore, int customModelData) {
        dataConfig.set("custom-token.material",          material.name());
        dataConfig.set("custom-token.name",              name);
        dataConfig.set("custom-token.lore",              lore);
        dataConfig.set("custom-token.custom-model-data", customModelData);
        saveDataFile();
    }

    /** Supprime la définition du jeton personnalisé (retour aux valeurs de config.yml). */
    public void resetCustomToken() {
        dataConfig.set("custom-token", null);
        saveDataFile();
    }

    /** @return {@code true} si un jeton personnalisé est enregistré. */
    public boolean hasCustomToken() {
        return dataConfig.contains("custom-token.material");
    }

    public Material getCustomTokenMaterial() {
        String matName = dataConfig.getString("custom-token.material", "GOLD_NUGGET");
        Material mat = Material.matchMaterial(matName);
        return mat != null ? mat : Material.GOLD_NUGGET;
    }

    /** Retourne le nom affiché (déjà traduit avec les codes §). */
    public String getCustomTokenName() {
        return dataConfig.getString("custom-token.name", "");
    }

    /** Retourne le lore (déjà traduit avec les codes §). */
    public List<String> getCustomTokenLore() {
        return dataConfig.getStringList("custom-token.lore");
    }

    public int getCustomTokenModelData() {
        return dataConfig.getInt("custom-token.custom-model-data", 0);
    }
}



