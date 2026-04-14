package fr.segame.armesiaCasino.commands;

import fr.segame.armesiaCasino.MainCasino;
import fr.segame.armesiaCasino.managers.CasinoManager;
import fr.segame.armesiaCasino.managers.PrizeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CasinoCommand implements CommandExecutor, TabCompleter {

    private final MainCasino    plugin;
    private final CasinoManager casinoManager;
    private final PrizeManager  prizeManager;

    public CasinoCommand(MainCasino plugin, CasinoManager casinoManager, PrizeManager prizeManager) {
        this.plugin        = plugin;
        this.casinoManager = casinoManager;
        this.prizeManager  = prizeManager;
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "setblock"    -> cmdSetBlock(sender);
            case "removeblock" -> cmdRemoveBlock(sender);
            case "give"        -> cmdGive(sender, args);
            case "reload"      -> cmdReload(sender);
            case "list"        -> cmdList(sender);
            case "settoken"    -> cmdSetToken(sender, args);
            default            -> sendHelp(sender);
        }
        return true;
    }

    // ── /casino setblock ──────────────────────────────────────────────────────

    private void cmdSetBlock(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg("player-only", "&cCommande réservée aux joueurs."));
            return;
        }
        if (!p.hasPermission("armesia.casino.admin")) {
            p.sendMessage(msg("no-permission", "&cVous n'avez pas la permission."));
            return;
        }

        Block target = p.getTargetBlockExact(8);
        if (target == null) {
            p.sendMessage(msg("no-target-block", "&cRegardez un bloc (portée max : 8 blocs)."));
            return;
        }

        casinoManager.addBlock(target.getLocation());
        p.sendMessage(msg("block-set", "&a✦ Bloc de casino enregistré en &f{x}, {y}, {z}&a !")
                .replace("{x}", String.valueOf(target.getX()))
                .replace("{y}", String.valueOf(target.getY()))
                .replace("{z}", String.valueOf(target.getZ())));
    }

    // ── /casino removeblock ───────────────────────────────────────────────────

    private void cmdRemoveBlock(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg("player-only", "&cCommande réservée aux joueurs."));
            return;
        }
        if (!p.hasPermission("armesia.casino.admin")) {
            p.sendMessage(msg("no-permission", "&cVous n'avez pas la permission."));
            return;
        }

        Block target = p.getTargetBlockExact(8);
        if (target == null) {
            p.sendMessage(msg("no-target-block", "&cRegardez un bloc (portée max : 8 blocs)."));
            return;
        }
        if (!casinoManager.isCasinoBlock(target.getLocation())) {
            p.sendMessage(msg("not-casino-block", "&cCe bloc n'est pas un bloc de casino."));
            return;
        }

        casinoManager.removeBlock(target.getLocation());
        p.sendMessage(msg("block-removed", "&a✦ Bloc de casino supprimé."));
    }

    // ── /casino give [joueur] [quantité] ─────────────────────────────────────

    private void cmdGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("armesia.casino.admin")) {
            sender.sendMessage(msg("no-permission", "&cVous n'avez pas la permission."));
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(msg("player-not-found", "&cJoueur introuvable : &f{player}")
                        .replace("{player}", args[1]));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(msg("give-no-player",
                    "&cSpécifiez un joueur : /casino give <joueur> [quantité]"));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(msg("invalid-amount", "&cQuantité invalide, utilisation de 1."));
            }
        }

        ItemStack token = plugin.createCasinoToken();
        token.setAmount(amount);
        target.getInventory().addItem(token);

        sender.sendMessage(msg("give-success", "&a✦ {amount} jeton(s) donné(s) à &f{player}&a.")
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", target.getName()));
        if (!target.equals(sender)) {
            target.sendMessage(msg("give-received", "&6✦ Vous avez reçu &e{amount} &6jeton(s) de casino !")
                    .replace("{amount}", String.valueOf(amount)));
        }
    }

    // ── /casino reload ────────────────────────────────────────────────────────

    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("armesia.casino.admin")) {
            sender.sendMessage(msg("no-permission", "&cVous n'avez pas la permission."));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(msg("reload-success",
                "&a✦ Configuration du casino rechargée avec succès."));
    }

    // ── /casino list ──────────────────────────────────────────────────────────

    private void cmdList(CommandSender sender) {
        if (!sender.hasPermission("armesia.casino.admin")) {
            sender.sendMessage(msg("no-permission", "&cVous n'avez pas la permission."));
            return;
        }

        var blocks = casinoManager.getCasinoBlocks();
        sender.sendMessage(ChatColor.GOLD + "══ Blocs de casino (" + blocks.size() + ") ══");
        if (blocks.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  Aucun bloc enregistré.");
            return;
        }
        for (Location loc : blocks) {
            sender.sendMessage(ChatColor.GRAY + "  » "
                    + ChatColor.WHITE + loc.getWorld().getName()
                    + ChatColor.GRAY + " ["
                    + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    + "]");
        }
    }

    // ── /casino settoken [reset] ──────────────────────────────────────────────

    private void cmdSetToken(CommandSender sender, String[] args) {
        if (!sender.hasPermission("armesia.casino.admin")) {
            sender.sendMessage(msg("no-permission", "&cVous n'avez pas la permission."));
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(msg("player-only", "&cCommande réservée aux joueurs."));
            return;
        }

        // /casino settoken reset → retour aux valeurs de config.yml
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            casinoManager.resetCustomToken();
            p.sendMessage(msg("settoken-reset",
                    "&a✦ Jeton de casino réinitialisé aux valeurs de la config."));
            return;
        }

        // /casino settoken → prend l'item en main
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            p.sendMessage(msg("settoken-no-item", "&cVous devez tenir un item dans la main !"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String       name     = (meta != null && meta.hasDisplayName())
                ? meta.getDisplayName()
                : ChatColor.WHITE + item.getType().name();
        List<String> lore     = (meta != null && meta.hasLore())
                ? meta.getLore()
                : new ArrayList<>();
        int          cmdData  = (meta != null && meta.hasCustomModelData())
                ? meta.getCustomModelData()
                : 0;

        casinoManager.saveCustomToken(item.getType(), name, lore, cmdData);
        p.sendMessage(msg("settoken-success",
                "&a✦ Jeton de casino personnalisé avec l'item en main !"));
    }

    // ── Aide ──────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "══ Casino — Aide ══");
        sender.sendMessage(ChatColor.YELLOW + "/casino setblock"
                + ChatColor.GRAY + " · Définir le bloc visé comme bloc de casino");
        sender.sendMessage(ChatColor.YELLOW + "/casino removeblock"
                + ChatColor.GRAY + " · Supprimer le bloc de casino visé");
        sender.sendMessage(ChatColor.YELLOW + "/casino give [joueur] [qté]"
                + ChatColor.GRAY + " · Donner un jeton de casino");
        sender.sendMessage(ChatColor.YELLOW + "/casino reload"
                + ChatColor.GRAY + " · Recharger la configuration");
        sender.sendMessage(ChatColor.YELLOW + "/casino list"
                + ChatColor.GRAY + " · Lister les blocs de casino");
        sender.sendMessage(ChatColor.YELLOW + "/casino settoken [reset]"
                + ChatColor.GRAY + " · Personnaliser le jeton (item en main) ou réinitialiser");
    }

    // ── Complétion automatique ────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.addAll(Arrays.asList("setblock", "removeblock", "give", "reload", "list", "settoken"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> result.add(p.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            result.addAll(Arrays.asList("1", "5", "10", "32", "64"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("settoken")) {
            result.add("reset");
        }
        return result;
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    private String msg(String key, String def) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages." + key, def));
    }
}
