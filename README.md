# Armesia — Documentation complète

> Suite de plugins **Paper 1.20.1** — Mobs personnalisés, zones de spawn, système de niveaux, économie, casino, gestion des mondes, scoreboard dynamique et addon CrackShot.
>
> **Auteur :** SeGame &nbsp;|&nbsp; **Version :** 1.0.0 &nbsp;|&nbsp; **Java :** 17+

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Installation](#2-installation)
3. [Architecture des modules](#3-architecture-des-modules)
4. [Module Core — Armesia](#4-module-core--armesia)
5. [Module Armesia-Level](#5-module-armesia-level)
6. [Module Armesia-Mobs](#6-module-armesia-mobs)
7. [Module Armesia-Casino](#7-module-armesia-casino)
8. [Module Armesia-World](#8-module-armesia-world)
9. [Module Armesia-Scoreboard](#9-module-armesia-scoreboard)
10. [Module Armesia-CrackShotAddon](#10-module-armesia-crackshotaddon)
11. [Permissions — Référence complète](#11-permissions--référence-complète)
12. [Fichiers de configuration](#12-fichiers-de-configuration)
13. [Guide de test complet (pas à pas)](#13-guide-de-test-complet-pas-à-pas)
14. [Comportements importants à connaître](#14-comportements-importants-à-connaître)

---

## 1. Vue d'ensemble

Armesia est une **suite modulaire** de plugins Minecraft développée pour Paper 1.20.1.
Chaque module est un plugin indépendant qui s'appuie sur le Core (`Armesia`) ou sur d'autres modules via des dépendances Bukkit déclarées.

| Module | JAR produit | Rôle principal |
|---|---|---|
| **Armesia** | `Armesia-1.0.0.jar` | Core : économie, stats, groupes, utilitaires joueur |
| **Armesia-Level** | `Armesia-Level-*.jar` | Système XP / niveaux avec paliers configurables |
| **Armesia-Mobs** | `Armesia-Mobs-*.jar` | Mobs custom, zones de spawn, tables de loot |
| **Armesia-Casino** | `Armesia-Casino-*.jar` | Machine à sous avec animation roulette et lots |
| **Armesia-World** | `Armesia-World-*.jar` | Gestion des mondes, spawn, portails, parachutes |
| **Armesia-Scoreboard** | `Armesia-Scoreboard-*.jar` | Sidebar latérale en temps réel |
| **Armesia-CrackShotAddon** | `Armesia-CrackShotAddon-*.jar` | Extension CrackShot (rechargement, trails, zones) |

### Dépendances entre modules

```
Armesia-Level          ← (standalone, API exposée)
Armesia (Core)         ← softdepend: Armesia-Level
Armesia-Mobs           ← depend: Armesia (Core)
Armesia-Casino         ← standalone (intègre l'économie Armesia via API)
Armesia-World          ← depend: Armesia (Core)
Armesia-Scoreboard     ← depend: Armesia (Core), softdepend: Armesia-Level
Armesia-CrackShotAddon ← depend: CrackShot
```

---

## 2. Installation

### Prérequis

- **Paper 1.20.1** (ou fork compatible)
- **Java 17+**
- **CrackShot** *(uniquement pour Armesia-CrackShotAddon)*

### Build

```bash
# Cloner le dépôt
git clone https://github.com/SeGamee/Armesia.git
cd Armesia

# Compiler tous les modules
./gradlew build

# Les JARs se trouvent dans :
# Armesia/build/libs/
# Armesia-Level/build/libs/
# Armesia-Mobs/build/libs/
# Armesia-Casino/build/libs/
# Armesia-World/build/libs/
# Armesia-Scoreboard/build/libs/
# Armesia-CrackShotAddon/build/libs/
```

### Déploiement

```
1. Copier TOUS les JARs voulus dans plugins/
2. (Re)démarrer le serveur
3. Les fichiers de config sont générés automatiquement au premier démarrage
```

> **Ordre de chargement recommandé :** Armesia-Level → Armesia → tous les autres

### Structure des fichiers générés

```
plugins/
├── Armesia/
│   ├── config.yml        ← groupes, jobs, vitesse, vanish, homes, tpa, chat, kill, killstreak
│   ├── players.yml       ← données joueurs (argent, tokens, stats, homes)
│   └── kits.yml          ← définitions des kits
├── Armesia-Level/
│   └── config.yml        ← niveau max, formule XP, paliers de récompenses
├── Armesia-Mobs/
│   ├── config.yml        ← config générale mobs
│   ├── mobs.yml          ← définitions des mobs personnalisés
│   ├── loots.yml         ← tables de loot
│   ├── zones.yml         ← zones de spawn
│   ├── stats.yml         ← statistiques de kills joueurs
│   └── messages.yml      ← messages personnalisables
├── Armesia-Casino/
│   └── config.yml        ← jeton, lots, animation roulette, messages
└── Armesia-World/
    └── config.yml        ← spawn global, mondes, zones de map, portails
```

---

## 3. Architecture des modules

### Diagramme de dépendances

```
┌──────────────┐
│ Armesia-Level│ ← API statique LevelAPI (niveau, XP, barre)
└──────┬───────┘
       │ softdepend
┌──────▼───────┐
│ Armesia Core │ ← EconomyAPI, StatsAPI, GroupManager, PlayerDataManager
└──┬───────────┘
   │ depend
   ├──► Armesia-Mobs      (mobs, zones, loots, stats kills)
   ├──► Armesia-World     (mondes, spawn, portails, parachutes)
   └──► Armesia-Scoreboard (sidebar intégrant Level + Économie + Stats)
```

---

## 4. Module Core — Armesia

Le plugin principal fournit les fondations partagées par tous les autres modules : économie, statistiques de combat, groupes, et une large palette de commandes utilitaires.

### 4.1 Économie

Système d'argent en double (`money`) et de jetons entiers (`tokens`) persistés en YAML.

**API Java :**

```java
EconomyAPI api = Main.getInstance().getEconomyAPI();
api.getMoney(uuid);           // double — solde
api.addMoney(uuid, amount);   // créditer
api.removeMoney(uuid, amount);// débiter (retourne false si insuffisant)
api.setMoney(uuid, amount);
api.getTokens(uuid);          // int
api.addTokens(uuid, amount);
api.formatMoney(amount);      // "1 234.50$"
api.formatTokens(amount);     // "1 234"
```

**Commandes :**

| Commande | Permission | Description |
|---|---|---|
| `/money` | `armesia.money` | Voir son solde |
| `/money give <j> <n>` | `armesia.money.admin` | Donner de l'argent |
| `/money take <j> <n>` | `armesia.money.admin` | Retirer de l'argent |
| `/money set <j> <n>` | `armesia.money.admin` | Définir l'argent |
| `/tokens` | `armesia.tokens` | Voir ses jetons |
| `/pay <joueur> <montant>` | `armesia.pay` | Transférer de l'argent |
| `/baltop [page]` | `armesia.baltop` | Classement des plus riches |

### 4.2 Statistiques de combat

Statistiques persistées par joueur : kills PvP, morts, killstreak actif, ratio K/D.

**API Java :**

```java
StatsAPI stats = Main.getInstance().getStatsAPI();
stats.getKills(uuid);      // int
stats.getDeaths(uuid);     // int
stats.getKillstreak(uuid); // int (streak actuel)
stats.getRatio(uuid);      // String "1.50"
```

**Récompenses de killstreak** (configurables dans `config.yml`) :

```yaml
killstreak:
  5:
    message: "§a{player} §7a un killstreak de {killstreak} !"
    commands:
      - "give {player} diamond 5"
      - "money add {player} 500"
```

**Commandes :**

| Commande | Permission | Description |
|---|---|---|
| `/stats [joueur]` | `armesia.stats` | Voir ses stats (K/D/KS) |
| `/statsadmin reset <j>` | `armesia.statsadmin` | Réinitialiser les stats |
| `/statsadmin add kills <j> <n>` | `armesia.statsadmin` | Ajouter des kills |

### 4.3 Système de groupes

Les groupes permettent de définir des préfixes, des couleurs et des permissions contextuelles affichées dans le chat et le tab.

**Commandes :**

| Commande | Permission | Description |
|---|---|---|
| `/group <joueur> <groupe>` | `armesia.group` | Affecter un groupe |
| `/group <joueur> reset` | `armesia.group` | Réinitialiser le groupe |

### 4.4 Téléportation

| Commande | Permission | Description |
|---|---|---|
| `/tp <joueur>` | `armesia.tp` | Téléporter à un joueur |
| `/tp <x> <y> <z>` | `armesia.tp` | Téléporter à des coordonnées |
| `/tphere <joueur>` | `armesia.tphere` | Rappeler un joueur |
| `/tpall` | `armesia.tpall` | Rappeler tous les joueurs |
| `/tpa <joueur>` | `armesia.tpa` | Demander à rejoindre un joueur |
| `/tpahere <joueur>` | `armesia.tpahere` | Demander à un joueur de venir |
| `/tpyes` / `/tpdeny` | — | Accepter / refuser une demande |
| `/tpacancel` | `armesia.tpacancel` | Annuler sa demande en cours |

Paramètres TPA (config.yml) : `timeout`, `teleport-delay`, `send-cooldown`, `move-threshold`.

### 4.5 Utilitaires joueur

| Commande | Permission | Description |
|---|---|---|
| `/heal [joueur]` | `armesia.heal` / `.others` | Se soigner / soigner |
| `/feed [joueur]` | `armesia.feed` / `.others` | Se nourrir / nourrir |
| `/god [joueur]` | `armesia.god` / `.others` | Invincibilité |
| `/speed [walk\|fly] <1-10> [joueur]` | `armesia.speed` / `.others` | Vitesse |
| `/vanish [joueur]` | `armesia.vanish` / `.others` | Invisible |
| `/repair` / `/repairall` | `armesia.repair` / `.repairall` | Réparer item / tout |
| `/kit [nom]` | `armesia.kit` | Réclamer un kit |
| `/kit create/delete/give/resetcd` | `armesia.kit.admin` | Gérer les kits |
| `/home [nom]` | `armesia.home` | Se téléporter à un home |
| `/sethome [nom]` | `armesia.sethome` | Définir un home |
| `/delhome <nom>` | `armesia.sethome` | Supprimer un home |
| `/homes` | `armesia.home` | Lister ses homes |
| `/ec [joueur]` | `armesia.enderchest` | Ouvrir l'Ender Chest |
| `/invsee <joueur>` | `armesia.invsee` | Voir l'inventaire |
| `/ping [joueur]` | `armesia.ping` / `.others` | Voir le ping |
| `/near` | `armesia.near` | Joueurs proches (100 blocs) |
| `/msg <joueur> <msg>` | `armesia.msg` | Message privé |
| `/r <msg>` | `armesia.msg` | Répondre au dernier MP |
| `/suicide` | `armesia.suicide` | Se suicider |
| `/ci [joueur]` | `armesia.clearinventory` | Vider l'inventaire |
| `/ce [joueur]` | `armesia.cleareffect` | Supprimer les effets de potion |
| `/item <sous-cmd>` | `armesia.item` | Modifier l'item en main |
| `/broadcast <msg>` | `armesia.broadcast` | Message global |
| `/reloadconfig` | `armesia.reloadconfig` | Recharger la config |
| `/help [page]` | `armesia.help` | Aide |

### 4.6 Anti-farm PvP

Protection intégrée contre le farm de kills :

```yaml
antifarm:
  enabled: true
  cooldown: 60        # secondes avant de pouvoir re-tuer le même joueur
  message: "§cKill non compté (anti-farm) — encore §e{remaining}s !"
```

### 4.7 Chat

Cooldown configurable entre deux messages :

```yaml
chat:
  cooldown: 3
  cooldown-bypass: "armesia.chat.nocooldown"
```

---

## 5. Module Armesia-Level

Système de progression par XP et niveaux, avec paliers de récompenses entièrement configurables.

### 5.1 Formule de progression

```
XP requis pour passer du niveau N au niveau N+1 = N × xp-formula-multiplier
```

Exemple avec `xp-formula-multiplier: 1000` :
- Niveau 1 → 2 : 1 000 XP
- Niveau 10 → 11 : 10 000 XP
- Niveau 99 → 100 : 99 000 XP

### 5.2 Commandes

| Commande | Permission | Description |
|---|---|---|
| `/level` | — | Voir son niveau et sa barre XP |
| `/level <joueur>` | — | Voir le niveau d'un autre joueur |
| `/level add xp <j> <n>` | admin | Ajouter de l'XP |
| `/level remove xp <j> <n>` | admin | Retirer de l'XP |
| `/level add level <j> <n>` | admin | Ajouter des niveaux |
| `/level remove level <j> <n>` | admin | Retirer des niveaux |
| `/reloadlevel` | admin | Recharger la config des niveaux |

### 5.3 Paliers de récompenses (milestones)

Actions déclenchées automatiquement quand un joueur atteint un niveau précis.

**Types d'actions disponibles :**

| Type | Description | Placeholders |
|---|---|---|
| `message` | Message privé au joueur | `%player%`, `%level%` |
| `broadcast` | Message global | `%player%`, `%level%` |
| `title` | Titre + sous-titre | `%player%`, `%level%` |
| `sound` | Son joué au joueur | — |
| `command` | Commande console | `%player%`, `%level%` |

**Exemple (config.yml) :**

```yaml
milestones:
  10:
    - type: broadcast
      value: "&6[Niveau] &e%player% &7a atteint le &6niveau 10&7 !"
    - type: command
      value: "give %player% diamond_sword 1"
  100:
    - type: broadcast
      value: "&c&l[LÉGENDE] &e%player% &7a atteint le NIVEAU MAX 100 !"
    - type: title
      value: "&c&l★ NIVEAU MAXIMUM ★"
      subtitle: "&7Tu as atteint la légende absolue !"
```

### 5.4 API Java

```java
LevelAPI.isAvailable();              // boolean
LevelAPI.getLevel(uuid);             // int
LevelAPI.getXP(uuid);                // int
LevelAPI.addXP(uuid, amount);
LevelAPI.removeXP(uuid, amount);
LevelAPI.addLevel(uuid, amount);
LevelAPI.removeLevel(uuid, amount);
LevelAPI.getXPBar(uuid, sizeBar);    // String "§b§m━━━━§7§m━━━━━"
```

---

## 6. Module Armesia-Mobs

Système complet de mobs personnalisés avec zones de spawn dynamiques et tables de loot.

### 6.1 Architecture interne

```
Mob système
  MobData       — définition statique (type, HP, argent, table de loot)
  MobInstance   — instance vivante (UUID entité ↔ zoneId ↔ mobId ↔ lastLoc)
  MobManager    — registre des définitions + instances actives
  MobSpawner    — spawn physique (spawnEntity + PDC tags + persistent=true)
  MobListener   — bloque spawns vanilla, gère mort/loot/argent/stats

Zone système
  ZoneData      — configuration complète d'une zone (spawn/despawn/frontière/poids)
  ZoneManager   — 5 tâches BukkitRunnable : spawn / despawn / rebond / patrouille / preview
  ZoneListener  — debug zone/seuil au mouvement + récupération ChunkLoad
  ZoneCommand   — toutes les commandes /zone
  ZoneConfig    — lecture/écriture zones.yml

Loot système
  LootData      — une entrée (material|item custom, qté min/max, chance)
  LootTable     — liste de LootData + méthode roll()
  LootManager   — registre des tables + drop au sol à la mort
  LootConfig    — lecture/écriture loots.yml

Stats système
  StatsManager  — kills par joueur et par mob (UUID → mobId → count)
                  persiste dans stats.yml
```

### 6.2 Cycle de vie d'un mob

```
ZoneManager (spawn task, 1s)
  → vérifie : timer / cap global / condition horaire / cible / chance / poids
  → getSmartSpawnLocation() → 10 tentatives dans le rayon autour du joueur
  → MobSpawner.spawnMob()
      → setCustomName / setHealth / setPersistent(true) / setRemoveWhenFarAway(false)
      → PDC : armesia_mob_id + armesia_zone_id  ← survivent aux redémarrages
      → MobManager.addInstance()

ZoneManager (bounce task, 0.2s)
  → mob hors zone > boundarytolerance :
      stopPathfinding() + setVelocity(vers bord zone, force progressive)

ZoneManager (despawn task, 1s)
  → entity null + chunk non chargé  → skip (mob sur disque, reviendra)
  → entity null + chunk chargé      → remove instance (mob vraiment mort)
  → hors zone > boundarytolerance+15 blocs → suppression HORS_ZONE_LIMITE
  → dans zone + conditions atteintes → palier distance → chance → suppression

ZoneListener (ChunkLoadEvent)
  → scan entités du chunk : PDC tag trouvé → ré-enregistre dans MobManager
  → zone/mob introuvable → supprime l'orphelin (CLEANUP)

MobListener.onDeath()
  → vide les drops vanilla
  → LootManager.dropLoot()  → drops items au sol
  → EconomyAPI.addMoney()   → récompense le tueur
  → StatsManager.addKill()  → incrémente les statistiques
  → retire de MobManager
```

### 6.3 Commandes — Mobs (`/mob`)

**Permission :** `armesia.mob` (défaut : OP)

| Commande | Description |
|---|---|
| `/mob list` | Lister tous les mobs enregistrés |
| `/mob info <id>` | Détails complets d'un mob |
| `/mob create <id> <type> <lvl> <hp> <argent> <nom...>` | Créer un mob |
| `/mob delete <id>` | Supprimer un mob |
| `/mob set <id> name <nom...>` | Renommer (codes `&c`, `&l`, etc.) |
| `/mob set <id> health <double>` | Modifier les HP |
| `/mob set <id> money <int>` | Modifier l'argent à la mort |
| `/mob set <id> type <EntityType>` | Changer le type d'entité |
| `/mob set <id> loot <tableId>` | Associer une table de loot |
| `/mob spawn <id>` | Spawner le mob à sa position (test) |

**Exemple :**
```
/mob create zombie_foret ZOMBIE 1 60 10 &2Zombie &ade la Foret
/mob set zombie_foret loot basic_drops
/mob info zombie_foret
```

### 6.4 Commandes — Loots (`/loot`)

**Permission :** `armesia.loot` (défaut : OP)

| Commande | Description |
|---|---|
| `/loot list` | Lister toutes les tables |
| `/loot info <tableId>` | Voir les entrées d'une table |
| `/loot create <tableId>` | Créer une table vide |
| `/loot delete <tableId>` | Supprimer une table |
| `/loot add <tableId> <MATERIAL> <min> <max> <chance>` | Ajouter un drop basique |
| `/loot addhand <tableId> <min> <max> <chance>` | Ajouter l'item en main (custom) |
| `/loot remove <tableId> <index>` | Supprimer une entrée (index 1-based) |

**Exemple :**
```
/loot create basic_drops
/loot add basic_drops BONE 1 3 0.8
/loot add basic_drops ROTTEN_FLESH 1 2 1.0
/loot add basic_drops DIAMOND 1 1 0.02
```

### 6.5 Commandes — Zones (`/zone`)

**Permission :** `armesia.zone` (défaut : OP)

#### Navigation

| Commande | Description |
|---|---|
| `/zone list` | Lister toutes les zones |
| `/zone info <id>` | Détails + probabilités de spawn |
| `/zone create <id>` | Créer une zone |
| `/zone delete <id>` | Supprimer zone + despawn ses mobs |
| `/zone pos1 <id>` / `/zone pos2 <id>` | Définir les coins de la zone |
| `/zone tp <id>` | Se téléporter au centre |
| `/zone check` | Diagnostic : zone active + mobs effectifs |
| `/zone debug [off\|normal\|verbose]` | Mode debug en jeu |
| `/zone preview <id>` | Prévisualisation particules |

#### Gestion des mobs

| Commande | Description |
|---|---|
| `/zone addmob <id> <mobId>` | Ajouter un mob à la zone |
| `/zone removemob <id> <mobId>` | Retirer un mob de la zone |
| `/zone setweight <id> <mobId> <poids>` | Définir le poids de spawn |

**Poids de spawn :**
```
Probabilité mob = poids_mob / somme_total_poids

Exemple :
  /zone setweight foret zombie 3.0
  /zone setweight foret skeleton 1.0
  → zombie : 75%   skeleton : 25%
```

### 6.6 Configuration des zones — `/zone set <id> <propriété> <valeur>`

#### ⚙️ Général

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `max` | int | 20 | Cap global de mobs simultanés (0 = illimité) |
| `priority` | int | 0 | Priorité si zones superposées |
| `inherit` | bool | false | Hérite les mobs des zones parentes |
| `override` | bool | false | Bloque l'héritage depuis cette zone |

#### 🟢 Spawn

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `spawnmin` | double | 20 | Rayon min autour du joueur (blocs) |
| `spawnmax` | double | 40 | Rayon max autour du joueur (blocs) |
| `targetmin` | int | 4 | Cible min de mobs par joueur |
| `targetmax` | int | 6 | Cible max de mobs par joueur |
| `spawninterval` | int | 5 | Secondes entre deux spawns |
| `spawnchance` | double | 1.0 | Probabilité de spawn à chaque intervalle |
| `condition` | enum | ALWAYS | `ALWAYS` / `DAY` / `NIGHT` |

**Cycle de spawn (dans l'ordre) :**
```
1. Timer >= spawnInterval ?         → sinon : attente silencieuse
2. Cap global (max) atteint ?      → [SKIP CAP_ATTEINT]
3. Condition horaire remplie ?     → [SKIP CONDITION]
4. nearby >= target ?               → [SKIP CIBLE_OK]
5. Tirage spawnchance réussi ?     → [SKIP CHANCE]
6. Position valide trouvée ?       → [SKIP PAS_DE_POSITION]
7. → Tirage pondéré du mob → SPAWN + reset timer
```

#### 🔴 Despawn

> ⚠️ **Distance XZ uniquement** — la hauteur (Y) n'est pas prise en compte.
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

**Schéma des paliers (despawn=100, ratios défaut) :**
```
Distance XZ du joueur :
0           50          75         100          +∞
|── CLOSE ──|──── MID ──|── FAR ───|── OUTER ──→
  0% chance    20% chance   40% chance   70% chance
```

#### 🟡 Frontière de zone (rebond)

| Propriété | Type | Défaut | Description |
|---|---|---|---|
| `boundarytolerance` | double | 0.0 | Blocs autorisés hors zone avant rebond |
| `bouncestrength` | double | 0.5 | Force du rebond (blocs/tick) |

```
Hard-kill automatique : mob > boundarytolerance+15 blocs hors zone → supprimé.
```

### 6.7 Prévisualisation particules (`/zone preview`)

| Couleur | Signification |
|---|---|
| ■ Blanc | Bordure réelle de la zone |
| ■ Orange | Zone + tolérance |
| ■ Cyan / Bleu | Rayons spawn min / max |
| ■ Vert / Jaune / Orange | Seuils despawn ratio1 / 2 / 3 |
| ■ Grands cercles joueur | Mêmes seuils relatifs à la position du joueur |

### 6.8 Système de debug (`/zone debug`)

```
/zone debug            → cycle NONE → NORMAL → VERBOSE → NONE
/zone debug normal     → SPAWN + DESPAWN + ZONE + SEUIL + RESTORE
/zone debug verbose    → tout le NORMAL + SKIP + CLEANUP + BLOCKED
```

**Exemples de messages debug :**
```
[DBG] §a[SPAWN]   zone=foret mob=zombie_foret pos=(145,65,-240) nearby=2/4 total=3/30
[DBG] §c[DESPAWN] zone=foret mob=zombie_foret dist=98.3/120.0 chance=10%
[DBG] §b[RESTORE] mob ré-enregistré zombie_foret zone=foret
[DBG] §c[CLEANUP] mob orphelin supprimé (zone=foret mob=zombie_foret)
[DBG] §a[ZONE+]   Entrée 'foret'  prio=0  max=30  spawn=[15-35blocs]  despawn=120blocs
[DBG] §e[SEUIL]   zone='foret' palier=§amid  (< 84 blocs  chance=10%)
```

**Tableau de diagnostic :**

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

### 6.9 Commandes — Statistiques de kills (`/mobstats`)

| Commande | Permission | Description |
|---|---|---|
| `/mobstats` | `armesia.mobstats` | Voir ses statistiques de kills |
| `/mobstats <joueur>` | `armesia.mobstats.others` | Voir les stats d'un autre joueur |
| `/mobstats top [n]` | `armesia.mobstats.admin` | Top N des meilleurs chasseurs |
| `/mobstats reset <joueur>` | `armesia.mobstats.admin` | Réinitialiser les stats |
| `/mobstats add <joueur> <mob> <n>` | `armesia.mobstats.admin` | Ajouter des kills |
| `/mobstats remove <joueur> <mob> <n>` | `armesia.mobstats.admin` | Retirer des kills |

### 6.10 Recettes de configuration

#### Zone de grind
```yaml
spawninterval: 2
spawnchance: 1.0
targetmin: 5
targetmax: 8
max: 20
despawn: 80
despawninterval: 3
despawnclose: 0.0
despawnmid: 0.3
despawnfar: 0.6
despawnouter: 1.0
boundarytolerance: 0
bouncestrength: 0.6
```

#### Zone de boss (1 mob persistant)
```yaml
max: 1
spawninterval: 60
targetmin: 1
targetmax: 1
despawn: 300
despawninterval: 30
despawnclose: 0.0
despawnmid: 0.0
despawnfar: 0.0
despawnouter: 0.1
boundarytolerance: 5
bouncestrength: 0.8
```

#### Zone nocturne
```yaml
condition: NIGHT
spawninterval: 5
targetmin: 4
targetmax: 6
despawn: 120
despawnouter: 0.8
```

---

## 7. Module Armesia-Casino

Machine à sous interactive avec animation de roulette, hologrammes et lots entièrement configurables.

### 7.1 Fonctionnement

1. L'administrateur pose un bloc et l'enregistre avec `/casino setblock`
2. Un hologramme flottant apparaît au-dessus du bloc
3. Les joueurs cliquent sur le bloc en tenant le **jeton de casino** dans la main
4. L'interface GUI s'ouvre
5. Le joueur lance la roulette
6. Animation de défilement sonore et visuel
7. Le lot est remis via commande console configurable

### 7.2 Commandes

| Commande | Permission | Description |
|---|---|---|
| `/casino setblock` | `armesia.casino.admin` | Enregistrer le bloc regardé |
| `/casino removeblock` | `armesia.casino.admin` | Supprimer le bloc casino regardé |
| `/casino give <joueur> [quantité]` | `armesia.casino.admin` | Donner des jetons |
| `/casino settoken` | `armesia.casino.admin` | Définir le jeton avec l'item en main |
| `/casino list` | `armesia.casino.admin` | Lister les blocs casino |
| `/casino reload` | `armesia.casino.admin` | Recharger la configuration |

### 7.3 Configuration des lots

```yaml
prizes:
  diamant:
    display-material: DIAMOND
    display-name: "&bDiamant"
    chance: 5                                         # poids relatif
    console-command: "give %player% diamond 1"
    broadcast-message: "&e✦ %player% &aa remporté &b&lun Diamant&a au casino !"
    player-message: "&a✦ Félicitations ! Vous avez gagné &b&lun Diamant&a !"
  charbon:
    display-material: COAL
    display-name: "&8Charbon"
    chance: 65
    console-command: "give %player% coal 1"
```

> La probabilité d'un lot = `chance_lot / somme_de_toutes_les_chances`.

### 7.4 Configuration de la roulette

```yaml
roulette:
  total-steps: 40      # nombre de défilements
  slow-down: false     # true = accélère puis ralentit (effet dramatique)
  max-delay: 8         # ticks entre défilements (début, lent)
  min-delay: 1         # ticks entre défilements (fin, rapide)
  sound: BLOCK_NOTE_BLOCK_PLING
  finish-sound: ENTITY_PLAYER_LEVELUP
```

---

## 8. Module Armesia-World

Gestion complète des mondes, du spawn global, des zones de map, des portails et du système de parachute.

### 8.1 Gestion des mondes

| Commande | Permission | Description |
|---|---|---|
| `/world create <nom>` | `armesia.world.use` | Créer un nouveau monde |
| `/world teleport <nom>` | `armesia.world.use` | Se téléporter dans un monde |
| `/world list` | `armesia.world.use` | Lister les mondes chargés |

Les mondes créés sont enregistrés dans `config.yml` et rechargés automatiquement au redémarrage.

### 8.2 Spawn global

| Commande | Permission | Description |
|---|---|---|
| `/spawn` | — | Se téléporter au spawn global |
| `/setspawn` | `armesia.world.setspawn` | Définir le spawn à sa position |

### 8.3 Zones de map (`/mapzone`)

Les zones de map sont des régions rectangulaires 3D utilisées comme **destinations de portails**.
L'outil de sélection (bâton) permet de définir les deux coins en cliquant gauche/droit.

| Commande | Permission | Description |
|---|---|---|
| `/mapzone wand` | `armesia.world.mapzone` | Obtenir l'outil de sélection |
| `/mapzone set <nom>` | `armesia.world.mapzone` | Créer une zone depuis la sélection |
| `/mapzone list` | `armesia.world.mapzone` | Lister les zones |
| `/mapzone info <nom>` | `armesia.world.mapzone` | Détails d'une zone |
| `/mapzone delete <nom>` | `armesia.world.mapzone` | Supprimer une zone |

### 8.4 Portails (`/portal`)

Les portails sont des régions 3D qui téléportent le joueur en **parachute** dans une zone de map cible.

| Commande | Permission | Description |
|---|---|---|
| `/portal wand` | `armesia.world.portal` | Outil de sélection portail |
| `/portal set <nom> <zone>` | `armesia.world.portal` | Créer un portail vers une zone |
| `/portal list` | `armesia.world.portal` | Lister les portails |
| `/portal info <nom>` | `armesia.world.portal` | Détails d'un portail |
| `/portal delete <nom>` | `armesia.world.portal` | Supprimer un portail |

### 8.5 Système de parachute

Quand un joueur traverse un portail :
1. Il est téléporté à une **position aléatoire** dans la zone cible (X/Z aléatoires)
2. Il apparaît **70 blocs au-dessus du sol** (plafonné à Y=319)
3. Il descend lentement jusqu'au sol (effet parachute)

La `MapZone` fournit la méthode `getParachuteSpawn()` qui calcule automatiquement la hauteur depuis le sol le plus haut (`getHighestBlockYAt`).

---

## 9. Module Armesia-Scoreboard

Sidebar latérale mise à jour en temps réel affichant les informations de chaque joueur.

### 9.1 Contenu de la sidebar

```
§6§lARMESIA
─────────────
Niveau: §7{level}✫
§b{xp} {barre_xp} §e{xp_requis}
─────────────
K/D: §a{kills}§7/§a{deaths}
KS/Ratio: §a{killstreak}§7/§a{ratio}
─────────────
Money: §6{argent}
Tokens: §b{jetons}
─────────────
```

### 9.2 Mise à jour

- **À la connexion** du joueur
- **Toutes les minutes** (refresh périodique)
- **Instantanément** lors d'un changement de niveau (callback LevelAPI)

---

## 10. Module Armesia-CrackShotAddon

Extension pour le plugin **CrackShot** ajoutant des fonctionnalités visuelles et correctives avancées.

### 10.1 Rechargement custom (ActionBar)

Barre de rechargement animée dans l'ActionBar avec couleurs, caractères et durée configurables.

```yaml
# Dans le fichier d'arme CrackShot
CustomReload:
  Enabled: true
  Char: "|"
  Char_Empty: "."
  Color_Full: "&a"
  Color_Empty: "&7"
```

```yaml
# config.yml global
reload:
  bars: 10
messages:
  format: "{message} {bar} &7{time}"
  reloading: "&eRECHARGEMENT"
  loaded: "&aChargé !"
```

**Fonctionnalités :**
- Barre animée avec temps restant
- Couleurs personnalisables
- Annulation automatique si changement d'arme/slot ou tir parasite

### 10.2 Fix Drop → Shoot

Corrige le bug CrackShot où lâcher un item déclenche un tir.

```yaml
Addon:
  FixDrop: true
  DropCancelShootDelay: 200   # ms
```

### 10.3 Zones dynamiques

Zones avec effets, particules et sons déclenchées au tir ou à l'impact.

```yaml
Zones:
  explosion:
    Enabled: true
    Trigger: IMPACT          # SHOOT | IMPACT
    Location: IMPACT         # IMPACT | PLAYER | SHOOT
    Shape: SPHERE            # CYLINDER | SPHERE | CUBE
    Radius: 5
    Points_Surface: 40
    Points_Inside: 80
    Particle: REDSTONE
    Color: 255-100-0
    Duration: 100
    Tick: 10
    Effects:
      - SLOW:40:2            # EFFET:DURÉE:AMPLIFICATEUR
    Sound: ENTITY_GENERIC_EXPLODE
    Sound_Loop: BLOCK_FIRE_AMBIENT
```

### 10.4 Trail projectile

Traînée de particules suivant le projectile.

```yaml
Trail:
  Trail: REDSTONE
  Trail_Color: 255-0-0
  Distance: 30
  Radius: 1.5
  Points: 20
  Shape: CYLINDER           # LINE | CYLINDER | SPHERE | SPIRAL
  Animated: true
  Follow_Player: true
  Duration: 20
```

### 10.5 Commande

| Commande | Description |
|---|---|
| `/armesiacsa reload` | Recharger la config + vider le cache des armes |

---

## 11. Permissions — Référence complète

### Module : Armesia (Core)

#### Économie
| Permission | Défaut | Description |
|---|---|---|
| `armesia.money` | false | `/money` — Voir son solde |
| `armesia.money.admin` | op | `/money give/take/set` |
| `armesia.pay` | false | `/pay` — Transférer de l'argent |
| `armesia.baltop` | false | `/baltop` — Classement des riches |
| `armesia.tokens` | false | `/tokens` — Voir ses tokens |

#### Stats
| Permission | Défaut | Description |
|---|---|---|
| `armesia.stats` | false | `/stats` — Voir ses statistiques PvP |
| `armesia.statsadmin` | op | `/statsadmin` — Gérer les stats |

#### Groupes & Jobs
| Permission | Défaut | Description |
|---|---|---|
| `armesia.group` | op | `/group` — Gérer les groupes |

#### Téléportation
| Permission | Défaut | Description |
|---|---|---|
| `armesia.tp` | op | `/tp` |
| `armesia.tphere` | op | `/tphere` |
| `armesia.tpall` | op | `/tpall` |
| `armesia.tpa` | false | `/tpa` |
| `armesia.tpahere` | false | `/tpahere` |
| `armesia.tpacancel` | false | `/tpacancel` |

#### Joueur
| Permission | Défaut | Description |
|---|---|---|
| `armesia.heal` | op | `/heal` |
| `armesia.heal.others` | op | `/heal <joueur>` |
| `armesia.feed` | op | `/feed` |
| `armesia.feed.others` | op | `/feed <joueur>` |
| `armesia.suicide` | true | `/suicide` |
| `armesia.god` | op | `/god` |
| `armesia.god.others` | op | `/god <joueur>` |
| `armesia.speed` | op | `/speed` |
| `armesia.speed.others` | op | `/speed <joueur>` |
| `armesia.vanish` | op | `/vanish` |
| `armesia.vanish.others` | op | `/vanish <joueur>` |
| `armesia.repair` | op | `/repair` |
| `armesia.repairall` | op | `/repairall` |
| `armesia.kit` | false | `/kit` |
| `armesia.kit.admin` | op | `/kit create/delete/give` |
| `armesia.ping` | false | `/ping` |
| `armesia.ping.others` | op | `/ping <joueur>` |
| `armesia.near` | false | `/near` (rayon 100 blocs) |
| `armesia.near.200` | false | Rayon 200 blocs |
| `armesia.near.300` | false | Rayon 300 blocs |
| `armesia.near.400` | false | Rayon 400 blocs |
| `armesia.near.500` | false | Rayon 500 blocs |
| `armesia.invsee` | op | `/invsee` |
| `armesia.invsee.take` | op | Prendre des items via /invsee |
| `armesia.clearinventory` | op | `/ci` |
| `armesia.item` | op | `/item` |
| `armesia.home` | false | `/home /homes` |
| `armesia.sethome` | false | `/sethome /delhome` |
| `armesia.homes.2` | false | Limite : 2 homes |
| `armesia.homes.5` | false | Limite : 5 homes |
| `armesia.homes.10` | false | Limite : 10 homes |
| `armesia.homes.unlimited` | op | Homes illimités |
| `armesia.enderchest` | false | `/ec` |
| `armesia.enderchest.others` | op | `/ec <joueur>` |
| `armesia.msg` | false | `/msg /r` |

#### Admin
| Permission | Défaut | Description |
|---|---|---|
| `armesia.broadcast` | op | `/broadcast` |
| `armesia.cleareffect` | op | `/ce` |
| `armesia.reloadconfig` | op | `/reloadconfig` |
| `armesia.help` | true | `/help` |
| `chat.color` | false | Codes couleur dans le chat |

### Module : Armesia-Mobs

| Permission | Défaut | Description |
|---|---|---|
| `armesia.mob` | op | `/mob` (parent) |
| `armesia.mob.list` | op | `/mob list` |
| `armesia.mob.info` | op | `/mob info` |
| `armesia.mob.create` | op | `/mob create` |
| `armesia.mob.delete` | op | `/mob delete` |
| `armesia.mob.set` | op | `/mob set` |
| `armesia.mob.spawn` | op | `/mob spawn` |
| `armesia.loot` | op | `/loot` (parent) |
| `armesia.loot.list` | op | `/loot list` |
| `armesia.loot.info` | op | `/loot info` |
| `armesia.loot.create` | op | `/loot create` |
| `armesia.loot.delete` | op | `/loot delete` |
| `armesia.loot.add` | op | `/loot add/addhand` |
| `armesia.loot.remove` | op | `/loot remove` |
| `armesia.zone` | op | `/zone` (parent) |
| `armesia.zone.list` | op | `/zone list` |
| `armesia.zone.info` | op | `/zone info` |
| `armesia.zone.create` | op | `/zone create` |
| `armesia.zone.delete` | op | `/zone delete` |
| `armesia.zone.manage` | op | pos1/pos2, addmob, removemob, setweight, set |
| `armesia.zone.tp` | op | `/zone tp` |
| `armesia.zone.check` | op | `/zone check` |
| `armesia.zone.debug` | op | `/zone debug` |
| `armesia.zone.preview` | op | `/zone preview` |
| `armesia.mobstats` | true | Voir ses propres stats de kills |
| `armesia.mobstats.others` | op | Voir les stats d'un autre joueur |
| `armesia.mobstats.admin` | op | reset, add, remove, top |

### Module : Armesia-Casino

| Permission | Défaut | Description |
|---|---|---|
| `armesia.casino.admin` | op | Toutes les commandes /casino |
| `armesia.casino.use` | true | Utiliser un bloc de casino |

### Module : Armesia-World

| Permission | Défaut | Description |
|---|---|---|
| `armesia.world.use` | op | `/world` — Gérer les mondes |
| `armesia.world.setspawn` | op | `/setspawn` |
| `armesia.world.mapzone` | op | `/mapzone` |
| `armesia.world.portal` | op | `/portal` |

---

## 12. Fichiers de configuration

### `Armesia/config.yml` (extrait des sections principales)

```yaml
# Groupes et jobs
groups: {}
jobs:
  - "Citoyen"

# Vitesse
speed:
  max-walk: 10
  max-fly: 10
  default-walk: 2
  default-fly: 1
  reset-on-login: true

# Vanish
vanish:
  silent-join: true
  silent-quit: true
  reset-on-login: false

# Homes
homes:
  default-limit: 1
  teleport-delay: 3
  move-threshold: 0.5
  limits:
    armesia.homes.2: 2
    armesia.homes.5: 5
    armesia.homes.10: 10
    armesia.homes.unlimited: -1

# TPA
tpa:
  timeout: 60
  teleport-delay: 5
  send-cooldown: 5

# Anti-farm PvP
antifarm:
  enabled: true
  cooldown: 60

# Récompenses de kill
kill:
  killerReward:
    minMoney: 40
    maxMoney: 60
    minXp: 20
    maxXp: 40

# Killstreaks
killstreak:
  5:
    message: "§a{player} §7a un killstreak de {killstreak} !"
    commands:
      - "give {player} diamond 5"
```

### `Armesia-Level/config.yml`

```yaml
settings:
  max-level: 100
  xp-formula-multiplier: 1000    # XP pour N→N+1 = N × 1000

level-up:
  message: "&bLevel up ! &7Niveau %level%"
  title:
    main: "&6&l⬆ LEVEL UP ⬆"
    sub: "&eNiveau &6%level%"

milestones:
  5:
    - type: broadcast
      value: "&6[Niveau] &e%player% &7a atteint le niveau 5 !"
    - type: command
      value: "give %player% iron_sword 1"
  100:
    - type: broadcast
      value: "&c&l[LÉGENDE] &e%player% &7a atteint le NIVEAU MAX 100 !"
```

### `Armesia-Mobs/mobs.yml`

```yaml
mobs:
  zombie_foret:
    name: "&2Zombie &ade la Foret"
    entity-type: ZOMBIE
    level: 1
    health: 60.0
    money: 10
    loot-table: basic_drops
```

### `Armesia-Mobs/zones.yml` (exemple complet)

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

## 13. Guide de test complet (pas à pas)

### Étape 1 — Setup minimal mobs

```
# Créer un mob
/mob create zombie_test ZOMBIE 1 20 5 §cZombie Test

# Créer une table de loot
/loot create drops_test
/loot add drops_test BONE 1 3 1.0
/mob set zombie_test loot drops_test

# Créer une zone
/zone create testzone
/zone pos1 testzone       ← coin 1 (se placer au coin)
/zone pos2 testzone       ← coin 2 (se placer au coin opposé)
/zone set testzone spawninterval 2
/zone set testzone condition ALWAYS
/zone set testzone boundarytolerance 5
/zone set testzone bouncestrength 0.6
/zone addmob testzone zombie_test

# Vérifier
/zone check
/zone preview testzone
/zone debug normal
```

✅ En entrant dans la zone : `[ZONE+]`
✅ En moins de 2s : `[SPAWN]`

### Étape 2 — Multi-mobs pondérés

```
/mob create skeleton_test SKELETON 1 15 5 Squelette
/zone addmob testzone skeleton_test
/zone setweight testzone zombie_test 3.0
/zone setweight testzone skeleton_test 1.0
/zone info testzone
```
✅ Attendu : `zombie_test §8(×3.0 §e75%§8), skeleton_test §8(×1.0 §e25%§8)`

### Étape 3 — Tester le niveau (Armesia-Level)

```
/level               ← voir son niveau (1) et son XP (0)
/level add xp MonJoueur 5000
/level               ← niveau 3 attendu
```

### Étape 4 — Tester le casino

```
# Poser un bloc quelconque, le regarder, puis :
/casino setblock
/casino give MonJoueur 5
# Cliquer sur le bloc avec le jeton dans la main
```

### Étape 5 — Tester les portails

```
# Obtenir l'outil, sélectionner une zone
/mapzone wand
# Clic gauche coin 1, clic droit coin 2
/mapzone set foret

# Créer un portail
/portal wand
# Sélectionner la région du portail
/portal set portail_nord foret
# Traverser la région du portail → téléportation en parachute
```

### Étape 6 — Tester la persistance mobs

```
# Se déconnecter → se reconnecter (serveur en marche)
```
✅ Les mobs sont toujours là (pas de despawn hors-ligne)

```
# Redémarrer le serveur + /zone debug normal
```
✅ Messages `[RESTORE]` — mobs récupérés via PDC

---

## 14. Comportements importants à connaître

### Spawns

- Les mobs ne spawent **jamais** dans le champ de vision du joueur
- Sélection par **tirage pondéré** (poids configurables par mob et par zone)
- Timer de spawn **partagé par zone** (pas par joueur individuel)
- Tous les spawns **vanilla bloqués** (spawners, naturels, œufs) — seuls les mobs plugin passent

### Despawn

- Distance **XZ uniquement** — la hauteur (Y) est ignorée
- **Suspendu** quand aucun joueur n'est connecté
- Mobs spawné manuellement (`/mob spawn`, zone `"manual"`) jamais despawnés automatiquement

### Frontière et rebond

- Rebond toutes les **0.2s** (4 ticks)
- Pathfinder stoppé avant la vélocité pour éviter la compensation de l'IA
- Force **progressive** : plus le mob est loin de la frontière, plus il est repoussé fort
- **Hard-kill** : `boundarytolerance + 15` blocs hors zone → suppression immédiate

### Persistance et récupération (PDC)

- `persistent=true` : mobs sauvegardés dans les fichiers monde
- Tags PDC (`armesia_mob_id` + `armesia_zone_id`) dans le NBT — survivent aux redémarrages
- `ChunkLoadEvent` : détecte et ré-enregistre les mobs au rechargement de chunk
- Si zone/mob n'existe plus → mob orphelin supprimé automatiquement (`[CLEANUP]`)
- Scan de récupération automatique 2 secondes après le démarrage du serveur

### Portails & Parachute

- La hauteur de spawn en parachute est calculée dynamiquement depuis le sol réel (+70 blocs, max Y=319)
- La destination est **aléatoire** dans les bornes XZ de la zone cible
- Les portails sont des régions 3D persistées dans `Armesia-World/config.yml`

### Scoreboard

- Un scoreboard distinct est créé par joueur (pas de scoreboard global partagé)
- Les teams de groupes sont re-appliquées sur chaque nouveau scoreboard à la connexion
- Le scoreboard se met à jour instantanément lors d'un changement de niveau (callback)

---

## Référence rapide — Setup minimal complet

```bash
# Module Mobs
/mob create zombie ZOMBIE 1 20 5 Zombie
/loot create drops
/loot add drops BONE 1 3 1.0
/mob set zombie loot drops
/zone create test
/zone pos1 test
/zone pos2 test
/zone set test spawninterval 3
/zone set test condition ALWAYS
/zone addmob test zombie
/zone check
/zone debug normal

# Module Level (XP gagné via kills PvP ou manuellement)
/level add xp MonJoueur 1000

# Module Casino
/casino setblock
/casino give MonJoueur 1

# Module World
/setspawn
/mapzone wand
/portal wand
```

---

*Dernière mise à jour : 2026-04-14 — Version Armesia 1.0.0*
