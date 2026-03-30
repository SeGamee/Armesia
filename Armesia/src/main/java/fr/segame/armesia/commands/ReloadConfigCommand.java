package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadConfigCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        Main.getInstance().reloadConfig();
        sender.sendMessage("§aVotre configuration a bien été rechargé.");

        return true;
    }
}
