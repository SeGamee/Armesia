# ═══════════════════════════════════════════════════════════════════════════════
# PERMISSIONS — Référence centralisée des permissions Armesia
# Mise à jour ce fichier à chaque ajout/suppression de permission.
# Format : permission | défaut | description
# ═══════════════════════════════════════════════════════════════════════════════

## Module : Armesia-Mobs

### /mob — Gestion des mobs personnalisés
| Permission            | Défaut | Description                                    |
|-----------------------|--------|------------------------------------------------|
| `armesia.mob`         | op     | Accès complet à /mob (parent de tous les enfants) |
| `armesia.mob.list`    | op     | `/mob list` — Lister tous les mobs             |
| `armesia.mob.info`    | op     | `/mob info <id>` — Voir les détails d'un mob   |
| `armesia.mob.create`  | op     | `/mob create ...` — Créer un mob               |
| `armesia.mob.delete`  | op     | `/mob delete <id>` — Supprimer un mob          |
| `armesia.mob.set`     | op     | `/mob set <id> <prop> <val>` — Modifier un mob |
| `armesia.mob.spawn`   | op     | `/mob spawn <id> <n>` — Spawner un mob         |

### /loot — Gestion des tables de loot
| Permission             | Défaut | Description                                          |
|------------------------|--------|------------------------------------------------------|
| `armesia.loot`         | op     | Accès complet à /loot (parent de tous les enfants)   |
| `armesia.loot.list`    | op     | `/loot list` — Lister les tables                     |
| `armesia.loot.info`    | op     | `/loot info <table>` — Voir le contenu d'une table   |
| `armesia.loot.create`  | op     | `/loot create <table>` — Créer une table             |
| `armesia.loot.delete`  | op     | `/loot delete <table>` — Supprimer une table         |
| `armesia.loot.add`     | op     | `/loot add / addhand` — Ajouter une entrée           |
| `armesia.loot.remove`  | op     | `/loot remove <table> <index>` — Supprimer une entrée|

### /zone — Gestion des zones de spawn
| Permission              | Défaut | Description                                               |
|-------------------------|--------|-----------------------------------------------------------|
| `armesia.zone`          | op     | Accès complet à /zone (parent de tous les enfants)        |
| `armesia.zone.list`     | op     | `/zone list` — Lister les zones                           |
| `armesia.zone.info`     | op     | `/zone info <id>` — Voir les détails d'une zone           |
| `armesia.zone.create`   | op     | `/zone create <id>` — Créer une zone                      |
| `armesia.zone.delete`   | op     | `/zone delete <id>` — Supprimer une zone                  |
| `armesia.zone.manage`   | op     | pos1/pos2, addmob, removemob, setweight, set              |
| `armesia.zone.tp`       | op     | `/zone tp <id>` — Se téléporter au centre d'une zone      |
| `armesia.zone.check`    | op     | `/zone check` — Vérifier la zone actuelle                 |
| `armesia.zone.debug`    | op     | `/zone debug` — Activer le mode debug                     |
| `armesia.zone.preview`  | op     | `/zone preview <id>` — Prévisualiser la bordure particules|

### /mobstats — Statistiques de kills
| Permission                 | Défaut | Description                                         |
|----------------------------|--------|-----------------------------------------------------|
| `armesia.mobstats`         | true   | `/mobstats` — Voir ses propres statistiques         |
| `armesia.mobstats.others`  | op     | `/mobstats <joueur>` — Voir les stats d'un autre    |
| `armesia.mobstats.admin`   | op     | reset, add, remove, top — Gérer les statistiques    |

---

## Module : Armesia (Core)

### Économie
| Permission              | Défaut | Description                              |
|-------------------------|--------|------------------------------------------|
| `armesia.money`         | false  | `/money` — Voir son solde                |
| `armesia.money.admin`   | op     | `/money give/take/set` — Gérer l'argent  |
| `armesia.pay`           | false  | `/pay` — Transférer de l'argent          |
| `armesia.baltop`        | false  | `/baltop` — Voir le classement des riches|
| `armesia.tokens`        | false  | `/tokens` — Voir ses tokens              |

### Stats
| Permission         | Défaut | Description                         |
|--------------------|--------|-------------------------------------|
| `armesia.stats`    | false  | `/stats` — Voir ses statistiques    |
| `armesia.statsadmin` | op   | `/statsadmin` — Gérer les stats     |

### Groupes & Rangs
| Permission          | Défaut | Description                              |
|---------------------|--------|------------------------------------------|
| `armesia.group`     | op     | `/group` — Gérer les groupes des joueurs |

### Téléportation
| Permission         | Défaut | Description                                |
|--------------------|--------|--------------------------------------------|
| `armesia.tp`       | op     | `/tp` — Téléporter un joueur               |
| `armesia.tpa`      | false  | `/tpa /tpahere` — Demander une téléportation|

### Joueur
| Permission               | Défaut | Description                              |
|--------------------------|--------|------------------------------------------|
| `armesia.heal`           | op     | `/heal` — Se soigner                     |
| `armesia.heal.others`    | op     | `/heal <joueur>` — Soigner un autre      |
| `armesia.feed`           | op     | `/feed` — Se nourrir                     |
| `armesia.feed.others`    | op     | `/feed <joueur>` — Nourrir un autre      |
| `armesia.suicide`        | true   | `/suicide` — Se suicider                 |
| `armesia.god`            | op     | `/god` — Mode invincible                 |
| `armesia.god.others`     | op     | `/god <joueur>` — Invincible un autre    |
| `armesia.speed`          | op     | `/speed` — Changer sa vitesse            |
| `armesia.speed.others`   | op     | `/speed <joueur>` — Changer celle d'un autre |
| `armesia.vanish`         | op     | `/vanish` — Se rendre invisible          |
| `armesia.vanish.others`  | op     | `/vanish <joueur>` — Rendre un autre invisible |
| `armesia.repair`         | op     | `/repair` — Réparer l'item en main       |
| `armesia.kit`            | false  | `/kit` — Réclamer un kit                 |
| `armesia.kit.admin`      | op     | `/kit create/delete/give` — Gérer les kits|
| `armesia.ping`           | false  | `/ping` — Voir son ping                  |
| `armesia.ping.others`    | op     | `/ping <joueur>` — Voir le ping d'un autre|
| `armesia.near`           | false  | `/near` — Voir les joueurs proches       |
| `armesia.invsee`         | op     | `/invsee` — Voir l'inventaire d'un joueur|
| `armesia.invsee.take`    | op     | Prendre des items via /invsee            |
| `armesia.clearinventory` | op     | `/ci` — Vider l'inventaire               |
| `armesia.item`           | op     | `/item` — Obtenir un item                |
| `armesia.home`           | false  | `/home /sethome /delhome /homes`         |
| `armesia.enderchest`     | false  | `/ec` — Ouvrir son enderchest            |

### Utilitaires admin
| Permission               | Défaut | Description                              |
|--------------------------|--------|------------------------------------------|
| `armesia.broadcast`      | op     | `/broadcast` — Envoyer un message global |
| `armesia.kill`           | op     | `/kill <joueur>` — Tuer un joueur        |
| `armesia.cleareffect`    | op     | `/ce` — Supprimer les effets             |
| `armesia.reloadconfig`   | op     | `/reloadconfig` — Recharger la config    |
| `armesia.help`           | true   | `/help` — Voir l'aide                    |
| `chat.color`             | false  | Utiliser les codes couleur dans le chat  |

---
*Dernière mise à jour : 2026-04-08*

