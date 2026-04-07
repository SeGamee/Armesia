package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.HomeManager;
import fr.segame.armesia.utils.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /sethome [nom]    — Définir un home
 * /home    [nom]    — Se téléporter à un home
 * /delhome [nom]    — Supprimer un home
 * /homes            — Lister ses homes
 */
public class HomeCommand implements CommandExecutor {

    private static final String DEFAULT = "home";
    private final HomeManager homeManager;

    public HomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
            return true;
        }

        String cmd = command.getName().toLowerCase();

        return switch (cmd) {
            case "sethome" -> handleSetHome(player, args);
            case "delhome" -> handleDelHome(player, args);
            case "homes"   -> handleHomes(player);
            default        -> handleHome(player, args);   // /home
        };
    }

    // ── /sethome [nom] ────────────────────────────────────────────────────────

    private boolean handleSetHome(Player player, String[] args) {
        if (!Main.hasGroupPermission(player, "armesia.sethome")) {
            player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        String name = args.length >= 1 ? args[0].toLowerCase() : DEFAULT;

        boolean existed = homeManager.hasHome(player.getUniqueId(), name);

        if (!existed) {
            int limit = homeManager.getHomeLimit(player);
            int count = homeManager.getHomeCount(player.getUniqueId());
            if (count >= limit) {
                player.sendMessage(cfg("homes.messages.limit-reached", "§cLimite atteinte (§f{limit} §chome(s) max).")
                        .replace("{limit}", String.valueOf(limit)));
                return true;
            }
        }

        homeManager.setHome(player.getUniqueId(), name, player.getLocation());
        player.sendMessage((existed
                ? cfg("homes.messages.updated", "§7Home §e{name} §7mis à jour.")
                : cfg("homes.messages.set",     "§aHome §e{name} §adéfini."))
                .replace("{name}", name));
        return true;
    }

    // ── /home [nom] ───────────────────────────────────────────────────────────

    private boolean handleHome(Player player, String[] args) {
        if (!Main.hasGroupPermission(player, "armesia.home")) {
            player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        String name = args.length >= 1 ? args[0].toLowerCase() : DEFAULT;
        Location dest = homeManager.getHome(player.getUniqueId(), name);

        if (dest == null) {
            if (dest == null && homeManager.getHome(player.getUniqueId(), name) == null
                    && homeManager.getHomes(player.getUniqueId()).isEmpty()) {
                player.sendMessage(cfg("homes.messages.empty", "§7Aucun home. Utilisez §f/sethome [nom]§7."));
            } else {
                player.sendMessage(cfg("homes.messages.not-found", "§cAucun home nommé §e{name}§c.")
                        .replace("{name}", name));
            }
            return true;
        }

        if (dest.getWorld() == null) {
            player.sendMessage(cfg("homes.messages.world-not-found", "§cLe monde de ce home n'existe plus."));
            return true;
        }

        int delay = Main.getInstance().getConfig().getInt("homes.teleport-delay", 3);
        if (delay > 0) {
            player.sendMessage(cfg("homes.messages.teleporting", "§aTéléportation vers §e{name} §adans §f{delay}§as...")
                    .replace("{name}", name).replace("{delay}", String.valueOf(delay)));
        }

        final String nameFinal = name;
        TeleportUtil.schedule(player, dest, delay,
                cfg("homes.messages.move-cancel", "§cTéléportation annulée, vous avez bougé."),
                () -> player.sendMessage(cfg("homes.messages.teleported", "§aTéléporté vers §e{name}§a.")
                        .replace("{name}", nameFinal)));
        return true;
    }

    // ── /delhome [nom] ────────────────────────────────────────────────────────

    private boolean handleDelHome(Player player, String[] args) {
        if (!Main.hasGroupPermission(player, "armesia.home")) {
            player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        String name = args.length >= 1 ? args[0].toLowerCase() : DEFAULT;
        if (!homeManager.deleteHome(player.getUniqueId(), name)) {
            player.sendMessage(cfg("homes.messages.not-found", "§cAucun home nommé §e{name}§c.")
                    .replace("{name}", name));
            return true;
        }
        player.sendMessage(cfg("homes.messages.deleted", "§aHome §e{name} §asupprimé.")
                .replace("{name}", name));
        return true;
    }

    // ── /homes ────────────────────────────────────────────────────────────────

    private boolean handleHomes(Player player) {
        if (!Main.hasGroupPermission(player, "armesia.home")) {
            player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        List<String> homes = homeManager.getHomes(player.getUniqueId());
        if (homes.isEmpty()) {
            player.sendMessage(cfg("homes.messages.empty", "§7Aucun home. Utilisez §f/sethome [nom]§7."));
            return true;
        }
        int limit = homeManager.getHomeLimit(player);
        String limitStr = limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit);
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§aVos homes §8(§f" + homes.size() + "§8/§f" + limitStr + "§8)");
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        for (String h : homes) {
            Location l = homeManager.getHome(player.getUniqueId(), h);
            String loc = l != null ? String.format("§8[§7%s §8| §7%.0f, %.0f, %.0f§8]",
                    l.getWorld().getName(), l.getX(), l.getY(), l.getZ()) : "§cmonde manquant";
            player.sendMessage("§7• §e" + h + " " + loc + " §8— §f/home " + h);
        }
        player.sendMessage("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        return true;
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}

