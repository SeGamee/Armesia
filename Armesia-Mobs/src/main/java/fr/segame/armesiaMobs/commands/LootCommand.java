package fr.segame.armesiaMobs.commands;

import fr.segame.armesiaMobs.config.LootConfig;
import fr.segame.armesiaMobs.loot.LootData;
import fr.segame.armesiaMobs.loot.LootManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /loot — Gestion des tables de loot
 *
 *  /loot list
 *  /loot info <tableId>
 *  /loot create <tableId>
 *  /loot delete <tableId>
 *  /loot add    <tableId> <material> <min> <max> <chance(0-1)>
 *  /loot addhand <tableId> <min> <max> <chance>   ← item en main
 *  /loot remove  <tableId> <index(1-based)>
 */
public class LootCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§dLoot§8] §r";
    private static final String ERR    = "§c";
    private static final String OK     = "§a";

    private final LootManager lootManager;
    private final LootConfig  lootConfig;

    public LootCommand(LootManager lootManager, LootConfig lootConfig) {
        this.lootManager = lootManager;
        this.lootConfig  = lootConfig;
    }

    // ─── Exécution ────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "list"    -> cmdList(sender);
            case "info"    -> { if (need(sender, args, 2)) cmdInfo(sender, args[1]); }
            case "create"  -> { if (need(sender, args, 2)) cmdCreate(sender, args[1]); }
            case "delete"  -> { if (need(sender, args, 2)) cmdDelete(sender, args[1]); }
            case "add"     -> { if (need(sender, args, 6)) cmdAdd(sender, args); }
            case "addhand" -> { if (need(sender, args, 5)) cmdAddHand(sender, args); }
            case "remove"  -> { if (need(sender, args, 3)) cmdRemove(sender, args); }
            default        -> sendHelp(sender);
        }
        return true;
    }

    // ─── Sous-commandes ───────────────────────────────────────────────────────

    private void cmdList(CommandSender s) {
        Set<String> ids = lootManager.getTableIds();
        if (ids.isEmpty()) { s.sendMessage(PREFIX + "§7Aucune table de loot."); return; }
        s.sendMessage(PREFIX + "§e" + ids.size() + " table(s) :");
        for (String id : ids) {
            int count = lootManager.getTable(id).size();
            s.sendMessage("  §7• §f" + id + " §7(" + count + " entrée(s))");
        }
    }

    private void cmdInfo(CommandSender s, String tableId) {
        List<LootData> loots = lootManager.getTable(tableId);
        if (loots == null) { s.sendMessage(PREFIX + ERR + "Table '" + tableId + "' introuvable."); return; }
        if (loots.isEmpty()) { s.sendMessage(PREFIX + "§7Table '" + tableId + "' est vide."); return; }
        s.sendMessage(PREFIX + "§e=== " + tableId + " ===");
        for (int i = 0; i < loots.size(); i++) {
            LootData l = loots.get(i);
            String chance = String.format("%.0f%%", l.getChance() * 100);
            s.sendMessage("  §7[" + (i + 1) + "] §f" + l.getDisplayName()
                    + " §7x" + l.getMin() + "-" + l.getMax()
                    + " §e" + chance
                    + (l.isCustomItem() ? " §b[Custom]" : ""));
        }
    }

    private void cmdCreate(CommandSender s, String tableId) {
        if (lootManager.getTable(tableId) != null) {
            s.sendMessage(PREFIX + ERR + "La table '" + tableId + "' existe déjà."); return;
        }
        lootManager.createTable(tableId);
        lootConfig.saveTable(tableId);
        s.sendMessage(PREFIX + OK + "Table '" + tableId + "' créée.");
    }

    private void cmdDelete(CommandSender s, String tableId) {
        if (lootManager.getTable(tableId) == null) {
            s.sendMessage(PREFIX + ERR + "Table '" + tableId + "' introuvable."); return;
        }
        lootManager.removeTable(tableId);
        lootConfig.deleteTable(tableId);
        s.sendMessage(PREFIX + OK + "Table '" + tableId + "' supprimée.");
    }

    private void cmdAdd(CommandSender s, String[] args) {
        // /loot add <tableId> <material> <min> <max> <chance>
        String tableId = args[1];
        Material mat;
        try { mat = Material.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { s.sendMessage(PREFIX + ERR + "Material invalide : " + args[2]); return; }

        int min, max;
        double chance;
        try {
            min    = Integer.parseInt(args[3]);
            max    = Integer.parseInt(args[4]);
            chance = Double.parseDouble(args[5]);
        } catch (NumberFormatException e) { s.sendMessage(PREFIX + ERR + "Valeurs numériques invalides."); return; }

        if (chance < 0 || chance > 1) { s.sendMessage(PREFIX + ERR + "La chance doit être entre 0.0 et 1.0"); return; }
        if (min > max)                 { s.sendMessage(PREFIX + ERR + "min doit être ≤ max."); return; }

        LootData loot = new LootData(mat, min, max, chance);
        lootManager.addEntry(tableId, loot);
        lootConfig.saveTable(tableId);
        s.sendMessage(PREFIX + OK + "Entrée ajoutée à '" + tableId + "' : §f" + mat.name()
                + " §7x" + min + "-" + max + " §e" + String.format("%.0f%%", chance * 100));
    }

    private void cmdAddHand(CommandSender s, String[] args) {
        // /loot addhand <tableId> <min> <max> <chance>
        if (!(s instanceof Player player)) { s.sendMessage(PREFIX + ERR + "Commande réservée aux joueurs."); return; }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) { s.sendMessage(PREFIX + ERR + "Vous ne tenez rien en main."); return; }

        String tableId = args[1];
        int min, max;
        double chance;
        try {
            min    = Integer.parseInt(args[2]);
            max    = Integer.parseInt(args[3]);
            chance = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) { s.sendMessage(PREFIX + ERR + "Valeurs numériques invalides."); return; }

        if (chance < 0 || chance > 1) { s.sendMessage(PREFIX + ERR + "La chance doit être entre 0.0 et 1.0."); return; }
        if (min > max)                 { s.sendMessage(PREFIX + ERR + "min doit être ≤ max."); return; }

        // Clone l'item en main (quantité = 1 en template)
        ItemStack template = hand.clone();
        template.setAmount(1);

        LootData loot = new LootData(template, min, max, chance);
        lootManager.addEntry(tableId, loot);
        lootConfig.saveTable(tableId);
        s.sendMessage(PREFIX + OK + "Item §f" + loot.getDisplayName()
                + " §aajouté à '" + tableId + "' §7x" + min + "-" + max
                + " §e" + String.format("%.0f%%", chance * 100) + " §b[Custom]");
    }

    private void cmdRemove(CommandSender s, String[] args) {
        // /loot remove <tableId> <index 1-based>
        String tableId = args[1];
        if (lootManager.getTable(tableId) == null) {
            s.sendMessage(PREFIX + ERR + "Table '" + tableId + "' introuvable."); return;
        }
        int index;
        try { index = Integer.parseInt(args[2]) - 1; }
        catch (NumberFormatException e) { s.sendMessage(PREFIX + ERR + "Index invalide."); return; }

        if (!lootManager.removeEntry(tableId, index)) {
            s.sendMessage(PREFIX + ERR + "Index hors limites."); return;
        }
        lootConfig.saveTable(tableId);
        s.sendMessage(PREFIX + OK + "Entrée #" + (index + 1) + " supprimée de '" + tableId + "'.");
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

    private List<String> materials(String prefix) {
        return Arrays.stream(Material.values())
                .filter(m -> !m.isAir() && m.isItem())
                .map(Material::name)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .limit(30)
                .collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender s) {
        s.sendMessage(PREFIX + "§eCommandes /loot :");
        s.sendMessage("  §f/loot list");
        s.sendMessage("  §f/loot info <table>");
        s.sendMessage("  §f/loot create <table>");
        s.sendMessage("  §f/loot delete <table>");
        s.sendMessage("  §f/loot add <table> <material> <min> <max> <chance>");
        s.sendMessage("  §f/loot addhand <table> <min> <max> <chance>  §7(item en main)");
        s.sendMessage("  §f/loot remove <table> <index>");
    }

    private boolean need(CommandSender s, String[] args, int min) {
        if (args.length >= min) return true;
        s.sendMessage(PREFIX + ERR + "Arguments insuffisants. Tapez /loot pour l'aide.");
        return false;
    }

    private List<String> filter(String prefix, String... options) {
        return Arrays.stream(options)
                .filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filter(String prefix, Set<String> options) {
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}


