package fr.segame.armesiaMobs.mobs;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesiaLevel.api.LevelAPI;
import fr.segame.armesiaMobs.loot.LootManager;
import fr.segame.armesiaMobs.managers.DebugManager;
import fr.segame.armesiaMobs.zones.ZoneData;
import fr.segame.armesiaMobs.zones.ZoneManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Random;

public class MobListener implements Listener {

    private final MobManager mobManager;
    private final LootManager lootManager;
    private final ZoneManager zoneManager;
    private final DebugManager debug;
    private final EconomyAPI economyAPI;
    private final Random random = new Random();

    public MobListener(MobManager mobManager, LootManager lootManager,
                       ZoneManager zoneManager, DebugManager debug,
                       EconomyAPI economyAPI) {
        this.mobManager  = mobManager;
        this.lootManager = lootManager;
        this.zoneManager = zoneManager;
        this.debug       = debug;
        this.economyAPI  = economyAPI;
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

        if (money > 0) economyAPI.addMoney(player.getUniqueId(), money);
        if (xp > 0)    LevelAPI.addXP(player.getUniqueId(), xp);

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

    // 🕊️ VILLAGEOIS PACIFIQUES — annuler tous les dégâts sauf ceux des joueurs
    //    Les dégâts de l'environnement / mobs déclenchent le Brain PANIC du villageois
    //    ce qui active un boost de vitesse via le pathfinder.
    //    Les joueurs peuvent toujours les tuer pour obtenir leur loot (direct + projectile).
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