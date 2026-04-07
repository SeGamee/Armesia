package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class KitCommand implements CommandExecutor {

    private final KitManager kitManager;

    public KitCommand(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.kit")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        if (args.length == 0) {
            listKits(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ── create ────────────────────────────────────────────────────────────
        if (sub.equals("create")) {
            if (!Main.checkPerm(sender, "armesia.kit.admin")) {
                sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage : §f/kit create <nom> [cooldown_s] [permission]");
                return true;
            }
            String name     = args[1].toLowerCase();
            long   cooldown = args.length >= 3 ? parseLong(args[2], 0L) : 0L;
            String perm     = args.length >= 4 ? args[3] : "armesia.kit." + name;
            kitManager.createKit(name, perm, cooldown, "§aKit §f" + name, player.getInventory());
            sender.sendMessage("§aKit §f" + name + " §acréé (§f" + countItems(player) + " §aitem(s), cooldown : §f"
                    + (cooldown > 0 ? KitManager.formatDuration(cooldown * 1000) : "aucun") + "§a).");
            return true;
        }

        // ── delete ────────────────────────────────────────────────────────────
        if (sub.equals("delete")) {
            if (!Main.checkPerm(sender, "armesia.kit.admin")) {
                sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
            }
            if (args.length < 2) { sender.sendMessage("§cUsage : §f/kit delete <nom>"); return true; }
            String name = args[1].toLowerCase();
            if (!kitManager.kitExists(name)) { sender.sendMessage("§cKit §f" + name + " §cinexistant."); return true; }
            kitManager.deleteKit(name);
            sender.sendMessage("§aKit §f" + name + " §asupprimé.");
            return true;
        }

        // ── give ──────────────────────────────────────────────────────────────
        if (sub.equals("give")) {
            if (!Main.checkPerm(sender, "armesia.kit.admin")) {
                sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
            }
            if (args.length < 3) { sender.sendMessage("§cUsage : §f/kit give <nom> <joueur>"); return true; }
            String kitName = args[1].toLowerCase();
            if (!kitManager.kitExists(kitName)) { sender.sendMessage("§cKit §f" + kitName + " §cinexistant."); return true; }
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) { sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable.")); return true; }
            giveKit(target, kitName, false);
            sender.sendMessage("§aKit §f" + kitName + " §adonné à §f" + target.getName() + "§a.");
            return true;
        }

        // ── resetcd ───────────────────────────────────────────────────────────
        if (sub.equals("resetcd")) {
            if (!Main.checkPerm(sender, "armesia.kit.admin")) {
                sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
            }
            if (args.length < 2) { sender.sendMessage("§cUsage : §f/kit resetcd <nom> [joueur]"); return true; }
            String kitName = args[1].toLowerCase();
            Player target = args.length >= 3 ? Bukkit.getPlayer(args[2])
                    : (sender instanceof Player p ? p : null);
            if (target == null) { sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable.")); return true; }
            kitManager.resetCooldown(target.getUniqueId(), kitName);
            sender.sendMessage("§aCooldown du kit §f" + kitName + " §aréinitialisé pour §f" + target.getName() + "§a.");
            return true;
        }

        // ── <nom> → réclamer ──────────────────────────────────────────────────
        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur.")); return true;
        }

        String kitName = sub;
        if (!kitManager.kitExists(kitName)) {
            sender.sendMessage("§cKit §f" + kitName + " §cinexistant. Tapez §f/kit §cpour la liste.");
            return true;
        }

        String kitPerm = kitManager.getKitPermission(kitName);
        if (!kitPerm.isEmpty() && !Main.hasGroupPermission(player, kitPerm)) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        if (kitManager.isOnCooldown(player.getUniqueId(), kitName)) {
            sender.sendMessage("§cKit en cooldown. Disponible dans §f"
                    + KitManager.formatDuration(kitManager.getRemainingMs(player.getUniqueId(), kitName)) + "§c.");
            return true;
        }

        giveKit(player, kitName, true);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void giveKit(Player player, String kitName, boolean setCooldown) {
        for (ItemStack item : kitManager.getKitItems(kitName)) {
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item).values()
                        .forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            }
        }
        if (setCooldown) kitManager.setCooldown(player.getUniqueId(), kitName);
        player.sendMessage("§aVous avez reçu le kit §f" + kitManager.getKitDisplayName(kitName) + "§a.");
    }

    private void listKits(CommandSender sender) {
        Set<String> kits = kitManager.getKitNames();
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§a§lKits disponibles");
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        int shown = 0;
        for (String k : kits) {
            String perm   = kitManager.getKitPermission(k);
            boolean canUse = perm.isEmpty() || Main.checkPerm(sender, perm);
            if (!canUse) continue;   // masquer les kits sans permission
            String cd = kitManager.getKitCooldown(k) > 0
                    ? " §8(cd: §7" + KitManager.formatDuration(kitManager.getKitCooldown(k) * 1000) + "§8)" : "";
            sender.sendMessage("§a" + kitManager.getKitDisplayName(k) + " §8— §f/kit " + k + cd);
            shown++;
        }
        if (shown == 0) sender.sendMessage("§7Aucun kit disponible.");
        sender.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private int countItems(Player p) {
        int c = 0;
        for (ItemStack i : p.getInventory().getContents()) if (i != null && !i.getType().isAir()) c++;
        return c;
    }

    private long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}
