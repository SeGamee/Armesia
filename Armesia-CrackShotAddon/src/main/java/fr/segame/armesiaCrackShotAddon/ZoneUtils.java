package fr.segame.armesiaCrackShotAddon;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ZoneUtils {

    public static Location resolveLocation(ConfigurationSection zone, Player shooter, Location impactLoc) {

        String locType = zone.getString("Location", "IMPACT");

        switch (locType.toUpperCase()) {

            case "PLAYER":
            case "SHOOT":
                return shooter.getLocation();

            case "IMPACT":
            default:
                return impactLoc != null ? impactLoc : shooter.getLocation();
        }
    }
}