package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.managers.TpaManager;
import fr.segame.armesia.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /tpa     <joueur>  — Demander à se téléporter chez un joueur
 * /tpahere <joueur>  — Demander à un joueur de venir chez vous
 * /tpyes | /tpaccept — Accepter la demande en attente
 * /tpdeny            — Refuser la demande en attente
 * /tpacancel         — Annuler sa propre demande en attente
 */
public class TpaCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg("general.player-only", "§cVous devez être un joueur."));
            return true;
        }

        String cmd = command.getName().toLowerCase();

        return switch (cmd) {
            case "tpa"               -> handleSend(player, args, TpaManager.Type.TPA);
            case "tpahere"           -> handleSend(player, args, TpaManager.Type.TPAHERE);
            case "tpyes", "tpaccept" -> handleAccept(player);
            case "tpdeny"            -> handleDeny(player);
            case "tpacancel"         -> handleCancel(player);
            default -> true;
        };
    }

    // ── /tpa | /tpahere ───────────────────────────────────────────────────────

    private boolean handleSend(Player sender, String[] args, TpaManager.Type type) {
        String perm = type == TpaManager.Type.TPA ? "armesia.tpa" : "armesia.tpahere";
        if (!Main.hasGroupPermission(sender, perm)) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage("§cUsage : §f/" + (type == TpaManager.Type.TPA ? "tpa" : "tpahere") + " <joueur>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(cfg("general.player-not-found", "§cJoueur introuvable.")); return true; }
        if (target.equals(sender)) { sender.sendMessage("§cVous ne pouvez pas vous envoyer une demande."); return true; }

        // ── Cooldown entre demandes ────────────────────────────────────────
        int cooldown = Main.getInstance().getConfig().getInt("tpa.send-cooldown", 5);
        long remaining = TpaManager.getSendCooldownRemaining(sender.getUniqueId(), cooldown);
        if (remaining > 0) {
            sender.sendMessage(cfg("tpa.messages.cooldown", "§cVeuillez attendre §f{remaining}§cs avant une nouvelle demande.")
                    .replace("{remaining}", String.valueOf(remaining)));
            return true;
        }

        int timeout = Main.getInstance().getConfig().getInt("tpa.timeout", 60);

        boolean ok = TpaManager.sendRequest(sender, target, type);
        if (!ok) {
            sender.sendMessage(cfg("tpa.messages.already-pending", "§cDemande déjà en attente vers §f{player}§c.")
                    .replace("{player}", target.getName()));
            return true;
        }

        String senderMsg = (type == TpaManager.Type.TPA
                ? cfg("tpa.messages.sent-to",   "§aDemande envoyée à §f{player}§a (expire dans §f{timeout}§as).")
                : cfg("tpa.messages.sent-here",  "§aDemande de rappel envoyée à §f{player}§a (expire dans §f{timeout}§as)."))
                .replace("{player}", target.getName()).replace("{timeout}", String.valueOf(timeout));

        String targetMsg = (type == TpaManager.Type.TPA
                ? cfg("tpa.messages.received-tpa",     "§f{player} §aveut se tp chez vous — §f/tpyes §aou §f/tpdeny §a(§f{timeout}§as).")
                : cfg("tpa.messages.received-tpahere",  "§f{player} §aveut que vous le rejoigniez — §f/tpyes §aou §f/tpdeny §a(§f{timeout}§as)."))
                .replace("{player}", sender.getName()).replace("{timeout}", String.valueOf(timeout));

        sender.sendMessage(senderMsg);
        target.sendMessage(targetMsg);
        return true;
    }

    // ── /tpyes ────────────────────────────────────────────────────────────────

    private boolean handleAccept(Player receiver) {
        if (!Main.hasGroupPermission(receiver, "armesia.tpa")) {
            receiver.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        TpaManager.Request req = TpaManager.getRequest(receiver.getUniqueId());
        if (req == null) {
            receiver.sendMessage(cfg("tpa.messages.no-request", "§cVous n'avez aucune demande en attente."));
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(req.sender);
        TpaManager.removeRequest(receiver.getUniqueId());

        if (senderPlayer == null) {
            receiver.sendMessage("§cL'envoyeur de la demande s'est déconnecté.");
            return true;
        }

        // Enregistre le moment d'envoi accepté pour le cooldown
        TpaManager.recordSend(req.sender);

        int prepDelay = Main.getInstance().getConfig().getInt("tpa.preparation-delay", 3);
        int tpDelay   = Main.getInstance().getConfig().getInt("tpa.teleport-delay", 5);

        Player whoTeleports = (req.type == TpaManager.Type.TPA) ? senderPlayer : receiver;
        Player destination  = (req.type == TpaManager.Type.TPA) ? receiver     : senderPlayer;

        receiver.sendMessage(cfg("tpa.messages.accepted", "§aDemande acceptée."));
        senderPlayer.sendMessage("§f" + receiver.getName() + " §aa accepté votre demande !");

        final String cancelMsg = cfg("tpa.messages.move-cancel", "§cTéléportation annulée, vous avez bougé.");
        final Player dest      = destination;
        final Player mover     = whoTeleports;

        // ── Phase 1 : préparation (le joueur peut encore bouger) ──────────
        if (prepDelay > 0) {
            mover.sendMessage(cfg("tpa.messages.preparation",
                    "§aTéléportation vers §f{player} §adans §f{delay}§as — préparez-vous !")
                    .replace("{player}", dest.getName())
                    .replace("{delay}", String.valueOf(prepDelay)));
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (!mover.isOnline()) return;

            // ── Phase 2 : immobilité (annulé si mouvement) ────────────────
            if (tpDelay > 0) {
                mover.sendMessage(cfg("tpa.messages.teleporting",
                        "§aTéléportation dans §f{delay}§as. Ne bougez plus !")
                        .replace("{delay}", String.valueOf(tpDelay)));
            }

            TeleportUtil.schedule(mover, dest.getLocation(), tpDelay,
                    "Vers §f" + dest.getName(), cancelMsg,
                    () -> mover.sendMessage("§aTéléporté vers §f" + dest.getName() + "§a."));

        }, prepDelay * 20L);
        return true;
    }

    // ── /tpdeny ───────────────────────────────────────────────────────────────

    private boolean handleDeny(Player receiver) {
        if (!Main.hasGroupPermission(receiver, "armesia.tpa")) {
            receiver.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        TpaManager.Request req = TpaManager.getRequest(receiver.getUniqueId());
        if (req == null) {
            receiver.sendMessage(cfg("tpa.messages.no-request", "§cVous n'avez aucune demande en attente."));
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(req.sender);
        TpaManager.removeRequest(receiver.getUniqueId());

        receiver.sendMessage(cfg("tpa.messages.denied", "§cDemande refusée."));
        if (senderPlayer != null) {
            senderPlayer.sendMessage(cfg("tpa.messages.denied-by", "§f{player} §ca refusé votre demande.")
                    .replace("{player}", receiver.getName()));
        }
        return true;
    }

    // ── /tpacancel ────────────────────────────────────────────────────────────

    private boolean handleCancel(Player sender) {
        if (!Main.hasGroupPermission(sender, "armesia.tpa") && !Main.hasGroupPermission(sender, "armesia.tpahere")) {
            sender.sendMessage(cfg("general.no-permission", "§cVous n'avez pas la permission.")); return true;
        }
        TpaManager.Request req = TpaManager.removeRequestBySender(sender.getUniqueId());
        if (req == null) {
            sender.sendMessage(cfg("tpa.messages.no-pending", "§cVous n'avez aucune demande en cours."));
            return true;
        }
        sender.sendMessage(cfg("tpa.messages.cancelled", "§cVotre demande a été annulée."));
        Player receiver = Bukkit.getPlayer(req.receiver);
        if (receiver != null) {
            receiver.sendMessage(cfg("tpa.messages.cancelled-by-sender",
                    "§f{player} §ca annulé sa demande de téléportation.")
                    .replace("{player}", sender.getName()));
        }
        return true;
    }

    private String cfg(String path, String def) {
        return Main.getInstance().getConfig().getString(path, def);
    }
}


