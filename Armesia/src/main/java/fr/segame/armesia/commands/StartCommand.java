package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.task.StartTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StartCommand implements CommandExecutor {

    private StartTask startTask = null;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(startTask != null) {
            sender.sendMessage("§cUne task a déjà été lancé.");
        } else {
            startTask = new StartTask((task) -> startTask = null);
            startTask.runTaskTimer(Main.getInstance(), 0, 20);
        }

        return false;
    }

}
