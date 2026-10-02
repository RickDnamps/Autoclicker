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
- **Assistant de configuration** guidé, étape par étape, avec ouverture directe des bons menus.

La vitesse maximale réelle dépend du téléphone (souvent 50 à 100 clics/seconde avec un intervalle
de 1 à 10 ms).

## Installer

1. Téléchargez `AutoClicker.apk` depuis la page **Releases** du dépôt (ou l'artefact
   `AutoClicker-apk` de la dernière exécution de l'action **Build APK**).
2. Ouvrez le fichier sur le téléphone et autorisez l'installation depuis cette source.
3. Ouvrez AutoClicker : un **assistant de configuration** s'affiche tout seul tant que le service
   n'est pas activé. Chaque étape a un bouton qui ouvre directement le bon menu, y compris le
   déblocage « paramètre restreint » d'Android 13+ (*Infos de l'appli → ⋮ → Autoriser les
   paramètres restreints*).
4. Appuyez sur **Afficher le panneau flottant**, ouvrez votre jeu, placez le point et appuyez sur ▶.

## Fonctionnement

L'application utilise un *service d'accessibilité* (`AccessibilityService.dispatchGesture`),
la seule méthode officielle sans root pour simuler des appuis dans d'autres applis. Le service
ne lit aucun contenu de l'écran (`canRetrieveWindowContent="false"`) et l'application n'a
aucune permission Internet.

> ⚠️ Certains jeux en ligne interdisent les auto-clickers dans leurs conditions d'utilisation :
> à utiliser à vos risques.

## Publier sur le Play Store

Tout est prêt dans [`docs/play-store/`](docs/play-store/README.md) : étapes, textes de la fiche,
réponses aux formulaires (accessibilité, sécurité des données…), icône 512 × 512 et image de
présentation. La politique de confidentialité est dans [`docs/privacy-policy.html`](docs/privacy-policy.html).

## Compiler

```bash
./gradlew assembleRelease   # APK : app/build/outputs/apk/release/app-release.apk
```

Requiert le SDK Android (compileSdk 36) et JDK 17. `./gradlew bundleRelease` produit le `.aab`
pour le Play Store. Sans clé privée (secrets `SIGNING_*`, voir le guide Play Store), les builds
release sont signés avec la clé de debug : installables directement, mais refusés par le Play Store.
