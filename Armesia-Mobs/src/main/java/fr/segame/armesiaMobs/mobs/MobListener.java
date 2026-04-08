package fr.segame.armesiaMobs.mobs;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesiaLevel.api.LevelAPI;
import fr.segame.armesiaMobs.ArmesiaMobs;
import fr.segame.armesiaMobs.loot.LootManager;
import fr.segame.armesiaMobs.managers.DebugManager;
import fr.segame.armesiaMobs.managers.StatsManager;
import fr.segame.armesiaMobs.zones.ZoneData;
import fr.segame.armesiaMobs.zones.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;


public class MobListener implements Listener {

    private final MobManager mobManager;
    private final LootManager lootManager;
    private final ZoneManager zoneManager;
    private final DebugManager debug;
    private final EconomyAPI economyAPI;
    private final StatsManager statsManager;
    private final Random random = new Random();

    public MobListener(MobManager mobManager, LootManager lootManager,
                       ZoneManager zoneManager, DebugManager debug,
                       EconomyAPI economyAPI, StatsManager statsManager) {
        this.mobManager   = mobManager;
        this.lootManager  = lootManager;
        this.zoneManager  = zoneManager;
        this.debug        = debug;
        this.economyAPI   = economyAPI;
        this.statsManager = statsManager;
    }

    // ❌ BLOQUE MOBS VANILLA
    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            debug.logVerbose("§6[BLOCKED]§7 type=§f" + event.getEntityType().name()
                    + "§7 raison=§6" + event.getSpawnReason().name());
            event.setCancelled(true);
        }
    }

    // 🔄 NETTOYAGE des chunks chargés — supprime/réenregistre les mobs sans tag PDC
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Différé d'un tick pour que les entités du chunk soient bien accessibles
        Bukkit.getScheduler().runTask(ArmesiaMobs.getInstance(), () -> {
            for (Entity entity : event.getChunk().getEntities()) {
                processLoadedEntity(entity);
            }
        });
    }

    /** Vérifie un mob chargé : sans PDC → supprimé ; PDC mais sans instance → réenregistré. */
    private void processLoadedEntity(Entity entity) {
        if (!(entity instanceof org.bukkit.entity.Mob)) return;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        if (!pdc.has(ArmesiaMobs.MOB_ID_KEY, PersistentDataType.STRING)) {
            // Mob vanilla ou orphelin sans tag → suppression
            entity.remove();
            debug.logVerbose("§c[CHUNK-CLEANUP]§7 entité=§f" + entity.getType().name()
                    + "§7 uuid=§f" + entity.getUniqueId() + "§7 raison=§cPAS_DE_PDC");
            return;
        }

        // Déjà enregistré → rien à faire
        if (mobManager.getInstance(entity.getUniqueId()) != null) return;

        String mobId  = pdc.get(ArmesiaMobs.MOB_ID_KEY,  PersistentDataType.STRING);
        String zoneId = pdc.get(ArmesiaMobs.ZONE_ID_KEY, PersistentDataType.STRING);

        if (mobId == null || mobManager.getMob(mobId) == null) {
            // La définition du mob n'existe plus → suppression
            entity.remove();
            debug.log("§c[CHUNK-CLEANUP]§7 mob=§f" + mobId + "§7 raison=§cDEF_SUPPRIMEE");
            return;
        }

        // Réenregistrement après redémarrage
        mobManager.addInstance(new MobInstance(entity.getUniqueId(), mobId,
                zoneId != null ? zoneId : "manual"));
        debug.logVerbose("§b[CHUNK-RESTORE]§7 mob=§f" + mobId + "§7 zone=§f" + zoneId);
    }

    // 🗑️ DROPS — vide pour TOUS les mobs (vanilla ou custom)
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // Toujours supprimer les drops et l'XP vanilla, peu importe le mob
        event.getDrops().clear();
        event.setDroppedExp(0);

        MobInstance instance = mobManager.getInstance(event.getEntity().getUniqueId());
        if (instance == null) return;

        Player player = event.getEntity().getKiller();
        if (player == null) return;

        MobData data = mobManager.getMob(instance.getMobId());
        if (data == null) return;

        lootManager.dropLoot(event.getEntity().getLocation(), data.getLootTable());

        int money = randBetween(data.getMoneyMin(), data.getMoneyMax());
        int xp    = randBetween(data.getXpMin(),    data.getXpMax());

        if (money > 0 && economyAPI != null) economyAPI.addMoney(player.getUniqueId(), money);
        if (xp > 0)    LevelAPI.addXP(player.getUniqueId(), xp);

        // Stats
        statsManager.addKill(player.getUniqueId(), data.getId());
        statsManager.save();

        String mobName = ChatColor.translateAlternateColorCodes('&', data.getName());
        ArmesiaMobs.getInstance().getMessages()
                .getLines("kill.notification", "mob", mobName, "money", money, "xp", xp)
                .forEach(player::sendMessage);

        mobManager.removeInstance(event.getEntity().getUniqueId());
    }

    /** Retourne un entier aléatoire entre min et max inclus. Si min >= max, retourne min. */
    private int randBetween(int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }

    // 🚫 PAS DE DÉGÂTS MOB VS MOB
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Mob victim)) return;

        MobInstance instance = mobManager.getInstance(victim.getUniqueId());
        if (instance == null) return;

        if (event.getDamager() instanceof org.bukkit.entity.Mob) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Monster monster)) return;

        MobInstance instance = mobManager.getInstance(monster.getUniqueId());
        if (instance == null) return;

        if (event.getTarget() == null) return;

        if (!(event.getTarget() instanceof org.bukkit.entity.Player player)) {
            event.setCancelled(true);
            return;
        }

        ZoneData zone = zoneManager.getZone(instance.getZoneId());
        if (zone == null) return;

        if (!zoneManager.isInZone(player.getLocation(), zone)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        MobInstance instance = mobManager.getInstance(event.getEntity().getUniqueId());
        if (instance == null) return;

        event.setCancelled(true);
    }

    // 🕊️ VILLAGEOIS PACIFIQUES — annuler tous les dégâts sauf joueurs et explosions
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager)) return;
        if (mobManager.getInstance(event.getEntity().getUniqueId()) == null) return;

        if (event instanceof EntityDamageByEntityEvent dmg) {
            // Coup direct d'un joueur
            if (dmg.getDamager() instanceof Player) return;
            // Projectile (flèche, trident…) tiré par un joueur
            if (dmg.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player) return;
        }

        // Explosions (TNT, Creeper, etc.) — autoriser le dégât
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;

        event.setCancelled(true);
    }

    // 💊 PAS DE RÉGÉNÉRATION — bloque tout regen vanilla (sommeil, nourriture, potion…)
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent event) {
        if (mobManager.getInstance(event.getEntity().getUniqueId()) == null) return;
        event.setCancelled(true);
    }

    // 🐾 PAS D'APPRIVOISEMENT
    @EventHandler
    public void onTame(EntityTameEvent event) {
        event.setCancelled(true);
    }

    // 🚫 PAS D'INTERACTION VILLAGEOIS (commerce, GUI)
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof AbstractVillager) {
            event.setCancelled(true);
        }
    }
}