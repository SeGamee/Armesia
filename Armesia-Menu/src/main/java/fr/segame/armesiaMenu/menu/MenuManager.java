package fr.segame.armesiaMenu.menu;

import fr.segame.armesiaMenu.action.*;
import fr.segame.armesiaMenu.condition.*;
import fr.segame.armesiaMenu.reward.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class MenuManager {

    private final JavaPlugin plugin;

    private final Map<String, Menu> menus = new HashMap<>();
    private final Map<Player, Menu> openMenus = new HashMap<>();
    private final Set<Player> refreshing = new HashSet<>();

    private final RewardManager rewardManager;

    public MenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rewardManager = new RewardManager(plugin);
    }

    // =========================
    // 🔥 OPEN MENU (WORLD CHECK)
    // =========================

    public void openMenu(Player player, Menu menu) {

        String world = player.getWorld().getName();

        if (menu.getAllowedWorlds() != null && !menu.getAllowedWorlds().isEmpty()) {
            if (!menu.getAllowedWorlds().contains(world)) {
                player.sendMessage("§cMenu indisponible ici");
                return;
            }
        }

        if (menu.getBlockedWorlds() != null && menu.getBlockedWorlds().contains(world)) {
            player.sendMessage("§cMenu bloqué dans ce monde");
            return;
        }

        player.openInventory(menu.createInventory(player));
        openMenus.put(player, menu);
    }

    public Menu getOpenMenu(Player player) {
        return openMenus.get(player);
    }

    public void closeMenu(Player player) {
        openMenus.remove(player);
    }

    public boolean isRefreshing(Player player) {
        return refreshing.contains(player);
    }

    // =========================
    // 🔄 RELOAD
    // =========================

    public void reloadMenus() {

        for (Player player : new ArrayList<>(openMenus.keySet())) {
            player.closeInventory();
        }

        openMenus.clear();
        loadMenus();

        Bukkit.getLogger().info("[Armesia-Menu] Menus rechargés !");
    }

    // =========================
    // 🔄 REFRESH
    // =========================

    public void refreshMenu(Player player) {

        Menu menu = openMenus.get(player);
        if (menu == null) return;

        refreshing.add(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                refreshing.remove(player);
                return;
            }

            player.openInventory(menu.createInventory(player));
            openMenus.put(player, menu);
            refreshing.remove(player);
        }, 1L);
    }

    // =========================
    // 📦 LOAD MENUS
    // =========================

    public void loadMenus() {
        menus.clear();

        File file = new File(plugin.getDataFolder(), "menus.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection menusSection = config.getConfigurationSection("menus");
        if (menusSection == null) return;

        for (String menuId : menusSection.getKeys(false)) {

            ConfigurationSection menuSec = menusSection.getConfigurationSection(menuId);

            String title = ChatColor.translateAlternateColorCodes('&', menuSec.getString("title"));
            int size = menuSec.getInt("size");

            List<String> allowedWorlds = menuSec.getStringList("worlds.allowed");
            List<String> blockedWorlds = menuSec.getStringList("worlds.blocked");

            Menu menu = new Menu(menuId, title, size, allowedWorlds, blockedWorlds);

            ConfigurationSection itemsSec = menuSec.getConfigurationSection("items");

            if (itemsSec != null) {
                for (String itemId : itemsSec.getKeys(false)) {

                    ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemId);

                    // ===== ITEM =====
                    Material mat = Material.valueOf(itemSec.getString("material"));
                    int slot = itemSec.getInt("slot");

                    ItemStack item = new ItemStack(mat);
                    ItemMeta meta = item.getItemMeta();
                    item.setItemMeta(meta);

                    // ===== NAME =====
                    String name = itemSec.getString("name");

                    // ===== LORE MIXTE =====
                    List<?> rawLore = itemSec.getList("lore");
                    List<Object> lore = rawLore != null ? new ArrayList<>(rawLore) : null;

                    // ===== ACTIONS =====
                    List<MenuAction> actions = new ArrayList<>();
                    List<MenuAction> failActions = new ArrayList<>();
                    List<Condition> conditions = new ArrayList<>();

                    if (itemSec.contains("actions")) {
                        for (Map<?, ?> map : itemSec.getMapList("actions")) {
                            MenuAction action = parseAction(map);
                            if (action != null) actions.add(action);
                        }
                    }

                    if (itemSec.contains("fail-actions")) {
                        for (Map<?, ?> map : itemSec.getMapList("fail-actions")) {
                            MenuAction action = parseAction(map);
                            if (action != null) failActions.add(action);
                        }
                    }

                    if (itemSec.contains("conditions")) {
                        for (Map<?, ?> map : itemSec.getMapList("conditions")) {
                            Condition condition = parseCondition((Map<String, Object>) map);
                            if (condition != null) conditions.add(condition);
                        }
                    }

                    menu.addItem(new MenuItem(
                            slot,
                            item,
                            name,
                            lore,
                            actions,
                            failActions,
                            conditions
                    ));
                }
            }

            menus.put(menuId, menu);
        }
    }

    // =========================
    // ⚙️ ACTION PARSER
    // =========================

    private MenuAction parseAction(Map<?, ?> map) {

        String type = (String) map.get("type");

        switch (type) {

            case "message":
                return new MessageAction(
                        (String) map.get("message"),
                        parseClicks(map)
                );

            case "open_menu":
                return new OpenMenuAction(this, (String) map.get("menu"));

            case "close":
                return new CloseAction(this, parseClicks(map));

            case "command": {

                boolean console = map.containsKey("console") && (boolean) map.get("console");

                return new CommandAction(
                        (String) map.get("command"),
                        console,
                        parseClicks(map)
                );
            }

            case "add_money": {
                Object amountObj = map.get("amount");
                double amount = amountObj != null ? ((Number) amountObj).doubleValue() : 0;

                return new AddMoneyAction(amount, parseClicks(map));
            }

            case "take_money": {
                Object amountObj = map.get("amount");
                double amount = amountObj != null ? ((Number) amountObj).doubleValue() : 0;

                return new TakeMoneyAction(amount, parseClicks(map));
            }

            case "add_tokens": {
                int amount = ((Number) map.get("amount")).intValue();
                return new AddTokensAction(amount, parseClicks(map));
            }

            case "take_tokens": {
                int amount = ((Number) map.get("amount")).intValue();
                return new TakeTokensAction(amount, parseClicks(map));
            }

            case "give_item": {
                Object amountObj = map.get("amount");
                int amount = amountObj != null ? ((Number) amountObj).intValue() : 1;

                return new GiveItemAction(
                        Material.valueOf((String) map.get("material")),
                        amount,
                        parseClicks(map)
                );
            }

            case "refresh":
                return new RefreshMenuAction(this, parseClicks(map));

            case "mark_claimed":
                return new MarkClaimedAction(
                        (String) map.get("key"),
                        rewardManager
                );

            }

        return null;
    }

    // =========================
    // 🧠 CONDITION PARSER
    // =========================

    private Condition parseCondition(Map<String, Object> map) {

        String type = (String) map.get("type");

        switch (type) {

            case "money":
                return new MoneyCondition(((Number) map.get("min")).doubleValue());

            case "tokens":
                return new TokensCondition(
                        ((Number) map.get("min")).doubleValue()
                );

            case "level":
                return new LevelCondition(((Number) map.get("min")).intValue());

            case "permission":
                return new PermissionCondition(
                        (String) map.get("permission")
                );

            case "not_claimed":
                return new NotClaimedCondition(
                        (String) map.get("key"),
                        rewardManager
                );

        }

        return null;
    }

    // =========================
    // 🖱️ CLICK PARSER
    // =========================

    private Set<ClickType> parseClicks(Map<?, ?> map) {

        if (!map.containsKey("click")) return null;

        Object obj = map.get("click");

        Set<ClickType> clicks = new HashSet<>();

        if (obj instanceof String str) {
            clicks.add(ClickType.valueOf(str.toUpperCase()));
        }

        if (obj instanceof List<?> list) {
            for (Object o : list) {
                clicks.add(ClickType.valueOf(o.toString().toUpperCase()));
            }
        }

        return clicks;
    }

    // =========================
    // 📦 GETTERS
    // =========================

    public Menu getMenu(String id) {
        return menus.get(id);
    }

    public Collection<Menu> getMenus() {
        return menus.values();
    }
}