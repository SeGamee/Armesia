# 📦 Armesia-Menu

Plugin de menus configurables entièrement en YAML pour serveurs Bukkit/Paper.  
Les menus sont définis dans `menus.yml`, sans aucun redémarrage grâce au reload à chaud.

---

## 📋 Sommaire

- [Commandes](#-commandes)
- [Structure d'un menu](#-structure-dun-menu)
- [Structure d'un item](#-structure-dun-item)
- [Actions](#-actions)
- [Conditions](#-conditions)
- [Lore conditionnel](#-lore-conditionnel)
- [Placeholders](#-placeholders)
- [Restriction par monde](#-restriction-par-monde)
- [API Java](#-api-java)

---

## 🕹️ Commandes

| Commande | Description | Permission |
|---|---|---|
| `/menu <id>` | Ouvre le menu avec l'ID donné | aucune |
| `/menu reload` | Recharge tous les menus depuis `menus.yml` | `armesia.menu.reload` |

---

## 🏗️ Structure d'un menu

```yaml
menus:
  mon_menu:
    title: "&6Mon Menu"   # Titre de l'inventaire (codes couleur &)
    size: 27              # Taille : 9, 18, 27, 36, 45, 54

    worlds:
      allowed:            # (optionnel) Seuls ces mondes autorisent le menu
        - world
      blocked:            # (optionnel) Ces mondes bloquent le menu
        - world_nether

    items:
      mon_item:
        # ... voir section item
```

---

## 🧩 Structure d'un item

```yaml
mon_item:
  material: DIAMOND          # Matériau Bukkit (PLAYER_HEAD pour tête de joueur)
  name: "&bNom de l'item"    # Nom affiché (codes couleur &)
  slot: 13                   # Slot dans l'inventaire (0-based)

  lore:
    - "&7Ligne 1"
    - "&7Ligne 2"
    # Les lignes peuvent être conditionnelles (voir section Lore conditionnel)

  conditions:                # (optionnel) Conditions à remplir pour exécuter actions
    - type: money
      min: 500

  actions:                   # Actions exécutées si conditions remplies
    - type: message
      message: "&aBonjour !"
      click: LEFT

  fail-actions:              # Actions exécutées si conditions NON remplies
    - type: message
      message: "&cCondition non remplie"
      click: LEFT
```

---

## ⚡ Actions

Toutes les actions acceptent un champ `click` pour filtrer le type de clic.  
Si `click` est absent, l'action s'exécute sur n'importe quel clic.

**Valeurs `click` disponibles :**  
`LEFT`, `RIGHT`, `SHIFT_LEFT`, `SHIFT_RIGHT`, `MIDDLE`  
Plusieurs valeurs possibles via liste : `click: [LEFT, RIGHT]`

---

### 💬 `message`
Envoie un message au joueur.
```yaml
- type: message
  message: "&aMessage envoyé !"
  click: LEFT
```

---

### 📂 `open_menu`
Ouvre un autre menu.
```yaml
- type: open_menu
  menu: shop       # ID du menu cible
  click: LEFT
```
> ⚠️ Pas besoin de `click` ici, le menu s'ouvre au clic par défaut.

---

### ❌ `close`
Ferme l'inventaire.
```yaml
- type: close
  click: RIGHT
```

---

### 🔄 `refresh`
Rafraîchit le menu actuel (met à jour les placeholders).
```yaml
- type: refresh
  click: LEFT
```

---

### 💻 `command`
Exécute une commande en tant que joueur ou console.
```yaml
- type: command
  command: "enderchest"   # Sans le /
  console: false          # (optionnel) true = exécuté par la console
  click: LEFT
```

---

### 💰 `add_money`
Ajoute de l'argent au joueur.
```yaml
- type: add_money
  amount: 1000
  click: LEFT
```

---

### 💸 `take_money`
Retire de l'argent au joueur.
```yaml
- type: take_money
  amount: 500
  click: RIGHT
```

---

### 🪙 `add_tokens`
Ajoute des tokens au joueur.
```yaml
- type: add_tokens
  amount: 50
  click: LEFT
```

---

### 🪙 `take_tokens`
Retire des tokens au joueur.
```yaml
- type: take_tokens
  amount: 100
  click: RIGHT
```

---

### 🎁 `give_item`
Donne un item au joueur.
```yaml
- type: give_item
  material: DIAMOND
  amount: 5
  click: LEFT
```

---

### 🏷️ `mark_claimed`
Marque une récompense comme récupérée (persistant dans `rewards.yml`).
```yaml
- type: mark_claimed
  key: daily       # Clé unique de la récompense
  click: LEFT
```

---

## 🔒 Conditions

Les conditions bloquent l'exécution des `actions` si elles ne sont pas remplies.  
Les `fail-actions` sont alors exécutées à la place.

---

### `money`
Vérifie que le joueur a un minimum d'argent.
```yaml
- type: money
  min: 1000
```

---

### `tokens`
Vérifie que le joueur a un minimum de tokens.
```yaml
- type: tokens
  min: 100
```

---

### `level`
Vérifie que le joueur a un niveau minimum.
```yaml
- type: level
  min: 10
```

---

### `permission`
Vérifie que le joueur a une permission.
```yaml
- type: permission
  permission: armesia.vip
```

---

### `not_claimed`
Vérifie que le joueur n'a pas encore récupéré une récompense.
```yaml
- type: not_claimed
  key: daily
```

---

## 📝 Lore conditionnel

Il est possible d'ajouter des lignes de lore conditionnelles directement dans la liste `lore`.  
Elles n'apparaissent que si la condition est vraie.

```yaml
lore:
  - "&7Ligne toujours visible"

  - condition:
      type: permission
      permission: armesia.vip
    lore:
      - "&a✔ VIP actif"

  - condition:
      type: permission
      permission: armesia.vip
    inverse: true          # inverse: true = s'affiche si la condition est FAUSSE
    lore:
      - "&c✘ VIP requis"

  - condition:
      type: not_claimed
      key: daily
    lore:
      - "&a✔ Disponible"
```

> Toutes les conditions disponibles dans la section [Conditions](#-conditions) sont utilisables ici.

---

## 🏷️ Placeholders

Utilisables dans `name`, `lore`, `message` et les titres.

### Joueur
| Placeholder | Description |
|---|---|
| `%player%` | Nom du joueur |
| `%group%` | Groupe du joueur |
| `%group_chat_prefix%` | Préfixe chat du groupe |
| `%group_tab_prefix%` | Préfixe tab du groupe |
| `%money%` | Argent formaté |
| `%tokens%` | Tokens |
| `%level%` | Niveau |
| `%xp%` | XP actuelle |
| `%xp_bar%` | Barre XP (10 caractères) |
| `%kills%` | Kills PvP |
| `%deaths%` | Morts PvP |
| `%killstreak%` | Killstreak actuelle |
| `%bestkillstreak%` | Meilleure killstreak |
| `%ratio%` | Ratio K/D formaté |
| `%mob_kills_total%` | Total de mobs tués |
| `%mob_kills_<mobId>%` | Mobs tués pour un type spécifique |

### Classements (remplacer N par le rang, ex: `1`, `2`, `3`…)
| Placeholder | Description |
|---|---|
| `%top_money_name_N%` | Nom du joueur au rang N (argent) |
| `%top_money_N%` | Valeur argent au rang N |
| `%top_tokens_name_N%` | Nom du joueur au rang N (tokens) |
| `%top_tokens_N%` | Valeur tokens au rang N |
| `%top_kills_name_N%` | Nom du joueur au rang N (kills) |
| `%top_kills_N%` | Kills au rang N |
| `%top_deaths_name_N%` | Nom du joueur au rang N (morts) |
| `%top_deaths_N%` | Morts au rang N |
| `%top_killstreak_name_N%` | Nom du joueur au rang N (killstreak) |
| `%top_killstreak_N%` | Killstreak au rang N |
| `%top_bestkillstreak_name_N%` | Nom du joueur au rang N (best KS) |
| `%top_bestkillstreak_N%` | Best KS au rang N |
| `%top_mob_kills_name_N%` | Nom du joueur au rang N (total mobs) |
| `%top_mob_kills_N%` | Total mobs tués au rang N |
| `%top_mob_kills_<mobId>_name_N%` | Nom du joueur au rang N pour un mob |
| `%top_mob_kills_<mobId>_N%` | Kills au rang N pour un mob |

> 💡 Si le rang n'existe pas (ex: moins de N joueurs), la ligne de lore est automatiquement supprimée.

---

## 🔌 API Java

Accès au `MenuManager` via l'instance du plugin :

```java
ArmesiaMenu plugin = (ArmesiaMenu) Bukkit.getPluginManager().getPlugin("Armesia-Menu");
MenuManager menuManager = plugin.getMenuManager(); // si exposée
```

> En attendant un ServiceProvider, passer par `ArmesiaMenu.getInstance()`.

### `MenuManager`

```java
// Ouvrir un menu à un joueur
menuManager.openMenu(Player player, Menu menu);

// Récupérer un menu par ID
Menu menu = menuManager.getMenu(String id);

// Récupérer le menu actuellement ouvert par un joueur
Menu menu = menuManager.getOpenMenu(Player player);

// Fermer le menu d'un joueur (et le désenregistrer)
menuManager.closeMenu(Player player);

// Rafraîchir le menu ouvert d'un joueur
menuManager.refreshMenu(Player player);

// Recharger tous les menus depuis menus.yml
menuManager.reloadMenus();

// Vérifier si un joueur est en cours de refresh
boolean refreshing = menuManager.isRefreshing(Player player);
```

### `RewardManager`

```java
// Vérifier si un joueur a déjà récupéré une récompense
boolean claimed = rewardManager.hasClaimed(UUID uuid, String key);

// Marquer une récompense comme récupérée
rewardManager.claim(UUID uuid, String key);
```

---

## 📁 Fichiers générés

| Fichier | Description |
|---|---|
| `menus.yml` | Définition de tous les menus |
| `rewards.yml` | Sauvegarde des récompenses récupérées par joueur |

