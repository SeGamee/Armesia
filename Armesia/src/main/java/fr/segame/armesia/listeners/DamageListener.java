package fr.segame.armesia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DamageListener implements Listener {

    private final JavaPlugin plugin; // 👈 1. variable

    // 👇 2. constructeur (C’EST ICI que tu mets ton code)
    public DamageListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 👇 3. ton event
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity entity)) return;

        EntityDamageEvent.DamageCause cause = e.getCause();

        if (cause != EntityDamageEvent.DamageCause.FIRE &&
                cause != EntityDamageEvent.DamageCause.FIRE_TICK) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            entity.setMaximumNoDamageTicks(0);
            entity.setNoDamageTicks(0);
        });
    }
}