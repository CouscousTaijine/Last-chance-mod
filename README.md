# Dernière Chance — Mod Fabric 1.20.1

Ajoute une troisième option à l'écran de mort : **"Dernière Chance"**.
Elle lance un combat façon Undertale (esquive de projectiles avec un
cœur façon Minecraft, dessiné en pixel art directement dans le mod, pas
de dépendance à une texture Undertale).

- **Victoire** → tu réapparais normalement.
- **Défaite** → mort **définitive** : le respawn est bloqué côté serveur,
  même si tu quittes et rejoins la partie (voir `ServerPlayNetworkHandlerMixin`).
- **Difficulté** : dès la 1ʳᵉ utilisation, le combat démarre déjà sur les
  6 patterns les plus durs que j'ai pu concevoir (`CombatDifficulty.BASE_HARDEST_PATTERNS`).
  Chaque utilisation supplémentaire ajoute encore une vague en plus (jouée
  en superposition d'un pattern existant), et fait grimper vitesse,
  densité de projectiles et dégâts par contact de façon volontairement
  agressive — à utiliser seulement quand tu n'as plus le choix.
- **Contrôles** : ZQSD/WASD pour déplacer le cœur dans l'arène.

## Compilation automatique sur GitHub (CI)

`.github/workflows/gradle.yml` compile le mod à chaque push sur `main`.
Le `.jar` compilé est mis à disposition dans l'onglet **Actions → (le
run correspondant) → Artifacts** de ce dépôt, sous le nom
`derniere-chance-mod`. Aucun environnement Java local n'est nécessaire.

En cas d'échec de build, l'onglet Actions affiche directement le détail
de l'erreur Gradle en annotation sur l'étape concernée.

`.github/workflows/gradle-publish.yml` publie en plus le jar sur GitHub
Packages, mais uniquement quand tu crées une **release** GitHub (pas à
chaque push).

## Compilation en local (optionnel)

Si tu veux compiler toi-même : JDK 17, puis `./gradlew build` à la racine
du projet (le wrapper Gradle 8.7 est déjà inclus). Le `.jar` apparaît
dans `build/libs/`. Place-le dans le dossier `mods/` de ton instance
Fabric 1.20.1, avec **Fabric API** installée aussi (dépendance
obligatoire).

## Structure du projet

```
src/main/java/com/derniere/chance/
├── DerniereChanceMod.java        # init commune, réseau, résolution victoire/défaite
├── DerniereChanceClient.java     # init client, réception sync difficulté
├── ClientCombatData.java         # cache client (tentatives, mort définitive)
├── LastChanceState.java          # sauvegarde persistante par joueur
├── gui/
│   ├── CombatDifficulty.java     # calcul de la difficulté selon le nb de tentatives
│   ├── WavePatterns.java         # les 6 patterns de base + combinaison
│   ├── Projectile.java
│   └── LastChanceCombatScreen.java  # l'écran de combat lui-même
└── mixin/
    ├── ServerPlayNetworkHandlerMixin.java  # bloque le respawn si mort définitive
    └── client/DeathScreenMixin.java        # ajoute le 3e bouton
```
