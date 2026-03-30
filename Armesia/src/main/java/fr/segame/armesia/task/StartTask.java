package fr.segame.armesia.task;

import fr.segame.armesia.Main;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class StartTask extends BukkitRunnable {

    private int seconds;
    private final Consumer<StartTask> endTask;

    public StartTask(@Nullable Consumer<StartTask> endTask) {
        this.seconds = Main.getInstance().getConfig().getInt("task.seconds");
        this.endTask = endTask;
    }

    @Override
    public void run() {
        Bukkit.broadcastMessage("§6Démarrage dans §9%s §6seconde%s".formatted(seconds, seconds <= 1 ? "" : "s"));
        seconds--;

        if(seconds == 0) {
            cancel();
            if(endTask != null) {
                endTask.accept(this);
            }
        }
    }
}
