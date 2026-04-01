# Armesia — Documentation complète

> Plugin Paper 1.20.1 — Système de mobs personnalisés, zones de spawn, loots, économie et niveaux.

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

---

## 1. Installation

### Prérequis
- **Paper 1.20.1**
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
├── mobs.yml         ← mobs personnalisés
├── loots.yml        ← tables de loot
└── zones.yml        ← zones de spawn
```

---

## 2. Architecture

```
Mob système
  MobData       — définition statique (type, HP, argent, loot)
  MobInstance   — instance vivante (UUID ↔ zoneId ↔ mobId ↔ lastLoc)
  MobManager    — registre des définitions + instances actives
  MobSpawner    — spawn physique (spawnEntity + PDC tags + persistent=true)
  MobListener   — bloque vanilla, gère mort/loot/argent

Zone système
  ZoneData      — configuration d'une zone (spawn + despawn + frontière + poids)
  ZoneManager   — 5 tâches : spawn / despawn / rebond / patrouille / preview
  ZoneListener  — debug zone/seuil au mouvement + récupération ChunkLoad
  ZoneCommand   — toutes les commandes /zone
  ZoneConfig    — lecture/écriture zones.yml

Loot système
  LootData      — une entrée (material|item custom, qté, chance)
  LootManager   — registre des tables, drop au sol à la mort
  LootConfig    — lecture/écriture loots.yml
```

### Cycle de vie d'un mob
```
ZoneManager (spawn task, 1s)
  → vérifie intervalles, cap, condition, cible, chance, poids
  → getSmartSpawnLocation() → 10 tentatives dans le rayon autour du joueur
  → MobSpawner.spawnMob()
      → setCustomName(nom) / setHealth / setPersistent(true) / setRemoveWhenFarAway(false)
      → PDC : armesia_mob_id + armesia_zone_id  ← survivent aux redémarrages
      → MobManager.addInstance()

ZoneManager (bounce task, 0.2s)
  → mob hors zone > boundarytolerance :
      stopPathfinding() + setVelocity(vers bord zone)

ZoneManager (despawn task, 1s)
  → entity null + chunk non chargé  → skip (mob sur disque, reviendra)
  → entity null + chunk chargé      → remove instance (mob vraiment mort)
  → hors zone > boundarytolerance+15 blocs → suppression (HORS_ZONE_LIMITE)
  → dans zone, si zone due + joueurs connectés → palier distance → chance → suppression

ZoneListener (ChunkLoadEvent)
  → scan entités du chunk : PDC tag trouvé → ré-enregistre dans MobManager
  → zone/mob introuvable → supprime l'orphelin (CLEANUP)

MobListener.onDeath()
  → vide les drops vanilla
  → LootManager.dropLoot() → drop items au sol
  → EconomyAPI.addMoney()  → donne l'argent au tueur
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
| `/mob set <id> health <double>` | Changer les HP |
| `/mob set <id> money <int>` | Changer l'argent à la mort |
| `/mob set <id> type <EntityType>` | Changer le type d'entité |
| `/mob set <id> loot <tableId>` | Associer une table de loot |
| `/mob spawn <id>` | Spawner le mob à ta position (test) |

> Le nom du mob en jeu affiche uniquement le `name` défini — sans suffixe de niveau.

### Exemple
```
/mob create zombie_foret ZOMBIE 1 60 10 &2Zombie &ade la Forêt
/mob set zombie_foret loot basic_drops
/mob info zombie_foret
```

---

## 4. Commandes — Loots (`/loot`)

**Permission :** `armesia.loot` (défaut : OP)

| Commande | Description |
|---|---|
| `/loot list` | Liste toutes les tables |
| `/loot info <tableId>` | Voir les entrées d'une table |
| `/loot create <tableId>` | Créer une table vide |
| `/loot delete <tableId>` | Supprimer une table |
| `/loot add <tableId> <MATERIAL> <min> <max> <chance>` | Ajouter un drop basique |
| `/loot addhand <tableId> <min> <max> <chance>` | Ajouter l'item en main (custom) |
| `/loot remove <tableId> <index>` | Supprimer une entrée (index 1-based) |

### Exemple
```
/loot create basic_drops
/loot add basic_drops BONE 1 3 0.8
/loot add basic_drops ROTTEN_FLESH 1 2 1.0
/loot add basic_drops DIAMOND 1 1 0.02
```

---

## 5. Commandes — Zones (`/zone`)

**Permission :** `armesia.zone` (défaut : OP)

### Navigation / Structure

