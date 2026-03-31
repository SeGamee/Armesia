package fr.segame.armesia.mobs;

import fr.segame.armesia.Main;
import fr.segame.armesia.loot.LootManager;
import fr.segame.armesia.zones.ZoneData;
import fr.segame.armesia.zones.ZoneManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;

public class MobListener implements Listener {

    private final MobManager mobManager;
    private final LootManager lootManager;
    private final ZoneManager zoneManager;

    public MobListener(MobManager mobManager, LootManager lootManager, ZoneManager zoneManager) {
        this.mobManager = mobManager;
        this.lootManager = lootManager;
        this.zoneManager = zoneManager;
    }

    // ❌ BLOQUE MOBS VANILLA
    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    // 🧹 CLEAN MOBS EXISTANTS
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (var entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity) {
                if (mobManager.getInstance(entity.getUniqueId()) == null) {
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

        lootManager.applyLoot(player, data.getLootTable());

        Main.getInstance().getLevelManager().addXP(player.getUniqueId(), data.getXp());
        Main.getInstance().getEconomyAPI().addMoney(player.getUniqueId(), data.getMoney());

        mobManager.removeInstance(event.getEntity().getUniqueId());
    }

    // 🚫 PAS DE MORT ENVIRONNEMENT
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof org.bukkit.entity.Mob victim)) return;

        MobInstance instance = mobManager.getInstance(victim.getUniqueId());
        if (instance == null) return;

        // si l'attaquant est un mob → on bloque
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

        // ❌ si ce n’est pas un joueur → on annule
        if (!(event.getTarget() instanceof org.bukkit.entity.Player player)) {
            event.setCancelled(true);
            return;
        }

        // 🔥 AJOUT ICI (ton code)
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