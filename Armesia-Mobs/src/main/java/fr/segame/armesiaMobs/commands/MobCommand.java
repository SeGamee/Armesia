package fr.segame.armesiaMobs.commands;

import fr.segame.armesiaMobs.config.MobConfig;
import fr.segame.armesiaMobs.loot.LootManager;
import fr.segame.armesiaMobs.mobs.MobData;
import fr.segame.armesiaMobs.mobs.MobManager;
import fr.segame.armesiaMobs.mobs.MobSpawner;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MobCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§6Mob§8] §r";
    private static final String ERR    = "§c";
    private static final String OK     = "§a";

    private final MobManager mobManager;
    private final MobConfig mobConfig;
    private final MobSpawner mobSpawner;
    private final LootManager lootManager;

    public MobCommand(MobManager mobManager, MobConfig mobConfig, MobSpawner mobSpawner, LootManager lootManager) {
        this.mobManager = mobManager;
        this.mobConfig = mobConfig;
        this.mobSpawner = mobSpawner;
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "list"   -> cmdList(sender);
            case "info"   -> { if (need(sender, args, 2)) cmdInfo(sender, args[1]); }
            case "create" -> { if (need(sender, args, 9)) cmdCreate(sender, args); }
            case "delete" -> { if (need(sender, args, 2)) cmdDelete(sender, args[1]); }
            case "set"    -> { if (need(sender, args, 4)) cmdSet(sender, args); }
            case "spawn"  -> { if (need(sender, args, 3)) cmdSpawn(sender, args); }
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ─── COMMANDES ─────────────────────────────────

    private void cmdList(CommandSender s) {
        Collection<MobData> all = mobManager.getAllMobs();
        if (all.isEmpty()) {
            s.sendMessage(PREFIX + "§7Aucun mob.");
            return;
        }

        for (MobData mob : all) {
            s.sendMessage("§7• §f" + mob.getId()
                    + " §7| " + mob.getName()
                    + " §7| §c" + mob.getHealth() + "❤"
                    + " §7| §b" + mob.getEntityType());
        }
    }

    private void cmdInfo(CommandSender s, String id) {
        MobData mob = mobManager.getMob(id);
        if (mob == null) {
            s.sendMessage(PREFIX + ERR + "Introuvable.");
            return;
        }

        double base = getBaseHealth(mob.getEntityType(), s);

        s.sendMessage("§e=== " + mob.getId() + " ===");
        s.sendMessage("§7Nom: " + mob.getName());
        s.sendMessage("§7Type: §b" + mob.getEntityType());
        s.sendMessage("§7HP: §c" + mob.getHealth()
                + (base > 0 ? " §7(base: " + base + ")" : ""));
        s.sendMessage("§7Money: §6" + mob.getMoneyMin() + " → " + mob.getMoneyMax());
        s.sendMessage("§7XP: §b" + mob.getXpMin() + " → " + mob.getXpMax());
        String lt = mob.getLootTable();
        s.sendMessage("§7LootTable: " + (lt != null && !lt.isEmpty() ? "§e" + lt : "§8aucune"));
    }

    private void cmdCreate(CommandSender s, String[] args) {

        String id = args[1];
        EntityType type;

        try { type = EntityType.valueOf(args[2].toUpperCase()); }
        catch (Exception e) { s.sendMessage("Type invalide"); return; }

        double health = Double.parseDouble(args[3]);
        int moneyMin = Integer.parseInt(args[4]);
        int moneyMax = Integer.parseInt(args[5]);
        int xpMin = Integer.parseInt(args[6]);
        int xpMax = Integer.parseInt(args[7]);

        String name = color(buildName(args, 8));

        MobData mob = new MobData(id, name, type, health, moneyMin, moneyMax, xpMin, xpMax, "");
        mobManager.registerMob(mob);
        mobConfig.saveMob(mob);

        s.sendMessage(PREFIX + OK + "Mob créé.");
    }

    private void cmdSet(CommandSender s, String[] args) {

        MobData mob = mobManager.getMob(args[1]);
        if (mob == null) return;

        switch (args[2].toLowerCase()) {
            case "name" -> mob.setName(color(buildName(args, 3)));
            case "health" -> mob.setHealth(Double.parseDouble(args[3]));
            case "moneymin" -> mob.setMoneyMin(Integer.parseInt(args[3]));
            case "moneymax" -> mob.setMoneyMax(Integer.parseInt(args[3]));
            case "xpmin" -> mob.setXpMin(Integer.parseInt(args[3]));
            case "xpmax" -> mob.setXpMax(Integer.parseInt(args[3]));
            case "type" -> mob.setEntityType(EntityType.valueOf(args[3].toUpperCase()));
            case "loottable" -> mob.setLootTable(args[3].equals("-") ? "" : args[3]);
        }

        mobConfig.saveMob(mob);
        s.sendMessage("§aModifié.");
    }

    private void cmdDelete(CommandSender s, String id) {
        mobManager.removeMob(id);
        mobConfig.deleteMob(id);
        s.sendMessage("§aSupprimé.");
    }

    private void cmdSpawn(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) return;

        MobData mob = mobManager.getMob(args[1]);
        if (mob == null) { s.sendMessage(PREFIX + ERR + "Mob introuvable."); return; }

        int count;
        try { count = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { s.sendMessage(PREFIX + ERR + "Nombre invalide."); return; }

        if (count <= 0) { s.sendMessage(PREFIX + ERR + "Le nombre doit être supérieur à 0."); return; }

        for (int i = 0; i < count; i++) {
            mobSpawner.spawnMob(p.getLocation(), mob, "manual");
        }

        s.sendMessage(PREFIX + OK + count + " §f" + mob.getName() + " §aspawné(s).");
    }

    // ─── TAB COMPLETER ─────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (args.length == 1)
            return List.of("list", "info", "create", "delete", "set", "spawn");

        // ───────── CREATE ─────────
        if (args[0].equalsIgnoreCase("create")) {

            // TYPE
            if (args.length == 3) {
                return Arrays.stream(EntityType.values())
                        .filter(EntityType::isAlive)
                        .map(EntityType::name)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            }

            // 💥 HP INTELLIGENT
            if (args.length == 4) {

                if (!(sender instanceof Player player)) {
                    return List.of("20");
                }

                try {
                    EntityType type = EntityType.valueOf(args[2].toUpperCase());
                    double baseHp = getBaseHealth(type, player);

                    if (baseHp > 0) {
                        return List.of(String.valueOf((int) baseHp));
                    }

                } catch (Exception ignored) {}

                return List.of("20");
            }

            if (args.length == 5) return List.of("<moneyMin>");
            if (args.length == 6) return List.of("<moneyMax>");
            if (args.length == 7) return List.of("<xpMin>");
            if (args.length == 8) return List.of("<xpMax>");
        }

        // ───────── SET ─────────
        if (args[0].equalsIgnoreCase("set")) {

            if (args.length == 2)
                return mobManager.getMobIds().stream().sorted().toList();

            if (args.length == 3)
                return List.of("name", "health", "moneymin", "moneymax", "xpmin", "xpmax", "type", "loottable");

            // 💥 HP basé sur le mob existant
            if (args.length == 4 && args[2].equalsIgnoreCase("health")) {

                if (!(sender instanceof Player player)) {
                    return List.of("20");
                }

                MobData mob = mobManager.getMob(args[1]);
                if (mob != null) {
                    double baseHp = getBaseHealth(mob.getEntityType(), player);

                    if (baseHp > 0) {
                        return List.of(String.valueOf((int) baseHp));
                    }
                }

                return List.of("20");
            }

            if (args.length == 4 && args[2].equalsIgnoreCase("type")) {
                return Arrays.stream(EntityType.values())
                        .filter(EntityType::isAlive)
                        .map(EntityType::name)
                        .collect(Collectors.toList());
            }

            if (args.length == 4 && args[2].equalsIgnoreCase("loottable")) {
                List<String> suggestions = new ArrayList<>(lootManager.getTableIds().stream().sorted().toList());
                suggestions.add(0, "-");   // "-" = retirer la loot table
                return suggestions;
            }
        }

        // ───────── SPAWN ─────────
        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length == 2)
                return mobManager.getMobIds().stream().sorted().toList();
            if (args.length == 3)
                return List.of("1", "5", "10");
        }

        return List.of();
    }

    // ─── UTILS ─────────────────────────────────────

    private double getBaseHealth(EntityType type, CommandSender sender) {
        if (!(sender instanceof Player p)) return -1;

        try {
            LivingEntity e = (LivingEntity) p.getWorld().spawnEntity(p.getLocation(), type);
            double hp = e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            e.remove();
            return hp;
        } catch (Exception ex) {
            return -1;
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private boolean need(CommandSender s, String[] args, int min) {
        if (args.length >= min) return true;
        s.sendMessage("§cArguments manquants");
        return false;
    }

    private String buildName(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("/mob create <id> <type> <hp> <moneyMin> <moneyMax> <xpMin> <xpMax> <nom>");
    }
}