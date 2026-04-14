package fr.segame.armesiaCasino;

import fr.segame.armesiaCasino.commands.CasinoCommand;
import fr.segame.armesiaCasino.listeners.CasinoListener;
import fr.segame.armesiaCasino.managers.CasinoManager;
import fr.segame.armesiaCasino.managers.HologramManager;
import fr.segame.armesiaCasino.managers.PrizeManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class MainCasino extends JavaPlugin {

    public static MainCasino instance;

    /** Clé PDC utilisée pour identifier un jeton de casino */
    private NamespacedKey tokenKey;

    private HologramManager      hologramManager;
    private PrizeManager         prizeManager;
    private CasinoManager        casinoManager;

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;
        tokenKey = new NamespacedKey(this, "casino_token");

        saveDefaultConfig();

        hologramManager      = new HologramManager(this);
        prizeManager         = new PrizeManager(this);
        casinoManager        = new CasinoManager(this, hologramManager);

        CasinoListener listener = new CasinoListener(this, casinoManager, prizeManager);
        getServer().getPluginManager().registerEvents(listener, this);

        CasinoCommand cmd = new CasinoCommand(this, casinoManager, prizeManager);
        getCommand("casino").setExecutor(cmd);
        getCommand("casino").setTabCompleter(cmd);

        getLogger().info("Armesia-Casino activé !");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) hologramManager.removeAll();
        getLogger().info("Armesia-Casino désactivé.");
    }

    // ── Rechargement ──────────────────────────────────────────────────────────

    public void reloadPlugin() {
        reloadConfig();
        prizeManager.loadPrizes();
        casinoManager.reloadData();
    }

    // ── Jeton de casino ───────────────────────────────────────────────────────

    /**
     * Crée un ItemStack "Jeton de Casino".
     * Si un jeton personnalisé est enregistré dans casinos.yml, il est utilisé.
     * Sinon, les valeurs de config.yml sont utilisées.
     * Dans tous les cas, la clé PDC est ajoutée pour identification fiable.
     */
    public ItemStack createCasinoToken() {
        Material     mat;
        String       name;
        List<String> lore     = new ArrayList<>();
        int          cmdValue;

        if (casinoManager != null && casinoManager.hasCustomToken()) {
            // Jeton personnalisé — nom/lore déjà traduits (viennent d'un ItemMeta)
            mat      = casinoManager.getCustomTokenMaterial();
            name     = casinoManager.getCustomTokenName();
            lore     = new ArrayList<>(casinoManager.getCustomTokenLore());
            cmdValue = casinoManager.getCustomTokenModelData();
        } else {
            // Valeurs de config.yml — traduction des codes &
            String matName = getConfig().getString("token.material", "GOLD_NUGGET");
            mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.GOLD_NUGGET;
            name = ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("token.name", "&6✦ Jeton de Casino ✦"));
            for (String l : getConfig().getStringList("token.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            cmdValue = getConfig().getInt("token.custom-model-data", 0);
        }

        ItemStack token = new ItemStack(mat);
        ItemMeta  meta  = token.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            if (cmdValue > 0) meta.setCustomModelData(cmdValue);
            // Marqueur PDC — identification sans ambiguïté
            meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.BYTE, (byte) 1);
            token.setItemMeta(meta);
        }
        return token;
    }

    /**
     * @return {@code true} si l'item est un jeton de casino (vérifié via PDC).
     */
    public boolean isCasinoToken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(tokenKey, PersistentDataType.BYTE);
    }

    /**
     * Retourne le nom affiché du jeton (personnalisé ou config.yml), sans créer d'ItemStack.
     */
    public String getTokenDisplayName() {
        if (casinoManager != null && casinoManager.hasCustomToken()) {
            return casinoManager.getCustomTokenName();
        }
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("token.name", "&6✦ Jeton de Casino ✦"));
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public HologramManager getHologramManager() { return hologramManager; }
    public CasinoManager   getCasinoManager()   { return casinoManager; }
    public PrizeManager    getPrizeManager()    { return prizeManager; }
}






