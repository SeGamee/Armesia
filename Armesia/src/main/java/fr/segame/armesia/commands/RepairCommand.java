package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * /repair      — Répare l'item en main
 * /repairall   — Répare tous les items de l'inventaire
 */
public class RepairCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
            return true;
        }

        boolean all = command.getName().equalsIgnoreCase("repairall");

        if (all) {
            if (!Main.hasGroupPermission(player, "armesia.repairall")) {
                player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
                return true;
            }
            int xpCost = Main.getInstance().getConfig().getInt("repair.xp-cost", 0);
            if (xpCost > 0 && player.getLevel() < xpCost) {
                player.sendMessage(cfg("repair.messages.not-enough-xp", "§cNiveaux insuffisants (requis : §f{cost}§c).")
                        .replace("{cost}", String.valueOf(xpCost)));
                return true;
            }
            int count = repairAll(player);
            if (xpCost > 0) player.setLevel(player.getLevel() - xpCost);
            player.sendMessage(cfg("repair.messages.success-all", "§aTous vos items ont été réparés (§f{count}§a).")
                    .replace("{count}", String.valueOf(count)));
        } else {
            if (!Main.hasGroupPermission(player, "armesia.repair")) {
                player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
                return true;
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().isAir()) {
                player.sendMessage(cfg("repair.messages.no-item", "§cVous n'avez aucun item en main."));
                return true;
            }
            if (!isDamageable(item)) {
                player.sendMessage(cfg("repair.messages.not-damageable", "§cCet item ne peut pas être réparé."));
                return true;
            }
            int xpCost = Main.getInstance().getConfig().getInt("repair.xp-cost", 0);
            if (xpCost > 0 && player.getLevel() < xpCost) {
                player.sendMessage(cfg("repair.messages.not-enough-xp", "§cNiveaux insuffisants (requis : §f{cost}§c).")
                        .replace("{cost}", String.valueOf(xpCost)));
                return true;
            }
            repair(item);
            if (xpCost > 0) player.setLevel(player.getLevel() - xpCost);
            player.sendMessage(cfg("repair.messages.success", "§aItem réparé avec succès."));
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isDamageable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable;
    }

    private void repair(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable d) {
            d.setDamage(0);
            item.setItemMeta(meta);
        }
    }

    private int repairAll(Player player) {
        int count = 0;
        ItemStack[] all = combine(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                new ItemStack[]{ player.getInventory().getItemInOffHand() }
        );
        for (ItemStack item : all) {
            if (item == null || item.getType().isAir()) continue;
            if (isDamageable(item)) { repair(item); count++; }
        }
        return count;
    }

    private ItemStack[] combine(ItemStack[]... arrays) {
        int total = 0;
        for (ItemStack[] a : arrays) total += a.length;
        ItemStack[] result = new ItemStack[total];
        int idx = 0;
        for (ItemStack[] a : arrays) { System.arraycopy(a, 0, result, idx, a.length); idx += a.length; }
        return result;
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}

