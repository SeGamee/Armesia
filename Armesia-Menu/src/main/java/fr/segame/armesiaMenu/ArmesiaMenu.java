package fr.segame.armesiaMenu;

import fr.segame.armesiaMenu.command.MenuCommand;
import fr.segame.armesiaMenu.listener.MenuListener;
import fr.segame.armesiaMenu.menu.MenuManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ArmesiaMenu extends JavaPlugin {

    private static ArmesiaMenu instance;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;

        saveResource("menus.yml", false);

        menuManager = new MenuManager(this);
        menuManager.loadMenus();

        getServer().getPluginManager().registerEvents(new MenuListener(menuManager), this);

        getCommand("menu").setExecutor(new MenuCommand(menuManager));
    }

    public static ArmesiaMenu getInstance() {
        return instance;
    }
}