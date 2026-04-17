package fr.segame.armesiaCrackShotAddon;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReloadListener implements Listener {

    private final CSUtility csUtility = new CSUtility();
    private final Map<String, ConfigurationSection> cache = new HashMap<>();

    private final Map<UUID, BukkitTask> reloadTasks = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Map<UUID, String> reloadWeapons = new HashMap<>();
    private final Map<UUID, Long> lastReload = new HashMap<>();

    // 🔥 identifiant unique du reload en cours
    private final Map<UUID, Integer> reloadSession = new HashMap<>();
    private int sessionCounter = 0;

    private final Pattern ammoPattern = Pattern.compile("«(\\d+)»");

    public void clearCache() {
        cache.clear();
    }

    @EventHandler
    public void onItemChange(PlayerItemHeldEvent e) {
        cancelReloadInterrupted(e.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        if (!reloadTasks.containsKey(uuid)) return;

        // Clic sur la hotbar (slot 0-8) → annule
        if (e.getSlot() >= 0 && e.getSlot() <= 8) {
            cancelReloadInterrupted(player);
            return;
        }

        // Touche numérique depuis l'inventaire (NUMBER_KEY) qui cible le slot de reload → annule
        if (e.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = e.getHotbarButton(); // 0-indexed
            int currentReloadSlot = reloadSlot.getOrDefault(uuid, -1);
            if (hotbarButton == currentReloadSlot) {
                cancelReloadInterrupted(player);
            }
        }
    }

    @EventHandler
    public void onReload(WeaponReloadEvent event) {

        Player player = event.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        long now = System.currentTimeMillis();
        long last = lastReload.getOrDefault(uuid, 0L);

        // évite les doubles triggers trop proches
        if (now - last < 250) {
            return;
        }

        lastReload.put(uuid, now);

        // si un reload visuel existe déjà, on ne le recrée pas
        if (reloadTasks.containsKey(uuid)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        String weapon = csUtility.getWeaponTitle(item);
        if (weapon == null) weapon = "Weapon";

        reloadWeapons.put(uuid, weapon);
        reloadSlot.put(uuid, player.getInventory().getHeldItemSlot());

        // 🔥 nouvelle session de reload
        int sessionId = ++sessionCounter;
        reloadSession.put(uuid, sessionId);

        ConfigurationSection section = getWeaponConfig(weapon);

        String charFull = Main.getInstance().getConfig().getString("default.char_full");
        String charEmpty = Main.getInstance().getConfig().getString("default.char_empty");
        String colorFull = Main.getInstance().getConfig().getString("default.color_full");
        String colorEmpty = Main.getInstance().getConfig().getString("default.color_empty");

        boolean enabled = true;
        int reloadTicks = 40;

        if (section != null) {

            if (section.contains("CustomReload")) {
                ConfigurationSection cr = section.getConfigurationSection("CustomReload");

                if (cr != null) {
                    enabled = cr.getBoolean("Enabled", true);

                    charFull = cr.getString("Char", charFull);
                    charEmpty = cr.getString("Char_Empty", charEmpty);
                    colorFull = cr.getString("Color_Full", colorFull);
                    colorEmpty = cr.getString("Color_Empty", colorEmpty);
                }
            }

            if (section.contains("Reload")) {
                ConfigurationSection reload = section.getConfigurationSection("Reload");

                int duration = reload.getInt("Reload_Duration", reloadTicks);
                double speed = Math.max(0.01, reload.getDouble("Reload_Speed", 1.0));

                reloadTicks = (int) (duration / speed);
            }
        }

        if (!enabled) return;

        startReloadBar(player, reloadTicks, weapon, charFull, charEmpty, colorFull, colorEmpty, sessionId);
    }

    private void startReloadBar(Player player, int reloadTicks, String weapon,
                                String charFull, String charEmpty,
                                String colorFull, String colorEmpty,
                                int sessionId) {

        UUID uuid = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            int progress = 0;

            @Override
            public void run() {

                // si ce n'est plus la bonne session, on stop
                if (reloadSession.getOrDefault(uuid, -1) != sessionId) {
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    cancelReloadInterrupted(player);
                    return;
                }

                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (currentItem == null) return;

                int currentSlot = player.getInventory().getHeldItemSlot();
                int startSlot = reloadSlot.getOrDefault(uuid, -1);

                if (currentSlot != startSlot) {
                    // Le slot a changé (touche numérique sans inventory ouvert)
                    cancelReloadInterrupted(player);
                    return;
                }

                String currentWeapon = csUtility.getWeaponTitle(currentItem);

                // Si l'item en main n'est plus une arme CrackShot ou a changé → annuler
                // (CrackShot annule déjà son reload interne dans ce cas)
                if (currentWeapon == null || !currentWeapon.equals(weapon)) {
                    cancelReloadInterrupted(player);
                    return;
                }

                int maxBars = Main.getInstance().getConfig().getInt("reload.bars", 10);

                // on laisse l'event complete gérer la vraie fin
                if (progress >= reloadTicks) {
                    return;
                }

                int currentBars = (int) ((double) progress / reloadTicks * maxBars);
                currentBars = Math.min(currentBars, maxBars);

                String bar =
                        color(colorFull) + charFull.repeat(currentBars) +
                                color(colorEmpty) + charEmpty.repeat(maxBars - currentBars);

                double seconds = Math.max(0, (reloadTicks - progress) / 20.0);
                String time = String.format("%.1fs", seconds);

                sendMessage(player, weapon, "reloading", bar, time);

                progress += 2;
            }

        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        reloadTasks.put(uuid, task);
    }

    @EventHandler
    public void onReloadComplete(WeaponReloadCompleteEvent e) {

        Player player = e.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        if (!reloadTasks.containsKey(uuid)) return;

        String reloadWeapon = reloadWeapons.get(uuid);
        if (reloadWeapon == null) return;

        if (!reloadWeapon.equals(e.getWeaponTitle())) return;

        // ── Rechargement balle par balle ? ─────────────────────────────────────
        // On vérifie si les munitions sont au maximum. Si non, c'est un rechargement
        // séquentiel (ex. sniper) : on annule silencieusement la barre pour laisser
        // le prochain WeaponReloadEvent démarrer une nouvelle barre pour la balle suivante.
        ItemStack item = player.getInventory().getItemInMainHand();
        int currentAmmo = getAmmoFromItem(item);
        int maxAmmo     = getMaxAmmo(reloadWeapon);
        boolean isFull  = (maxAmmo <= 0 || currentAmmo < 0 || currentAmmo >= maxAmmo);

        if (!isFull) {
            // Annule immédiatement pour que le prochain WeaponReloadEvent puisse reprendre
            silentCancelReload(player);
            return;
        }

        // Chargeur plein → afficher "Chargé" et libérer immédiatement les maps
        // pour éviter que "Rechargement interrompu" s'affiche si le joueur change d'arme juste après
        String loadedMsg = Main.getInstance().getConfig().getString("messages.loaded");
        player.sendActionBar(color(loadedMsg));

        // Retirer immédiatement le joueur du reload actif
        BukkitTask finishedTask = reloadTasks.remove(uuid);
        if (finishedTask != null) finishedTask.cancel();
        reloadSlot.remove(uuid);
        reloadWeapons.remove(uuid);
        reloadSession.remove(uuid);

        // Effacer l'action bar après 20 ticks
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!player.isOnline()) return;
            player.sendActionBar("");
        }, 20L);
    }

    @EventHandler
    public void onPrepareShoot(WeaponPrepareShootEvent e) {

        Player player = e.getPlayer();
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        if (!reloadTasks.containsKey(uuid)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        int currentSlot = player.getInventory().getHeldItemSlot();
        int startSlot = reloadSlot.getOrDefault(uuid, -1);

        if (currentSlot != startSlot) return;

        String currentWeapon = csUtility.getWeaponTitle(item);
        String reloadWeapon = reloadWeapons.get(uuid);

        if (currentWeapon == null) {
            currentWeapon = reloadWeapon;
        }

        if (currentWeapon == null || reloadWeapon == null) return;
        if (!currentWeapon.equals(reloadWeapon)) return;

        int ammo = getAmmoFromItem(item);

        if (ammo > 0) {
            long start = lastReload.getOrDefault(uuid, 0L);
            long elapsed = System.currentTimeMillis() - start;

            // ignore les triggers trop rapides
            if (elapsed < 300) return;

            cancelReload(player);
        }
    }

    private int getAmmoFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;

        String name = item.getItemMeta().getDisplayName();
        if (name == null) return -1;

        Matcher matcher = ammoPattern.matcher(name);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {}
        }

        return -1;
    }

    private void cancelReload(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = reloadTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        reloadSlot.remove(uuid);
        reloadWeapons.remove(uuid);
        reloadSession.remove(uuid);

        player.sendActionBar("");
    }

    /**
     * Annule le reload sans effacer l'action bar, utilisé pour le rechargement balle par balle
     * (la barre du prochain bullet sera affichée immédiatement).
     * On réinitialise lastReload pour que le prochain WeaponReloadEvent passe le check de 250 ms.
     */
    private void silentCancelReload(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = reloadTasks.remove(uuid);
        if (task != null) task.cancel();

        reloadSlot.remove(uuid);
        reloadWeapons.remove(uuid);
        reloadSession.remove(uuid);
        lastReload.remove(uuid);   // ← reset pour que la prochaine balle démarre immédiatement
    }

    /**
     * Annule le reload et affiche le message "Rechargement Interrompu" (configurable).
     * Utilisé lors d'un changement de slot ou d'une interaction avec la hotbar.
     * Ne fait rien si aucun rechargement n'est en cours.
     */
    private void cancelReloadInterrupted(Player player) {
        UUID uuid = player.getUniqueId();

        // Garde : ne rien faire si aucun rechargement visuel n'est actif
        if (!reloadTasks.containsKey(uuid)) return;

        BukkitTask task = reloadTasks.remove(uuid);
        if (task != null) task.cancel();

        reloadSlot.remove(uuid);
        reloadWeapons.remove(uuid);
        reloadSession.remove(uuid);

        String msg = Main.getInstance().getConfig()
                .getString("messages.interrupted", "&cRechargement interrompu");
        player.sendActionBar(color(msg));
    }

    /**
     * Lit la capacité maximale du chargeur depuis la config CrackShot.
     * Retourne -1 si introuvable (munitions illimitées ou config absente).
     */
    private int getMaxAmmo(String weapon) {
        ConfigurationSection section = getWeaponConfig(weapon);
        if (section == null) return -1;
        ConfigurationSection ammoSection = section.getConfigurationSection("Ammo");
        if (ammoSection == null) return -1;
        return ammoSection.getInt("Max_Ammo", -1);
    }

    private void sendMessage(Player player, String weapon, String key, String bar, String time) {

        String format = Main.getInstance().getConfig().getString("messages.format");
        String message = Main.getInstance().getConfig().getString("messages." + key);

        String finalMsg = format
                .replace("{weapon}", weapon)
                .replace("{message}", color(message))
                .replace("{bar}", bar)
                .replace("{time}", time);

        player.sendActionBar(color(finalMsg));
    }

    private String color(String text) {
        return text.replace("&", "§");
    }

    public ConfigurationSection getWeaponConfig(String weapon) {

        if (cache.containsKey(weapon))
            return cache.get(weapon);

        File folder = new File(Bukkit.getPluginManager()
                .getPlugin("CrackShot")
                .getDataFolder(), "weapons");

        if (!folder.exists()) return null;

        File[] files = folder.listFiles();
        if (files == null) return null;

        for (File file : files) {

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            if (config.contains(weapon)) {
                ConfigurationSection section = config.getConfigurationSection(weapon);
                cache.put(weapon, section);
                return section;
            }
        }

        return null;
    }
}
