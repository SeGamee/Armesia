# Armesia — CrackShot Addon

> **Addon CrackShot** qui étend les armes avec des trails de particules, des effets d'impact, des zones de particules/effets et un fix anti-drop.

---

## Sommaire

- [Dépendances](#dépendances)
- [Installation](#installation)
- [Commandes & Permissions](#commandes--permissions)
- [Configuration globale](#configuration-globale-configyml)
- [Sections disponibles dans un weapon config](#sections-disponibles-dans-un-weapon-config)
  - [CustomParticles — Trail sur le projectile](#1-customparticles--trail-sur-le-projectile)
  - [Impact — Burst de particules à l'impact](#2-impact--burst-de-particules-à-limpact)
  - [Zones — Zone d'effets persistante](#3-zones--zone-deffets-persistante)
  - [CustomReload — Barre de rechargement](#4-customreload--barre-de-rechargement)
  - [Addon — Options diverses](#5-addon--options-diverses)
- [Référence des particules Bukkit](#référence-des-particules-bukkit)
- [Référence des effets de potion](#référence-des-effets-de-potion)
- [Exemples complets](#exemples-complets)

---

## Dépendances

| Plugin | Version minimale |
|---|---|
| **CrackShot** | toute version compatible 1.20 |
| **Paper / Spigot** | 1.20+ |

---

## Installation

1. Place `Armesia-CrackShotAddon.jar` dans `plugins/`
2. Lance le serveur — le fichier `config.yml` est généré automatiquement
3. Ajoute tes sections (`CustomParticles`, `Impact`, `Zones`…) directement dans les fichiers d'armes **CrackShot** (`plugins/CrackShot/weapons/*.yml`)
4. Reload avec `/armesiacsa reload`

---

## Commandes & Permissions

| Commande | Description | Permission |
|---|---|---|
| `/armesiacsa reload` | Recharge la config + vide le cache des armes | `armesiacsa.reload` (OP par défaut) |

---

## Configuration globale (`config.yml`)

```yaml
messages:
  reloading: "&7&lRECHARGEMENT..."    # Texte affiché pendant le reload
  loaded:    "&a&lChargé !"            # Texte affiché quand le reload est fini
  format: "{message} {bar} &7&l{time}" # Format global de la barre
  # Variables disponibles : {weapon} {message} {bar} {time}

default:
  char_full:   "█"   # Caractère rempli de la barre
  char_empty:  "█"   # Caractère vide de la barre
  color_full:  "&a"  # Couleur partie remplie
  color_empty: "&7"  # Couleur partie vide

reload:
  bars: 8   # Nombre de segments dans la barre
```

---

## Sections disponibles dans un weapon config

Toutes les sections ci-dessous se placent **à la racine du nom de l'arme** dans les fichiers CrackShot :

```yaml
MonArme:
  Shoot_Projectile: ARROW
  # ... options CrackShot normales ...

  CustomParticles:   # Trail sur le projectile
    ...
  Impact:            # Burst de particules à l'impact
    ...
  Zones:             # Zones d'effets
    ...
  CustomReload:      # Barre de rechargement
    ...
  Addon:             # Options diverses
    ...
```

---

## 1. `CustomParticles` — Trail sur le projectile

Spawn des particules **sur le projectile** à chaque tir.

### Clés disponibles

| Clé | Type | Défaut | Description |
|---|---|---|---|
| `Enabled` | boolean | `false` | Active le trail |
| `Follow_Projectile` | boolean | `false` | **Trail qui suit la balle** tick par tick (recommandé) |
| `Animated` | boolean | `false` | Trail qui avance progressivement depuis la position de tir |
| `Shape` | string | `LINE` | Forme du trail : `LINE` `CYLINDER` `SPHERE` `SPIRAL` |
| `Trail` | string | `REDSTONE` | Type de particule Bukkit |
| `Trail_Color` | string | `255-255-255` | Couleur RGB (`R-G-B`) — uniquement pour `Trail: REDSTONE` |
| `Radius` | double | `1.5` | Rayon pour CYLINDER / SPHERE / SPIRAL |
| `Points` | int | `20` | Nombre de particules par anneau (CYLINDER / SPHERE) |
| `Distance` | double | `30` | Longueur de la traîne derrière la balle (en blocs) |
| `Space_Between_Trails` | double | `0.2` | Espacement entre chaque point du trail (plus petit = plus dense) |
| `PassThrough_Walls` | boolean | `true` | `false` = le trail **s'arrête** au premier bloc solide (ne réapparaît pas de l'autre côté) |

### Modes de fonctionnement

```
Follow_Projectile: true  →  Trail calculé TICK PAR TICK depuis la position du projectile
                             → La queue s'étend sur Distance blocs DERRIÈRE la balle
                             → Recommandé pour tous les effets "visuels"

Animated: true           →  Trail fixé à la position de tir, qui avance PROGRESSIVEMENT
                             vers la direction du tir (1 point par tick)
                             → Effet "laser beam" ou "tracer"

(aucun des deux)         →  Trail STATIQUE spawné en une seule fois à la position de tir
```

### Shapes

| Shape | Effet |
|---|---|
| `LINE` | Simple point de particule à chaque position |
| `CYLINDER` | Anneau de `Points` particules autour de l'axe de déplacement |
| `SPHERE` | Nuage sphérique de `Points` particules aléatoires |
| `SPIRAL` | Un seul point qui tourne autour de l'axe (spirale continue) |

### Exemples

```yaml
# Trail LINE rouge (laser sniper)
CustomParticles:
  Enabled: true
  Follow_Projectile: true
  Shape: LINE
  Trail: REDSTONE
  Trail_Color: 255-0-0
  Distance: 2
  Space_Between_Trails: 0.05

# Tube CYLINDER bleu (canon énergie)
CustomParticles:
  Enabled: true
  Follow_Projectile: true
  Shape: CYLINDER
  Radius: 1.2
  Points: 20
  Trail: REDSTONE
  Trail_Color: 0-150-255
  Distance: 2
  Space_Between_Trails: 0.2

# Spirale SPIRAL violette
CustomParticles:
  Enabled: true
  Follow_Projectile: true
  Shape: SPIRAL
  Radius: 1.5
  Trail: REDSTONE
  Trail_Color: 180-0-255
  Distance: 2
  Space_Between_Trails: 0.1

# Trail ANIMÉ blanc (tracer)
CustomParticles:
  Enabled: true
  Animated: true
  Shape: LINE
  Trail: REDSTONE
  Trail_Color: 255-255-255
  Distance: 40
  Space_Between_Trails: 0.5
```

---

## 2. `Impact` — Burst de particules à l'impact

Spawn un **burst instantané** de particules **là où le projectile s'arrête**.

### Clés disponibles

| Clé | Type | Défaut | Description |
|---|---|---|---|
| `Enabled` | boolean | `false` | Active l'impact |
| `Shape` | string | `SPHERE` | Forme de l'impact : `SPHERE` `LINE` `CYLINDER` `SPIRAL` |
| `Color` | string | `255-255-255` | Couleur RGB (`R-G-B`) |
| `Trail` | string | `REDSTONE` | Type de particule Bukkit |
| `Radius` | double | `2.0` | Rayon de l'explosion de particules |
| `Points` | int | `30` | Nombre de particules spawnées |
| `PassThrough_Walls` | boolean | `true` | `false` = les particules d'impact ne s'affichent pas dans les blocs solides |

### Exemples

```yaml
# Explosion orange
Impact:
  Enabled: true
  Shape: SPHERE
  Radius: 2.5
  Points: 40
  Color: 255-50-0

# Impact électrique bleu
Impact:
  Enabled: true
  Shape: SPHERE
  Radius: 2.0
  Points: 30
  Color: 0-200-255

# Impact radioactif vert
Impact:
  Enabled: true
  Shape: SPHERE
  Radius: 3.0
  Points: 50
  Color: 0-255-0
```

---

## 3. `Zones` — Zone d'effets persistante

Crée une **zone de particules** qui applique des **effets de potion** aux joueurs qui la traversent, pendant une durée définie.

Plusieurs zones peuvent coexister par arme (sous des clés différentes : `Zone1`, `Zone2`…).

### Clés disponibles

| Clé | Type | Défaut | Description |
|---|---|---|---|
| `Enabled` | boolean | `false` | Active cette zone |
| `Trigger` | string | `IMPACT` | Déclenchement : `IMPACT` ou `SHOOT` |
| `Location` | string | `IMPACT` | Position de la zone : `IMPACT`, `PLAYER` ou `SHOOT` |
| `Shape` | string | `CYLINDER` | Forme : `CYLINDER` `SPHERE` `CUBE` |
| `Radius` | double | `3.0` | Rayon de la zone |
| `Height_Up` | double | `1.0` | Hauteur vers le haut (CYLINDER / CUBE) |
| `Height_Down` | double | `1.0` | Hauteur vers le bas (CYLINDER / CUBE) |
| `Duration` | int | `100` | Durée de vie de la zone en ticks (`100` = 5s) |
| `Tick` | int | `10` | Fréquence de mise à jour en ticks |
| `Particle` | string | `REDSTONE` | Type de particule Bukkit |
| `Color` | string | `255-255-255` | Couleur RGB (`R-G-B`) — pour `REDSTONE` |
| `Points_Surface` | int | `30` | Particules sur la surface par tick |
| `Points_Inside` | int | `0` | Particules à l'intérieur par tick |
| `Effects` | list | `[]` | Effets de potion : `TYPE:durée:amplificateur` |
| `Sound` | string | *(aucun)* | Son au déclenchement |
| `Sound_Loop` | string | *(aucun)* | Son joué en boucle pour les joueurs dans la zone |
| `PassThrough_Walls` | boolean | `true` | `false` = les particules de la zone ne s'affichent pas dans les blocs solides |

### Shapes de zone

| Shape | Détection joueur |
|---|---|
| `CYLINDER` | Cylindre (rayon XZ + hauteur Y) |
| `SPHERE` | Sphère (distance 3D) |
| `CUBE` | Cube (±rayon XZ + hauteur Y) |

### Locations

| Valeur | Position de la zone |
|---|---|
| `IMPACT` | Là où le projectile atterrit *(défaut)* |
| `PLAYER` / `SHOOT` | Sur le joueur qui tire |

### Exemple

```yaml
Zones:

  # Zone de poison à l'impact
  Poison:
    Enabled: true
    Trigger: IMPACT
    Location: IMPACT
    Shape: CYLINDER
    Radius: 4
    Height_Up: 2
    Height_Down: 0.5
    Duration: 200
    Tick: 10
    Particle: REDSTONE
    Color: 100-200-0
    Points_Surface: 20
    Effects:
      - POISON:100:1
      - SLOW:60:0

  # Zone de feu au tir (autour du joueur)
  FeuTir:
    Enabled: true
    Trigger: SHOOT
    Location: PLAYER
    Shape: SPHERE
    Radius: 3
    Duration: 60
    Tick: 5
    Particle: FLAME
    Points_Surface: 15
    Effects:
      - FIRE_RESISTANCE:80:0
    Sound_Loop: ENTITY_BLAZE_BURN
```

---

## 4. `CustomReload` — Barre de rechargement

Personnalise l'affichage de la barre de reload **par arme** (en action bar).

### Clés disponibles

| Clé | Type | Défaut (config.yml) | Description |
|---|---|---|---|
| `Enabled` | boolean | `true` | Active la barre pour cette arme |
| `Char` | string | `█` | Caractère pour la partie remplie |
| `Char_Empty` | string | `█` | Caractère pour la partie vide |
| `Color_Full` | string | `&a` | Couleur partie remplie |
| `Color_Empty` | string | `&7` | Couleur partie vide |

### Exemple

```yaml
CustomReload:
  Enabled: true
  Char: "▰"
  Char_Empty: "▱"
  Color_Full: "&c"
  Color_Empty: "&8"
```

---

## 5. `Addon` — Options diverses

Options techniques par arme.

### Clés disponibles

| Clé | Type | Défaut | Description |
|---|---|---|---|
| `FixDrop` | boolean | `false` | Empêche un tir accidentel juste après avoir lâché l'arme |
| `DropCancelShootDelay` | long | `200` | Délai en ms pendant lequel le tir est bloqué après un drop |

### Exemple

```yaml
Addon:
  FixDrop: true
  DropCancelShootDelay: 250
```

---

## Référence des particules Bukkit

| Nom | Effet visuel |
|---|---|
| `REDSTONE` | Poussière colorée *(utilise `Trail_Color` / `Color`)* |
| `FLAME` | Flamme |
| `SMOKE_NORMAL` | Fumée légère |
| `SMOKE_LARGE` | Grosse fumée |
| `CRIT` | Critique (étoile dorée) |
| `MAGIC_CRIT` | Critique magique |
| `SPELL_WITCH` | Particule de sorcière (violette) |
| `DRAGON_BREATH` | Souffle de dragon |
| `CLOUD` | Nuage blanc |
| `HEART` | Cœur rose |
| `VILLAGER_HAPPY` | Émeraude verte |
| `VILLAGER_ANGRY` | Croix rouge |
| `ENCHANTMENT_TABLE` | Glyphes dorés |
| `DRIP_WATER` | Gouttes d'eau |
| `DRIP_LAVA` | Gouttes de lave |
| `PORTAL` | Portail violet |
| `FIREWORKS_SPARK` | Étincelle de feu d'artifice |
| `EXPLOSION_NORMAL` | Mini-explosion |
| `END_ROD` | Particule blanche end rod |
| `SNOWBALL` | Impact de boule de neige |

---

## Référence des effets de potion

Format dans `Effects` : `TYPE:durée_ticks:amplificateur`

| Type | Effet |
|---|---|
| `SPEED` | Vitesse |
| `SLOW` | Lenteur |
| `FAST_DIGGING` | Hâte |
| `SLOW_DIGGING` | Fatigue |
| `INCREASE_DAMAGE` | Force |
| `HEAL` | Soin instantané |
| `HARM` | Dégâts instantanés |
| `JUMP` | Saut |
| `CONFUSION` | Nausée |
| `REGENERATION` | Régénération |
| `DAMAGE_RESISTANCE` | Résistance |
| `FIRE_RESISTANCE` | Résistance au feu |
| `WATER_BREATHING` | Respiration aquatique |
| `INVISIBILITY` | Invisibilité |
| `BLINDNESS` | Cécité |
| `NIGHT_VISION` | Vision nocturne |
| `HUNGER` | Faim |
| `WEAKNESS` | Faiblesse |
| `POISON` | Poison |
| `WITHER` | Flétrissement |
| `LEVITATION` | Lévitation |
| `GLOWING` | Incandescence |

---

## Exemples complets

### Sniper laser rouge + explosion à l'impact

```yaml
SniperLaser:
  Shoot_Sound: ENTITY_ARROW_SHOOT
  Shoot_Projectile: ARROW
  Shoot_Velocity: 5.0

  CustomParticles:
    Enabled: true
    Follow_Projectile: true
    Shape: LINE
    Trail: REDSTONE
    Trail_Color: 255-0-0
    Distance: 2
    Space_Between_Trails: 0.05

  Impact:
    Enabled: true
    Shape: SPHERE
    Radius: 2.5
    Points: 40
    Color: 255-50-0

  Addon:
    FixDrop: true
```

---

### Fusil à gaz empoisonnant (zone à l'impact)

```yaml
FusilGaz:
  Shoot_Sound: ENTITY_ARROW_SHOOT
  Shoot_Projectile: ARROW
  Shoot_Velocity: 3.0

  CustomParticles:
    Enabled: true
    Follow_Projectile: true
    Shape: CYLINDER
    Radius: 0.6
    Points: 12
    Trail: REDSTONE
    Trail_Color: 100-200-0
    Distance: 2
    Space_Between_Trails: 0.2

  Impact:
    Enabled: true
    Shape: SPHERE
    Radius: 1.5
    Points: 25
    Color: 80-220-0

  Zones:
    Nuage:
      Enabled: true
      Trigger: IMPACT
      Location: IMPACT
      Shape: SPHERE
      Radius: 4
      Duration: 300
      Tick: 10
      Particle: REDSTONE
      Color: 100-200-0
      Points_Surface: 20
      Points_Inside: 5
      Effects:
        - POISON:80:1
        - SLOW:60:0
      Sound_Loop: ENTITY_CREEPER_HURT
```

---

### Pistolet galactique (laser violet + explosion cosmique)

```yaml
PistoletGalaxie:
  Shoot_Sound: ENTITY_ARROW_SHOOT
  Shoot_Projectile: ARROW
  Shoot_Velocity: 4.0

  CustomParticles:
    Enabled: true
    Follow_Projectile: true
    Shape: CYLINDER
    Radius: 0.8
    Points: 25
    Trail: REDSTONE
    Trail_Color: 150-0-255
    Distance: 2
    Space_Between_Trails: 0.15

  Impact:
    Enabled: true
    Shape: SPHERE
    Radius: 2.5
    Points: 40
    Color: 200-0-255

  CustomReload:
    Enabled: true
    Char: "◈"
    Char_Empty: "◇"
    Color_Full: "&5"
    Color_Empty: "&8"
```
