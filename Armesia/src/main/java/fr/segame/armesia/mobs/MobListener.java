package fr.segame.armesia.mobs;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.loot.LootManager;
import fr.segame.armesia.managers.DebugManager;
import fr.segame.armesia.managers.LevelManager;
import fr.segame.armesia.zones.ZoneData;
import fr.segame.armesia.zones.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Random;

public class MobListener implements Listener {

    private final MobManager mobManager;
    private final LootManager lootManager;
    private final ZoneManager zoneManager;
    private final DebugManager debug;
    private final EconomyAPI economyAPI;
    private final LevelManager levelManager;
    private final Random random = new Random();

    public MobListener(MobManager mobManager, LootManager lootManager,
                       ZoneManager zoneManager, DebugManager debug,
                       EconomyAPI economyAPI, LevelManager levelManager) {
        this.mobManager  = mobManager;
        this.lootManager = lootManager;
        this.zoneManager = zoneManager;
        this.debug       = debug;
        this.economyAPI  = economyAPI;
        this.levelManager = levelManager;
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

    // 🧹 CLEAN MOBS EXISTANTS AU CHARGEMENT DE CHUNK
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (var entity : event.getChunk().getEntities()) {
            if (entity instanceof org.bukkit.entity.Player) continue;
            if (entity instanceof org.bukkit.entity.Monster
                    || entity instanceof org.bukkit.entity.Animals
                    || entity instanceof org.bukkit.entity.Golem) {
                if (mobManager.getInstance(entity.getUniqueId()) == null) {
                    debug.logVerbose("§6[CLEANUP]§7 type=§f" + entity.getType().name()
                            + "§7 raison=§6NON_ENREGISTRÉ");
                    entity.remove();
                }
            }
        }
    }

    // 💀 MORT
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        MobInstance instance = mobManager.getInstance(event.getEntity().getUniqueId());
        if (instance == null) return;

        event.getDrops().clear();

        Player player = event.getEntity().getKiller();
        if (player == null) return;

        MobData data = mobManager.getMob(instance.getMobId());
        if (data == null) return;

        lootManager.dropLoot(event.getEntity().getLocation(), data.getLootTable());

        // Tirage aléatoire argent
        int money = randBetween(data.getMoneyMin(), data.getMoneyMax());
        // Tirage aléatoire XP
        int xp = randBetween(data.getXpMin(), data.getXpMax());

        if (money > 0) economyAPI.addMoney(player.getUniqueId(), money);
        if (xp > 0)    levelManager.addXP(player.getUniqueId(), xp);

        // Message de kill
        String mobName = ChatColor.translateAlternateColorCodes('&', data.getName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&aVous avez tué " + mobName + " &a→ &6+" + money + "$ &7| &b+" + xp + "XP"));

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
}