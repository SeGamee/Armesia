package fr.segame.armesia.commands;

import org.bukkit.Bukkit;
import fr.segame.armesia.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * /god [joueur]
 *
 * Active / désactive le mode invincible (god mode) d'un joueur.
 * Implémente Listener pour bloquer les dégâts des joueurs en god mode.
 */
public class GodCommand implements CommandExecutor, Listener {

    /** UUID des joueurs en god mode. */
    private static final Set<UUID> godPlayers = new HashSet<>();

    // ── Commande ─────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!Main.checkPerm(sender, "armesia.god")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length == 0) {
            // /god → soi-même
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage : /god <joueur>");
                return true;
            }
            toggle(player, player);

        } else {
            // /god <joueur>
            if (!Main.checkPerm(sender, "armesia.god.others")) {
                sender.sendMessage("§cVous n'avez pas la permission d'activer le god mode d'un autre joueur.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cCe joueur n'est pas connecté.");
                return true;
            }
            toggle(sender, target);
        }

        return true;
    }

    private void toggle(CommandSender actor, Player target) {
        UUID uuid = target.getUniqueId();

        if (godPlayers.contains(uuid)) {
            godPlayers.remove(uuid);
            target.sendMessage("§cGod mode §l§cdésactivé§r§c.");
            if (!target.equals(actor)) {
                actor.sendMessage("§7God mode de §f" + target.getName() + " §7: §cdésactivé§7.");
            }
        } else {
            godPlayers.add(uuid);
            target.sendMessage("§aGod mode §l§aactivé§r§a.");
            if (!target.equals(actor)) {
                actor.sendMessage("§7God mode de §f" + target.getName() + " §7: §aactivé§7.");
            }
        }
    }

    // ── Listener — bloque les dégâts ────────────────────────────────────────

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (godPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // On retire le god mode au déconnect (optionnel — à adapter selon besoin)
        godPlayers.remove(event.getPlayer().getUniqueId());
    }

    // ── Utilitaires statiques ────────────────────────────────────────────────

    public static boolean isGod(UUID uuid) {
        return godPlayers.contains(uuid);
    }

    /** Retire explicitement un joueur du god mode (ex : à la connexion). */
    public static void remove(UUID uuid) {
        godPlayers.remove(uuid);
    }
}



