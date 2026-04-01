# Armesia — Documentation complète

> Plugin Spigot/Paper 1.20.1 — Système de mobs personnalisés, zones de spawn, loots, économie et niveaux.

---

## Table des matières

1. [Installation](#1-installation)
2. [Architecture](#2-architecture)
3. [Commandes — Mobs (`/mob`)](#3-commandes--mobs-mob)
4. [Commandes — Loots (`/loot`)](#4-commandes--loots-loot)
5. [Commandes — Zones (`/zone`)](#5-commandes--zones-zone)
6. [Fichiers de configuration](#6-fichiers-de-configuration)
7. [Système de debug (`/zone debug`)](#7-système-de-debug-zone-debug)
8. [Guide de test complet (pas à pas)](#8-guide-de-test-complet-pas-à-pas)
9. [Comportements importants à connaître](#9-comportements-importants-à-connaître)
10. [Fonctionnalités à ajouter / améliorer](#10-fonctionnalités-à-ajouter--améliorer)

---

## 1. Installation

### Prérequis
- **Paper 1.20.1** (ou Spigot 1.20.1)
- Java 17+

### Déploiement
```
1. ./gradlew :Armesia:build
2. Copier Armesia/build/libs/Armesia-1.0.0.jar → plugins/
3. (Re)démarrer le serveur
```

### Fichiers générés au premier démarrage
```
plugins/Armesia/
├── config.yml       ← config générale (groupes, etc.)
├── players.yml      ← données joueurs (XP, niveau, argent)
├── mobs.yml         ← mobs personnalisés (vide au départ)
├── loots.yml        ← tables de loot  (vide au départ)
└── zones.yml        ← zones de spawn  (vide au départ)
```

---

## 2. Architecture

```
Mob système
  MobData       — définition statique (type, HP, argent, loot)
  MobInstance   — instance vivante (UUID ↔ zoneId ↔ mobId)
  MobManager    — registre des définitions + instances actives
  MobSpawner    — spawn physique (spawnEntity + setPersistent)
  MobListener   — bloque vanilla, nettoie chunks, gère mort/combat/feu

Zone système
  ZoneData      — configuration d'une zone (spawn + despawn + condition)
  ZoneManager   — 3 tâches planifiées : spawn, despawn, patrouille
  ZoneCommand   — toutes les commandes /zone
  ZoneConfig    — lecture/écriture zones.yml

Loot système
  LootData      — une entrée (material|item custom, qté, chance)
  LootManager   — registre des tables, drop au sol à la mort
  LootConfig    — lecture/écriture loots.yml

Économie
  EconomyManager — argent + tokens par UUID (players.yml)
  EconomyAPI     — API publique (addMoney, removeMoney, etc.)

Niveaux
  GamePlayer     — XP + niveau en mémoire
  PlayerManager  — map UUID → GamePlayer
  LevelManager   — logique de montée de niveau

Groupes
  GroupManager   — rangs/préfixes (config.yml)
```

### Cycle de vie d'un mob
```
ZoneManager (spawn task, 1s)
  → getZoneAt(joueur) → vérifie intervalles, cap, condition, cible, chance
  → getSmartSpawnLocation() → 10 tentatives dans le rayon autour du joueur
  → MobSpawner.spawnMob()
      → world.spawnEntity()  [SpawnReason.CUSTOM → passe le filtre]
      → setCustomName / setHealth / setPersistent(true) / setRemoveWhenFarAway(false)
      → MobManager.addInstance()

ZoneManager (despawn task, 1s avec timer par zone)
  → pour chaque mob actif :
      hors zone → redirection vers centre (si trop loin : suppression)
      dans zone → si zone due : calcul distance joueur → chance despawn → remove

MobListener.onDeath()
  → vide les drops vanilla
  → appelle LootManager.dropLoot()  → drop items au sol
  → appelle EconomyAPI.addMoney()   → donne l'argent au tueur
  → retire de MobManager
```

---

## 3. Commandes — Mobs (`/mob`)

**Permission :** `armesia.mob` (défaut : OP)

| Commande | Description |
|---|---|
| `/mob list` | Liste tous les mobs enregistrés |
| `/mob info <id>` | Détails d'un mob |
| `/mob create <id> <type> <lvl> <hp> <argent> <nom...>` | Créer un mob |
| `/mob delete <id>` | Supprimer un mob |
| `/mob set <id> name <nom...>` | Renommer (supporte `&c`, `&l`, etc.) |
| `/mob set <id> level <int>` | Changer le niveau |
| `/mob set <id> health <double>` | Changer les HP |
| `/mob set <id> money <int>` | Changer l'argent à la mort |
| `/mob set <id> type <EntityType>` | Changer le type d'entité |
| `/mob set <id> loot <tableId>` | Associer une table de loot |
| `/mob spawn <id>` | Spawner le mob à sa position (test) |

### Types d'entités disponibles (EntityType)
Tout type vivant de Bukkit : `ZOMBIE`, `SKELETON`, `SPIDER`, `CREEPER`, `ENDERMAN`, `WITCH`, `BLAZE`, `WITHER_SKELETON`, `PILLAGER`, `VINDICATOR`, `RAVAGER`, `DROWNED`, `HUSK`, `STRAY`, `PHANTOM`, `GUARDIAN`, `ELDER_GUARDIAN`, etc.

### Exemple de création
```
/mob create zombie_foret ZOMBIE 5 60 10 &2Zombie &ade la Forêt
/mob set zombie_foret loot basic_drops
/mob info zombie_foret
```

---

## 4. Commandes — Loots (`/loot`)

**Permission :** `armesia.loot` (défaut : OP)

| Commande | Description |
|---|---|
| `/loot list` | Liste toutes les tables |
| `/loot info <tableId>` | Voir les entrées d'une table (avec index) |
| `/loot create <tableId>` | Créer une table vide |
| `/loot delete <tableId>` | Supprimer une table |
| `/loot add <tableId> <MATERIAL> <min> <max> <chance>` | Ajouter un drop basique |
| `/loot addhand <tableId> <min> <max> <chance>` | Ajouter l'item en main (custom) |
| `/loot remove <tableId> <index>` | Supprimer une entrée (index 1-based) |

### Valeur `chance`
- `1.0` = 100% (drop garanti)
- `0.5` = 50%
- `0.05` = 5%

### Exemple
```
/loot create basic_drops
/loot add basic_drops BONE 1 3 0.8
/loot add basic_drops ROTTEN_FLESH 1 2 1.0
/loot add basic_drops DIAMOND 1 1 0.02

# Item custom (tenir un item dans la main, ex : épée enchantée)
/loot addhand basic_drops 1 1 0.01
```

---

## 5. Commandes — Zones (`/zone`)

**Permission :** `armesia.zone` (défaut : OP)

### Navigation / Structure

| Commande | Description |
|---|---|
| `/zone list` | Liste toutes les zones |
| `/zone info <id>` | Détails complets d'une zone |
| `/zone create <id>` | Créer une zone (pos1=pos2=ta position) |
| `/zone delete <id>` | Supprimer une zone + despawn tous ses mobs |
| `/zone pos1 <id>` | Définir le coin 1 à ta position |
| `/zone pos2 <id>` | Définir le coin 2 à ta position |
| `/zone tp <id>` | Se téléporter au centre de la zone |
| `/zone check` | Diagnostic : es-tu dans une zone ? mobs OK ? |
| `/zone debug [off\|normal\|verbose]` | Activer le debug en jeu |

### Gestion des mobs dans une zone

| Commande | Description |
|---|---|
| `/zone addmob <id> <mobId>` | Ajouter un mob à la zone |
| `/zone removemob <id> <mobId>` | Retirer un mob de la zone |

### Configuration complète : `/zone set <id> <propriété> <valeur>`

---

#### ⚙️ Général

##### `max` — Cap global de mobs dans la zone
```
Type    : int   |   Défaut : 20   |   0 = illimité
```
Nombre **maximum de mobs simultanément vivants** dans cette zone, toutes instances confondues.  
La tâche de spawn ne fera rien tant que ce plafond est atteint.

```
Exemple : /zone set foret max 30
→ La zone ne contiendra jamais plus de 30 zombies en même temps.
→ Quand un mob meurt, le compteur baisse et un nouveau peut spawner.
```

> ⚠️ Si plusieurs joueurs sont dans la zone, chaque joueur peut déclencher un spawn indépendant.
> Il est possible de dépasser `max` de 1 ou 2 dans ce cas. Mettre une marge de sécurité.

---

##### `priority` — Priorité si zones superposées
```
Type    : int   |   Défaut : 0
```
Quand deux zones se superposent (l'une dans l'autre), c'est la zone avec la **priorité la plus haute** qui est active pour le spawn.

```
Exemple :
  /zone set monde priority 0       ← zone globale
  /zone set chateau priority 5     ← zone intérieure, plus spécifique

→ Dans le château : zone "chateau" utilisée (prio 5 > 0)
→ En dehors du château mais dans "monde" : zone "monde" utilisée
```

---

##### `inherit` — Hériter les mobs d'une zone parente
```
Type    : bool (true/false)   |   Défaut : false
```
Si `true`, la zone **ajoute** aussi les mobs des zones de priorité inférieure qui la contiennent.

```
Exemple :
  Zone "monde" (prio 0) → mobs : [zombie]
  Zone "foret" (prio 1) → mobs : [loup], inherit=true

→ Dans "foret" : spawn de zombies ET de loups
→ Dans "monde" (hors foret) : spawn de zombies seulement
```

---

##### `override` — Bloquer l'héritage depuis cette zone
```
Type    : bool (true/false)   |   Défaut : false
```
Si `true`, cette zone **empêche** les zones de priorité encore plus haute d'hériter ses mobs.  
Sert à créer des "barrières" dans la hiérarchie d'héritage.

---

#### 🟢 Spawn — Comment fonctionne le cycle de spawn

Le cycle se répète **toutes les secondes** (`spawninterval` contrôle la vraie fréquence).  
À chaque cycle, pour chaque joueur dans la zone, les conditions sont vérifiées **dans cet ordre** :

```
1. Le compteur de ticks de la zone est-il ≥ spawnInterval ?   → sinon : attente silencieuse
2. Le cap global (max) est-il atteint ?                        → sinon : [SKIP CAP_ATTEINT]
3. La condition heure est-elle remplie ?                       → sinon : [SKIP CONDITION]
4. nearby >= target (cible déjà atteinte) ?                    → sinon : [SKIP CIBLE_OK]
5. Le tirage de spawnchance réussit-il ?  (ignoré si = 1.0)   → sinon : [SKIP CHANCE]
6. Une position valide est-elle trouvée ? (10 tentatives)      → sinon : [SKIP PAS_DE_POSITION]
7. → SPAWN + reset du compteur de ticks à 0
```

> 💡 Les étapes 2–6 ne réinitialisent PAS le compteur de ticks.  
> Seul un **spawn réussi** remet le timer à zéro.

---

##### `spawninterval` — Fréquence des tentatives de spawn
```
Type    : int (secondes)   |   Défaut : 5   |   Min : 1
```
Nombre de **secondes entre deux spawns** dans cette zone.  
Le timer est basé sur des **ticks de jeu** (pas l'horloge système), donc fiable même sous lag.

```
spawninterval=1  → 1 mob spawn par seconde (toutes les 20 ticks)
spawninterval=5  → 1 mob spawn toutes les 5 secondes
spawninterval=30 → 1 mob spawn toutes les 30 secondes (zones rares/boss)
```

> 💡 Le timer **repart à zéro** après chaque spawn réussi.  
> Si le spawn est bloqué (cap, cible atteinte, condition...), le timer continue de tourner.  
> → Si la cible est atteinte, le timer ne se réinitialise pas, donc dès qu'un mob meurt,  
> le prochain spawn aura lieu dans `spawninterval` secondes au maximum.

> ⚠️ `spawninterval=1` + `targetmax=4` → au maximum **4 secondes** pour atteindre la cible depuis 0.  
> Le système spawne **1 mob par intervalle**, pas plusieurs à la fois.

---

##### `spawnmin` et `spawnmax` — Rayon de spawn autour du joueur
```
Type    : double (blocs)   |   Défauts : 20 / 40
```
Le mob est spawné à une distance **aléatoire entre `spawnmin` et `spawnmax` blocs** autour du joueur.

```
Schéma (vue de dessus) :
        spawnmax (40)
       ┌────────────┐
       │  spawnmin  │
       │  ┌──────┐  │
       │  │  🧑  │  │  ← joueur au centre
       │  └──────┘  │
       │  (20 blocs)│
       └────────────┘
       (40 blocs)

→ Le mob apparaît dans l'anneau entre les deux cercles.
→ Jamais trop près (évite le spawn dans la face du joueur).
→ Jamais trop loin (évite le spawn hors zone).
```

> ⚠️ Si `spawnmax` > taille de la zone, beaucoup de tentatives échoueront (`PAS_DE_POSITION`).  
> Règle : `spawnmax` ≤ (largeur ou longueur de la zone) / 2.

```
Exemple pour une zone de 100×100 blocs :
  /zone set foret spawnmin 15
  /zone set foret spawnmax 40    ← OK, 40 < 50
```

---

##### `targetmin` et `targetmax` — Nombre cible de mobs par joueur
```
Type    : int   |   Défauts : 4 / 6
```
Le système calcule un **nombre cible aléatoire** entre `targetmin` et `targetmax` pour chaque joueur.  
Tant que le nombre de mobs **autour du joueur** (dans un rayon de `spawnmax × 2`) est inférieur à cette cible, le spawn continue.

```
Exemple : targetmin=3, targetmax=5
→ Cible tirée au sort : 4 (par exemple)
→ Si 0 mob autour → proba de spawn = 100% × spawnchance
→ Si 2 mobs autour → proba de spawn = (4-2)/4 × spawnchance = 50%
→ Si 4 mobs autour → CIBLE_OK, pas de spawn
```

> 💡 **La probabilité de spawn diminue proportionnellement** au nombre de mobs déjà présents.
> C'est pour avoir une densité progressive, pas un remplissage instantané.

```
Pour une zone agressive (toujours pleine) :
  targetmin=8, targetmax=10, spawninterval=2, spawnchance=1.0

Pour une zone calme (quelques mobs épars) :
  targetmin=2, targetmax=3, spawninterval=10, spawnchance=0.5
```

---

##### `spawnchance` — Probabilité de spawn à chaque intervalle
```
Type    : double (0.0 à 1.0)   |   Défaut : 1.0
```
Probabilité **pure** d'un spawn à chaque fois que l'intervalle s'écoule.  
**Complètement indépendante** du nombre de mobs déjà présents.

```
spawnchance=1.0  → spawn GARANTI à chaque intervalle (tant que nearby < target)
spawnchance=0.5  → 50% de chance de spawner à chaque intervalle
spawnchance=0.1  → 10% de chance (zone très rare)
spawnchance=0.0  → jamais de spawn (zone désactivée sans la supprimer)
```

**Comportement concret avec `spawninterval=1` et `spawnchance=1.0` :**
```
t=0s  : 0 mob présent, target=4 → SPAWN (1er mob)
t=1s  : 1 mob présent, target=4 → SPAWN (2e mob)
t=2s  : 2 mobs présents         → SPAWN (3e mob)
t=3s  : 3 mobs présents         → SPAWN (4e mob)
t=4s  : 4 mobs = target         → CIBLE_OK, arrêt
→ Zone pleine en exactement 4 secondes, 1 mob/s garanti ✓
```

**Comportement avec `spawnchance=0.5` :**
```
→ Chaque seconde, tirage au sort → ~50% de chance de spawner
→ La zone atteint sa cible en ~8s en moyenne (non garantie)
→ Utile pour des zones imprévisibles
```

> ✅ `spawnchance=1.0` est la valeur à utiliser pour un spawn **prévisible et régulier**.  
> Baisser `spawnchance` uniquement si tu veux **de l'aléatoire** dans le rythme de spawn.

---

##### `condition` — Condition d'heure pour spawner
```
Type    : ALWAYS / DAY / NIGHT   |   Défaut : ALWAYS
```
Restreint le spawn à une période de la journée Minecraft.

```
ALWAYS → spawn 24h/24 (par défaut)
DAY    → spawn uniquement entre tick 0 et 12299   (~ 10 min, soleil levé → coucher)
NIGHT  → spawn uniquement entre tick 12300 et 23999 (~ 7 min, coucher → lever)
```

```
Exemples d'usage :
  /zone set foret condition NIGHT    ← zombies uniquement la nuit
  /zone set desert condition DAY     ← créatures du désert le jour
  /zone set donjeon condition ALWAYS ← donjon sombre, toujours actif
```

> 💡 En mode `DAY` ou `NIGHT`, les mobs **déjà spawnés** ne despawnent pas quand la condition
> devient fausse — ils continuent de vivre jusqu'au despawn normal par distance.

---

#### 🔴 Despawn — Comment fonctionne le cycle de despawn

La tâche tourne **toutes les secondes** mais chaque zone n'est évaluée que selon son `despawninterval`.

```
Pour chaque mob vivant :
  1. Hors zone ?
       → Si dist. au centre > despawn × 1.5 : suppression immédiate
       → Sinon : redirection pathfinder vers le centre
  2. Dans sa zone, zone due pour vérification ?
       → Calculer la distance au joueur le plus proche
       → Choisir le palier de chance selon les ratios
       → Lancer le dé → suppression ou survie
```

---

##### `despawn` — Distance seuil de despawn
```
Type    : double (blocs)   |   Défaut : 150
```
Distance de référence (en blocs) entre le mob et le **joueur le plus proche**.  
Cette valeur est utilisée comme **base de calcul** pour les 4 paliers de probabilité.

```
Exemple : despawn=100
→ Un mob à 120 blocs du joueur le plus proche est "au-delà du seuil" → forte chance de despawn
→ Un mob à 40 blocs est "proche" → faible/nulle chance de despawn
```

---

##### `despawninterval` — Fréquence des vérifications de despawn
```
Type    : int (secondes)   |   Défaut : 5   |   Min : 1
```
Délai entre deux évaluations de despawn **pour cette zone**.

```
despawninterval=1  → vérification chaque seconde (très réactif, léger impact CPU)
despawninterval=5  → vérification toutes les 5 secondes (défaut, bon équilibre)
despawninterval=30 → mobs très persistants (zones de boss, events)
```

> ⚠️ Si `despawninterval=1` + `despawnfar=0.5`, un mob a 50% de chance de disparaître
> **chaque seconde** quand il est loin. Il sera supprimé en moyenne en 2 secondes.

---

##### Les 4 paliers de despawn — Ratios et chances

Le despawn fonctionne avec **4 zones de distance** définies par `despawnratio1/2/3` :

```
Schéma (despawn=100 blocs, ratios par défaut 0.50 / 0.75 / 1.00) :

Distance du joueur :
0                50           75          100          +∞
|── CLOSE ───────|── MID ──────|── FAR ────|── OUTER ──→
  (0–50 blocs)    (50–75 blocs) (75–100 b)  (>100 blocs)

Chance de despawn :
  despawnclose=0%  despawnmid=20%  despawnfar=40%  despawnouter=70%
```

**Chaque `despawninterval` secondes**, pour chaque mob, un dé est lancé :
- Si `random() < chance_du_palier` → mob supprimé
- Sinon → mob survit jusqu'au prochain check

---

##### `despawnratio1` — Limite entre palier CLOSE et MID
```
Type    : double   |   Défaut : 0.50   |   (× despawn)
```
```
despawn=100, despawnratio1=0.50
→ CLOSE = distance < 50 blocs du joueur

despawn=100, despawnratio1=0.30
→ CLOSE = distance < 30 blocs (mobs très proches protégés sur moins de distance)
```

---

##### `despawnratio2` — Limite entre palier MID et FAR
```
Type    : double   |   Défaut : 0.75   |   (× despawn)
```
```
despawn=100, despawnratio2=0.75
→ MID = entre 50 et 75 blocs

despawn=100, despawnratio2=0.90
→ MID = entre 50 et 90 blocs (plage "moyenne" plus large)
```

---

##### `despawnratio3` — Limite entre palier FAR et OUTER
```
Type    : double   |   Défaut : 1.00   |   (× despawn)
```
```
despawn=100, despawnratio3=1.00
→ FAR = entre 75 et 100 blocs, OUTER = au-delà de 100 blocs

despawn=100, despawnratio3=1.20
→ FAR = entre 75 et 120 blocs, OUTER = au-delà de 120 blocs
→ Utile pour étendre la plage FAR au-delà du seuil nominal
```

> 💡 `despawnratio3` peut être **supérieur à 1.0**. Cela permet d'avoir une plage FAR
> qui dépasse le seuil `despawn`, et de réserver OUTER aux mobs vraiment très loin.

---

##### `despawnclose` — Chance de despawn dans le palier CLOSE
```
Type    : double (0.0–1.0)   |   Défaut : 0.0
```
Probabilité de despawn lorsque le mob est **très proche** du joueur (< ratio1 × despawn).

```
despawnclose=0.0  → mobs proches du joueur jamais supprimés (recommandé)
despawnclose=0.1  → 10% de chance toutes les despawninterval secondes (très rare)
despawnclose=0.5  → mobs instables même proches (déconseillé)
```

> ✅ En pratique, garder `despawnclose=0.0` pour ne jamais faire disparaître un mob
> pendant un combat ou une exploration active.

---

##### `despawnmid` — Chance de despawn dans le palier MID
```
Type    : double (0.0–1.0)   |   Défaut : 0.2
```
Probabilité de despawn à **distance intermédiaire** (entre ratio1 et ratio2 × despawn).

```
Avec despawninterval=5 et despawnmid=0.2 :
→ 20% de chance toutes les 5 secondes
→ Durée de vie moyenne d'un mob dans cette plage : ~25 secondes

Avec despawninterval=5 et despawnmid=0.05 :
→ 5% de chance toutes les 5 secondes
→ Durée de vie moyenne : ~100 secondes (mobs persistants à distance moyenne)
```

---

##### `despawnfar` — Chance de despawn dans le palier FAR
```
Type    : double (0.0–1.0)   |   Défaut : 0.4
```
Probabilité de despawn quand le mob est **éloigné** (entre ratio2 et ratio3 × despawn).

```
Calcul durée de vie moyenne (en secondes) :
  durée = despawninterval / despawnfar

Exemples :
  despawninterval=5, despawnfar=0.4  → ~12 secondes
  despawninterval=5, despawnfar=0.1  → ~50 secondes
  despawninterval=5, despawnfar=0.01 → ~500 secondes (très persistant)
```

---

##### `despawnouter` — Chance de despawn dans le palier OUTER
```
Type    : double (0.0–1.0)   |   Défaut : 0.7
```
Probabilité de despawn quand le mob est **très loin** (au-delà de ratio3 × despawn).  
C'est le palier le plus agressif : les mobs vraiment distants doivent être nettoyés rapidement.

```
despawnouter=0.7 avec despawninterval=5
→ 70% de chance toutes les 5 secondes
→ Durée de vie moyenne au-delà du seuil : ~7 secondes
→ Les mobs disparaissent rapidement une fois que le joueur s'éloigne

despawnouter=1.0
→ Suppression garantie au premier check (comportement "snap" instantané)
```

> ⚠️ Si `despawnouter=0.0`, les mobs ne despawnent **jamais** par distance, même très loin.
> Combiné à une grande zone, cela peut créer des milliers de mobs non supprimés.

---

### Recettes de configuration selon le style de jeu

#### Zone de quête / exploration (mobs qui persistent)
```
spawninterval=10, spawnchance=0.6, targetmin=2, targetmax=4
despawn=200, despawninterval=10
despawnratio1=0.4, despawnratio2=0.7, despawnratio3=1.2
despawnclose=0.0, despawnmid=0.05, despawnfar=0.1, despawnouter=0.5
→ Mobs rares mais tenaces, disparaissent lentement quand le joueur s'éloigne
```

#### Zone de grind (spawn rapide, nettoie vite)
```
spawninterval=2, spawnchance=1.0, targetmin=5, targetmax=8, max=20
despawn=80, despawninterval=3
despawnratio1=0.5, despawnratio2=0.75, despawnratio3=1.0
despawnclose=0.0, despawnmid=0.3, despawnfar=0.6, despawnouter=1.0
→ Beaucoup de mobs, mais la zone se vide dès que le joueur part
```

#### Zone de boss (un seul mob, très résistant au despawn)
```
max=1, spawninterval=60, spawnchance=1.0, targetmin=1, targetmax=1
despawn=300, despawninterval=30
despawnclose=0.0, despawnmid=0.0, despawnfar=0.0, despawnouter=0.1
→ 1 seul boss, respawn toutes les 60s s'il est mort, quasi-invincible au despawn
```

#### Zone nocturne (uniquement la nuit)
```
condition=NIGHT
spawninterval=5, targetmin=4, targetmax=6
despawn=120, despawnouter=0.8
→ La nuit : spawn actif / Le jour : plus de nouveaux spawns, les survivants dégagent vite
```

### Exemple de zone configurée complète
```
/zone create foret
/zone pos1 foret
/zone pos2 foret

# Général
/zone set foret max 30
/zone set foret spawninterval 3
/zone set foret spawnchance 0.8
/zone set foret targetmin 3
/zone set foret targetmax 5
/zone set foret spawnmin 15
/zone set foret spawnmax 35
/zone set foret condition NIGHT

# Despawn
/zone set foret despawn 120
/zone set foret despawninterval 3
/zone set foret despawnratio1 0.4   ← CLOSE < 48 blocs
/zone set foret despawnratio2 0.7   ← MID entre 48 et 84 blocs
/zone set foret despawnratio3 1.0   ← FAR entre 84 et 120 blocs, OUTER > 120
/zone set foret despawnclose 0.0    ← 0% si < 48 blocs (en combat = safe)
/zone set foret despawnmid 0.1      ← 10% toutes les 3s (30s durée moyenne)
/zone set foret despawnfar 0.3      ← 30% toutes les 3s (~10s durée moyenne)
/zone set foret despawnouter 0.8    ← 80% toutes les 3s (~4s durée moyenne)

/zone addmob foret zombie_foret
/zone info foret
```

---

## 6. Fichiers de configuration

### `mobs.yml`
```yaml
mobs:
  zombie_foret:
    name: "&2Zombie &ade la Forêt"
    entity-type: ZOMBIE
    level: 5
    health: 60.0
    money: 10
    loot-table: basic_drops
```

### `loots.yml`
```yaml
tables:
  basic_drops:
    0:
      material: ROTTEN_FLESH
      min: 1
      max: 2
      chance: 1.0
      custom: false
    1:
      material: BONE
      min: 1
      max: 3
      chance: 0.8
      custom: false
    2:
      material: DIAMOND
      min: 1
      max: 1
      chance: 0.02
      custom: true
      # L'item custom est sérialisé (avec nom/enchantements)
```

### `zones.yml`
```yaml
zones:
  foret:
    world: world
    pos1: {x: 100.0, y: 64.0, z: -200.0}
    pos2: {x: 300.0, y: 64.0, z: -400.0}
    mobs: [zombie_foret]
    max: 30
    priority: 0
    inherit-mobs: false
    override-mobs: false
    # Spawn
    spawn-radius-min: 15.0
    spawn-radius-max: 35.0
    target-min: 3
    target-max: 5
    spawn-interval: 3
    spawn-chance: 0.8
    spawn-condition: NIGHT
    # Despawn
    despawn-distance: 120.0
    despawn-check-interval: 3
    despawn-chance-close: 0.0
    despawn-chance-mid: 0.1
    despawn-chance-far: 0.3
    despawn-chance-outer: 0.8
    despawn-ratio1: 0.4
    despawn-ratio2: 0.7
    despawn-ratio3: 1.0
```

---

## 7. Système de debug (`/zone debug`)

### Modes
```
/zone debug            → alterne NONE → NORMAL → VERBOSE → NONE
/zone debug off        → désactiver
/zone debug normal     → activer mode normal
/zone debug verbose    → activer mode verbeux
```

### Messages reçus

#### Mode NORMAL
```
§8[§eDBG§8] §a[SPAWN]§7 zone=§fforet§7 mob=§fzombie_foret§7 pos=(§f145,65,-240§7) nearby=§f2§7/§f4§7 total=§f3§7/§f30
§8[§eDBG§8] §c[DESPAWN]§7 zone=§fforet§7 mob=§fzombie_foret§7 dist=§f98.3§7/§f120.0§7 chance=§f10%
§8[§eDBG§8] §c[DESPAWN]§7 mob=§fzombie_foret§7 raison=§cZONE_SUPPRIMÉE
§8[§eDBG§8] §c[DESPAWN]§7 zone=§fforet§7 mob=§fzombie_foret§7 raison=§cTROP_LOIN dist=§f310.5
```

#### Mode VERBOSE (en plus du NORMAL)
```
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§eCAP_ATTEINT total=§f30§7/§f30
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§eCIBLE_OK nearby=§f5§7/§f5
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§eCONDITION§7 requis=§fNIGHT
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§eCHANCE proba=§f60%
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§ePAS_DE_POSITION
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§ePAS_DE_MOB
§8[§eDBG§8] §e[SKIP]§7 zone=§fforet§7 raison=§eMOB_INCONNU id=§fzombie_foret
§8[§eDBG§8] §6[CLEANUP]§7 type=§fZOMBIE§7 raison=§6NON_ENREGISTRÉ
§8[§eDBG§8] §6[BLOCKED]§7 type=§fZOMBIE§7 raison=§6NATURAL
```

### Lire le debug pour diagnostiquer

| Message | Cause probable | Solution |
|---|---|---|
| Jamais de `[SPAWN]` | Pas dans une zone | `/zone check` — es-tu dans la zone ? |
| `CIBLE_OK nearby=5/5` | Cap par joueur atteint | Augmenter `targetmax` |
| `CAP_ATTEINT 30/30` | Cap global atteint | Augmenter `max` |
| `CONDITION requis=NIGHT` | C'est le jour | Attendre la nuit ou mettre `condition ALWAYS` |
| `CHANCE proba=60%` | Spawn bloqué par la chance | Augmenter `spawnchance` |
| `PAS_DE_POSITION` | Zone trop petite vs rayon spawn | Agrandir la zone ou réduire `spawnmin/spawnmax` |
| `PAS_DE_MOB` | Aucun mob dans la zone | `/zone addmob <id> <mobId>` |
| `MOB_INCONNU id=xxx` | Le mob n'existe pas dans mobs.yml | `/mob create xxx ...` |
| `[CLEANUP] NON_ENREGISTRÉ` | Mob persistant sans instance (après restart) | Normal — la tâche de spawn recrée les mobs |
| Mobs qui disparaissent vite | despawninterval trop court + chances hautes | Réduire `despawnclose/mid/far` ou augmenter `despawninterval` |

---

## 8. Guide de test complet (pas à pas)

### Étape 1 — Créer un mob

```
/mob create zombie_test ZOMBIE 1 20 5 §cZombie Test
/mob info zombie_test
```

✅ Attendu : `[Mob] zombie_test créé : §cZombie Test`

### Étape 2 — Créer une table de loot

```
/loot create drops_test
/loot add drops_test BONE 1 3 1.0
/loot add drops_test ROTTEN_FLESH 1 2 0.5
/loot info drops_test
```

✅ Attendu : 2 entrées listées avec les bonnes valeurs

### Étape 3 — Lier le loot au mob

```
/mob set zombie_test loot drops_test
/mob info zombie_test
```

✅ Attendu : `LootTable : drops_test`

### Étape 4 — Tester le spawn manuel

```
/mob spawn zombie_test
```

✅ Attendu : Un zombie apparaît à ta position avec le nom `§cZombie Test [Niv.1]`  
✅ Le zombie ne brûle pas au soleil  
✅ Le zombie ne despawn pas (il est en zone "manual")  
⚠️ **Si** le zombie disparaît en 5 secondes → le bug zone "manual" est présent, vérifier le fix dans ZoneManager

### Étape 5 — Tester la mort et les loots

```
# Tuer le zombie (épée ou commande /kill si tu as les perms)
```

✅ Attendu : Des os tombent au sol (100%), de la chair avec 50% de chance  
✅ Attendu : +5 argent reçu (vérifier avec `/money`)  
⚠️ **Si** rien ne tombe → vérifier que la table de loot est bien liée au mob

### Étape 6 — Créer une zone

```
# Position toi au coin nord-ouest de la zone voulue
/zone create testzone
/zone pos1 testzone

# Va au coin sud-est
/zone pos2 testzone

/zone info testzone
```

✅ Attendu : Zone créée avec Pos1 ≠ Pos2

### Étape 7 — Configurer la zone

```
/zone set testzone max 10
/zone set testzone spawnmin 10
/zone set testzone spawnmax 25
/zone set testzone targetmin 2
/zone set testzone targetmax 3
/zone set testzone spawninterval 2
/zone set testzone spawnchance 1.0
/zone set testzone condition ALWAYS

/zone set testzone despawn 100
/zone set testzone despawninterval 5
/zone set testzone despawnratio1 0.5
/zone set testzone despawnratio2 0.75
/zone set testzone despawnratio3 1.0
/zone set testzone despawnclose 0.0
/zone set testzone despawnmid 0.2
/zone set testzone despawnfar 0.5
/zone set testzone despawnouter 0.9

/zone addmob testzone zombie_test
```

### Étape 8 — Vérifier la zone

```
# Entre dans la zone et tape :
/zone check
```

✅ Attendu :
```
[Zone] ✔ Zone active : testzone
[Zone] Mobs effectifs : [zombie_test]
[Zone] Cap : 10  Target/joueur : 2-3  Interval : 2s  Chance× : 100%
[Zone] Condition : ALWAYS
[Zone] Despawn : 100 blocs  Vérif: 5s
[Zone] Seuils : <50%=0% | 50-75%=20% | 75-100%=50% | >100%=90%
```

### Étape 9 — Activer le debug et attendre le spawn

```
/zone debug normal
# Rester dans la zone, attendre 2-3 secondes
```

✅ Attendu en moins de `spawninterval` secondes :
```
[DBG] [SPAWN] zone=testzone mob=zombie_test pos=(x,y,z) nearby=0/2 total=1/10
```

Si tu vois des `[SKIP]` en verbose, consulter le tableau de diagnostic (§7).

### Étape 10 — Tester le despawn

```
/zone debug normal
# Éloigne-toi de la zone jusqu'à dépasser 100 blocs
# Attendre 5 secondes (despawninterval)
```

✅ Attendu :
```
[DBG] [DESPAWN] zone=testzone mob=zombie_test dist=105.3/100.0 chance=90%
```

### Étape 11 — Vérifier la persistance (redémarrage)

```
# Redémarrer le serveur
# Les mobs sont supprimés au chargement des chunks (CLEANUP NON_ENREGISTRÉ)
# La tâche de spawn recréera de nouveaux mobs dès qu'un joueur entrera dans la zone
/zone debug normal
# Entrer dans la zone
```

✅ Attendu : nouveau `[SPAWN]` dans les `spawninterval` secondes

### Étape 12 — Tester les zones superposées

```
# Créer une zone de base (prio 0) et une zone spéciale (prio 1) à l'intérieur
/zone create base
/zone pos1 base & /zone pos2 base    ← grande zone
/zone set base priority 0
/zone addmob base zombie_test

/zone create boss_arena
/zone pos1 boss_arena & /zone pos2 boss_arena   ← petite zone à l'intérieur de base
/zone set boss_arena priority 1
/zone addmob boss_arena zombie_foret

# Entrer dans boss_arena
/zone check  → doit afficher boss_arena (priorité 1 > 0)
```

---

## 9. Comportements importants à connaître

### Spawn
- Les mobs **ne spawent pas dans le champ de vision** du joueur (dot product > 0.85)
- 10 tentatives de position sont faites, puis fallback sur une position aléatoire dans la zone
- Le spawn est **par joueur** : chaque joueur dans la zone peut déclencher un spawn indépendant
- Si plusieurs joueurs sont dans la même zone, la **même zone est traitée une seule fois** par tick (le timer de zone est commun)

### Despawn
- La vérification se fait **par zone** avec un timer configurable (`despawninterval`)
- Un mob **hors de sa zone** est redirigé vers le centre (pathfinder)
- Si un mob est à plus de `despawn × 1.5` du centre ET hors zone → supprimé immédiatement
- Les mobs avec zone `"manual"` (`/mob spawn`) **ne sont jamais despawnés** par la tâche de zone

### Après un redémarrage
- Les mobs `setPersistent(true)` sont sauvegardés en chunk
- Au chargement du chunk, `MobListener.onChunkLoad` les **supprime** car non enregistrés en `MobManager`
- → Normal : la tâche de spawn les recrée dans les prochains `spawninterval` secondes

### Blocage des mobs vanilla
- **Tous** les spawns non-CUSTOM sont bloqués (`CreatureSpawnEvent`)
- Cela inclut les spawners de donjon, les spawns naturels, les œufs de spawn
- Seuls les mobs créés via `world.spawnEntity()` (plugin) passent

### Condition `DAY` / `NIGHT`
- `DAY` = ticks `0–12299` (environ 10 min de jour vanilla)
- `NIGHT` = ticks `12300–23999`

---

## 10. Fonctionnalités à ajouter / améliorer

### 🔴 Prioritaires (bugs potentiels)

| N° | Problème | Impact |
|---|---|---|
| 1 | **Spawn par joueur dupliqué** : si 2 joueurs sont dans la même zone, le timer est partagé mais le spawn se fait pour les 2 — on peut dépasser `max` | Trop de mobs |
| 2 | **Pas de despawn sur déconnexion** : si le seul joueur se déconnecte, les mobs restent vivants indéfiniment (aucun joueur → `closestPlayer = MAX_VALUE` → chance despawn = outer) | Accumulation de mobs |
| 3 | **Chunk non chargé** : `getHighestBlockAt()` charge le chunk en sync, peut causer des lags si la zone est très étendue | Performance |

### 🟡 Améliorations souhaitées

| N° | Fonctionnalité | Description |
|---|---|---|
| 4 | **Réspawn après mort** : quand un mob meurt, la zone peut en respawner un immédiatement (timer reset) | Zones toujours peuplées |
| 5 | **Loot par joueur** : actuellement le loot tombe au sol — option pour l'ajouter directement dans l'inventaire du tueur | QoL |
| 6 | **Spawn en intérieur** : `getHighestBlockAt` donne le toit, pas l'intérieur d'un donjon — ajouter un mode "scan vertical" | Precision |
| 7 | **Commande `/zone reload`** : recharger zones.yml sans redémarrer | Admin |
| 8 | **Limite de mobs par type** : `max_per_mob_type` pour ne pas avoir que le même mob dans une zone multi-mobs | Équilibrage |
| 9 | **Échelons de spawn selon distance** : spawn-chance différent si joueur est au centre ou à la bordure de la zone | Immersion |
| 10 | **Mob aggro radius** : les mobs custom ignorent le joueur au-delà d'une distance configurable | Performance IA |
| 11 | **`/zone killall <id>`** : tuer tous les mobs d'une zone en jeu | Debug admin |
| 12 | **`/zone info <id>` count en temps réel** : afficher le nombre de mobs actuellement vivants | Monitoring |
| 13 | **Visualisation de zone** : `/zone visualize <id>` pour afficher les particules aux coins | Debug build |
| 14 | **Mobs sans zone héritage correct** : si `inherit=true` mais aucune zone parente → message d'avertissement dans debug | UX |

### 🟢 Futures fonctionnalités

| N° | Idée |
|---|---|
| 15 | **Boss mobs** : mob unique par zone avec HP, phases, annonce |
| 16 | **Drops d'expérience** : ajouter XP joueur (LevelManager) à la mort du mob |
| 17 | **Aggro de groupe** : quand un mob est attaqué, les mobs proches s'aggroent aussi |
| 18 | **Zones dynamiques** : activer/désactiver une zone selon une condition (heure du serveur, event, etc.) |
| 19 | **Import/export JSON** : pouvoir partager une configuration de zone/mob/loot entre serveurs |

---

## Référence rapide — Séquence de setup minimale

```bash
# 1. Créer le mob
/mob create zombie ZOMBIE 1 20 5 Zombie

# 2. Créer le loot
/loot create drops
/loot add drops BONE 1 3 1.0

# 3. Lier
/mob set zombie loot drops

# 4. Créer la zone (se déplacer aux deux coins)
/zone create test
/zone pos1 test
# [aller à l'autre coin]
/zone pos2 test

# 5. Configurer (minimal)
/zone set test spawninterval 3
/zone set test condition ALWAYS
/zone addmob test zombie

# 6. Vérifier
/zone check
/zone debug normal

# 7. Observer le spawn
# → [DBG] [SPAWN] zone=test ...
```

---

*Dernière mise à jour : 2026-04-01 — Version Armesia 1.0.0*





