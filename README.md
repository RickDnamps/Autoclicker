# AutoClicker (Android)

Auto-clicker Android **sans publicité, sans pistage, sans achats intégrés**.
Il simule des appuis rapides à l'écran, y compris dans les jeux.

## Fonctions

- **Un ou plusieurs points** : ajoutez autant de cibles que vous voulez et déplacez-les du doigt.
- Multi-points **un après l'autre** ou **tous en même temps**.
- **Intervalle** réglable à la milliseconde et **durée d'appui** réglable.
- **Intervalle aléatoire** (± ms) et **position aléatoire** (± pixels) pour des clics plus « humains ».
- **Arrêt automatique** : jamais, après N cycles ou après N secondes.
- **Taille des points** et **taille de la barre de commande** réglables.
- Barre flottante déplaçable : ▶/■ démarrer-arrêter, ＋/－ points, ⚙ réglages, ✕ fermer.
- Les positions des points sont mémorisées.

La vitesse maximale réelle dépend du téléphone (souvent 50 à 100 clics/seconde avec un intervalle
de 1 à 10 ms).

## Installer

1. Téléchargez `AutoClicker.apk` depuis la page **Releases** du dépôt (ou l'artefact
   `AutoClicker-apk` de la dernière exécution de l'action **Build APK**).
2. Ouvrez le fichier sur le téléphone et autorisez l'installation depuis cette source.
3. Ouvrez AutoClicker → **Activer dans les paramètres** → activez le service *AutoClicker*.
   - Android 13+ : si l'option est grisée (« paramètre restreint »), allez dans
     *Paramètres → Applications → AutoClicker → ⋮ → Autoriser les paramètres restreints*, puis réessayez.
4. Appuyez sur **Afficher le panneau flottant**, ouvrez votre jeu, placez le point et appuyez sur ▶.

## Fonctionnement

L'application utilise un *service d'accessibilité* (`AccessibilityService.dispatchGesture`),
la seule méthode officielle sans root pour simuler des appuis dans d'autres applis. Le service
ne lit aucun contenu de l'écran (`canRetrieveWindowContent="false"`) et l'application n'a
aucune permission Internet.

> ⚠️ Certains jeux en ligne interdisent les auto-clickers dans leurs conditions d'utilisation :
> à utiliser à vos risques.

## Compiler

```bash
./gradlew assembleRelease   # APK : app/build/outputs/apk/release/app-release.apk
```

Requiert le SDK Android (compileSdk 35) et JDK 17. Le build release est signé avec la clé de
debug pour pouvoir être installé directement.
