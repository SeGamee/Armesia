package fr.segame.armesiaMobs.zones;

import fr.segame.armesiaMobs.managers.DebugManager;
import fr.segame.armesiaMobs.mobs.*;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class ZoneManager {

    private final Map<String, ZoneData> zones = new HashMap<>();
    private final MobSpawner spawner;
    private final MobManager mobManager;
    private final DebugManager debug;
    private final Random random = new Random();
    private final JavaPlugin plugin;

    /** Compteur de ticks écoulés depuis le dernier spawn, par zoneId */
    private final Map<String, Integer> zoneSpawnTicker        = new HashMap<>();
    /** Timestamp (ms) du dernier despawn-check par zoneId */
    private final Map<String, Long>    zoneLastDespawnCheckMs = new HashMap<>();
    /** Joueurs ayant une prévisualisation de bordure active : UUID → set des IDs de zones */
    private final Map<UUID, Set<String>> previewingPlayers    = new HashMap<>();

    public ZoneManager(JavaPlugin plugin, MobSpawner spawner, MobManager mobManager, DebugManager debug) {
        this.plugin     = plugin;
        this.spawner    = spawner;
        this.mobManager = mobManager;
        this.debug      = debug;
        startTasks();
    }

    public JavaPlugin getPlugin() { return plugin; }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    public void registerZone(ZoneData zone) { zones.put(zone.getId(), zone); }
    public ZoneData getZone(String id)      { return zones.get(id); }
    public Collection<ZoneData> getAllZones() { return zones.values(); }
    public Set<String> getZoneIds()          { return zones.keySet(); }

    public void removeZone(String id) {
        zones.remove(id);
        zoneSpawnTicker.remove(id);
        zoneLastDespawnCheckMs.remove(id);
        killZoneMobs(id);
    }

    // ─── Héritage de mobs ────────────────────────────────────────────────────

    public List<String> getEffectiveMobs(ZoneData primary, Location loc) {
        List<String> result = new ArrayList<>(primary.getMobs());
        if (!primary.isInheritMobs()) return result;

        List<ZoneData> lowerZones = zones.values().stream()
                .filter(z -> !z.getId().equals(primary.getId()))
                .filter(z -> isInZone(loc, z))
                .filter(z -> z.getPriority() < primary.getPriority())
                .sorted(Comparator.comparingInt(ZoneData::getPriority).reversed())
                .collect(Collectors.toList());

        for (ZoneData lower : lowerZones) {
            boolean blocked = zones.values().stream()
                    .anyMatch(z -> !z.getId().equals(primary.getId())
                            && !z.getId().equals(lower.getId())
                            && isInZone(loc, z)
                            && z.getPriority() > lower.getPriority()
                            && z.getPriority() < primary.getPriority()
                            && z.isOverrideMobs());
            if (!blocked) result.addAll(lower.getMobs());
        }
        return result;
    }

    // ─── Comptage ─────────────────────────────────────────────────────────────

    private int countMobsAroundPlayer(Player player, ZoneData zone) {
        double radius = zone.getSpawnRadiusMax() * 2;
        int count = 0;
        for (MobInstance instance : mobManager.getAllInstances()) {
            Entity entity = Bukkit.getEntity(instance.getUuid());
            if (entity == null) continue;
            if (!instance.getZoneId().equals(zone.getId())) continue;
            if (hDist(entity.getLocation(), player.getLocation()) <= radius) count++;
        }
        return count;
    }

    private long countTotalMobsInZone(String zoneId) {
        return mobManager.getAllInstances().stream()
                .filter(i -> i.getZoneId().equals(zoneId))
                .count();
    }

    // ─── Condition d'heure ────────────────────────────────────────────────────

    private boolean checkCondition(ZoneData zone, Player player) {
        if (zone.getSpawnCondition() == SpawnCondition.ALWAYS) return true;
        // 0–12299 = jour, 12300–23999 = nuit
        boolean isDay = player.getWorld().getTime() < 12300;
        if (zone.getSpawnCondition() == SpawnCondition.DAY   && !isDay) return false;
        if (zone.getSpawnCondition() == SpawnCondition.NIGHT &&  isDay) return false;
        return true;
    }

    // ─── Localisation de spawn ────────────────────────────────────────────────

    private Location getSmartSpawnLocation(Player player, ZoneData zone) {
        for (int i = 0; i < 10; i++) {
            double radius = zone.getSpawnRadiusMin()
                    + random.nextDouble() * (zone.getSpawnRadiusMax() - zone.getSpawnRadiusMin());
            double angle = random.nextDouble() * Math.PI * 2;
            double x = player.getLocation().getX() + Math.cos(angle) * radius;
            double z = player.getLocation().getZ() + Math.sin(angle) * radius;
            Location loc = new Location(player.getWorld(), x, player.getLocation().getY(), z);
            loc = loc.getWorld().getHighestBlockAt(loc).getLocation().add(0, 1, 0);
            if (!isInZone(loc, zone)) continue;
            if (loc.getBlock().isLiquid()) continue;
            if (loc.clone().subtract(0, 1, 0).getBlock().isLiquid()) continue; // surface liquide
            if (isLookingAt(player, loc)) continue;
            return loc;
        }
        return getRandomLocation(zone);
    }

    private boolean isLookingAt(Player player, Location loc) {
        Vector dir   = player.getLocation().getDirection().normalize();
        Vector toLoc = loc.toVector().subtract(player.getLocation().toVector()).normalize();
        return dir.dot(toLoc) > 0.85;
    }

    public ZoneData getZoneAt(Location loc) {
        ZoneData best = null;
        for (ZoneData zone : zones.values()) {
            if (!isInZone(loc, zone)) continue;
            if (best == null || zone.getPriority() > best.getPriority()) best = zone;
        }
        return best;
    }

    // ─── Tâches périodiques ───────────────────────────────────────────────────

    private void startTasks() {


        // ── Despawn + Redirection — 1 tick/s, intervalle réel par zone ────
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            long now = System.currentTimeMillis();

            // Déterminer quelles zones sont dues pour un check de despawn
            Set<String> dueZones = new HashSet<>();
            for (ZoneData zone : zones.values()) {
                long intervalMs = zone.getDespawnCheckInterval() * 1000L;
                Long last = zoneLastDespawnCheckMs.get(zone.getId());
                if (last == null || now - last >= intervalMs) {
                    dueZones.add(zone.getId());
                    zoneLastDespawnCheckMs.put(zone.getId(), now);
                }
            }

            Iterator<MobInstance> it = mobManager.getAllInstances().iterator();
            while (it.hasNext()) {
                MobInstance instance = it.next();
                Entity entity = Bukkit.getEntity(instance.getUuid());

                if (entity == null) {
                    it.remove();
                    continue;
                }

                if (!(entity instanceof Mob mob)) {
                    it.remove();
                    continue;
                }


                ZoneData homeZone = zones.get(instance.getZoneId());

                // Zone inconnue (spawn manuel ou zone supprimée)
                if (homeZone == null) {
                    if (!"manual".equals(instance.getZoneId())) {
                        debug.log("§c[DESPAWN]§7 mob=§f" + instance.getMobId()
                                + "§7 raison=§cZONE_SUPPRIMÉE");
                        mob.remove(); it.remove();
                    }
                    continue;
                }

                // Redirection si hors zone (toujours, indépendamment de l'intervalle)
                boolean inZone = isInZone(mob.getLocation(), homeZone);
                if (!inZone) {
                    Location center = getCenter(homeZone);
                    if (center == null) {
                        debug.log("§c[DESPAWN]§7 zone=§f" + homeZone.getId()
                                + "§7 mob=§f" + instance.getMobId() + "§7 raison=§cPAS_DE_CENTRE");
                        mob.remove(); it.remove(); continue;
                    }
                    double distFromCenter = hDist(mob.getLocation(), center);
                    // Hard-kill : trop loin du bord (tolérance + 15 blocs) OU trop loin du centre (XZ)
                    double distOutsideCheck = getDistanceOutside(mob.getLocation(), homeZone);
                    double hardKill = homeZone.getBoundaryTolerance() + 15.0;
                    if (distOutsideCheck > hardKill || distFromCenter > homeZone.getDespawnDistance() * 1.5) {
                        debug.log("§c[DESPAWN]§7 zone=§f" + homeZone.getId()
                                + "§7 mob=§f" + instance.getMobId()
                                + "§7 raison=§c" + (distOutsideCheck > hardKill ? "HORS_ZONE_LIMITE" : "TROP_LOIN")
                                + " distBord=§f" + String.format("%.1f", distOutsideCheck)
                                + " distCentre=§f" + String.format("%.1f", distFromCenter));
                        mob.remove(); it.remove();
                    }
                    // Pas de pathfind au centre : la tâche de rebond gère le retour
                    continue;
                }

                // Check despawn par distance uniquement si la zone est due ET si des joueurs sont en ligne
                if (!dueZones.contains(homeZone.getId())) continue;
                if (Bukkit.getOnlinePlayers().isEmpty()) continue; // pas de despawn sans joueurs en ligne

                double closestPlayer = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getWorld().equals(mob.getWorld()))
                        .mapToDouble(p -> hDist(p.getLocation(), mob.getLocation()))
                        .min().orElse(Double.MAX_VALUE);

                double despawn = homeZone.getDespawnDistance();
                double chance;
                if      (closestPlayer < despawn * homeZone.getDespawnRatio1()) chance = homeZone.getDespawnChanceClose();
                else if (closestPlayer < despawn * homeZone.getDespawnRatio2()) chance = homeZone.getDespawnChanceMid();
                else if (closestPlayer < despawn * homeZone.getDespawnRatio3()) chance = homeZone.getDespawnChanceFar();
                else                                                            chance = homeZone.getDespawnChanceOuter();

                if (Math.random() < chance) {
                    debug.log("§c[DESPAWN]§7 zone=§f" + homeZone.getId()
                            + "§7 mob=§f" + instance.getMobId()
                            + "§7 dist=§f" + String.format("%.1f", closestPlayer)
                            + "§7/§f" + String.format("%.1f", despawn)
                            + "§7 chance=§f" + String.format("%.0f%%", chance * 100));
                    // Burst rouge visible par les joueurs en debug
                    if (debug.hasAny()) {
                        final Location dl = mob.getLocation().clone().add(0, 1, 0);
                        debug.forEachDebugPlayer(p -> p.spawnParticle(Particle.REDSTONE,
                                dl, 20, 0.4, 0.4, 0.4, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.5f)));
                    }
                    mob.remove(); it.remove();
                }
            }

        }, 0L, 20L); // 1 tick/s — l'intervalle réel est géré par zone via dueZones

        // ── Rebond mobs hors zone — toutes les 4 ticks (0.2 s) ───────────────
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (MobInstance instance : new ArrayList<>(mobManager.getAllInstances())) {
                Entity entity = Bukkit.getEntity(instance.getUuid());
                if (!(entity instanceof Mob mob)) continue;

                ZoneData zone = zones.get(instance.getZoneId());
                if (zone == null || zone.getPos1() == null || zone.getPos2() == null) continue;

                Location mobLoc = mob.getLocation();

                // ── Anti-eau : téléporter hors du liquide ──────────────────
                if (mobLoc.getBlock().isLiquid()
                        || mobLoc.clone().subtract(0, 1, 0).getBlock().isLiquid()) {
                    Location dry = getRandomLocation(zone);
                    if (dry != null) {
                        mob.getPathfinder().stopPathfinding();
                        mob.teleport(dry);
                        debug.logVerbose("§b[WATER-TP]§7 mob=§f" + instance.getMobId()
                                + "§7 zone=§f" + zone.getId());
                    }
                    continue;
                }

                if (isInZone(mobLoc, zone)) continue;

                // Mob hors zone — téléporter à l'intérieur
                Location back = getRandomLocation(zone);
                if (back != null) {
                    mob.getPathfinder().stopPathfinding();
                    mob.teleport(back);
                    debug.logVerbose("§e[BOUNCE]§7 mob=§f" + instance.getMobId()
                            + "§7 zone=§f" + zone.getId());
                }
            }

        }, 0L, 4L);

        // ── Visualisation des zones (joueurs en mode preview) ─────────────────
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (previewingPlayers.isEmpty()) return;

            for (Map.Entry<UUID, Set<String>> entry : previewingPlayers.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) continue;
                for (String zoneId : entry.getValue()) {
                    ZoneData zone = zones.get(zoneId);
                    if (zone != null) drawZoneBorder(player, zone);
                }
            }

        }, 0L, 10L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            for (MobInstance instance : new ArrayList<>(mobManager.getAllInstances())) {
                Entity entity = Bukkit.getEntity(instance.getUuid());
                if (!(entity instanceof Mob mob)) continue;
                if (mob.getTarget() != null) continue;

                ZoneData zone = zones.get(instance.getZoneId());
                if (zone == null || zone.getPos1() == null || zone.getPos2() == null) continue;

                Location mobLoc = mob.getLocation();
                if (!isInZone(mobLoc, zone)) continue;

                Location center = getCenter(zone);
                double halfW = Math.abs(zone.getPos2().getX() - zone.getPos1().getX()) / 2.0;
                double halfD = Math.abs(zone.getPos2().getZ() - zone.getPos1().getZ()) / 2.0;
                if (halfW <= 0 || halfD <= 0) continue;

                double dx = Math.abs(mobLoc.getX() - center.getX());
                double dz = Math.abs(mobLoc.getZ() - center.getZ());
                if (!(dx > halfW * 0.70 || dz > halfD * 0.70)) continue;

                double minDim = Math.min(halfW, halfD);
                double angle  = random.nextDouble() * Math.PI * 2;
                double r      = random.nextDouble() * minDim * 0.5;
                int tx = (int)(center.getX() + Math.cos(angle) * r);
                int tz = (int)(center.getZ() + Math.sin(angle) * r);
                Location patrol = center.getWorld()
                        .getHighestBlockAt(tx, tz).getLocation().add(0.5, 1, 0.5);
                mob.getPathfinder().moveTo(patrol, 1.0);
            }

        }, 0L, 40L);

        // ── Visualisation des rayons (debug) — toutes les 10 ticks ──────────
        // Cercles DUST colorés autour du joueur :
        //   CYAN   = spawnmin   BLEU  = spawnmax
        //   VERT   = ratio1×despawn   JAUNE = ratio2×despawn   ORANGE = ratio3×despawn
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (!debug.hasAny()) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (debug.getLevel(player.getUniqueId()) == DebugManager.Level.NONE) continue;

                ZoneData zone = getZoneAt(player.getLocation());
                if (zone == null) continue;

                Location origin = player.getLocation();
                double y = origin.getY() + 1.0; // légèrement au-dessus du sol

                // Rayons de spawn
                drawCircle(player, origin, zone.getSpawnRadiusMin(), y,
                        Color.fromRGB(0, 230, 230));    // CYAN   — limite intérieure spawn
                drawCircle(player, origin, zone.getSpawnRadiusMax(), y,
                        Color.fromRGB(80, 150, 255));   // BLEU   — limite extérieure spawn

                // Seuils de despawn
                double d = zone.getDespawnDistance();
                drawCircle(player, origin, d * zone.getDespawnRatio1(), y,
                        Color.fromRGB(0, 255, 50));     // VERT   — limite close / mid
                drawCircle(player, origin, d * zone.getDespawnRatio2(), y,
                        Color.fromRGB(255, 230, 0));    // JAUNE  — limite mid / far
                drawCircle(player, origin, d * zone.getDespawnRatio3(), y,
                        Color.fromRGB(255, 100, 0));    // ORANGE — limite far / outer
            }

        }, 0L, 10L);

        // ── Spawn — 1 tick/s, compteur de ticks par zone (fiable) ───────────
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            // ── Avancer le compteur de toutes les zones connues ────────────
            for (String zoneId : zones.keySet()) {
                zoneSpawnTicker.merge(zoneId, 1, Integer::sum);
            }

            // ── Déduplication : un seul joueur représentant par zone ─────────
            // Évite le double-spawn quand plusieurs joueurs sont au même endroit.
            // On garde le joueur le plus "nécessiteux" (le moins de mobs nearby).
            Map<String, Player> repPerZone = new LinkedHashMap<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                ZoneData z = getZoneAt(p.getLocation());
                if (z == null) continue;
                if (!repPerZone.containsKey(z.getId())) {
                    repPerZone.put(z.getId(), p);
                } else {
                    Player existing = repPerZone.get(z.getId());
                    if (countMobsAroundPlayer(p, z) < countMobsAroundPlayer(existing, z))
                        repPerZone.put(z.getId(), p);
                }
            }

            for (Map.Entry<String, Player> repEntry : repPerZone.entrySet()) {
                Player player = repEntry.getValue();
                ZoneData zone = zones.get(repEntry.getKey());
                if (zone == null) continue;

                // ── Intervalle (en exécutions = secondes à 20 TPS) ─────────
                // spawnInterval=1 → spawn à chaque exécution
                // spawnInterval=5 → spawn 1 fois toutes les 5 exécutions
                int elapsed = zoneSpawnTicker.getOrDefault(zone.getId(), zone.getSpawnInterval());
                if (elapsed < zone.getSpawnInterval()) continue;

                // ── Cap global ────────────────────────────────────────────
                long total = countTotalMobsInZone(zone.getId());
                if (zone.getMax() > 0 && total >= zone.getMax()) {
                    debug.logVerbose("§e[SKIP]§7 zone=§f" + zone.getId()
                            + "§7 raison=§eCAP_ATTEINT total=§f" + total + "§7/§f" + zone.getMax());
                    continue;
                }

                // ── Condition heure ───────────────────────────────────────
                if (!checkCondition(zone, player)) {
                    debug.logVerbose("§e[SKIP]§7 zone=§f" + zone.getId()
                            + "§7 raison=§eCONDITION§7 requis=§f" + zone.getSpawnCondition());
                    continue;
                }

                // ── Cible par joueur (hard cap) ───────────────────────────
                int nearby = countMobsAroundPlayer(player, zone);
                int range  = zone.getTargetMax() - zone.getTargetMin();
                int target = zone.getTargetMin() + (range > 0 ? random.nextInt(range + 1) : 0);
                if (nearby >= target) {
                    debug.logVerbose("§e[SKIP]§7 zone=§f" + zone.getId()
                            + "§7 raison=§eCIBLE_OK nearby=§f" + nearby + "§7/§f" + target);
                    continue;
                }

                // ── Palier de boost spawn ─────────────────────────────────
                // fill = fraction du target atteinte (0.0 = aucun mob, 1.0 = cap)
                double fill = target > 0 ? (double) nearby / target : 1.0;
                double effectiveChance;
                int    burstCount;
                int    boostTier;

                if (fill < zone.getSpawnBoostRatio1()) {
                    effectiveChance = Math.min(1.0, zone.getSpawnChance() * zone.getSpawnBoostMultiplier1());
                    burstCount      = zone.getSpawnBoostCount1();
                    boostTier       = 1;
                } else if (fill < zone.getSpawnBoostRatio2()) {
                    effectiveChance = Math.min(1.0, zone.getSpawnChance() * zone.getSpawnBoostMultiplier2());
                    burstCount      = zone.getSpawnBoostCount2();
                    boostTier       = 2;
                } else if (fill < zone.getSpawnBoostRatio3()) {
                    effectiveChance = Math.min(1.0, zone.getSpawnChance() * zone.getSpawnBoostMultiplier3());
                    burstCount      = zone.getSpawnBoostCount3();
                    boostTier       = 3;
                } else {
                    effectiveChance = zone.getSpawnChance();
                    burstCount      = 1;
                    boostTier       = 0;
                }

                // ── Chance (avec boost éventuel) ──────────────────────────
                if (effectiveChance < 1.0 && random.nextDouble() >= effectiveChance) {
                    debug.logVerbose("§e[SKIP]§7 zone=§f" + zone.getId()
                            + "§7 raison=§eCHANCE proba=§f"
                            + String.format("%.0f%%", effectiveChance * 100)
                            + (boostTier > 0 ? " §8(boost tier§f" + boostTier + "§8)" : ""));
                    continue;
                }

                // ── Burst spawn ────────────────────────────────────────────
                int  spawned    = 0;
                long totalAfter = total;
                for (int b = 0; b < burstCount; b++) {
                    if (zone.getMax() > 0 && totalAfter >= zone.getMax()) break;
                    int nearbyNow = (b == 0) ? nearby : countMobsAroundPlayer(player, zone);
                    if (nearbyNow >= target) break;

                    Location loc = getSmartSpawnLocation(player, zone);
                    if (loc == null) break;

                    List<String> effectiveMobs = getEffectiveMobs(zone, loc);
                    if (effectiveMobs.isEmpty()) break;

                    String mobId = pickWeightedMob(effectiveMobs, zone);
                    MobData data = mobManager.getMob(mobId);
                    if (data == null) break;

                    spawner.spawnMob(loc, data, zone.getId());
                    totalAfter++;
                    spawned++;

                    if (debug.hasAny()) {
                        final Location bl = loc.clone().add(0, 1, 0);
                        debug.forEachDebugPlayer(p -> p.spawnParticle(Particle.REDSTONE,
                                bl, 30, 0.4, 0.6, 0.4, 0,
                                new Particle.DustOptions(Color.fromRGB(0, 255, 80), 1.5f)));
                    }
                }

                if (spawned > 0) {
                    zoneSpawnTicker.put(zone.getId(), 0);
                    debug.log("§a[SPAWN]§7 zone=§f" + zone.getId()
                            + "§7 burst=§f" + spawned
                            + (boostTier > 0 ? " §8(tier§f" + boostTier + "×" + String.format("%.1f", boostTier == 1 ? zone.getSpawnBoostMultiplier1() : boostTier == 2 ? zone.getSpawnBoostMultiplier2() : zone.getSpawnBoostMultiplier3()) + "§8)" : "")
                            + "§7 fill=§f" + String.format("%.0f%%", fill * 100)
                            + "§7 nearby=§f" + nearby + "§7/§f" + target
                            + "§7 total=§f" + totalAfter + "§7/§f" + zone.getMax());
                }
            }

        }, 0L, 20L);

        // ── Villageois pacifiques : annuler le boost vitesse de panique ──────
        //    Le Brain PANIC peut ajouter des modificateurs à l'attribut de vitesse.
        //    On les purge toutes les 10 ticks (0.5 s) comme filet de sécurité.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (MobInstance instance : new ArrayList<>(mobManager.getAllInstances())) {
                Entity entity = Bukkit.getEntity(instance.getUuid());
                if (!(entity instanceof Villager villager)) continue;
                var attr = villager.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (attr == null || attr.getModifiers().isEmpty()) continue;
                new ArrayList<>(attr.getModifiers()).forEach(attr::removeModifier);
            }
        }, 0L, 10L);
    }


    // ─── Utilitaires ─────────────────────────────────────────────────────────

    /** Distance horizontale XZ (sans Y) — évite les faux despawns en hauteur. */
    private double hDist(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Sélectionne un mob parmi la liste selon les poids définis dans la zone.
     * Un mob sans poids explicite a un poids de 1.0.
     */
    private String pickWeightedMob(List<String> mobIds, ZoneData zone) {
        if (mobIds.size() == 1) return mobIds.get(0);
        double total = mobIds.stream().mapToDouble(zone::getMobWeight).sum();
        double roll  = random.nextDouble() * total;
        double cum   = 0;
        for (String id : mobIds) {
            cum += zone.getMobWeight(id);
            if (roll < cum) return id;
        }
        return mobIds.get(mobIds.size() - 1); // fallback (arrondi flottant)
    }

    private void killZoneMobs(String zoneId) {
        Iterator<MobInstance> it = mobManager.getAllInstances().iterator();
        while (it.hasNext()) {
            MobInstance inst = it.next();
            if (!inst.getZoneId().equals(zoneId)) continue;
            Entity e = Bukkit.getEntity(inst.getUuid());
            if (e != null) e.remove();
            it.remove();
        }
    }

    private Location getRandomLocation(ZoneData zone) {
        if (zone.getPos1() == null || zone.getPos2() == null) return null;
        if (zone.getPos1().getWorld() == null) return null;

        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());
        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());

        if (minX == maxX && minZ == maxZ)
            return zone.getPos1().getWorld()
                    .getHighestBlockAt(zone.getPos1()).getLocation().add(0.5, 1, 0.5);

        for (int i = 0; i < 10; i++) {
            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);
            Location loc = zone.getPos1().getWorld()
                    .getHighestBlockAt((int) x, (int) z).getLocation().add(0.5, 1, 0.5);
            if (!loc.getBlock().isLiquid()
                    && !loc.clone().subtract(0, 1, 0).getBlock().isLiquid()) return loc;
        }
        return getCenter(zone);
    }

    public boolean isInZone(Location loc, ZoneData zone) {
        if (zone.getPos1() == null || zone.getPos2() == null) return false;
        if (zone.getPos1().getWorld() == null) return false;
        if (!zone.getPos1().getWorld().equals(loc.getWorld())) return false;
        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());
        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public Location getCenter(ZoneData zone) {
        if (zone.getPos1() == null || zone.getPos2() == null) return null;
        double x = (zone.getPos1().getX() + zone.getPos2().getX()) / 2;
        double z = (zone.getPos1().getZ() + zone.getPos2().getZ()) / 2;
        double y = (zone.getPos1().getY() + zone.getPos2().getY()) / 2;
        return new Location(zone.getPos1().getWorld(), x, y, z);
    }

    // ─── Visualisation ───────────────────────────────────────────────────────

    // ── API prévisualisation bordures ──────────────────────────────────────

    /** Active/désactive la prévisualisation de la bordure d'une zone pour un joueur. */
    public void toggleBorderPreview(Player player, String zoneId) {
        UUID uuid = player.getUniqueId();
        Set<String> set = previewingPlayers.computeIfAbsent(uuid, k -> new HashSet<>());
        if (!set.add(zoneId)) {
            set.remove(zoneId);
            if (set.isEmpty()) previewingPlayers.remove(uuid);
        }
    }

    /** Retourne true si le joueur prévisualise actuellement la bordure de cette zone. */
    public boolean isPreviewingBorder(Player player, String zoneId) {
        Set<String> set = previewingPlayers.get(player.getUniqueId());
        return set != null && set.contains(zoneId);
    }

    // ── Calcul distance hors zone ─────────────────────────────────────────

    /**
     * Retourne la distance (XZ) entre la position et le bord de la zone.
     * Retourne 0 si la position est à l'intérieur.
     */
    private double getDistanceOutside(Location loc, ZoneData zone) {
        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());
        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());
        double dx = Math.max(0.0, Math.max(minX - loc.getX(), loc.getX() - maxX));
        double dz = Math.max(0.0, Math.max(minZ - loc.getZ(), loc.getZ() - maxZ));
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ── Rebond ────────────────────────────────────────────────────────────

    /**
     * Applique une vélocité de rebond vers l'intérieur de la zone.
     * La force augmente légèrement avec la distance au-delà de la tolérance.
     */
    private void applyBounce(Mob mob, ZoneData zone, double distOutside) {
        Location loc = mob.getLocation();
        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());
        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());

        // Point le plus proche à l'intérieur de la zone
        double nearX = Math.max(minX, Math.min(maxX, loc.getX()));
        double nearZ = Math.max(minZ, Math.min(maxZ, loc.getZ()));

        double dx = nearX - loc.getX();
        double dz = nearZ - loc.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) return;

        // Force légèrement progressive selon l'excès de distance
        double excess = distOutside - zone.getBoundaryTolerance();
        double scale  = Math.min(1.0 + excess * 0.15, 2.5);
        double str    = zone.getBounceStrength() * scale;

        mob.setVelocity(new Vector(dx / len * str, 0.12, dz / len * str));
    }

    // ── Dessin bordure de zone ────────────────────────────────────────────

    /**
     * Dessine la prévisualisation complète d'une zone pour un joueur :
     *  ■ Blanc        = bordure réelle (rectangle XZ)
     *  ■ Orange       = bordure + tolérance (si tolérance > 0)
     *  ■ Cyan         = rayon de spawn min   (cercle centré sur la zone)
     *  ■ Bleu clair   = rayon de spawn max
     *  ■ Vert         = seuil despawn ratio1 × distance
     *  ■ Jaune        = seuil despawn ratio2 × distance
     *  ■ Rouge-orange = seuil despawn ratio3 × distance
     */
    private void drawZoneBorder(Player player, ZoneData zone) {
        if (zone.getPos1() == null || zone.getPos2() == null) return;
        World world = zone.getPos1().getWorld();
        if (world == null || !world.equals(player.getWorld())) return;

        double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX());
        double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX());
        double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ());
        double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ());

        Location pLoc = player.getLocation();
        final double MAX_DIST_SQ = 100.0 * 100.0;

        // Pas adaptatif : ~60 points max par bord pour éviter le lag
        double longestEdge = Math.max(maxX - minX, maxZ - minZ);
        double step = Math.max(1.0, longestEdge / 60.0);
        double y = pLoc.getY() + 1.0;

        // ── Rectangles (bordure zone) ──────────────────────────────────────
        Particle.DustOptions white  = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f);
        for (double yOff : new double[]{0.1, 2.0}) {
            drawBorderRect(player, world, minX, maxX, minZ, maxZ, pLoc.getY() + yOff, step, white, pLoc, MAX_DIST_SQ);
        }

        double tol = zone.getBoundaryTolerance();
        if (tol > 0) {
            Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.2f);
            for (double yOff : new double[]{0.1, 2.0}) {
                drawBorderRect(player, world,
                        minX - tol, maxX + tol, minZ - tol, maxZ + tol,
                        pLoc.getY() + yOff, step, orange, pLoc, MAX_DIST_SQ);
            }
        }

        // ── Cercles centrés sur la zone (spawn + despawn) ──────────────────
        Location center = getCenter(zone);
        if (center == null) return;

        // Spawn
        drawCircleFiltered(player, center, zone.getSpawnRadiusMin(), y,
                Color.fromRGB(0, 230, 230),   MAX_DIST_SQ); // CYAN   — spawn min
        drawCircleFiltered(player, center, zone.getSpawnRadiusMax(), y,
                Color.fromRGB(80, 150, 255),  MAX_DIST_SQ); // BLEU   — spawn max

        // Despawn
        double d = zone.getDespawnDistance();
        drawCircleFiltered(player, center, d * zone.getDespawnRatio1(), y,
                Color.fromRGB(0, 255, 50),    MAX_DIST_SQ); // VERT   — ratio1
        drawCircleFiltered(player, center, d * zone.getDespawnRatio2(), y,
                Color.fromRGB(255, 230, 0),   MAX_DIST_SQ); // JAUNE  — ratio2
        drawCircleFiltered(player, center, d * zone.getDespawnRatio3(), y,
                Color.fromRGB(255, 80, 0),    MAX_DIST_SQ); // ORANGE — ratio3

        // ── Cercles centrés sur le JOUEUR — grandes particules (spawn + seuils despawn) ──
        // Ces cercles n'ont pas de filtre de distance : ils sont toujours centrés sur le joueur.
        // Dessinés à deux hauteurs pour une meilleure visibilité.
        for (double yOff : new double[]{0.1, 2.0}) {
            double yP = pLoc.getY() + yOff;
            drawCircleNoFilter(player, pLoc, zone.getSpawnRadiusMin(), yP,
                    Color.fromRGB(0, 230, 230), 2.5f);   // CYAN   — spawn min
            drawCircleNoFilter(player, pLoc, zone.getSpawnRadiusMax(), yP,
                    Color.fromRGB(80, 150, 255), 2.5f);  // BLEU   — spawn max
            drawCircleNoFilter(player, pLoc, d * zone.getDespawnRatio1(), yP,
                    Color.fromRGB(0, 255, 50), 2.5f);    // VERT   — ratio1
            drawCircleNoFilter(player, pLoc, d * zone.getDespawnRatio2(), yP,
                    Color.fromRGB(255, 230, 0), 2.5f);   // JAUNE  — ratio2
            drawCircleNoFilter(player, pLoc, d * zone.getDespawnRatio3(), yP,
                    Color.fromRGB(255, 80, 0), 2.5f);    // ORANGE — ratio3
        }
    }

    /** Cercle de particules centré sur {@code center}, filtré à {@code maxDistSq} du joueur. */
    private void drawCircleFiltered(Player player, Location center, double radius, double y,
                                    Color color, double maxDistSq) {
        if (radius <= 0) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
        int points = (int) Math.min(200, Math.max(40, radius * 2.5));
        double step = 2 * Math.PI / points;
        Location pLoc = player.getLocation();
        for (double a = 0; a < 2 * Math.PI; a += step) {
            double px = center.getX() + Math.cos(a) * radius;
            double pz = center.getZ() + Math.sin(a) * radius;
            double ddx = px - pLoc.getX(), ddz = pz - pLoc.getZ();
            if (ddx * ddx + ddz * ddz <= maxDistSq)
                player.spawnParticle(Particle.REDSTONE,
                        new Location(center.getWorld(), px, y, pz), 1, 0, 0, 0, 0, dust);
        }
    }

    /**
     * Cercle de particules centré sur {@code center} SANS filtre de distance.
     * Utilisé pour les cercles centrés sur le joueur (tous les points sont toujours visibles).
     *
     * @param size  taille des particules REDSTONE (2.0–3.0 pour une bonne visibilité)
     */
    private void drawCircleNoFilter(Player player, Location center, double radius, double y,
                                    Color color, float size) {
        if (radius <= 0) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        // Densité adaptée : ~1 particule / 1.5 bloc de périmètre, max 300 points
        int points = (int) Math.min(300, Math.max(80, radius * 2.1));
        double step = 2 * Math.PI / points;
        for (double a = 0; a < 2 * Math.PI; a += step) {
            player.spawnParticle(Particle.REDSTONE,
                    new Location(center.getWorld(),
                            center.getX() + Math.cos(a) * radius,
                            y,
                            center.getZ() + Math.sin(a) * radius),
                    1, 0, 0, 0, 0, dust);
        }
    }

    /** Dessine les 4 bords d'un rectangle de particules, filtré par distance au joueur. */
    private void drawBorderRect(Player player, World world,
                                double minX, double maxX, double minZ, double maxZ,
                                double y, double step, Particle.DustOptions dust,
                                Location pLoc, double maxDistSq) {
        int stepsX = Math.max(1, (int) Math.ceil((maxX - minX) / step));
        int stepsZ = Math.max(1, (int) Math.ceil((maxZ - minZ) / step));
        double dx = (maxX - minX) / stepsX;
        double dz = (maxZ - minZ) / stepsZ;

        for (int i = 0; i <= stepsX; i++) {
            double x = minX + i * dx;
            borderParticle(player, world, x, y, minZ, dust, pLoc, maxDistSq);
            borderParticle(player, world, x, y, maxZ, dust, pLoc, maxDistSq);
        }
        for (int i = 1; i < stepsZ; i++) { // coins déjà couverts par la boucle X
            double z = minZ + i * dz;
            borderParticle(player, world, minX, y, z, dust, pLoc, maxDistSq);
            borderParticle(player, world, maxX, y, z, dust, pLoc, maxDistSq);
        }
    }

    /** Spawn une particule uniquement si elle est à portée du joueur. */
    private void borderParticle(Player player, World world, double x, double y, double z,
                                 Particle.DustOptions dust, Location pLoc, double maxDistSq) {
        double ddx = x - pLoc.getX(), ddz = z - pLoc.getZ();
        if (ddx * ddx + ddz * ddz <= maxDistSq)
            player.spawnParticle(Particle.REDSTONE, new Location(world, x, y, z), 1, 0, 0, 0, 0, dust);
    }

    /**
     * Dessine un cercle horizontal de particules DUST colorées,
     * visible UNIQUEMENT par le joueur spécifié (pas d'impact pour les autres).
     *
     * @param player  destinataire des particules
     * @param center  centre du cercle (XZ utilisés, Y remplacé par le paramètre y)
     * @param radius  rayon en blocs
     * @param y       hauteur fixe du cercle
     * @param color   couleur Bukkit des particules
     */
    private void drawCircle(Player player, Location center, double radius, double y, Color color) {
        if (radius <= 0) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
        // Nombre de points proportionnel au rayon (min 40, max 180)
        int points = (int) Math.min(180, Math.max(40, radius * 2.0));
        double step = 2 * Math.PI / points;
        for (double a = 0; a < 2 * Math.PI; a += step) {
            player.spawnParticle(Particle.REDSTONE,
                    new Location(center.getWorld(),
                            center.getX() + Math.cos(a) * radius,
                            y,
                            center.getZ() + Math.sin(a) * radius),
                    1, 0, 0, 0, 0, dust);
        }
    }
}
