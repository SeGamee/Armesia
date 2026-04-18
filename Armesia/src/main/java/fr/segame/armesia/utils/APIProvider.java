package fr.segame.armesia.utils;

import fr.segame.armesia.api.EconomyAPI;
import fr.segame.armesia.api.GroupAPI;
import fr.segame.armesia.api.StatsAPI;
import org.bukkit.Bukkit;

public final class APIProvider {

    private APIProvider() {}

    public static EconomyAPI getEconomy() {
        return Bukkit.getServicesManager().load(EconomyAPI.class);
    }

    public static GroupAPI getGroups() {
        return Bukkit.getServicesManager().load(GroupAPI.class);
    }

    public static StatsAPI getStats() {
        return Bukkit.getServicesManager().load(StatsAPI.class);
    }
}


