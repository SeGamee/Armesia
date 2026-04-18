package fr.segame.armesiaMenu.command;

import fr.segame.armesiaMenu.menu.Menu;
import fr.segame.armesiaMenu.menu.MenuManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class MenuCommand implements CommandExecutor {

    private final MenuManager manager;

    public MenuCommand(MenuManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // 👉 RELOAD
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("armesia.menu.reload")) {
                sender.sendMessage("§cPas la permission");
                return true;
            }

            manager.reloadMenus();
            sender.sendMessage("§aMenus rechargés !");
            return true;
        }

        // 👉 OPEN MENU
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande joueur uniquement");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§c/menu <id>");
            return true;
        }

        Menu menu = manager.getMenu(args[0]);

        if (menu != null) {
            manager.openMenu(player, menu);
        } else {
            player.sendMessage("§cMenu introuvable");
        }

        return true;
    }
}