package fr.segame.armesia.api;

import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * API publique de gestion des groupes / permissions exposée via le ServicesManager de Bukkit.
 *
 * <p>Utilisation depuis n'importe quel plugin :
 * <pre>{@code
 *     GroupAPI groups = APIProvider.getGroups();
 *     if (groups != null) {
 *         groups.setPlayerGroup(player, "Admin");
 *         boolean canFly = groups.playerHasPermission(player.getUniqueId(), "armesia.fly");
 *     }
 * }</pre>
 */
public interface GroupAPI {

    // ─── Gestion des groupes ────────────────────────────────────────────────────

    /** @return {@code true} si le groupe existe. */
    boolean exists(String group);

    /**
     * Crée un nouveau groupe vide.
     * @return {@code false} si le groupe existait déjà ou si le nom est invalide.
     */
    boolean createGroup(String group);

    /**
     * Supprime un groupe et réaffecte ses membres au groupe par défaut.
     * @return {@code false} si le groupe n'existe pas ou s'il s'agit du groupe par défaut.
     */
    boolean deleteGroup(String group);

    /** @return l'ensemble des noms de groupes configurés. */
    Set<String> getGroups();

    // ─── Permissions par groupe ─────────────────────────────────────────────────

    /**
     * Ajoute une permission à un groupe.
     * @return {@code false} si la permission était déjà présente ou si le groupe n'existe pas.
     */
    boolean addPermission(String group, String permission);

    /**
     * Retire une permission d'un groupe.
     * @return {@code false} si la permission n'était pas dans ce groupe.
     */
    boolean removePermission(String group, String permission);

    /** @return la liste des permissions du groupe, ou liste vide si le groupe n'existe pas. */
    List<String> getPermissions(String group);

    /** @return {@code true} si le groupe possède cette permission. */
    boolean groupHasPermission(String group, String permission);

    // ─── Vérification joueur ────────────────────────────────────────────────────

    /**
     * Vérifie si un joueur (identifié par son UUID) possède une permission
     * via son groupe Armesia.
     */
    boolean playerHasPermission(UUID uuid, String permission);

    // ─── Groupe du joueur ───────────────────────────────────────────────────────

    /**
     * @return le nom du groupe actuel du joueur, ou le groupe par défaut s'il n'en a pas.
     */
    String getPlayerGroup(UUID uuid);

    /**
     * Affecte un joueur à un groupe.
     * @return {@code false} si le groupe n'existe pas ou si le joueur est inconnu.
     */
    boolean setPlayerGroup(OfflinePlayer player, String group);

    // ─── Préfixes ───────────────────────────────────────────────────────────────

    /** @return le préfixe de chat du groupe (codes couleur décodés). */
    String getChatPrefix(String group);

    /** @return le préfixe du tableau des scores du groupe (codes couleur décodés). */
    String getTabPrefix(String group);

    /**
     * Modifie le préfixe de chat d'un groupe.
     * @return {@code false} si le groupe n'existe pas.
     */
    boolean setChatPrefix(String group, String prefix);

    /**
     * Modifie le préfixe du tableau des scores d'un groupe.
     * @return {@code false} si le groupe n'existe pas.
     */
    boolean setTabPrefix(String group, String prefix);

    /** Efface le préfixe de chat d'un groupe. */
    boolean clearChatPrefix(String group);

    /** Efface le préfixe du tableau des scores d'un groupe. */
    boolean clearTabPrefix(String group);

    // ─── Priorité ───────────────────────────────────────────────────────────────

    /** @return la priorité du groupe (plus la valeur est haute, plus le groupe est prioritaire). */
    int getPriority(String group);

    /**
     * Modifie la priorité d'un groupe.
     * @return {@code false} si le groupe n'existe pas.
     */
    boolean setPriority(String group, int priority);
}

