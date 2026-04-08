package fr.segame.armesiaMobs.commands;

import fr.segame.armesiaMobs.ArmesiaMobs;
import fr.segame.armesiaMobs.config.MessagesConfig;
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

    private final MobManager  mobManager;
    private final MobConfig   mobConfig;
    private final MobSpawner  mobSpawner;
    private final LootManager lootManager;

    private MessagesConfig msg() { return ArmesiaMobs.getInstance().getMessages(); }

    public MobCommand(MobManager mobManager, MobConfig mobConfig,
                      MobSpawner mobSpawner, LootManager lootManager) {
        this.mobManager  = mobManager;
        this.mobConfig   = mobConfig;
        this.mobSpawner  = mobSpawner;
        this.lootManager = lootManager;
    }

    // ─── Exécution ─────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "list"   -> { if (perm(sender, "armesia.mob.list"))             cmdList(sender); }
            case "info"   -> { if (perm(sender, "armesia.mob.info")   && need(sender, args, 2)) cmdInfo(sender, args[1]); }
            case "create" -> { if (perm(sender, "armesia.mob.create") && need(sender, args, 9)) cmdCreate(sender, args); }
            case "delete" -> { if (perm(sender, "armesia.mob.delete") && need(sender, args, 2)) cmdDelete(sender, args[1]); }
            case "set"    -> { if (perm(sender, "armesia.mob.set")    && need(sender, args, 4)) cmdSet(sender, args); }
            case "spawn"  -> { if (perm(sender, "armesia.mob.spawn")  && need(sender, args, 3)) cmdSpawn(sender, args); }
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ─── Sous-commandes ────────────────────────────────────────────────────

    private void cmdList(CommandSender s) {
        Collection<MobData> all = mobManager.getAllMobs();
        if (all.isEmpty()) { s.sendMessage(msg().get("mob.none")); return; }
        for (MobData mob : all) {
            s.sendMessage("§7• §f" + mob.getId()
                    + " §7| " + mob.getName()
                    + " §7| §c" + mob.getHealth() + "❤"
                    + " §7| §b" + mob.getEntityType());
        }
    }

    private void cmdInfo(CommandSender s, String id) {
        MobData mob = mobManager.getMob(id);
        if (mob == null) { s.sendMessage(msg().get("mob.not-found", "id", id)); return; }

        double base = getBaseHealth(mob.getEntityType(), s);
        s.sendMessage("§e=== " + mob.getId() + " ===");
        s.sendMessage("§7Nom: " + mob.getName());
        s.sendMessage("§7Type: §b" + mob.getEntityType());
        s.sendMessage("§7HP: §c" + mob.getHealth() + (base > 0 ? " §7(base: " + base + ")" : ""));
        s.sendMessage("§7Money: §6" + mob.getMoneyMin() + " → " + mob.getMoneyMax());
        s.sendMessage("§7XP: §b" + mob.getXpMin() + " → " + mob.getXpMax());
        String lt = mob.getLootTable();
        s.sendMessage("§7LootTable: " + (lt != null && !lt.isEmpty() ? "§e" + lt : "§8aucune"));
    }

    private void cmdCreate(CommandSender s, String[] args) {
        String id = args[1];
        EntityType type;
        try { type = EntityType.valueOf(args[2].toUpperCase()); }
        catch (Exception e) { s.sendMessage(msg().get("mob.invalid-type", "type", args[2])); return; }

        double health;
        int moneyMin, moneyMax, xpMin, xpMax;
        try {
            health   = Double.parseDouble(args[3]);
            moneyMin = Integer.parseInt(args[4]);
            moneyMax = Integer.parseInt(args[5]);
            xpMin    = Integer.parseInt(args[6]);
            xpMax    = Integer.parseInt(args[7]);
        } catch (NumberFormatException e) { s.sendMessage(msg().get("common.invalid-number")); return; }

        String name = color(buildName(args, 8));
        MobData mob = new MobData(id, name, type, health, moneyMin, moneyMax, xpMin, xpMax, "");
        mobManager.registerMob(mob);
        mobConfig.saveMob(mob);
        s.sendMessage(msg().get("mob.created", "id", id));
    }

    private void cmdSet(CommandSender s, String[] args) {
        MobData mob = mobManager.getMob(args[1]);
        if (mob == null) { s.sendMessage(msg().get("mob.not-found", "id", args[1])); return; }

        try {
            switch (args[2].toLowerCase()) {
                case "name"      -> mob.setName(color(buildName(args, 3)));
                case "health"    -> mob.setHealth(Double.parseDouble(args[3]));
                case "moneymin"  -> mob.setMoneyMin(Integer.parseInt(args[3]));
                case "moneymax"  -> mob.setMoneyMax(Integer.parseInt(args[3]));
                case "xpmin"     -> mob.setXpMin(Integer.parseInt(args[3]));
                case "xpmax"     -> mob.setXpMax(Integer.parseInt(args[3]));
                case "type"      -> mob.setEntityType(EntityType.valueOf(args[3].toUpperCase()));
                case "loottable" -> mob.setLootTable(args[3].equals("-") ? "" : args[3]);
                default -> { s.sendMessage(msg().get("common.invalid-value", "value", args[2])); return; }
            }
        } catch (Exception e) { s.sendMessage(msg().get("common.invalid-value", "value", args[3])); return; }

        mobConfig.saveMob(mob);
        s.sendMessage(msg().get("mob.modified", "id", mob.getId()));
    }

    private void cmdDelete(CommandSender s, String id) {
        if (mobManager.getMob(id) == null) { s.sendMessage(msg().get("mob.not-found", "id", id)); return; }
        mobManager.removeMob(id);
        mobConfig.deleteMob(id);
        s.sendMessage(msg().get("mob.deleted", "id", id));
    }

    private void cmdSpawn(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage(msg().get("common.player-only")); return; }
        MobData mob = mobManager.getMob(args[1]);
        if (mob == null) { s.sendMessage(msg().get("mob.not-found", "id", args[1])); return; }
        int count;
        try { count = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { s.sendMessage(msg().get("common.invalid-number")); return; }
        if (count <= 0) { s.sendMessage(msg().get("common.invalid-number")); return; }
        for (int i = 0; i < count; i++) mobSpawner.spawnMob(p.getLocation(), mob, "manual");
        s.sendMessage(msg().get("mob.spawn-ok", "count", count, "name", mob.getName()));
    }

    // ─── Tab Completion ────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return List.of("list", "info", "create", "delete", "set", "spawn");

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 3) return Arrays.stream(EntityType.values()).filter(EntityType::isAlive)
                    .map(EntityType::name).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .sorted().collect(Collectors.toList());
            if (args.length == 4) {
                if (!(sender instanceof Player player)) return List.of("20");
                try {
                    EntityType type = EntityType.valueOf(args[2].toUpperCase());
                    double baseHp = getBaseHealth(type, player);
                    if (baseHp > 0) return List.of(String.valueOf((int) baseHp));
                } catch (Exception ignored) {}
                return List.of("20");
            }
            if (args.length == 5) return List.of("<moneyMin>");
            if (args.length == 6) return List.of("<moneyMax>");
            if (args.length == 7) return List.of("<xpMin>");
            if (args.length == 8) return List.of("<xpMax>");
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2) return mobManager.getMobIds().stream().sorted().toList();
            if (args.length == 3) return List.of("name","health","moneymin","moneymax","xpmin","xpmax","type","loottable");
            if (args.length == 4 && args[2].equalsIgnoreCase("health")) {
                if (!(sender instanceof Player player)) return List.of("20");
                MobData mob = mobManager.getMob(args[1]);
                if (mob != null) { double b = getBaseHealth(mob.getEntityType(), player); if (b > 0) return List.of(String.valueOf((int) b)); }
                return List.of("20");
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("type"))
                return Arrays.stream(EntityType.values()).filter(EntityType::isAlive).map(EntityType::name).collect(Collectors.toList());
            if (args.length == 4 && args[2].equalsIgnoreCase("loottable")) {
                List<String> s2 = new ArrayList<>(lootManager.getTableIds().stream().sorted().toList());
                s2.add(0, "-"); return s2;
            }
        }
        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length == 2) return mobManager.getMobIds().stream().sorted().toList();
            if (args.length == 3) return List.of("1", "5", "10");
        }
        if ((args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete")) && args.length == 2)
            return mobManager.getMobIds().stream().sorted().toList();
        return List.of();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private boolean perm(CommandSender s, String node) {
        if (s.hasPermission(node)) return true;
        s.sendMessage(msg().get("common.no-permission"));
        return false;
    }

    private double getBaseHealth(EntityType type, CommandSender sender) {
        if (!(sender instanceof Player p)) return -1;
        try {
            LivingEntity e = (LivingEntity) p.getWorld().spawnEntity(p.getLocation(), type);
            double hp = e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            e.remove(); return hp;
        } catch (Exception ex) { return -1; }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private boolean need(CommandSender s, String[] args, int min) {
        if (args.length >= min) return true;
        s.sendMessage(msg().get("common.args-missing")); return false;
    }

    private String buildName(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§8[§6Mob§8] §r§e/mob create <id> <type> <hp> <moneyMin> <moneyMax> <xpMin> <xpMax> <nom>");
        s.sendMessage("  §f/mob list §7· info <id> §7· delete <id> §7· set <id> <prop> <val> §7· spawn <id> <n>");
    }
}

