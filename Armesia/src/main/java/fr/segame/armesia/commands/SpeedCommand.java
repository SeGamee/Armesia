package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /speed [walk|fly] <1-10> [joueur]
 * /speed reset [joueur]
 */
public class SpeedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.speed")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // ── /speed reset [joueur] ─────────────────────────────────────────────
        if (args[0].equalsIgnoreCase("reset")) {
            Player target = resolveTarget(sender, args.length >= 2 ? args[1] : null);
            if (target == null) return true;
            if (!target.equals(sender) && !Main.checkPerm(sender, "armesia.speed.others")) {
                sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
                return true;
            }
            resetSpeed(target);
            target.sendMessage(cfg("speed.messages.reset", "§aVitesse réinitialisée."));
            if (!target.equals(sender)) {
                sender.sendMessage(cfg("speed.messages.reset-other", "§aVitesse de §f{player} §aréinitialisée.")
                        .replace("{player}", target.getName()));
            }
            return true;
        }

        // ── /speed [walk|fly] <valeur> [joueur] ───────────────────────────────
        String type = "walk";
        int    valueIdx = 0;

        if (args[0].equalsIgnoreCase("walk") || args[0].equalsIgnoreCase("fly")) {
            type     = args[0].toLowerCase();
            valueIdx = 1;
        }

        if (args.length <= valueIdx) { sendUsage(sender); return true; }

        int value;
        try {
            value = Integer.parseInt(args[valueIdx]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cLa vitesse doit être un entier entre §f1 §cet §f" + maxFor(type) + "§c.");
            return true;
        }

        int max = maxFor(type);
        if (value < 1 || value > max) {
            sender.sendMessage("§cVitesse §f" + type + " §cde 1 à §f" + max + "§c.");
            return true;
        }

        Player target = resolveTarget(sender, args.length > valueIdx + 1 ? args[valueIdx + 1] : null);
        if (target == null) return true;

        if (!target.equals(sender) && !Main.checkPerm(sender, "armesia.speed.others")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        float speed = value * 0.1f;
        if (type.equals("fly")) {
            target.setFlySpeed(speed);
            target.setAllowFlight(true);
        } else {
            target.setWalkSpeed(speed);
        }

        String typeLabel = type.equals("fly") ? "de vol" : "de marche";
        target.sendMessage(cfg("speed.messages.set", "§aVitesse {type} définie à §f{value}§a.")
                .replace("{type}", typeLabel).replace("{value}", String.valueOf(value)));

        if (!target.equals(sender)) {
            sender.sendMessage(cfg("speed.messages.set-other", "§aVitesse {type} de §f{player} §adéfinie à §f{value}§a.")
                    .replace("{type}", typeLabel)
                    .replace("{player}", target.getName())
                    .replace("{value}", String.valueOf(value)));
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static void resetSpeed(Player player) {
        int dw = Main.getInstance().getConfig().getInt("speed.default-walk", 2);
        int df = Main.getInstance().getConfig().getInt("speed.default-fly",  1);
        player.setWalkSpeed(dw * 0.1f);
        player.setFlySpeed(df * 0.1f);
    }

    private int maxFor(String type) {
        return type.equals("fly")
                ? Main.getInstance().getConfig().getInt("speed.max-fly",  10)
                : Main.getInstance().getConfig().getInt("speed.max-walk", 10);
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name == null) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cUsage console : /speed [walk|fly] <valeur> <joueur>");
                return null;
            }
            return p;
        }
        Player t = Bukkit.getPlayer(name);
        if (t == null) sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable."));
        return t;
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }

    private void sendUsage(CommandSender s) {
        s.sendMessage("§cUsage : §f/speed [walk|fly] <1-10> [joueur]");
        s.sendMessage("§cOu    : §f/speed reset [joueur]");
    }
}

