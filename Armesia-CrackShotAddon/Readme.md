# 🚀 Armesia CrackShot Addon

Addon avancé pour **CrackShot** ajoutant :
- 🔁 Reload visuel custom (ActionBar)
- 🎯 Zones dynamiques (tir / impact)
- ✨ Particules avancées (trail + zones)
- 🧠 Fix intelligent du bug drop → tir
- ⚡ Système modulaire ultra configurable

---

## 📦 Installation

1. Installer **CrackShot**
2. Placer le plugin dans `/plugins`
3. Démarrer le serveur
4. Modifier les armes dans :

/plugins/CrackShot/weapons/

---

## ⚙️ Configuration

Toutes les options se configurent directement dans les fichiers d’armes CrackShot.

---

# 🔁 Custom Reload (ActionBar)

Ajoute une barre de rechargement stylée.

    CustomReload:
      Enabled: true
      Char: "|"
      Char_Empty: "."
      Color_Full: "&a"
      Color_Empty: "&7"

### Configuration globale (`config.yml`)

    reload:
      bars: 10

    messages:
      format: "{message} {bar} &7{time}"
      reloading: "&eRECHARGEMENT"
      loaded: "&aChargé !"

### ✅ Fonctionnalités

- Barre animée
- Temps restant affiché
- Couleurs personnalisables
- Annulation automatique si :
    - changement d’arme
    - changement de slot
    - tir parasite (drop)

---

# 🔥 Fix Drop → Shoot (Bug CrackShot)

Corrige le bug où un drop déclenche un tir.

    Addon:
      FixDrop: true
      DropCancelShootDelay: 200

### ✅ Fonctionnement

- Le joueur peut drop normalement
- Le tir est bloqué juste après

---

# 🎯 Zones dynamiques

Permet de créer des zones avec effets, particules et sons.

    Zones:
      explosion:
        Enabled: true
        Trigger: IMPACT
        Location: IMPACT

---

## ⚡ Triggers

| Trigger | Description |
|--------|------------|
| SHOOT | Déclenché au tir |
| IMPACT | Déclenché à l’impact |

---

## 📍 Location

    Location: IMPACT

| Type | Description |
|------|------------|
| IMPACT | Position de l’impact |
| PLAYER | Position du joueur |
| SHOOT | Position du tir |

---

## 📐 Shapes

    Shape: CYLINDER

| Shape | Description |
|------|------------|
| CYLINDER | Cylindre |
| SPHERE | Sphère |
| CUBE | Cube |

---

## 🎨 Particules

    Particle: REDSTONE
    Color: 255-0-0

- Compatible toutes particules Bukkit
- REDSTONE support RGB

---

## ✨ Densité des particules

    Points_Surface: 30
    Points_Inside: 50

| Type | Description |
|------|------------|
| Surface | Contour |
| Inside | Intérieur |

---

## 📏 Taille & durée

    Radius: 4
    Height_Up: 2
    Height_Down: 2

    Duration: 100
    Tick: 10

---

## 🧪 Effets

    Effects:
      - POISON:40:1
      - SLOW:60:2

Format :
EFFET:DURATION:AMPLIFIER

---

## 🔊 Sons

    Sound: ENTITY_GENERIC_EXPLODE
    Sound_Loop: BLOCK_FIRE_AMBIENT

---

# ✨ Trail System

---

## 🔫 Trail projectile

    Trail:
      Trail: REDSTONE
      Trail_Color: 255-0-0
      Distance: 30
      Radius: 1.5
      Points: 20
      Shape: CYLINDER

---

## 📐 Shapes disponibles

- LINE
- CYLINDER
- SPHERE
- SPIRAL

---

## 🎞 Animation

    Animated: true

---

## 👤 Follow Player

    Follow_Player: true
    Duration: 20

---

# 🧠 Système interne

- Cache des configs armes
- Multi reload sécurisé (session ID)
- Anti double reload
- Anti glitch changement slot
- Gestion multi-armes

---

# 🛠 Commande

    /armesiacsa reload

- Reload config
- Reset cache armes

---

# 💡 Exemples

---

## 💥 Explosion

    Zones:
      boom:
        Enabled: true
        Trigger: IMPACT
        Shape: SPHERE
        Radius: 5
        Points_Surface: 40
        Points_Inside: 80
        Particle: REDSTONE
        Color: 255-100-0
        Effects:
          - SLOW:40:2

---

## ❄️ Zone de gel

    Effects:
      - SLOW:100:3
      - WEAKNESS:60:1

---

## ⚡ Dash énergie

    Trail:
      Shape: SPIRAL
      Animated: true
      Follow_Player: true

---

# 🔥 Roadmap

- [ ] Knockback
- [ ] Damage zone
- [ ] Particules avancées
- [ ] Shapes avancées
- [ ] Multi-trigger
- [ ] Support mobs

---

# ❤️ Crédit

Développé pour **Armesia**; Par **SeGame**.