package fr.segame.armesia.commands;

import com.google.common.base.Joiner;
import org.bukkit.ChatColor;
import fr.segame.armesia.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /item <sous-commande> [arguments]
 *
 * Sous-commandes :
 *   name <nom>                    – Renommer l'item en main
 *   lore set <ligne1> [...]       – Définir le lore (lignes séparées par ' | ')
 *   lore add <texte>              – Ajouter une ligne au lore
 *   lore remove <numéro>          – Supprimer la ligne n° (1-based)
 *   lore clear                    – Supprimer tout le lore
 *   enchant <enchantement> <lvl>  – Ajouter/modifier un enchantement
 *   unenchant <enchantement>      – Supprimer un enchantement
 *   flag <flag>                   – Activer/désactiver un ItemFlag
 *   unbreakable                   – Basculer le mode incassable
 *   clear                         – Réinitialiser toutes les métadonnées
 */
public class ItemCommand implements CommandExecutor {

    private static final String PREFIX = "§8[§aItem§8] §r";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }

        if (!Main.hasGroupPermission(player, "armesia.item")) {
            player.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(PREFIX + "§cVous n'avez aucun item en main.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(PREFIX + "§cCet item ne supporte pas les métadonnées.");
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── name ─────────────────────────────────────────────────────────
            case "name" -> {
                if (args.length < 2) { player.sendMessage(PREFIX + "§cUsage : /item name <nom>"); return true; }
                String name = ChatColor.translateAlternateColorCodes('&', Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length)));
                meta.setDisplayName(name);
                item.setItemMeta(meta);
                player.sendMessage(PREFIX + "§7Nom défini : " + name);
            }

            // ── lore ─────────────────────────────────────────────────────────
            case "lore" -> {
                if (args.length < 2) { player.sendMessage(PREFIX + "§cUsage : /item lore <set|add|remove|clear>"); return true; }

                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                switch (args[1].toLowerCase()) {

                    case "set" -> {
                        if (args.length < 3) { player.sendMessage(PREFIX + "§cUsage : /item lore set <texte> (utilisez | pour plusieurs lignes)"); return true; }
                        String raw = Joiner.on(" ").join(Arrays.copyOfRange(args, 2, args.length));
                        lore.clear();
                        for (String line : raw.split("\\|")) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', line.trim()));
                        }
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        player.sendMessage(PREFIX + "§7Lore défini (" + lore.size() + " ligne(s)).");
                    }

                    case "add" -> {
                        if (args.length < 3) { player.sendMessage(PREFIX + "§cUsage : /item lore add <texte>"); return true; }
                        String line = ChatColor.translateAlternateColorCodes('&', Joiner.on(" ").join(Arrays.copyOfRange(args, 2, args.length)));
                        lore.add(line);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        player.sendMessage(PREFIX + "§7Ligne ajoutée : " + line);
                    }

                    case "remove" -> {
                        if (args.length < 3) { player.sendMessage(PREFIX + "§cUsage : /item lore remove <numéro>"); return true; }
                        try {
                            int idx = Integer.parseInt(args[2]) - 1;
                            if (idx < 0 || idx >= lore.size()) { player.sendMessage(PREFIX + "§cNuméro de ligne invalide."); return true; }
                            String removed = lore.remove(idx);
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                            player.sendMessage(PREFIX + "§7Ligne §f" + (idx + 1) + " §7supprimée : " + removed);
                        } catch (NumberFormatException e) {
                            player.sendMessage(PREFIX + "§cUsage : /item lore remove <numéro>");
                        }
                    }

                    case "clear" -> {
                        meta.setLore(new ArrayList<>());
                        item.setItemMeta(meta);
                        player.sendMessage(PREFIX + "§7Lore supprimé.");
                    }

                    default -> player.sendMessage(PREFIX + "§cSous-commande inconnue. Utilisez : set, add, remove, clear.");
                }
            }

            // ── enchant ───────────────────────────────────────────────────────
            case "enchant" -> {
                if (args.length < 3) { player.sendMessage(PREFIX + "§cUsage : /item enchant <enchantement> <niveau>"); return true; }
                Enchantment ench = findEnchantment(args[1]);
                if (ench == null) { player.sendMessage(PREFIX + "§cEnchantement inconnu : §f" + args[1]); return true; }
                try {
                    int level = Integer.parseInt(args[2]);
                    if (level < 1) { player.sendMessage(PREFIX + "§cLe niveau doit être ≥ 1."); return true; }
                    meta.addEnchant(ench, level, true);
                    item.setItemMeta(meta);
                    player.sendMessage(PREFIX + "§7Enchantement §f" + ench.getKey().getKey() + " §7niveau §f" + level + " §7ajouté.");
                } catch (NumberFormatException e) {
                    player.sendMessage(PREFIX + "§cLe niveau doit être un nombre.");
                }
            }

            // ── unenchant ─────────────────────────────────────────────────────
            case "unenchant" -> {
                if (args.length < 2) { player.sendMessage(PREFIX + "§cUsage : /item unenchant <enchantement>"); return true; }
                Enchantment ench = findEnchantment(args[1]);
                if (ench == null) { player.sendMessage(PREFIX + "§cEnchantement inconnu : §f" + args[1]); return true; }
                if (!meta.hasEnchant(ench)) { player.sendMessage(PREFIX + "§cCet item n'a pas cet enchantement."); return true; }
                meta.removeEnchant(ench);
                item.setItemMeta(meta);
                player.sendMessage(PREFIX + "§7Enchantement §f" + ench.getKey().getKey() + " §7supprimé.");
            }

            // ── flag ──────────────────────────────────────────────────────────
            case "flag" -> {
                if (args.length < 2) {
                    player.sendMessage(PREFIX + "§cUsage : /item flag <flag>");
                    player.sendMessage(PREFIX + "§7Flags : " + flagList());
                    return true;
                }
                ItemFlag flag;
                try {
                    flag = ItemFlag.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(PREFIX + "§cFlag inconnu : §f" + args[1]);
                    player.sendMessage(PREFIX + "§7Flags valides : " + flagList());
                    return true;
                }
                if (meta.hasItemFlag(flag)) {
                    meta.removeItemFlags(flag);
                    item.setItemMeta(meta);
                    player.sendMessage(PREFIX + "§7Flag §f" + flag.name() + " §7: §cdésactivé§7.");
                } else {
                    meta.addItemFlags(flag);
                    item.setItemMeta(meta);
                    player.sendMessage(PREFIX + "§7Flag §f" + flag.name() + " §7: §aactivé§7.");
                }
            }

            // ── unbreakable ───────────────────────────────────────────────────
            case "unbreakable" -> {
                boolean current = meta.isUnbreakable();
                meta.setUnbreakable(!current);
                item.setItemMeta(meta);
                player.sendMessage(PREFIX + "§7Incassable : " + (!current ? "§aactivé" : "§cdésactivé") + "§7.");
            }

            // ── clear ─────────────────────────────────────────────────────────
            case "clear" -> {
                meta.setDisplayName(null);
                meta.setLore(null);
                meta.setUnbreakable(false);
                for (Enchantment e : meta.getEnchants().keySet()) meta.removeEnchant(e);
                for (ItemFlag f : meta.getItemFlags()) meta.removeItemFlags(f);
                item.setItemMeta(meta);
                player.sendMessage(PREFIX + "§7Toutes les métadonnées de l'item ont été effacées.");
            }

            default -> sendHelp(player);
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private Enchantment findEnchantment(String name) {
        // Essai par clé Namespaced (minecraft:sharpness…)
        try {
            return Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception ignored) {}
        return null;
    }

    private String flagList() {
        StringBuilder sb = new StringBuilder();
        for (ItemFlag f : ItemFlag.values()) sb.append("§f").append(f.name()).append("§7, ");
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";
    }

    private void sendHelp(Player player) {
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§a§l/item §7— Modifier l'item en main");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§f/item name §7<nom>              §8— §7Renommer l'item");
        player.sendMessage("§f/item lore set §7<texte | ...>  §8— §7Définir le lore");
        player.sendMessage("§f/item lore add §7<texte>        §8— §7Ajouter une ligne");
        player.sendMessage("§f/item lore remove §7<n°>        §8— §7Supprimer la ligne n°");
        player.sendMessage("§f/item lore clear                §8— §7Effacer tout le lore");
        player.sendMessage("§f/item enchant §7<ench> <lvl>    §8— §7Ajouter un enchantement");
        player.sendMessage("§f/item unenchant §7<ench>        §8— §7Supprimer un enchantement");
        player.sendMessage("§f/item flag §7<flag>             §8— §7Basculer un flag");
        player.sendMessage("§f/item unbreakable               §8— §7Basculer incassable");
        player.sendMessage("§f/item clear                     §8— §7Réinitialiser toutes les meta");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}

