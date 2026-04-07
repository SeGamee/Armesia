package fr.segame.armesia.commands;

import com.google.common.base.Joiner;
import fr.segame.armesia.Main;
import fr.segame.armesia.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * /msg <joueur> <message>   — Message privé
 * /m   <joueur> <message>   — Alias de /msg
 * /r   <message>            — Répondre au dernier interlocuteur
 * /spy                      — Activer/désactiver le social spy
 */
public class MessageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        String cmd = command.getName().toLowerCase();

        // ── /spy ──────────────────────────────────────────────────────────────
        if (cmd.equals("spy")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
                return true;
            }
            if (!Main.hasGroupPermission(player, "armesia.spy")) {
                player.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
                return true;
            }
            boolean now = MessageManager.toggleSpy(player.getUniqueId());
            player.sendMessage(now
                    ? cfg("private-messages.spy-on",  "§aSpy mode activé.")
                    : cfg("private-messages.spy-off", "§cSpy mode désactivé."));
            return true;
        }

        if (!Main.checkPerm(sender, "armesia.msg")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission."));
            return true;
        }

        // ── /r <message> ──────────────────────────────────────────────────────
        if (cmd.equals("r")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
                return true;
            }
            if (args.length == 0) { player.sendMessage("§cUsage : §f/r <message>"); return true; }
            java.util.UUID lastId = MessageManager.getLastRecipient(player.getUniqueId());
            if (lastId == null) {
                player.sendMessage(cfg("private-messages.no-reply", "§cVous n'avez personne à qui répondre."));
                return true;
            }
            Player recipient = Bukkit.getPlayer(lastId);
            if (recipient == null) {
                player.sendMessage(cfg("private-messages.no-reply", "§cCe joueur n'est plus connecté."));
                return true;
            }
            sendPrivateMessage(player, recipient, Joiner.on(" ").join(args));
            return true;
        }

        // ── /msg | /m <joueur> <message> ──────────────────────────────────────
        if (args.length < 2) {
            sender.sendMessage("§cUsage : §f/" + label + " <joueur> <message>");
            return true;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null) {
            sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable."));
            return true;
        }

        if (recipient.equals(sender)) {
            sender.sendMessage("§cVous ne pouvez pas vous envoyer un message à vous-même.");
            return true;
        }

        String message = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));

        if (sender instanceof Player p) {
            sendPrivateMessage(p, recipient, message);
        } else {
            // Console → joueur
            String fmtSend = cfg("private-messages.format-send", "§7[§fConsole §7→ §f{to}§7] §f{message}")
                    .replace("{to}", recipient.getName()).replace("{message}", message);
            String fmtRecv = cfg("private-messages.format-receive", "§7[§fConsole §7→ §fVous§7] §f{message}")
                    .replace("{from}", "Console").replace("{message}", message);
            sender.sendMessage(fmtSend);
            recipient.sendMessage(fmtRecv);
            dispatchSpy(null, recipient, message);
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendPrivateMessage(Player from, Player to, String message) {
        String fmtSend = cfg("private-messages.format-send", "§7[§fVous §7→ §f{to}§7] §f{message}")
                .replace("{to}", to.getName()).replace("{message}", message);
        String fmtRecv = cfg("private-messages.format-receive", "§7[§f{from} §7→ §fVous§7] §f{message}")
                .replace("{from}", from.getDisplayName()).replace("{message}", message);

        from.sendMessage(fmtSend);
        to.sendMessage(fmtRecv);

        MessageManager.setLastRecipient(from.getUniqueId(), to.getUniqueId());
        dispatchSpy(from, to, message);
    }

    private void dispatchSpy(Player from, Player to, String message) {
        String fmt = cfg("private-messages.format-spy", "§8[SPY] §7{from} §8→ §7{to}§8: §f{message}")
                .replace("{from}", from != null ? from.getName() : "Console")
                .replace("{to}",   to.getName())
                .replace("{message}", message);

        for (java.util.UUID uid : MessageManager.getSpyPlayers()) {
            Player spy = Bukkit.getPlayer(uid);
            if (spy == null) continue;
            if (spy.equals(from) || spy.equals(to)) continue;
            spy.sendMessage(fmt);
        }
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}

