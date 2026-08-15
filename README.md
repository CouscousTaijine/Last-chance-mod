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

## Compilation automatique sur GitHub (CI)

Le projet inclut `.github/workflows/build.yml` : à chaque push sur `main`
(ou pull request), GitHub Actions compile le mod tout seul et met le
`.jar` résultant en téléchargement dans l'onglet **Actions → (le run) →
Artifacts** de ton dépôt. Cette CI ne dépend pas du wrapper Gradle : elle
provisionne Gradle 8.7 elle-même, donc ça fonctionne même si
`gradle-wrapper.jar` n'est pas commité.

## Compilation en local

Le dossier `gradle/wrapper/` et les scripts `gradlew` / `gradlew.bat` sont
inclus, **sauf** le fichier binaire `gradle-wrapper.jar` (je ne peux pas
générer de binaire depuis cet environnement). Deux options :

- **La plus simple** : installe Gradle une fois (`gradle` disponible dans
  le PATH), puis à la racine du projet lance `gradle wrapper` — ça génère
  le `gradle-wrapper.jar` manquant. Ensuite `./gradlew build` fonctionnera
  normalement, y compris sans Gradle installé globalement.
- **Alternative** : installe un **JDK 17** puis un **Gradle 8.7** complet,
  et lance directement `gradle build` (sans passer par le wrapper).

Le `.jar` compilé apparaît dans `build/libs/`. Place-le ensuite dans le
dossier `mods/` de ton instance Fabric (avec **Fabric API** installée
aussi, c'est une dépendance obligatoire).

## Pousser sur GitHub

Je n'ai pas d'accès direct à ton dépôt GitHub (aucun connecteur GitHub
n'est disponible dans cette conversation). Pour l'y mettre toi-même :

```bash
cd derniere-chance
git init
git add .
git commit -m "Mod Dernière Chance"
git branch -M main
git remote add origin https://github.com/CouscousTaijine/Last-chance-mod.git
git push -u origin main
```

## Points à vérifier après compilation (pas testé en conditions réelles)

Je n'ai pas d'environnement Minecraft/Gradle ici pour compiler et tester
ce mod en conditions réelles — voici les points les plus susceptibles de
nécessiter un petit ajustement selon ta version exacte des mappings Yarn :

- **`DeathScreenMixin`** cible la méthode `initWidgets` de `DeathScreen`.
  Si le build échoue avec une erreur "method not found", ouvre
  `DeathScreen` via *Yarn/genSources* dans ton IDE et vérifie le nom exact
  de la méthode qui construit les boutons (`init` selon les versions), puis
  ajuste `method = "..."` dans le mixin.
- **`ServerPlayNetworkHandlerMixin`** cible `onClientStatus`. Même remarque
  si le nom diffère légèrement dans ta version de mappings.
- **`PlayerManager#respawnPlayer`** (appelé dans `DerniereChanceMod`) : la
  signature exacte peut varier légèrement ; vérifie-la dans les sources
  générées si erreur de compilation.

Tout le reste (réseau, sauvegarde de la difficulté, moteur du combat,
patterns, rendu) est du code Fabric standard et ne dépend pas de mappings
fragiles.

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
