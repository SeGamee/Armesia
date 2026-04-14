package fr.segame.armesiaCasino;

import org.bukkit.Material;

import java.util.List;

public class Prize {

    private final String       id;
    private final Material     displayMaterial;
    private final String       displayName;
    private final List<String> displayLore;
    private final int          chance;
    private final String       consoleCommand;
    private final String       broadcastMessage;
    private final String       playerMessage;

    public Prize(String id, Material displayMaterial, String displayName,
                 List<String> displayLore, int chance,
                 String consoleCommand, String broadcastMessage, String playerMessage) {
        this.id               = id;
        this.displayMaterial  = displayMaterial;
        this.displayName      = displayName;
        this.displayLore      = displayLore;
        this.chance           = chance;
        this.consoleCommand   = consoleCommand;
        this.broadcastMessage = broadcastMessage;
        this.playerMessage    = playerMessage;
    }

    public String       getId()               { return id; }
    public Material     getDisplayMaterial()  { return displayMaterial; }
    public String       getDisplayName()      { return displayName; }
    public List<String> getDisplayLore()      { return displayLore; }
    public int          getChance()           { return chance; }
    public String       getConsoleCommand()   { return consoleCommand; }
    public String       getBroadcastMessage() { return broadcastMessage; }
    public String       getPlayerMessage()    { return playerMessage; }
}

