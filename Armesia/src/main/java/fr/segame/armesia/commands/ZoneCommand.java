package fr.segame.armesia.commands;

import fr.segame.armesia.config.ZoneConfig;
import fr.segame.armesia.managers.DebugManager;
import fr.segame.armesia.mobs.MobManager;
import fr.segame.armesia.zones.SpawnCondition;
import fr.segame.armesia.zones.ZoneData;
import fr.segame.armesia.zones.ZoneManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /zone — Gestion des zones de spawn
 *
 *  /zone list | info <id> | create <id> | delete <id>
 *  /zone pos1|pos2 <id>
 *  /zone tp <id>
 *  /zone check
 *  /zone debug [off|normal|verbose]
 *  /zone addmob <id> <mobId> | removemob <id> <mobId>
 *  /zone set <id> <prop> <val>
 *    — Spawn     : max · spawnmin · spawnmax · targetmin · targetmax
 *                  spawninterval · spawnchance · condition · weather
 *    — Despawn   : despawn · despawnclose · despawnmid · despawnfar · despawnouter
 *    — Général   : priority · inherit · override
 */
public class ZoneCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§8[§2Zone§8] §r";
    private static final String ERR    = "§c";
    private static final String OK     = "§a";

    private final ZoneManager  zoneManager;
    private final MobManager   mobManager;
    private final ZoneConfig   zoneConfig;
    private final DebugManager debugManager;

    public ZoneCommand(ZoneManager zoneManager, MobManager mobManager,
                       ZoneConfig zoneConfig, DebugManager debugManager) {
        this.zoneManager  = zoneManager;
        this.mobManager   = mobManager;
        this.zoneConfig   = zoneConfig;
        this.debugManager = debugManager;
    }

    // ─── Exécution ────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "list"      -> cmdList(sender);
            case "info"      -> { if (need(sender, args, 2)) cmdInfo(sender, args[1]); }
            case "create"    -> { if (needPlayer(sender) && need(sender, args, 2)) cmdCreate((Player) sender, args[1]); }
            case "delete"    -> { if (need(sender, args, 2)) cmdDelete(sender, args[1]); }
            case "pos1"      -> { if (needPlayer(sender) && need(sender, args, 2)) cmdPos((Player) sender, args[1], 1); }
            case "pos2"      -> { if (needPlayer(sender) && need(sender, args, 2)) cmdPos((Player) sender, args[1], 2); }
            case "tp"        -> { if (needPlayer(sender) && need(sender, args, 2)) cmdTp((Player) sender, args[1]); }
            case "check"     -> { if (needPlayer(sender)) cmdCheck((Player) sender); }
            case "debug"     -> { if (needPlayer(sender)) cmdDebug((Player) sender, args); }
            case "addmob"    -> { if (need(sender, args, 3)) cmdAddMob(sender, args[1], args[2]); }
            case "removemob" -> { if (need(sender, args, 3)) cmdRemoveMob(sender, args[1], args[2]); }
            case "setweight" -> { if (need(sender, args, 4)) cmdSetWeight(sender, args[1], args[2], args[3]); }
            case "set"       -> { if (need(sender, args, 4)) cmdSet(sender, args); }
            case "preview"   -> { if (needPlayer(sender) && need(sender, args, 2)) cmdPreview((Player) sender, args[1]); }
            default          -> sendHelp(sender);
        }
        return true;
    }

    // ─── Sous-commandes ───────────────────────────────────────────────────────

    private void cmdList(CommandSender s) {
        Collection<ZoneData> all = zoneManager.getAllZones();
        if (all.isEmpty()) { s.sendMessage(PREFIX + "§7Aucune zone enregistrée."); return; }
        s.sendMessage(PREFIX + "§e" + all.size() + " zone(s) :");
        for (ZoneData z : all)
            s.sendMessage("  §7• §f" + z.getId()
                    + " §7| prio §f" + z.getPriority()
                    + " §7| max §f" + z.getMax()
                    + " §7| mobs §f" + z.getMobs().size()
                    + " §7| interval §f" + z.getSpawnInterval() + "s"
                    + (z.isInheritMobs()  ? " §b[hérite]"   : "")
                    + (z.isOverrideMobs() ? " §c[override]" : ""));
    }

    private void cmdInfo(CommandSender s, String id) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { s.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        s.sendMessage(PREFIX + "§e=== " + z.getId() + " ===");
        s.sendMessage("  §7Priorité        : §f" + z.getPriority());
        s.sendMessage("  §7Cap global      : §f" + z.getMax() + " mobs");
        s.sendMessage("  §7Mobs            : " + fmtMobsWithWeights(z));
        s.sendMessage("  §7Hérite/Override : §f" + z.isInheritMobs() + " / §f" + z.isOverrideMobs());
        s.sendMessage("  §7─── Spawn ───────────────────────────────");
        s.sendMessage("  §7Radius          : §f" + z.getSpawnRadiusMin() + " §7- §f" + z.getSpawnRadiusMax() + " blocs");
        s.sendMessage("  §7Target/joueur   : §f" + z.getTargetMin() + " §7- §f" + z.getTargetMax());
        s.sendMessage("  §7Intervalle      : §f" + z.getSpawnInterval() + "s");
        s.sendMessage("  §7Multiplicateur  : §f" + String.format("%.0f%%", z.getSpawnChance() * 100));
        s.sendMessage("  §7Condition       : §f" + z.getSpawnCondition());
        s.sendMessage("  §7─── Despawn ─────────────────────────────");
        s.sendMessage("  §7Distance seuil  : §f" + z.getDespawnDistance() + " blocs");
        s.sendMessage("  §7Vérif. toutes   : §f" + z.getDespawnCheckInterval() + "s");
        s.sendMessage("  §7Seuils (ratio)  : §f<" + pct(z.getDespawnRatio1())
                + " §7| §f" + pct(z.getDespawnRatio1()) + "-" + pct(z.getDespawnRatio2())
                + " §7| §f" + pct(z.getDespawnRatio2()) + "-" + pct(z.getDespawnRatio3())
                + " §7| §f>" + pct(z.getDespawnRatio3()));
        s.sendMessage("  §7Chances         : §f" + pct(z.getDespawnChanceClose())
                + " §7| §f" + pct(z.getDespawnChanceMid())
                + " §7| §f" + pct(z.getDespawnChanceFar())
                + " §7| §f" + pct(z.getDespawnChanceOuter()));
        s.sendMessage("  §7─── Frontière ────────────────────────────");
        s.sendMessage("  §7Tolérance hors zone : §f" + z.getBoundaryTolerance() + " blocs"
                + (z.getBoundaryTolerance() == 0 ? " §8(rebond immédiat)" : ""));
        s.sendMessage("  §7Force rebond        : §f" + z.getBounceStrength());
        if (z.getPos1() != null) s.sendMessage("  §7Pos1 : §f" + fmtLoc(z.getPos1()));
        if (z.getPos2() != null) s.sendMessage("  §7Pos2 : §f" + fmtLoc(z.getPos2()));
    }

    private void cmdCreate(Player p, String id) {
        if (zoneManager.getZone(id) != null) {
            p.sendMessage(PREFIX + ERR + "La zone '" + id + "' existe déjà."); return;
        }
        Location loc = p.getLocation();
        ZoneData zone = new ZoneData(id, loc.clone(), loc.clone(), List.of(), 20);
        zoneManager.registerZone(zone);
        zoneConfig.saveZone(zone);
        p.sendMessage(PREFIX + OK + "Zone '" + id + "' créée.");
        p.sendMessage(PREFIX + "§7Définissez les coins : §f/zone pos1 " + id + " §7et §f/zone pos2 " + id);
    }

    private void cmdDelete(CommandSender s, String id) {
        if (zoneManager.getZone(id) == null) { s.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        zoneManager.removeZone(id);
        zoneConfig.deleteZone(id);
        s.sendMessage(PREFIX + OK + "Zone '" + id + "' supprimée (mobs despawnés).");
    }

    private void cmdPos(Player p, String id, int corner) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { p.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        Location loc = p.getLocation();
        if (corner == 1) z.setPos1(loc.clone());
        else             z.setPos2(loc.clone());
        zoneConfig.saveZone(z);
        p.sendMessage(PREFIX + OK + "Pos" + corner + " de '" + id + "' : §f" + fmtLoc(loc));
    }

    private void cmdTp(Player p, String id) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { p.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        Location center = zoneManager.getCenter(z);
        if (center == null || center.getWorld() == null) {
            p.sendMessage(PREFIX + ERR + "La zone n'a pas de position valide."); return;
        }
        Location safe = center.getWorld().getHighestBlockAt(center).getLocation().add(0.5, 1, 0.5);
        p.teleport(safe);
        p.sendMessage(PREFIX + OK + "Téléporté au centre de '" + id + "'.");
    }

    private void cmdCheck(Player p) {
        ZoneData z = zoneManager.getZoneAt(p.getLocation());
        if (z == null) {
            p.sendMessage(PREFIX + ERR + "Vous n'êtes dans aucune zone.");
            p.sendMessage(PREFIX + "§7Zones : §f" + zoneManager.getZoneIds());
            p.sendMessage(PREFIX + "§7Pos : §f" + fmtLoc(p.getLocation()));
            return;
        }
        p.sendMessage(PREFIX + "§e✔ Zone active : §f" + z.getId());
        List<String> effectiveMobs = zoneManager.getEffectiveMobs(z, p.getLocation());
        if (effectiveMobs.isEmpty()) {
            p.sendMessage(PREFIX + ERR + "⚠ Aucun mob ! /zone addmob " + z.getId() + " <mobId>"); return;
        }
        // Affichage avec poids
        p.sendMessage(PREFIX + "§7Mobs effectifs  : " + fmtMobsWithWeights(z));
        List<String> missing = effectiveMobs.stream()
                .filter(id -> mobManager.getMob(id) == null).collect(Collectors.toList());
        if (!missing.isEmpty())
            p.sendMessage(PREFIX + ERR + "⚠ MobIds introuvables : §f" + missing);
        p.sendMessage(PREFIX + "§7Cap : §f" + z.getMax()
                + "  §7Target/joueur : §f" + z.getTargetMin() + "-" + z.getTargetMax()
                + "  §7Interval : §f" + z.getSpawnInterval() + "s"
                + "  §7Chance×: §f" + String.format("%.0f%%", z.getSpawnChance() * 100));
        p.sendMessage(PREFIX + "§7Condition : §f" + z.getSpawnCondition());
        p.sendMessage(PREFIX + "§7Despawn : §f" + z.getDespawnDistance() + " blocs"
                + "  §7Vérif: §f" + z.getDespawnCheckInterval() + "s");
        p.sendMessage(PREFIX + "§7Seuils : §f<" + pct(z.getDespawnRatio1())
                + "=" + pct(z.getDespawnChanceClose())
                + " §f" + pct(z.getDespawnRatio1()) + "-" + pct(z.getDespawnRatio2())
                + "=" + pct(z.getDespawnChanceMid())
                + " §f" + pct(z.getDespawnRatio2()) + "-" + pct(z.getDespawnRatio3())
                + "=" + pct(z.getDespawnChanceFar())
                + " §f>" + pct(z.getDespawnRatio3()) + "=" + pct(z.getDespawnChanceOuter()));
    }

    /** /zone debug [off|normal|verbose] */
    private void cmdDebug(Player p, String[] args) {
        DebugManager.Level level;
        if (args.length >= 2) {
            level = switch (args[1].toLowerCase()) {
                case "off", "none" -> DebugManager.Level.NONE;
                case "normal"      -> DebugManager.Level.NORMAL;
                case "verbose", "v" -> DebugManager.Level.VERBOSE;
                default -> null;
            };
            if (level == null) {
                p.sendMessage(PREFIX + ERR + "Usage : /zone debug [off|normal|verbose]");
                return;
            }
            debugManager.setLevel(p.getUniqueId(), level);
        } else {
            level = debugManager.cycle(p.getUniqueId());
        }
        String display = switch (level) {
            case NONE    -> "§cDÉSACTIVÉ";
            case NORMAL  -> "§aNORMAL §7(§a[SPAWN]§7 + §c[DESPAWN]§7)";
            case VERBOSE -> "§eVERBEUX §7(NORMAL + §e[SKIP]§7 + §6[CLEANUP]§7 + §6[BLOCKED]§7)";
        };
        p.sendMessage(PREFIX + "Debug : " + display);

        if (level != DebugManager.Level.NONE) {
            p.sendMessage(PREFIX + "§7Cercles autour de toi (toutes les 0.5s) :");
            p.sendMessage("  §b■ §fCyan   §8│ §7Rayon spawn intérieur §8(spawnmin)");
            p.sendMessage("  §9■ §fBleu   §8│ §7Rayon spawn extérieur §8(spawnmax)");
            p.sendMessage("  §a■ §fVert   §8│ §7Seuil close→mid §8(ratio1 × despawn)");
            p.sendMessage("  §e■ §fJaune  §8│ §7Seuil mid→far   §8(ratio2 × despawn)");
            p.sendMessage("  §6■ §fOrange §8│ §7Seuil far→outer §8(ratio3 × despawn)");
            p.sendMessage(PREFIX + "§7Bursts : §a■ §fvert §7= spawn  §c■ §frouge §7= despawn");
        }
    }

    private void cmdAddMob(CommandSender s, String id, String mobId) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { s.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        if (mobManager.getMob(mobId) == null) { s.sendMessage(PREFIX + ERR + "Mob '" + mobId + "' inexistant."); return; }
        if (z.getMobs().contains(mobId)) { s.sendMessage(PREFIX + ERR + "Mob '" + mobId + "' déjà dans la zone."); return; }
        z.getMobs().add(mobId);
        zoneConfig.saveZone(z);
        s.sendMessage(PREFIX + OK + "Mob '" + mobId + "' ajouté à '" + id + "'.");
    }

    private void cmdRemoveMob(CommandSender s, String id, String mobId) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { s.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        if (!z.getMobs().remove(mobId)) { s.sendMessage(PREFIX + ERR + "Mob '" + mobId + "' absent de cette zone."); return; }
        z.setMobWeight(mobId, 0); // nettoyer le poids s'il était défini
        zoneConfig.saveZone(z);
        s.sendMessage(PREFIX + OK + "Mob '" + mobId + "' retiré de '" + id + "'.");
    }

    /** /zone setweight <zoneId> <mobId> <poids> */
    private void cmdSetWeight(CommandSender s, String id, String mobId, String weightStr) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null)              { s.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        if (!z.getMobs().contains(mobId)) { s.sendMessage(PREFIX + ERR + "Mob '" + mobId + "' absent de la zone (utilisez /zone addmob)."); return; }
        double weight;
        try { weight = Double.parseDouble(weightStr); } catch (NumberFormatException e) {
            s.sendMessage(PREFIX + ERR + "Poids invalide : §f" + weightStr + " §c(nombre > 0)"); return; }
        if (weight <= 0) { s.sendMessage(PREFIX + ERR + "Le poids doit être > 0."); return; }
        z.setMobWeight(mobId, weight);
        zoneConfig.saveZone(z);
        s.sendMessage(PREFIX + OK + "Poids de §f'" + mobId + "' §adans §f'" + id + "' §a: §f" + weight
                + "  §7(" + fmtProb(z, mobId) + " de chance)");
    }

    /**
     * Formate la liste des mobs avec leur poids et probabilité calculée.
     * Ex : zombie §8(×3.0 §775%) §7| skeleton §8(×1.0 §725%)
     */
    private String fmtMobsWithWeights(ZoneData z) {
        List<String> mobs = z.getMobs();
        if (mobs.isEmpty()) return "§cAucun";
        double total = mobs.stream().mapToDouble(z::getMobWeight).sum();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mobs.size(); i++) {
            String m = mobs.get(i);
            double w = z.getMobWeight(m);
            double prob = total > 0 ? w / total * 100 : 0;
            if (i > 0) sb.append("§7, ");
            sb.append("§f").append(m)
              .append(" §8(§7×").append(w == (long) w ? String.valueOf((long) w) : String.format("%.1f", w))
              .append(" §e").append(String.format("%.0f%%", prob)).append("§8)");
        }
        return sb.toString();
    }

    /** Retourne la probabilité formatée d'un mob dans sa zone (ex : "75%"). */
    private String fmtProb(ZoneData z, String mobId) {
        double total = z.getMobs().stream().mapToDouble(z::getMobWeight).sum();
        if (total <= 0) return "0%";
        return String.format("%.1f%%", z.getMobWeight(mobId) / total * 100);
    }

    /** /zone preview <id> — active/désactive la prévisualisation particules de la bordure */
    private void cmdPreview(Player p, String id) {
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { p.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }
        zoneManager.toggleBorderPreview(p, id);
        if (zoneManager.isPreviewingBorder(p, id)) {
            p.sendMessage(PREFIX + OK + "Prévisualisation activée §f'" + id + "'§a — légende :");
            p.sendMessage("  §f■ Blanc        §7= bordure réelle de la zone");
            if (z.getBoundaryTolerance() > 0)
                p.sendMessage("  §6■ Orange       §7= zone + tolérance §f(" + z.getBoundaryTolerance() + " blocs§7)");
            p.sendMessage("  §b■ Cyan         §7= rayon spawn min §f(" + z.getSpawnRadiusMin() + " blocs§7)");
            p.sendMessage("  §9■ Bleu         §7= rayon spawn max §f(" + z.getSpawnRadiusMax() + " blocs§7)");
            double d = z.getDespawnDistance();
            p.sendMessage("  §a■ Vert         §7= despawn ratio1 §f(" + String.format("%.0f", d * z.getDespawnRatio1()) + " blocs§7)");
            p.sendMessage("  §e■ Jaune        §7= despawn ratio2 §f(" + String.format("%.0f", d * z.getDespawnRatio2()) + " blocs§7)");
            p.sendMessage("  §c■ Orange-rouge §7= despawn ratio3 §f(" + String.format("%.0f", d * z.getDespawnRatio3()) + " blocs§7)");
            p.sendMessage(PREFIX + "§8(Les cercles sont centrés sur le centre de la zone)");
        } else {
            p.sendMessage(PREFIX + "§7Prévisualisation désactivée §f'" + id + "'§7.");
        }
    }

    private void cmdSet(CommandSender s, String[] args) {
        String id   = args[1];
        String prop = args[2].toLowerCase();
        String val  = args[3];
        ZoneData z = zoneManager.getZone(id);
        if (z == null) { s.sendMessage(PREFIX + ERR + "Zone '" + id + "' introuvable."); return; }

        try {
            switch (prop) {
                // ── Général ──
                case "max"           -> z.setMax(Integer.parseInt(val));
                case "priority"      -> z.setPriority(Integer.parseInt(val));
                case "inherit"       -> z.setInheritMobs(Boolean.parseBoolean(val));
                case "override"      -> z.setOverrideMobs(Boolean.parseBoolean(val));
                // ── Spawn ──
                case "spawnmin"       -> z.setSpawnRadiusMin(Double.parseDouble(val));
                case "spawnmax"       -> z.setSpawnRadiusMax(Double.parseDouble(val));
                case "targetmin"      -> z.setTargetMin(Integer.parseInt(val));
                case "targetmax"      -> z.setTargetMax(Integer.parseInt(val));
                case "spawninterval"  -> z.setSpawnInterval(Integer.parseInt(val));
                case "spawnchance"    -> z.setSpawnChance(Double.parseDouble(val));
                case "condition"      -> z.setSpawnCondition(SpawnCondition.fromString(val));
                // ── Despawn ──
                case "despawn"        -> z.setDespawnDistance(Double.parseDouble(val));
                case "despawninterval"-> z.setDespawnCheckInterval(Integer.parseInt(val));
                case "despawnclose"   -> z.setDespawnChanceClose(Double.parseDouble(val));
                case "despawnmid"     -> z.setDespawnChanceMid(Double.parseDouble(val));
                case "despawnfar"     -> z.setDespawnChanceFar(Double.parseDouble(val));
                case "despawnouter"   -> z.setDespawnChanceOuter(Double.parseDouble(val));
                case "despawnratio1"  -> z.setDespawnRatio1(Double.parseDouble(val));
                case "despawnratio2"  -> z.setDespawnRatio2(Double.parseDouble(val));
                case "despawnratio3"  -> z.setDespawnRatio3(Double.parseDouble(val));
                // ── Frontière ──
                case "boundarytolerance", "tolerance" -> z.setBoundaryTolerance(Double.parseDouble(val));
                case "bouncestrength",    "bounce"    -> z.setBounceStrength(Double.parseDouble(val));
                default -> { s.sendMessage(PREFIX + ERR + "Propriété inconnue : " + prop); return; }
            }
        } catch (NumberFormatException e) {
            s.sendMessage(PREFIX + ERR + "Valeur invalide : " + val); return;
        }
        zoneConfig.saveZone(z);
        s.sendMessage(PREFIX + OK + "Zone '" + id + "' : §f" + prop + " §a= §f" + val);
    }

    // ─── Tab Completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return filter(args[0], "list", "info", "create", "delete", "pos1", "pos2",
                    "tp", "check", "debug", "addmob", "removemob", "setweight", "set", "preview");
        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "info", "delete", "pos1", "pos2", "tp", "preview" ->
                    args.length == 2 ? filter(args[1], zoneManager.getZoneIds()) : List.of();
            case "setweight" -> {
                if (args.length == 2) yield filter(args[1], zoneManager.getZoneIds());
                if (args.length == 3) {
                    ZoneData z = zoneManager.getZone(args[1]);
                    yield z != null ? filter(args[2], new HashSet<>(z.getMobs())) : List.of();
                }
                yield List.of();
            }
            case "debug" ->
                    args.length == 2 ? filter(args[1], "off", "normal", "verbose") : List.of();
            case "addmob" -> {
                if (args.length == 2) yield filter(args[1], zoneManager.getZoneIds());
                if (args.length == 3) yield filter(args[2], mobManager.getMobIds());
                yield List.of();
            }
            case "removemob" -> {
                if (args.length == 2) yield filter(args[1], zoneManager.getZoneIds());
                if (args.length == 3) {
                    ZoneData z = zoneManager.getZone(args[1]);
                    yield z != null ? filter(args[2], new HashSet<>(z.getMobs())) : List.of();
                }
                yield List.of();
            }
            case "set" -> {
                if (args.length == 2) yield filter(args[1], zoneManager.getZoneIds());
                if (args.length == 3) yield filter(args[2],
                        // général
                        "max", "priority", "inherit", "override",
                        // spawn
                        "spawnmin", "spawnmax", "targetmin", "targetmax",
                        "spawninterval", "spawnchance", "condition",
                        // despawn
                        "despawn", "despawninterval",
                        "despawnclose", "despawnmid", "despawnfar", "despawnouter",
                        "despawnratio1", "despawnratio2", "despawnratio3",
                        // frontière
                        "boundarytolerance", "bouncestrength");
                if (args.length == 4) yield switch (args[2].toLowerCase()) {
                    case "inherit", "override" -> filter(args[3], "true", "false");
                    case "condition"           -> filter(args[3], "ALWAYS", "DAY", "NIGHT");
                    default                    -> List.of();
                };
                yield List.of();
            }
            default -> List.of();
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender s) {
        s.sendMessage(PREFIX + "§eCommandes /zone :");
        s.sendMessage("  §f/zone list / info <id> / create <id> / delete <id>");
        s.sendMessage("  §f/zone pos1|pos2 <id>  §7· tp <id>  §7· check  §7· debug [off|normal|verbose]");
        s.sendMessage("  §f/zone preview <id>     §7· §8(active/désactive prévisualisation particules)");
        s.sendMessage("  §f/zone addmob <id> <mobId>  §7· removemob <id> <mobId>");
        s.sendMessage("  §f/zone setweight <id> <mobId> <poids>  §8(ex: 3.0 = 3× plus probable qu'un poids 1.0)");
        s.sendMessage("  §f/zone set <id> <prop> <val>");
        s.sendMessage("  §7  Général   : max  priority  inherit  override");
        s.sendMessage("  §7  Spawn     : spawnmin  spawnmax  targetmin  targetmax");
        s.sendMessage("  §7             spawninterval§8(s)  spawnchance§8(0-1)  condition§8(ALWAYS/DAY/NIGHT)");
        s.sendMessage("  §7  Despawn   : despawn§8(blocs)  despawninterval§8(s)");
        s.sendMessage("  §7             despawnclose  despawnmid  despawnfar  despawnouter§8(0-1, chances)");
        s.sendMessage("  §7             despawnratio1  despawnratio2  despawnratio3§8(0-2, seuils×distance)");
        s.sendMessage("  §7  Frontière : boundarytolerance§8(blocs hors zone avant rebond)  bouncestrength§8(force)");
    }

    private boolean need(CommandSender s, String[] args, int min) {
        if (args.length >= min) return true;
        s.sendMessage(PREFIX + ERR + "Arguments insuffisants."); return false;
    }
    private boolean needPlayer(CommandSender s) {
        if (s instanceof Player) return true;
        s.sendMessage(PREFIX + ERR + "Réservé aux joueurs."); return false;
    }
    private String fmtLoc(Location l) {
        return String.format("§7(§f%.1f§7, §f%.1f§7, §f%.1f§7) §f%s",
                l.getX(), l.getY(), l.getZ(), l.getWorld() != null ? l.getWorld().getName() : "?");
    }
    private String pct(double v) { return String.format("%.0f%%", v * 100); }

    private List<String> filter(String prefix, String... options) {
        return Arrays.stream(options).filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
    private List<String> filter(String prefix, Set<String> options) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).sorted().collect(Collectors.toList());
    }
}
