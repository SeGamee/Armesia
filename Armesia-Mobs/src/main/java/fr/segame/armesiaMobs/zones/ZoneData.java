package fr.segame.armesiaMobs.zones;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZoneData {

    private final String id;
    private Location pos1;
    private Location pos2;
    private final List<String> mobs;
    /** Poids de spawn par mobId (défaut 1.0 = probabilité égale). */
    private final Map<String, Double> mobWeights = new HashMap<>();
    private int priority;
    private boolean inheritMobs;
    private boolean overrideMobs;

    /** Cap global de mobs simultanés dans la zone (0 = illimité) */
    private int max;

    // ── Spawn ─────────────────────────────────────────────────────────────────
    /** Distance min/max autour du joueur pour choisir le point de spawn */
    private double spawnRadiusMin;
    private double spawnRadiusMax;

    /** Nombre cible de mobs autour d'un joueur */
    private int targetMin;
    private int targetMax;

    /** Intervalle en secondes entre deux tentatives de spawn (1 = chaque seconde) */
    private int spawnInterval;

    // ── Despawn ───────────────────────────────────────────────────────────────
    /** Distance joueur → mob au-delà de laquelle le mob peut despawner */
    private double despawnDistance;

    // ── Conditions de spawn ───────────────────────────────────────────────────
    /** Heure requise pour spawner (ALWAYS / DAY / NIGHT) */
    private SpawnCondition spawnCondition = SpawnCondition.ALWAYS;

    /** Multiplicateur appliqué à la probabilité de spawn calculée (0.0 – 1.0) */
    private double spawnChance = 1.0;

    // ── Paliers de boost spawn ────────────────────────────────────────────────
    //  Quand nearby < ratioN × target, la chance est multipliée et un burst est déclenché.
    //  Tier 1 = moins de mobs (spawn le plus agressif), tier 3 = presque au cap.

    /** Seuil tier 1 : nearby < ratio1 × target  (défaut 0.25 = < 25% du target) */
    private double spawnBoostRatio1 = 0.25;
    /** Seuil tier 2 : nearby < ratio2 × target  (défaut 0.50 = < 50% du target) */
    private double spawnBoostRatio2 = 0.50;
    /** Seuil tier 3 : nearby < ratio3 × target  (défaut 0.75 = < 75% du target) */
    private double spawnBoostRatio3 = 0.75;

    /** Multiplicateur de chance pour le tier 1 (appliqué à spawnChance) */
    private double spawnBoostMultiplier1 = 3.0;
    /** Multiplicateur de chance pour le tier 2 */
    private double spawnBoostMultiplier2 = 2.0;
    /** Multiplicateur de chance pour le tier 3 */
    private double spawnBoostMultiplier3 = 1.5;

    /** Nombre de mobs spawnés en une seule vague pour le tier 1 */
    private int spawnBoostCount1 = 3;
    /** Nombre de mobs spawnés en une seule vague pour le tier 2 */
    private int spawnBoostCount2 = 2;
    /** Nombre de mobs spawnés en une seule vague pour le tier 3 (1 = pas de burst) */
    private int spawnBoostCount3 = 1;

    // ── Paliers de despawn — probabilités ────────────────────────────────────
    /** dist < ratio1 × despawnDistance  → probabilité de despawn */
    private double despawnChanceClose = 0.0;
    /** ratio1 – ratio2 × despawnDistance → probabilité de despawn */
    private double despawnChanceMid   = 0.2;
    /** ratio2 – ratio3 × despawnDistance → probabilité de despawn */
    private double despawnChanceFar   = 0.4;
    /** > ratio3 × despawnDistance        → probabilité de despawn */
    private double despawnChanceOuter = 0.7;

    // ── Paliers de despawn — seuils de distance (ratios × despawnDistance) ──
    private double despawnRatio1 = 0.50;  // limite close / mid
    private double despawnRatio2 = 0.75;  // limite mid  / far
    private double despawnRatio3 = 1.00;  // limite far  / outer

    /** Fréquence de vérification du despawn en secondes (min 1) */
    private int despawnCheckInterval = 5;

    // ── Frontière ─────────────────────────────────────────────────────────────
    /** Distance max autorisée hors de la zone avant repulsion (0 = dès la sortie) */
    private double boundaryTolerance = 0.0;

    /** Intensité du rebond appliqué aux mobs sortant de la zone (blocs/tick, ~0.3–1.0) */
    private double bounceStrength = 0.5;

    public ZoneData(String id, Location pos1, Location pos2, List<String> mobs, int max) {
        this.id   = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.mobs = new ArrayList<>(mobs);
        this.max  = max;

        this.spawnRadiusMin  = 20;
        this.spawnRadiusMax  = 40;
        this.targetMin       = 4;
        this.targetMax       = 6;
        this.spawnInterval   = 5;
        this.despawnDistance = 150;
    }

    public String getId() { return id; }

    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }

    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }

    public List<String> getMobs() { return mobs; }

    public int getPriority() { return priority; }
    public void setPriority(int v) { this.priority = v; }

    public boolean isInheritMobs() { return inheritMobs; }
    public void setInheritMobs(boolean v) { this.inheritMobs = v; }

    public boolean isOverrideMobs() { return overrideMobs; }
    public void setOverrideMobs(boolean v) { this.overrideMobs = v; }

    public int getMax() { return max; }
    public void setMax(int v) { this.max = v; }

    public double getSpawnRadiusMin() { return spawnRadiusMin; }
    public void setSpawnRadiusMin(double v) { this.spawnRadiusMin = v; }

    public double getSpawnRadiusMax() { return spawnRadiusMax; }
    public void setSpawnRadiusMax(double v) { this.spawnRadiusMax = v; }

    public int getTargetMin() { return targetMin; }
    public void setTargetMin(int v) { this.targetMin = v; }

    public int getTargetMax() { return targetMax; }
    public void setTargetMax(int v) { this.targetMax = v; }

    public int getSpawnInterval() { return spawnInterval; }
    public void setSpawnInterval(int v) { this.spawnInterval = Math.max(1, v); }

    public double getDespawnDistance() { return despawnDistance; }
    public void setDespawnDistance(double v) { this.despawnDistance = v; }

    public SpawnCondition getSpawnCondition() { return spawnCondition; }
    public void setSpawnCondition(SpawnCondition v) { this.spawnCondition = v; }

    public double getSpawnChance() { return spawnChance; }
    public void setSpawnChance(double v) { this.spawnChance = Math.max(0.0, Math.min(1.0, v)); }

    // ── Boost spawn ───────────────────────────────────────────────────────────

    public double getSpawnBoostRatio1() { return spawnBoostRatio1; }
    public void setSpawnBoostRatio1(double v) { this.spawnBoostRatio1 = Math.max(0.0, v); }

    public double getSpawnBoostRatio2() { return spawnBoostRatio2; }
    public void setSpawnBoostRatio2(double v) { this.spawnBoostRatio2 = Math.max(0.0, v); }

    public double getSpawnBoostRatio3() { return spawnBoostRatio3; }
    public void setSpawnBoostRatio3(double v) { this.spawnBoostRatio3 = Math.max(0.0, v); }

    public double getSpawnBoostMultiplier1() { return spawnBoostMultiplier1; }
    public void setSpawnBoostMultiplier1(double v) { this.spawnBoostMultiplier1 = Math.max(1.0, v); }

    public double getSpawnBoostMultiplier2() { return spawnBoostMultiplier2; }
    public void setSpawnBoostMultiplier2(double v) { this.spawnBoostMultiplier2 = Math.max(1.0, v); }

    public double getSpawnBoostMultiplier3() { return spawnBoostMultiplier3; }
    public void setSpawnBoostMultiplier3(double v) { this.spawnBoostMultiplier3 = Math.max(1.0, v); }

    public int getSpawnBoostCount1() { return spawnBoostCount1; }
    public void setSpawnBoostCount1(int v) { this.spawnBoostCount1 = Math.max(1, v); }

    public int getSpawnBoostCount2() { return spawnBoostCount2; }
    public void setSpawnBoostCount2(int v) { this.spawnBoostCount2 = Math.max(1, v); }

    public int getSpawnBoostCount3() { return spawnBoostCount3; }
    public void setSpawnBoostCount3(int v) { this.spawnBoostCount3 = Math.max(1, v); }

    public double getDespawnChanceClose() { return despawnChanceClose; }
    public void setDespawnChanceClose(double v) { this.despawnChanceClose = v; }

    public double getDespawnChanceMid() { return despawnChanceMid; }
    public void setDespawnChanceMid(double v) { this.despawnChanceMid = v; }

    public double getDespawnChanceFar() { return despawnChanceFar; }
    public void setDespawnChanceFar(double v) { this.despawnChanceFar = v; }

    public double getDespawnChanceOuter() { return despawnChanceOuter; }
    public void setDespawnChanceOuter(double v) { this.despawnChanceOuter = v; }

    public double getDespawnRatio1() { return despawnRatio1; }
    public void setDespawnRatio1(double v) { this.despawnRatio1 = Math.max(0.0, v); }

    public double getDespawnRatio2() { return despawnRatio2; }
    public void setDespawnRatio2(double v) { this.despawnRatio2 = Math.max(0.0, v); }

    public double getDespawnRatio3() { return despawnRatio3; }
    public void setDespawnRatio3(double v) { this.despawnRatio3 = Math.max(0.0, v); }

    public int getDespawnCheckInterval() { return despawnCheckInterval; }
    public void setDespawnCheckInterval(int v) { this.despawnCheckInterval = Math.max(1, v); }

    public double getBoundaryTolerance() { return boundaryTolerance; }
    public void setBoundaryTolerance(double v) { this.boundaryTolerance = Math.max(0.0, v); }

    public double getBounceStrength() { return bounceStrength; }
    public void setBounceStrength(double v) { this.bounceStrength = Math.max(0.0, v); }

    // ── Poids de spawn ────────────────────────────────────────────────────────

    /** Retourne le poids de spawn d'un mob (1.0 si non défini). */
    public double getMobWeight(String mobId) {
        return mobWeights.getOrDefault(mobId, 1.0);
    }

    /**
     * Définit le poids de spawn d'un mob.
     * @param weight positif ; si ≤ 0 le poids explicite est retiré (retour à 1.0 par défaut)
     */
    public void setMobWeight(String mobId, double weight) {
        if (weight <= 0) mobWeights.remove(mobId);
        else             mobWeights.put(mobId, weight);
    }

    /** Vue non-modifiable des poids explicitement définis (ne contient pas les défauts 1.0). */
    public Map<String, Double> getMobWeights() { return Collections.unmodifiableMap(mobWeights); }
}