| Commande | Description |
|---|---|
| `/zone list` | Liste toutes les zones |
| `/zone info <id>` | Détails complets (avec probabilités de spawn par mob) |
| `/zone create <id>` | Créer une zone |
| `/zone delete <id>` | Supprimer une zone + despawn tous ses mobs |
| `/zone pos1 <id>` | Définir le coin 1 à ta position |
| `/zone pos2 <id>` | Définir le coin 2 à ta position |
| `/zone tp <id>` | Se téléporter au centre de la zone |
| `/zone check` | Diagnostic : zone active, mobs effectifs avec probabilités |
| `/zone debug [off\|normal\|verbose]` | Activer le debug en jeu |
| `/zone preview <id>` | Activer/désactiver la prévisualisation particules |

### Gestion des mobs

| Commande | Description |
|---|---|
| `/zone addmob <id> <mobId>` | Ajouter un mob à la zone |
| `/zone removemob <id> <mobId>` | Retirer un mob de la zone |
| `/zone setweight <id> <mobId> <poids>` | Définir le poids de spawn d'un mob |

#### Poids de spawn (`setweight`)
```
Poids = nombre positif. Probabilité = poids_mob / somme_total_poids.

Exemple :
  /zone setweight foret zombie 3.0
  /zone setweight foret skeleton 1.0
  → zombie : 3/(3+1) = 75%   skeleton : 1/(3+1) = 25%

Défaut = 1.0 pour tout mob sans poids explicite → probabilité égale.
```

---

### Configuration : `/zone set <id> <propriété> <valeur>`

#### ⚙️ Général

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `max` | int | 20 | Cap global de mobs simultanés (0 = illimité) |
| `priority` | int | 0 | Priorité si zones superposées (plus haute = gagne) |
| `inherit` | bool | false | Hérite les mobs des zones parentes |
| `override` | bool | false | Bloque l'héritage depuis cette zone |

---

#### 🟢 Spawn

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `spawnmin` | double | 20 | Rayon min autour du joueur (blocs) |
| `spawnmax` | double | 40 | Rayon max autour du joueur (blocs) |
| `targetmin` | int | 4 | Cible min de mobs par joueur |
| `targetmax` | int | 6 | Cible max de mobs par joueur |
| `spawninterval` | int | 5 | Secondes entre deux spawns |
| `spawnchance` | double | 1.0 | Probabilité de spawn à chaque intervalle (0.0–1.0) |
| `condition` | enum | ALWAYS | `ALWAYS` / `DAY` / `NIGHT` |

##### Cycle de spawn (dans l'ordre)
```
1. Timer ≥ spawnInterval ?              → sinon : attente silencieuse
2. Cap global (max) atteint ?           → [SKIP CAP_ATTEINT]
3. Condition horaire remplie ?          → [SKIP CONDITION]
4. nearby ≥ target ?                   → [SKIP CIBLE_OK]
5. Tirage spawnchance réussi ?          → [SKIP CHANCE]
6. Position valide trouvée ?            → [SKIP PAS_DE_POSITION]
7. → Tirage pondéré du mob → SPAWN + reset timer
```

---

#### 🔴 Despawn

> ⚠️ **Distance XZ uniquement** — la hauteur (Y) n'est pas prise en compte.
> Un joueur qui vole ou monte sur une tour ne déclenche pas de faux despawns.

> ⚠️ **Suspendu si aucun joueur connecté** — les mobs survivent pendant la déconnexion.

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `despawn` | double | 150 | Distance XZ de référence (blocs) |
| `despawninterval` | int | 5 | Secondes entre deux vérifications |
| `despawnratio1` | double | 0.50 | Seuil CLOSE→MID (× despawn) |
| `despawnratio2` | double | 0.75 | Seuil MID→FAR (× despawn) |
| `despawnratio3` | double | 1.00 | Seuil FAR→OUTER (× despawn) |
| `despawnclose` | double | 0.0 | Chance despawn palier CLOSE |
| `despawnmid` | double | 0.2 | Chance despawn palier MID |
| `despawnfar` | double | 0.4 | Chance despawn palier FAR |
| `despawnouter` | double | 0.7 | Chance despawn palier OUTER |

##### Schéma des paliers (despawn=100, ratios défaut)
```
Distance XZ du joueur :
0           50          75         100          +∞
|── CLOSE ──|──── MID ──|── FAR ───|── OUTER ──→
  0% chance    20% chance   40% chance   70% chance
```

---

#### 🟡 Frontière de zone (rebond)

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `boundarytolerance` | double | 0.0 | Blocs autorisés hors zone avant rebond |
| `bouncestrength` | double | 0.5 | Force du rebond (blocs/tick) |

```
boundarytolerance=0   → rebond immédiat dès que le mob sort (strict)
boundarytolerance=10  → les mobs peuvent vagabonder 10 blocs hors zone librement
bouncestrength=0.3    → petit rebond doux
bouncestrength=1.0    → rebond fort

Hard-kill automatique : mob > boundarytolerance+15 blocs hors zone → supprimé.
```

