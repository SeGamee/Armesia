package fr.segame.armesiaMobs.commands;

import fr.segame.armesiaMobs.ArmesiaMobs;
import fr.segame.armesiaMobs.config.LootConfig;
import fr.segame.armesiaMobs.config.MessagesConfig;
import fr.segame.armesiaMobs.loot.LootData;
import fr.segame.armesiaMobs.loot.LootManager;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class LootCommand implements CommandExecutor, TabCompleter {

    private final LootManager lootManager;
    private final LootConfig  lootConfig;

    private MessagesConfig msg() { return ArmesiaMobs.getInstance().getMessages(); }

    public LootCommand(LootManager lootManager, LootConfig lootConfig) {
        this.lootManager = lootManager;
        this.lootConfig  = lootConfig;
    }

    // ─── Exécution ────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "list"    -> { if (perm(sender, "armesia.loot.list"))                              cmdList(sender); }
            case "info"    -> { if (perm(sender, "armesia.loot.info")   && need(sender, args, 2))  cmdInfo(sender, args[1]); }
            case "create"  -> { if (perm(sender, "armesia.loot.create") && need(sender, args, 2))  cmdCreate(sender, args[1]); }
            case "delete"  -> { if (perm(sender, "armesia.loot.delete") && need(sender, args, 2))  cmdDelete(sender, args[1]); }
            case "add"     -> { if (perm(sender, "armesia.loot.add")    && need(sender, args, 6))  cmdAdd(sender, args); }
            case "addhand" -> { if (perm(sender, "armesia.loot.add")    && need(sender, args, 5))  cmdAddHand(sender, args); }
            case "remove"  -> { if (perm(sender, "armesia.loot.remove") && need(sender, args, 3))  cmdRemove(sender, args); }
            default        -> sendHelp(sender);
        }
        return true;
    }

    // ─── Sous-commandes ───────────────────────────────────────────────────────

    private void cmdList(CommandSender s) {
        Set<String> ids = lootManager.getTableIds();
        if (ids.isEmpty()) { s.sendMessage(msg().get("loot.none")); return; }
        s.sendMessage("§8[§dLoot§8] §r§e" + ids.size() + " table(s) :");
        for (String id : ids) {
            int count = lootManager.getTable(id).size();
            s.sendMessage("  §7• §f" + id + " §7(" + count + " entrée(s))");
        }
    }

    private void cmdInfo(CommandSender s, String tableId) {
        List<LootData> loots = lootManager.getTable(tableId);
        if (loots == null) { s.sendMessage(msg().get("loot.not-found", "table", tableId)); return; }
        if (loots.isEmpty()) { s.sendMessage(msg().get("loot.empty", "table", tableId)); return; }
        s.sendMessage("§8[§dLoot§8] §r§e=== " + tableId + " ===");
        for (int i = 0; i < loots.size(); i++) {
            LootData l = loots.get(i);
            s.sendMessage("  §7[" + (i + 1) + "] §f" + l.getDisplayName()
                    + " §7×" + l.getMin() + "-" + l.getMax()
                    + " §e" + formatChance(l.getChance())
                    + (l.isCustomItem() ? " §b[Custom]" : ""));
        }
    }

    private void cmdCreate(CommandSender s, String tableId) {
        if (lootManager.getTable(tableId) != null) {
            s.sendMessage(msg().get("loot.exists", "table", tableId)); return;
        }
        lootManager.createTable(tableId);
        lootConfig.saveTable(tableId);
        s.sendMessage(msg().get("loot.created", "table", tableId));
    }

    private void cmdDelete(CommandSender s, String tableId) {
        if (lootManager.getTable(tableId) == null) {
            s.sendMessage(msg().get("loot.not-found", "table", tableId)); return;
        }
        lootManager.removeTable(tableId);
        lootConfig.deleteTable(tableId);
        s.sendMessage(msg().get("loot.deleted", "table", tableId));
    }

    private void cmdAdd(CommandSender s, String[] args) {
        String tableId = args[1];
        Material mat;
        try { mat = Material.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { s.sendMessage(msg().get("loot.material-invalid", "material", args[2])); return; }

        int min, max; double chance;
        try { min = Integer.parseInt(args[3]); max = Integer.parseInt(args[4]); chance = Double.parseDouble(args[5]); }
        catch (NumberFormatException e) { s.sendMessage(msg().get("common.invalid-number")); return; }

        if (chance < 0 || chance > 1) { s.sendMessage(msg().get("loot.chance-invalid")); return; }
        if (min > max)                 { s.sendMessage(msg().get("loot.min-max-invalid")); return; }

        LootData loot = new LootData(mat, min, max, chance);
        lootManager.addEntry(tableId, loot);
        lootConfig.saveTable(tableId);
        s.sendMessage(msg().get("loot.entry-added",
                "table", tableId, "item", mat.name(), "min", min, "max", max, "chance", formatChance(chance)));
    }

    private void cmdAddHand(CommandSender s, String[] args) {
        if (!(s instanceof Player player)) { s.sendMessage(msg().get("common.player-only")); return; }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) { s.sendMessage(msg().get("loot.hand-empty")); return; }

        String tableId = args[1];
        int min, max; double chance;
        try { min = Integer.parseInt(args[2]); max = Integer.parseInt(args[3]); chance = Double.parseDouble(args[4]); }
        catch (NumberFormatException e) { s.sendMessage(msg().get("common.invalid-number")); return; }

        if (chance < 0 || chance > 1) { s.sendMessage(msg().get("loot.chance-invalid")); return; }
        if (min > max)                 { s.sendMessage(msg().get("loot.min-max-invalid")); return; }

        ItemStack template = hand.clone(); template.setAmount(1);
        LootData loot = new LootData(template, min, max, chance);
        lootManager.addEntry(tableId, loot);
        lootConfig.saveTable(tableId);
        s.sendMessage(msg().get("loot.entry-added-custom",
                "table", tableId, "item", loot.getDisplayName(), "min", min, "max", max, "chance", formatChance(chance)));
    }

    private void cmdRemove(CommandSender s, String[] args) {
        String tableId = args[1];
        if (lootManager.getTable(tableId) == null) {
            s.sendMessage(msg().get("loot.not-found", "table", tableId)); return;
        }
        int index;
        try { index = Integer.parseInt(args[2]) - 1; }
        catch (NumberFormatException e) { s.sendMessage(msg().get("common.invalid-number")); return; }

        if (!lootManager.removeEntry(tableId, index)) {
            s.sendMessage(msg().get("loot.index-invalid")); return;
        }
        lootConfig.saveTable(tableId);
        s.sendMessage(msg().get("loot.entry-removed", "table", tableId, "index", index + 1));
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return filter(args[0], "list", "info", "create", "delete", "add", "addhand", "remove");

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "info", "delete", "remove", "add", "addhand" -> {
                if (args.length == 2) yield filter(args[1], lootManager.getTableIds());
                if (sub.equals("add") && args.length == 3) yield materials(args[2]);
                yield List.of();
            }
            default -> List.of();
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean perm(CommandSender s, String node) {
        if (s.hasPermission(node)) return true;
        s.sendMessage(msg().get("common.no-permission"));
        return false;
    }

    private boolean need(CommandSender s, String[] args, int min) {
        if (args.length >= min) return true;
        s.sendMessage(msg().get("common.args-missing")); return false;
    }

    private static String formatChance(double chance) {
        double pct = chance * 100.0;
        if (pct == 0.0)  return "0%";
        if (pct >= 10.0) return String.format("%.0f%%", pct);
        if (pct >= 1.0)  return String.format("%.1f%%", pct);
        if (pct >= 0.1)  return String.format("%.2f%%", pct);
        return String.format("%.3f%%", pct);
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§8[§dLoot§8] §r§e/loot list §7· create <t> §7· delete <t> §7· info <t>");
        s.sendMessage("  §f/loot add <t> <mat> <min> <max> <chance>  §7· addhand <t> <min> <max> <chance>");
        s.sendMessage("  §f/loot remove <t> <index>");
    }

    private List<String> materials(String prefix) {
        return Arrays.stream(Material.values()).filter(m -> !m.isAir() && m.isItem())
                .map(Material::name).filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted().limit(30).collect(Collectors.toList());
    }

    private List<String> filter(String prefix, String... options) {
        return Arrays.stream(options).filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }

    private List<String> filter(String prefix, Set<String> options) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).sorted().collect(Collectors.toList());
    }
}
