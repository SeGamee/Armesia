# GroupAPI — Documentation

L'API de gestion des groupes et permissions d'**Armesia**.  
Elle permet à n'importe quel plugin du serveur (Armesia-Casino, Armesia-Level, etc.)
de lire et modifier les groupes, permissions et préfixes sans dépendre directement
des classes internes du plugin.

---

## Accès à l'API

L'API est exposée via le `ServicesManager` de Bukkit. Utilisez le point d'entrée
centralisé `APIProvider` :

```java
import fr.segame.armesia.api.GroupAPI;
import fr.segame.armesia.utils.APIProvider;

GroupAPI groups = APIProvider.getGroups();
if (groups == null) {
    // Le plugin Armesia n'est pas chargé
    return;
}
```

> **Conseil** : vérifiez toujours que `getGroups()` ne retourne pas `null`
> avant de l'utiliser. Armesia doit être listé dans `depend` ou `softdepend`
> du `plugin.yml` de votre plugin.

---

## Système de permissions — fonctionnement

### Vue d'ensemble

Armesia dispose de son propre système de groupes (stockés dans `config.yml`).
Chaque groupe possède une liste de permissions :

```yaml
groups:
  Admin:
    permissions:
      - armesia.vanish
      - armesia.god
      - armesia.tp
    chat-prefix: "&c[Admin]"
    tab-prefix:  "&c[Admin]"
    priority: 100
  Citoyen:
    permissions: []
    chat-prefix: ""
    tab-prefix:  ""
    priority: 0
```

### Intégration avec Bukkit (`PermissionAttachment`)

À chaque connexion d'un joueur, Armesia **injecte automatiquement** les permissions
de son groupe dans le système de permissions natif de Bukkit via un
`PermissionAttachment`. Concrètement :

1. `PlayerJoinEvent` → `loadPlayer()` → création d'un `PermissionAttachment`
   contenant toutes les permissions du groupe du joueur.
2. Le joueur peut désormais être testé avec **`player.hasPermission("ma.permission")`**
   nativement — aucune dépendance à Armesia requise dans le code de vérification.
3. Si le groupe du joueur change en cours de jeu (`/group set <joueur> <groupe>`),
   l'attachment est **détruit et recréé** immédiatement avec les nouvelles permissions.
4. `PlayerQuitEvent` → l'attachment est **supprimé** proprement.

### Wildcard `"*"`

Si la liste de permissions d'un groupe contient `"*"`, **toutes les permissions
enregistrées dans Bukkit** sont accordées à tous les membres de ce groupe
(équivalent d'un OP via l'attachment, sans `/op`).

### Priorité de vérification dans Armesia

La méthode `Main.hasGroupPermission(player, perm)` suit l'ordre suivant :

| Priorité | Condition |
|---|---|
| 1 | Joueur OP → `true` automatiquement |
| 2 | `player.hasPermission(perm)` (inclut l'attachment Armesia + permissions Bukkit natives) |
| 3 | Lecture directe de la liste du groupe dans `config.yml` (filet de sécurité) |

---

## Référence de l'API

### Gestion des groupes

```java
GroupAPI groups = APIProvider.getGroups();

// Vérifier l'existence
boolean exists = groups.exists("Admin");

// Lister tous les groupes
Set<String> all = groups.getGroups();

// Créer / supprimer
groups.createGroup("Modérateur");
groups.deleteGroup("Ancien"); // les membres sont renvoyés vers "Citoyen"
```

### Permissions par groupe

```java
// Ajouter / retirer une permission d'un groupe
groups.addPermission("Admin", "armesia.vanish");
groups.removePermission("Admin", "armesia.vanish");

// Lister les permissions d'un groupe
List<String> perms = groups.getPermissions("Admin");

// Tester si un groupe possède une permission
boolean ok = groups.groupHasPermission("Admin", "armesia.vanish");
```

### Vérification côté joueur

Grâce à l'intégration Bukkit, la méthode native suffit dans la majorité des cas :

```java
// ✅ Recommandé — fonctionne grâce à l'attachment injecté au login
if (player.hasPermission("armesia.vanish")) { ... }

// ✅ Aussi disponible via l'API — utile si vous avez seulement l'UUID
GroupAPI groups = APIProvider.getGroups();
if (groups.playerHasPermission(player.getUniqueId(), "armesia.vanish")) { ... }

// ✅ Méthode interne Armesia (inclut le filet de sécurité)
if (Main.hasGroupPermission(player, "armesia.vanish")) { ... }
```

### Groupe d'un joueur

```java
// Lire le groupe actuel (retourne le groupe par défaut si absent)
String group = groups.getPlayerGroup(player.getUniqueId());

// Changer le groupe (met à jour le tab, le chat ET l'attachment Bukkit)
groups.setPlayerGroup(player, "Admin");
```

### Préfixes

```java
// Lire (codes couleur déjà décodés, espace de séparation inclus si pertinent)
String chatPrefix = groups.getChatPrefix("Admin"); // "§c[Admin] "
String tabPrefix  = groups.getTabPrefix("Admin");

// Modifier (utiliser les codes &)
groups.setChatPrefix("Admin", "&c[Admin]");
groups.setTabPrefix("Admin", "&c[Admin]");

// Effacer
groups.clearChatPrefix("Admin");
groups.clearTabPrefix("Admin");
```

### Priorité

La priorité détermine l'ordre d'affichage dans le tableau des scores (tab-list).
Plus la valeur est **haute**, plus le groupe est affiché **en haut**.

```java
int priority = groups.getPriority("Admin"); // ex: 100
groups.setPriority("Admin", 200);
```

---

## Exemple complet — plugin externe

```java
import fr.segame.armesia.api.GroupAPI;
import fr.segame.armesia.utils.APIProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class MyListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Option 1 : Bukkit natif (fonctionne grâce à l'attachment Armesia)
        if (!player.hasPermission("monplugin.utiliser")) {
            player.sendMessage("§cVous n'avez pas la permission.");
            return;
        }

        // Option 2 : via GroupAPI (si vous avez besoin d'infos supplémentaires)
        GroupAPI groups = APIProvider.getGroups();
        if (groups != null) {
            String groupe = groups.getPlayerGroup(player.getUniqueId());
            player.sendMessage("Votre groupe : " + groupe);
        }
    }
}
```

---

## Dépendance Gradle (depuis un sous-module Armesia)

Les sous-modules du projet (Armesia-Casino, Armesia-Level…) ont déjà accès
aux classes du module principal via la dépendance dans leur `build.gradle` :

```groovy
dependencies {
    compileOnly project(':Armesia')
}
```

Pour un plugin externe indépendant, ajoutez `Armesia.jar` en `compileOnly`
et déclarez la dépendance dans votre `plugin.yml` :

```yaml
depend:
  - Armesia
```

---

## Résumé des classes

| Classe | Rôle |
|---|---|
| `GroupAPI` | Interface publique — contrat de l'API |
| `GroupImpl` | Implémentation — délègue au `GroupManager` |
| `APIProvider.getGroups()` | Point d'entrée recommandé pour les plugins tiers |
| `GroupManager` | Logique métier interne (config.yml) |
| `PlayerDataManager` | Gère les `PermissionAttachment` Bukkit par joueur |