---

### Prévisualisation : `/zone preview <id>`

Active/désactive un affichage particules en temps réel.

| Couleur | Signification |
|---|---|
| ■ Blanc (rectangle) | Bordure réelle de la zone |
| ■ Orange (rectangle) | Zone + tolérance |
| ■ Cyan / Bleu (cercle centré zone) | Rayons spawn min / max |
| ■ Vert / Jaune / Orange (cercle centré zone) | Seuils despawn ratio1 / 2 / 3 |
| ■ Grands cercles centrés joueur | Mêmes seuils relatifs à ta position (particules 2.5×) |

> Les grands cercles joueur-centrés montrent exactement à quelle distance les mobs
> autour de toi commencent à despawn selon chaque palier.

---

### Recettes de configuration

#### Zone de grind
```
spawninterval=2, spawnchance=1.0, targetmin=5, targetmax=8, max=20
despawn=80, despawninterval=3
despawnclose=0.0, despawnmid=0.3, despawnfar=0.6, despawnouter=1.0
boundarytolerance=0, bouncestrength=0.6
```

#### Zone de boss (1 mob persistant)
```
max=1, spawninterval=60, targetmin=1, targetmax=1
despawn=300, despawninterval=30
despawnclose=0.0, despawnmid=0.0, despawnfar=0.0, despawnouter=0.1
boundarytolerance=5, bouncestrength=0.8
```

#### Zone nocturne
```
condition=NIGHT, spawninterval=5, targetmin=4, targetmax=6
despawn=120, despawnouter=0.8
```

#### Zone multi-mobs pondérée
```
/zone addmob foret zombie
/zone addmob foret skeleton
/zone setweight foret zombie 4.0    ← 80%
/zone setweight foret skeleton 1.0  ← 20%
```

---

## 6. Fichiers de configuration

### `mobs.yml`
```yaml
mobs:
  zombie_foret:
    name: "&2Zombie &ade la Forêt"
    entity-type: ZOMBIE
    level: 1
    health: 60.0
    money: 10
    loot-table: basic_drops
```

### `zones.yml` (exemple complet)
```yaml
zones:
  foret:
    world: world
    pos1: {x: 100.0, y: 64.0, z: -200.0}
    pos2: {x: 300.0, y: 64.0, z: -400.0}
    mobs: [zombie_foret, skeleton_foret]
    mob-weights:
      zombie_foret: 3.0
      skeleton_foret: 1.0
    max: 30
    priority: 0
    inherit-mobs: false
    override-mobs: false
    spawn-radius-min: 15.0
    spawn-radius-max: 35.0
    target-min: 3
    target-max: 5
    spawn-interval: 3
    spawn-chance: 0.8
    spawn-condition: NIGHT
    despawn-distance: 120.0
    despawn-check-interval: 3
    despawn-chance-close: 0.0
    despawn-chance-mid: 0.1
    despawn-chance-far: 0.3
    despawn-chance-outer: 0.8
    despawn-ratio1: 0.4
    despawn-ratio2: 0.7
    despawn-ratio3: 1.0
    boundary-tolerance: 5.0
    bounce-strength: 0.6
```

---

## 7. Système de debug (`/zone debug`)

### Modes
```
/zone debug            → alterne NONE → NORMAL → VERBOSE → NONE
/zone debug normal     → SPAWN + DESPAWN + ZONE + SEUIL + RESTORE
/zone debug verbose    → tout le NORMAL + SKIP + CLEANUP + BLOCKED
```

### Messages

#### Spawn / Despawn / Récupération
```
[DBG] §a[SPAWN]   zone=foret mob=zombie_foret pos=(145,65,-240) nearby=2/4 total=3/30
[DBG] §c[DESPAWN] zone=foret mob=zombie_foret dist=98.3/120.0 chance=10%
[DBG] §c[DESPAWN] zone=foret mob=zombie_foret raison=HORS_ZONE_LIMITE distBord=18.2
[DBG] §b[RESTORE] mob ré-enregistré zombie_foret zone=foret   ← après restart/chunk reload
[DBG] §c[CLEANUP] mob orphelin supprimé (zone=foret mob=zombie_foret)
```

#### Zone & seuils (se déplacer avec debug actif)
```
[DBG] §a[ZONE+]  Entrée 'foret'  prio=0  max=30  spawn=[15-35blocs]  despawn=120blocs
[DBG] §7[ZONE-]  Sortie de 'foret'
[DBG] §e[ZONE→]  'foret' → 'chateau'  prio=5
[DBG] §e[SEUIL]  zone='foret' palier=§amid  (< 84 blocs  chance=10%)
[DBG] §e[SEUIL]  zone='foret' palier=§6far  (< 120 blocs  chance=30%)
```

### Tableau de diagnostic

