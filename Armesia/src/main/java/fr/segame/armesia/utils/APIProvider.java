package fr.segame.armesia.utils;

import fr.segame.armesia.api.EconomyAPI;
import org.bukkit.Bukkit;

/**
 * Fournisseur centralisé pour accéder aux APIs du plugin Armesia
 * via le {@link org.bukkit.plugin.ServicesManager} de Bukkit.
 *
 * <p>Utilisation :
 * <pre>{@code
 *     EconomyAPI eco = APIProvider.getEconomy();
 *     if (eco != null) eco.addMoney(uuid, 500);
 * }</pre>
 */
public final class APIProvider {

    private APIProvider() { /* classe utilitaire */ }

    /**
     * Retourne l'implémentation de {@link EconomyAPI} enregistrée dans le ServicesManager.
     *
     * @return l'instance d'économie, ou {@code null} si le plugin Armesia n'est pas chargé.
     */
    public static EconomyAPI getEconomy() {
        return Bukkit.getServicesManager().load(EconomyAPI.class);
    }
}

