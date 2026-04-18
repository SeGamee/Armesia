package fr.segame.armesiaMenu.menu;

import fr.segame.armesiaMenu.action.MenuAction;
import fr.segame.armesiaMenu.condition.Condition;
import fr.segame.armesiaMenu.placeholder.PlaceholderManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import fr.segame.armesiaMenu.condition.ConditionParser;
import fr.segame.armesiaMenu.action.CloseAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuItem {

    private final int slot;
    private final ItemStack baseItem;

    private final String name;
    private final List<Object> lore;

    private final List<MenuAction> actions;
    private final List<MenuAction> failActions;
    private final List<Condition> conditions;

    public MenuItem(int slot,
                    ItemStack baseItem,
                    String name,
                    List<Object> lore,
                    List<MenuAction> actions,
                    List<MenuAction> failActions,
                    List<Condition> conditions) {

        this.slot = slot;
        this.baseItem = baseItem;
        this.name = name;
        this.lore = lore;

        this.actions = actions != null ? actions : new ArrayList<>();
        this.failActions = failActions != null ? failActions : new ArrayList<>();
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public int getSlot() {
        return slot;
    }

    // =========================
    // 🔥 BUILD ITEM (NAME + LORE)
    // =========================
    public ItemStack build(Player player) {

        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        // NAME
        if (name != null) {
            String parsed = PlaceholderManager.parse(player, name);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', parsed));
        }

        // LORE
        List<String> finalLore = new ArrayList<>();

        if (lore != null) {
            for (Object obj : lore) {

                // ligne simple
                if (obj instanceof String line) {
                    line = PlaceholderManager.parse(player, line);
                    if (line == null) continue; // ligne masquée (top vide)
                    finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }

                // bloc conditionnel
                else if (obj instanceof Map<?, ?> rawMap) {

                    Map<String, Object> map = (Map<String, Object>) rawMap;

                    if (!map.containsKey("condition")) continue;

                    Map<String, Object> condMap =
                            (Map<String, Object>) map.get("condition");

                    Condition condition =
                            ConditionParser.parse(condMap);

                    boolean inverse =
                            map.containsKey("inverse") && Boolean.TRUE.equals(map.get("inverse"));

                    if (condition != null) {

                        boolean result = condition.isValid(player);

                        if (result != inverse) {

                            List<String> lines =
                                    (List<String>) map.get("lore");

                            if (lines != null) {
                                for (String line : lines) {
                                    line = PlaceholderManager.parse(player, line);
                                    if (line == null) continue; // ligne masquée (top vide)
                                    finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                }
                            }
                        }
                    }
                }
            }
        }

        meta.setLore(finalLore);
        item.setItemMeta(meta);

        return item;
    }

    // =========================
    // 🔥 EXECUTE (CLICK + REFRESH)
    // =========================
    public void execute(Player player, ClickType click, MenuManager manager) {

        boolean shouldRefresh = false;

        for (Condition condition : conditions) {
            if (!condition.isValid(player)) {

                for (MenuAction action : failActions) {
                    if (action.matches(click)) {
                        action.execute(player);

                        if (action.shouldRefresh()) {
                            shouldRefresh = true;
                        }
                    }
                }

                if (shouldRefresh) {
                    manager.refreshMenu(player);
                }
                return;
            }
        }

        for (MenuAction action : actions) {
            if (action.matches(click)) {
                action.execute(player);

                if (action.shouldRefresh()) {
                    shouldRefresh = true;
                }
            }
        }

        if (shouldRefresh) {
            manager.refreshMenu(player);
        }
    }
}