package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /tp <joueur>                          — Téléporter vers un joueur
 * /tp <joueur1> <joueur2>               — Téléporter joueur1 vers joueur2 (admin)
 * /tp <x> <y> <z> [monde]              — Téléporter vers des coordonnées
 * /tphere <joueur>                      — Téléporter un joueur vers soi
 * /tpall                                — Téléporter tout le monde vers soi
 */
public class TpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        String cmd = command.getName().toLowerCase();

        // ── /tphere <joueur> ──────────────────────────────────────────────────
        if (cmd.equals("tphere")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur.")); return true;
            }
            if (!Main.hasGroupPermission(player, "armesia.tphere")) {
                player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
            }
            if (args.length < 1) { player.sendMessage("§cUsage : §f/tphere <joueur>"); return true; }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { player.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable.")); return true; }
            target.teleport(player.getLocation());
            target.sendMessage("§aVous avez été téléporté vers §f" + player.getName() + "§a.");
            player.sendMessage("§f" + target.getName() + " §aa été téléporté vers vous.");
            return true;
        }

        // ── /tpall ────────────────────────────────────────────────────────────
        if (cmd.equals("tpall")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur.")); return true;
            }
            if (!Main.hasGroupPermission(player, "armesia.tpall")) {
                player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
            }
            int count = 0;
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;
                other.teleport(player.getLocation());
                other.sendMessage("§aVous avez été téléporté vers §f" + player.getName() + "§a.");
                count++;
            }
            player.sendMessage("§f" + count + " §ajoueur(s) téléporté(s) vers vous.");
            return true;
        }

        // ── /tp ───────────────────────────────────────────────────────────────
        if (!Main.checkPerm(sender, "armesia.tp")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage : §f/tp <joueur> | /tp <x> <y> <z> | /tp <j1> <j2>");
            return true;
        }

        // Essaye de parser des coordonnées
        if (args.length >= 3 && isDouble(args[0]) && isDouble(args[1]) && isDouble(args[2])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur.")); return true;
            }
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            World world = args.length >= 4 ? Bukkit.getWorld(args[3]) : player.getWorld();
            if (world == null) { sender.sendMessage("§cMonde introuvable."); return true; }
            player.teleport(new Location(world, x, y, z, player.getYaw(), player.getPitch()));
            player.sendMessage("§aTéléporté en §f" + world.getName() + " §7(" + (int)x + ", " + (int)y + ", " + (int)z + ")§a.");
            return true;
        }

        // /tp <joueur>
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur.")); return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable.")); return true; }
            player.teleport(target.getLocation());
            player.sendMessage("§aTéléporté vers §f" + target.getName() + "§a.");
            return true;
        }

        // /tp <joueur1> <joueur2>
        if (args.length >= 2) {
            Player p1 = Bukkit.getPlayer(args[0]);
            Player p2 = Bukkit.getPlayer(args[1]);
            if (p1 == null) { sender.sendMessage("§cJoueur §f" + args[0] + " §cintrouvable."); return true; }
            if (p2 == null) { sender.sendMessage("§cJoueur §f" + args[1] + " §cintrouvable."); return true; }
            p1.teleport(p2.getLocation());
            p1.sendMessage("§aVous avez été téléporté vers §f" + p2.getName() + "§a.");
            sender.sendMessage("§f" + p1.getName() + " §atéléporté vers §f" + p2.getName() + "§a.");
            return true;
        }

        return true;
    }

    private boolean isDouble(String s) {
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}

