package fr.segame.armesia.commands;

import fr.segame.armesia.config.MobConfig;
import fr.segame.armesia.mobs.MobData;
import fr.segame.armesia.mobs.MobManager;
import fr.segame.armesia.mobs.MobSpawner;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /mob — Gestion des mobs personnalisés
 *
 *  /mob list
 *  /mob info <id>
 *  /mob create <id> <entityType> <level> <health> <moneyMin> <moneyMax> <xpMin> <xpMax> <nom...>
 *  /mob delete <id>
 *  /mob set <id> name <nom...>    ← supporte &c &l etc.
 *  /mob set <id> level <int>
 *  /mob set <id> health <double>
 *  /mob set <id> moneymin <int>
 *  /mob set <id> moneymax <int>
 *  /mob set <id> xpmin <int>
 *  /mob set <id> xpmax <int>
 *  /mob set <id> type <EntityType>
 *  /mob set <id> loot <tableId>
 *  /mob spawn <id>               ← force un spawn à votre position
 */
public class MobCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§6Mob§8] §r";
    private static final String ERR    = "§c";
    private static final String OK     = "§a";

    private final MobManager mobManager;
    private final MobConfig  mobConfig;
    private final MobSpawner mobSpawner;

    public MobCommand(MobManager mobManager, MobConfig mobConfig, MobSpawner mobSpawner) {
        this.mobManager = mobManager;
        this.mobConfig  = mobConfig;
        this.mobSpawner = mobSpawner;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "list"   -> cmdList(sender);
            case "info"   -> { if (need(sender, args, 2)) cmdInfo(sender, args[1]); }
            case "create" -> { if (need(sender, args, 10)) cmdCreate(sender, args); }
            case "delete" -> { if (need(sender, args, 2)) cmdDelete(sender, args[1]); }
            case "set"    -> { if (need(sender, args, 4)) cmdSet(sender, args); }
            case "spawn"  -> { if (need(sender, args, 2)) cmdSpawn(sender, args[1]); }
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ─── Sous-commandes ───────────────────────────────────────────────────────

    private void cmdList(CommandSender s) {
        Collection<MobData> all = mobManager.getAllMobs();
        if (all.isEmpty()) { s.sendMessage(PREFIX + "§7Aucun mob enregistré."); return; }
        s.sendMessage(PREFIX + "§e" + all.size() + " mob(s) :");
        for (MobData mob : all)
            s.sendMessage("  §7• §f" + mob.getId()
                    + " §7| " + mob.getName() + " §r"
                    + " §7| Niv.§f" + mob.getLevel()
                    + " §7| §c" + mob.getHealth() + "❤"
                    + " §7| §b" + mob.getEntityType().name());
    }

    private void cmdInfo(CommandSender s, String id) {
        MobData mob = mobManager.getMob(id);
        if (mob == null) { s.sendMessage(PREFIX + ERR + "Mob '" + id + "' introuvable."); return; }
        s.sendMessage(PREFIX + "§e=== " + mob.getId() + " ===");
        s.sendMessage("  §7Nom       : " + mob.getName());
        s.sendMessage("  §7Type      : §b" + mob.getEntityType().name());
        s.sendMessage("  §7Niveau    : §f" + mob.getLevel());
        s.sendMessage("  §7Santé     : §c" + mob.getHealth());
        s.sendMessage("  §7Argent    : §6" + mob.getMoneyMin() + " §7→ §6" + mob.getMoneyMax() + "$");
        s.sendMessage("  §7XP        : §b" + mob.getXpMin() + " §7→ §b" + mob.getXpMax() + " XP");
        s.sendMessage("  §7LootTable : §d" + (mob.getLootTable().isEmpty() ? "§7(aucune)" : mob.getLootTable()));
    }

    private void cmdCreate(CommandSender s, String[] args) {
        // /mob create <id> <type> <level> <health> <moneyMin> <moneyMax> <xpMin> <xpMax> <nom...>
        //              1     2      3       4          5          6         7       8       9+
        String id = args[1].toLowerCase();
        if (mobManager.getMob(id) != null) {
            s.sendMessage(PREFIX + ERR + "Le mob '" + id + "' existe déjà."); return;
        }
        EntityType type;
        try { type = EntityType.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { s.sendMessage(PREFIX + ERR + "Type invalide : " + args[2]); return; }

        int level, moneyMin, moneyMax, xpMin, xpMax;
        double health;
        try {
            level    = Integer.parseInt(args[3]);
            health   = Double.parseDouble(args[4]);
            moneyMin = Integer.parseInt(args[5]);
            moneyMax = Integer.parseInt(args[6]);
            xpMin    = Integer.parseInt(args[7]);
            xpMax    = Integer.parseInt(args[8]);
        } catch (NumberFormatException e) { s.sendMessage(PREFIX + ERR + "Valeurs numériques invalides."); return; }

        String name = color(buildName(args, 9));
        MobData mob = new MobData(id, name, type, level, health, moneyMin, moneyMax, xpMin, xpMax, "");
        mobManager.registerMob(mob);
        mobConfig.saveMob(mob);
        s.sendMessage(PREFIX + OK + "Mob '" + id + "' créé : " + name);
    }

    private void cmdDelete(CommandSender s, String id) {
        if (mobManager.getMob(id) == null) { s.sendMessage(PREFIX + ERR + "Mob '" + id + "' introuvable."); return; }
        mobManager.removeMob(id);
        mobConfig.deleteMob(id);
        s.sendMessage(PREFIX + OK + "Mob '" + id + "' supprimé.");
    }

    private void cmdSet(CommandSender s, String[] args) {
        String id   = args[1].toLowerCase();
        String prop = args[2].toLowerCase();
        MobData mob = mobManager.getMob(id);
        if (mob == null) { s.sendMessage(PREFIX + ERR + "Mob '" + id + "' introuvable."); return; }

        switch (prop) {
            case "name"     -> mob.setName(color(buildName(args, 3)));
            case "level"    -> { try { mob.setLevel(Integer.parseInt(args[3])); } catch (NumberFormatException e) { invalid(s); return; } }
            case "health"   -> { try { mob.setHealth(Double.parseDouble(args[3])); } catch (NumberFormatException e) { invalid(s); return; } }
            case "moneymin" -> { try { mob.setMoneyMin(Integer.parseInt(args[3])); } catch (NumberFormatException e) { invalid(s); return; } }
            case "moneymax" -> { try { mob.setMoneyMax(Integer.parseInt(args[3])); } catch (NumberFormatException e) { invalid(s); return; } }
            case "xpmin"    -> { try { mob.setXpMin(Integer.parseInt(args[3])); } catch (NumberFormatException e) { invalid(s); return; } }
            case "xpmax"    -> { try { mob.setXpMax(Integer.parseInt(args[3])); } catch (NumberFormatException e) { invalid(s); return; } }
            case "type"     -> {
                try { mob.setEntityType(EntityType.valueOf(args[3].toUpperCase())); }
                catch (IllegalArgumentException e) { s.sendMessage(PREFIX + ERR + "Type invalide : " + args[3]); return; }
            }
            case "loot"     -> mob.setLootTable(args[3]);
            default         -> { s.sendMessage(PREFIX + ERR + "Propriété inconnue : " + prop); return; }
        }
        mobConfig.saveMob(mob);
        s.sendMessage(PREFIX + OK + "Mob '" + id + "' mis à jour (§f" + prop + "§a).");
    }

    private void cmdSpawn(CommandSender s, String id) {
        if (!(s instanceof Player player)) { s.sendMessage(PREFIX + ERR + "Réservé aux joueurs."); return; }
        MobData mob = mobManager.getMob(id);
        if (mob == null) { s.sendMessage(PREFIX + ERR + "Mob '" + id + "' introuvable."); return; }
        mobSpawner.spawnMob(player.getLocation(), mob, "manual");
        s.sendMessage(PREFIX + OK + "Mob '" + id + "' spawné à votre position.");
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return filter(args[0], "list", "info", "create", "delete", "set", "spawn");
        return switch (args[0].toLowerCase()) {
            case "info", "delete", "spawn" -> args.length == 2 ? filter(args[1], mobManager.getMobIds()) : List.of();
            case "create" -> switch (args.length) {
                case 3  -> entityTypes(args[2]);
                case 4  -> List.of("<level>");
                case 5  -> List.of("<health>");
                case 6  -> List.of("<moneyMin>");
                case 7  -> List.of("<moneyMax>");
                case 8  -> List.of("<xpMin>");
                case 9  -> List.of("<xpMax>");
                default -> args.length >= 10 ? List.of("<nom...>") : List.of();
            };
            case "set" -> {
                if (args.length == 2) yield filter(args[1], mobManager.getMobIds());
                if (args.length == 3) yield filter(args[2], "name", "level", "health", "moneymin", "moneymax", "xpmin", "xpmax", "type", "loot");
                if (args.length == 4 && args[2].equalsIgnoreCase("type")) yield entityTypes(args[3]);
                yield List.of();
            }
            default -> List.of();
        };
    }

    private List<String> entityTypes(String prefix) {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isAlive)
                .map(EntityType::name)
                .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                .sorted().collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Convertit &c, &l, etc. en codes couleur Minecraft */
    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(PREFIX + "§eCommandes /mob :");
        s.sendMessage("  §f/mob list");
        s.sendMessage("  §f/mob info <id>");
        s.sendMessage("  §f/mob create <id> <type> <lvl> <hp> <moneyMin> <moneyMax> <xpMin> <xpMax> <nom...>  §7(&c pour couleurs)");
        s.sendMessage("  §f/mob delete <id>");
        s.sendMessage("  §f/mob set <id> <name|level|health|moneymin|moneymax|xpmin|xpmax|type|loot> <valeur>");
        s.sendMessage("  §f/mob spawn <id>");
    }

    private boolean need(CommandSender s, String[] args, int min) {
        if (args.length >= min) return true;
        s.sendMessage(PREFIX + ERR + "Arguments insuffisants. Tapez /mob pour l'aide.");
        return false;
    }

    private void invalid(CommandSender s) { s.sendMessage(PREFIX + ERR + "Valeur numérique invalide."); }

    private String buildName(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) { if (i > from) sb.append(" "); sb.append(args[i]); }
        return sb.toString();
    }

    private List<String> filter(String prefix, String... options) {
        return Arrays.stream(options).filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }

    private List<String> filter(String prefix, Set<String> options) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).sorted().collect(Collectors.toList());
    }
}