| Message | Cause | Solution |
|---|---|---|
| Jamais de `[SPAWN]` | Pas dans une zone | `/zone check` |
| `CIBLE_OK` | Cap par joueur atteint | Augmenter `targetmax` |
| `CAP_ATTEINT` | Cap global atteint | Augmenter `max` |
| `CONDITION` | Mauvaise heure | `condition ALWAYS` |
| `CHANCE` | spawnchance trop bas | Augmenter `spawnchance` |
| `PAS_DE_POSITION` | Rayon trop grand vs zone | Réduire `spawnmax` |
| Mobs disparaissent vite | Chances despawn hautes | Réduire `despawnfar/outer` |
| Mobs sortent de zone | `bouncestrength` trop faible | Augmenter à 0.8–1.0 |
| `HORS_ZONE_LIMITE` | Mob > tolérance+15 blocs hors zone | Hard-kill normal ✓ |
| `[RESTORE]` au démarrage | Mobs récupérés via PDC | Normal ✓ |

---

## 8. Guide de test complet (pas à pas)

### Étape 1 — Créer un mob
```
/mob create zombie_test ZOMBIE 1 20 5 §cZombie Test
```
✅ Le nom en jeu sera `§cZombie Test` (sans niveau)

### Étape 2 — Loot
```
/loot create drops_test
/loot add drops_test BONE 1 3 1.0
/mob set zombie_test loot drops_test
```

### Étape 3 — Zone minimale
```
/zone create testzone
/zone pos1 testzone      ← coin 1
/zone pos2 testzone      ← coin 2
/zone set testzone spawninterval 2
/zone set testzone condition ALWAYS
/zone set testzone boundarytolerance 5
/zone set testzone bouncestrength 0.6
/zone addmob testzone zombie_test
```

### Étape 4 — Vérifier et prévisualiser
```
/zone check
/zone preview testzone   ← affiche toutes les frontières en particules
/zone debug normal
```
✅ En entrant dans la zone : `[ZONE+]`
✅ En moins de 2s : `[SPAWN]`

### Étape 5 — Poids multi-mobs
```
/mob create skeleton_test SKELETON 1 15 5 Squelette
/zone addmob testzone skeleton_test
/zone setweight testzone zombie_test 3.0
/zone setweight testzone skeleton_test 1.0
/zone info testzone
```
✅ Attendu : `zombie_test §8(×3.0 §e75%§8), skeleton_test §8(×1.0 §e25%§8)`

### Étape 6 — Test de persistance
```
# Se déconnecter → se reconnecter (serveur en marche)
```
✅ Les mobs sont toujours là (pas de despawn hors-ligne)

```
# Redémarrer le serveur
```
✅ Message `[RESTORE]` en debug — mobs récupérés via PDC

---

## 9. Comportements importants à connaître

### Spawn
- Mobs ne spawent pas dans le **champ de vision** du joueur
- Sélection par **tirage pondéré** (poids configurables par mob)
- Timer de spawn **partagé par zone** (pas par joueur)

### Despawn
- Distance **XZ uniquement** — la hauteur est ignorée
- **Suspendu** quand aucun joueur n'est connecté
- Mobs zone `"manual"` (`/mob spawn`) jamais despawnés par la tâche

### Frontière et rebond
- Rebond toutes les **0.2s** (4 ticks)
- Pathfinder stoppé avant la vélocité pour éviter la compensation de l'IA
- Force **progressive** : plus le mob est loin, plus il est poussé fort
- **Hard-kill** : `boundarytolerance + 15` blocs hors zone → suppression immédiate

### Persistance et récupération (PDC)
- `persistent=true` : mobs sauvegardés dans le fichier monde
- Tags PDC (`armesia_mob_id` + `armesia_zone_id`) dans le NBT — survivent aux redémarrages
- `ChunkLoadEvent` : détecte et ré-enregistre les mobs au rechargement de chunk
- Si zone/mob n'existe plus → mob orphelin supprimé (`[CLEANUP]`)
- Scan de récupération au démarrage (2s après le lancement)

### Blocage des spawns vanilla
- Tous les spawns non-CUSTOM bloqués : spawners, spawns naturels, œufs
- Seuls les mobs créés par le plugin passent

---

## Référence rapide — Setup minimal

```bash
# 1. Mob
/mob create zombie ZOMBIE 1 20 5 Zombie

# 2. Loot
/loot create drops
/loot add drops BONE 1 3 1.0
/mob set zombie loot drops

# 3. Zone (aux deux coins)
/zone create test
/zone pos1 test
/zone pos2 test

# 4. Config
/zone set test spawninterval 3
/zone addmob test zombie

# 5. Vérifier
/zone check
/zone debug normal
/zone preview test
```

---

*Dernière mise à jour : 2026-04-01 — Version Armesia 1.0.0*
