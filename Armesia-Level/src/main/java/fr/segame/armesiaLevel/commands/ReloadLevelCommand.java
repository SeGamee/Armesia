package fr.segame.armesiaLevel.commands;

import fr.segame.armesiaLevel.ArmesiaLevel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadLevelCommand implements CommandExecutor {

    private final ArmesiaLevel plugin;

    public ReloadLevelCommand(ArmesiaLevel plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("armesia.level.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        plugin.reloadLevelConfig();
        sender.sendMessage("§a[Armesia-Level] §7Configuration rechargée avec succès !");
        plugin.getLogger().info("Config rechargée par " + sender.getName());
        return true;
    }
}

