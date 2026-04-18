package fr.segame.armesiaMobs.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API publique pour les statistiques de kills de mobs custom (Armesia-Mobs).
 * Accessible via le ServicesManager : {@code Bukkit.getServicesManager().load(MobStatsAPI.class)}
 */
public interface MobStatsAPI {

    /**
     * Retourne le nombre de kills d'un joueur pour un mob précis.
     * @param playerUUID UUID du joueur
     * @param mobId      identifiant du mob (ex : "zombie_alpha")
     */
    int getKills(UUID playerUUID, String mobId);

    /**
     * Retourne le total de kills (tous mobs confondus) pour un joueur.
     */
    int getTotalKills(UUID playerUUID);

    /**
     * Retourne la map mobId → kills pour un joueur.
     */
    Map<String, Integer> getMobKills(UUID playerUUID);

    /**
     * Retourne le top N des joueurs pour un mob donné (ou tous mobs si mobId == null).
     * @param mobId  mob cible, ou {@code null} pour toutes les tues confondues
     * @param limit  taille max du classement
     */
    List<Map.Entry<UUID, Integer>> getTop(String mobId, int limit);

    /**
     * Retourne le top N des joueurs (kills totaux, tous mobs confondus).
     */
    List<Map.Entry<UUID, Integer>> getTopTotal(int limit);
}

