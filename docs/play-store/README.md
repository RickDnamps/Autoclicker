# Publier AutoClicker sur le Google Play Store

Ce dossier contient tout ce qu'il faut pour la fiche et les formulaires de la Play Console.
Les textes ci-dessous sont prêts à copier-coller.

| Fichier | Où l'utiliser |
| --- | --- |
| `icon-512.png` | Fiche Play Store → Icône de l'application (512 × 512) |
| `feature-graphic-1024x500.png` | Fiche Play Store → Image de présentation (1024 × 500) |
| `../privacy-policy.html` | Politique de confidentialité (URL publique, voir étape 2) |

---

## Étape 1 – Créer la clé de signature (une seule fois)

Le Play Store refuse les applis signées avec une clé de test. Il faut une clé privée,
**à garder précieusement** (sans elle, impossible de publier des mises à jour).

Sur un ordinateur avec Java installé :

```bash
keytool -genkeypair -v -keystore autoclicker-release.jks -alias autoclicker \
  -keyalg RSA -keysize 2048 -validity 10000
# Choisis un mot de passe solide et note-le.

# Encode la clé en texte pour GitHub :
base64 -w0 autoclicker-release.jks > keystore-base64.txt      # Linux
base64 -i autoclicker-release.jks -o keystore-base64.txt       # macOS
# Windows (PowerShell) :
# [Convert]::ToBase64String([IO.File]::ReadAllBytes("autoclicker-release.jks")) > keystore-base64.txt
```

Puis sur GitHub : **dépôt → Settings → Secrets and variables → Actions → New repository secret**,
crée ces 4 secrets :

| Nom | Valeur |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | tout le contenu de `keystore-base64.txt` |
| `SIGNING_STORE_PASSWORD` | le mot de passe choisi |
| `SIGNING_KEY_ALIAS` | `autoclicker` |
| `SIGNING_KEY_PASSWORD` | le mot de passe choisi (le même par défaut) |

Ensuite, relance l'action **Build APK** (onglet *Actions → Build APK → Run workflow*).
Elle produit :
- `AutoClicker-play-store-aab` → le fichier **`AutoClicker.aab` à envoyer sur la Play Console** ;
- `AutoClicker-apk` → l'APK, signé avec la même clé (les mises à jour s'installent par-dessus,
  plus besoin de désinstaller).

> ⚠️ Ne mets jamais le fichier `.jks` ni le mot de passe dans le dépôt. Garde une copie de la clé
> en lieu sûr (clé USB, gestionnaire de mots de passe).

## Étape 2 – Publier la politique de confidentialité

**Dépôt GitHub → Settings → Pages → Source : « Deploy from a branch »**, branche par défaut,
dossier **`/docs`** → Save. Après une minute, la page est en ligne ici :

**https://rickdnamps.github.io/Autoclicker/privacy-policy.html**

(C'est aussi l'adresse ouverte par le bouton « Politique de confidentialité » dans l'appli.)
Le dépôt doit être public pour que GitHub Pages fonctionne avec un compte gratuit.

## Étape 3 – Compte développeur

1. https://play.google.com/console → créer un compte **personnel** (25 $ US, une seule fois).
2. Vérification d'identité (pièce d'identité, téléphone).
3. **Créer une application** : nom `AutoClicker`, langue par défaut Français, *Application*,
   *Gratuite*.

## Étape 4 – Fiche Play Store

**Nom de l'application** (30 caractères max) :
```
AutoClicker – Clic automatique
```

**Description courte** (80 caractères max) :
```
Clics automatiques rapides et précis, multi-points. Sans pub et sans pistage.
```

**Description complète** :
```
AutoClicker automatise les appuis répétitifs à l'écran : placez un ou plusieurs points, réglez la vitesse, appuyez sur ▶ et laissez faire.

✔ 100 % gratuit, sans publicité, sans achat intégré
✔ Aucune donnée collectée, aucun accès à Internet

FONCTIONS
• Un seul point ou plusieurs points, cliqués l'un après l'autre ou tous en même temps
• Intervalle réglable à la milliseconde et durée d'appui réglable
• Intervalle aléatoire et position aléatoire pour des appuis plus naturels
• Arrêt automatique après un nombre de cycles ou une durée
• Barre de commande flottante et déplaçable : démarrer / arrêter, ajouter ou retirer des points, réglages
• Taille des points et de la barre réglable
• Les positions des points sont mémorisées
• Assistant de configuration guidé, étape par étape

UTILISATION DE L'ACCESSIBILITÉ
AutoClicker utilise l'API Service d'accessibilité d'Android uniquement pour simuler des appuis aux endroits de l'écran que vous choisissez, lorsque vous appuyez sur ▶, et pour afficher la barre flottante. L'application ne lit pas le contenu de l'écran, n'enregistre pas ce que vous tapez et ne collecte aucune donnée. Le service n'est activé qu'avec votre accord et peut être désactivé à tout moment dans les paramètres d'accessibilité.
```

**Catégorie** : Outils · **Tags** : Automatisation, Productivité
**Coordonnées** : ton adresse e-mail (obligatoire).
**Captures d'écran** : au moins 2 captures du téléphone (format portrait). Idéalement :
1. l'écran principal de l'appli ;
2. la barre flottante + des points au-dessus d'une appli ;
3. l'assistant de configuration.

### Version anglaise (facultatif, pour être visible dans plus de pays)

```
Name: AutoClicker – Auto Tap
Short: Fast, precise auto clicker with multiple points. No ads, no tracking.
```
```
AutoClicker automates repetitive taps: place one or more targets, set the speed, press ▶ and you're done.

✔ 100% free, no ads, no in-app purchases
✔ No data collected, no Internet access

FEATURES
• One or several targets, tapped one after another or all at once
• Interval adjustable to the millisecond and adjustable press duration
• Random interval and random position for more natural taps
• Auto-stop after a number of cycles or a duration
• Movable floating control bar: start / stop, add or remove targets, settings
• Adjustable target and bar size
• Target positions are remembered
• Step-by-step guided setup

ACCESSIBILITY SERVICE USE
AutoClicker uses Android's Accessibility Service API only to perform taps at the screen positions you choose, when you press ▶, and to display the floating bar. The app does not read screen content, does not record what you type and collects no data. The service is only enabled with your consent and can be turned off at any time in the accessibility settings.
```

> Ne présente pas l'appli comme un outil de triche pour les jeux : Google refuse plus souvent ces
> fiches. Parle d'automatisation de tâches répétitives.

## Étape 5 – Contenu de l'application (menu « Règles et programmes → Contenu de l'appli »)

**Politique de confidentialité** : `https://rickdnamps.github.io/Autoclicker/privacy-policy.html`

**Annonces** : Non, l'application ne contient pas d'annonces.

**Accès aux applications** : Toutes les fonctionnalités sont disponibles sans restriction
(pas de compte, pas de connexion).

**Classification du contenu** : catégorie *Utilitaire, productivité, communication ou autre* →
répondre **Non** à toutes les questions (violence, contenu sexuel, jeux d'argent, etc.).

**Public cible** : **18 ans et plus** uniquement (évite les règles « Familles » ; l'appli ne vise
pas les enfants).

**Sécurité des données** :
- L'application collecte-t-elle ou partage-t-elle des données utilisateur ? → **Non**
- (Les réglages restent sur l'appareil et ne sont jamais transmis : ce n'est pas une « collecte ».)

**Applications d'actualités / santé / finance / gouvernement** : Non.

**API d'accessibilité** (formulaire *Déclaration relative à l'API AccessibilityService*) :

- L'appli est-elle un outil d'accessibilité (`isAccessibilityTool`) ? → **Non**
- Fonctionnalité principale qui utilise l'API :
```
AutoClicker est un outil d'automatisation de clics choisi et configuré par l'utilisateur. Le service d'accessibilité est utilisé uniquement pour :
1) exécuter des appuis (dispatchGesture) aux positions de l'écran que l'utilisateur a placées lui-même, uniquement après qu'il a appuyé sur le bouton ▶ de la barre flottante, et jusqu'à ce qu'il appuie sur ■ ou que la condition d'arrêt qu'il a choisie soit atteinte ;
2) afficher la barre de commande flottante et les points de clic (TYPE_ACCESSIBILITY_OVERLAY).
L'application ne lit pas le contenu des fenêtres (canRetrieveWindowContent=false), n'écoute aucun événement à des fins de collecte, ne prend aucune décision autonome, ne collecte et ne transmet aucune donnée (aucune permission Internet). Avant d'ouvrir le réglage d'accessibilité, l'application affiche un écran d'information clair et demande le consentement explicite de l'utilisateur.
```
- Vidéo : lien YouTube **non répertorié** (voir le scénario ci-dessous).

### Scénario de la vidéo (1 à 2 minutes, enregistrement d'écran du téléphone)

1. Ouvrir AutoClicker (fraîchement installé) : l'assistant de configuration s'affiche.
2. Appuyer sur « Ouvrir le réglage » → **montrer l'écran « Utilisation de l'accessibilité »**
   et appuyer sur « J'accepte ».
3. Activer le service AutoClicker dans les paramètres d'accessibilité, revenir :
   « Tout est prêt ! ».
4. Sur l'écran principal, régler l'intervalle, puis « Afficher le panneau flottant ».
5. Dans une autre appli (par ex. une appli de dessin ou un compteur de clics), placer le point,
   appuyer sur ▶ : on voit les appuis. Appuyer sur ■ pour arrêter, puis ✕ pour fermer.

## Étape 6 – Test fermé (obligatoire pour un compte personnel)

1. **Tests → Test fermé → Créer un canal** (ex. « Testeurs »).
2. Ajouter **au moins 12 testeurs** (adresses Gmail) — famille, amis.
3. **Créer une version** → importer `AutoClicker.aab` → notes de version :
   ```
   Première version : clics automatiques multi-points, intervalle et position aléatoires, arrêt automatique, assistant de configuration.
   ```
4. Envoyer en examen. Une fois approuvée, chaque testeur ouvre le lien d'inscription et installe
   l'appli.
5. Les 12 testeurs doivent rester inscrits **14 jours d'affilée**. Ensuite, dans le *Tableau de
   bord*, demande l'**accès à la production** (quelques questions sur le test) puis publie.

## Mises à jour

Avant chaque nouvelle version, augmente `versionCode` (et `versionName`) dans
`app/build.gradle.kts`, laisse l'action GitHub construire le `.aab`, puis importe-le dans une
nouvelle version de la Play Console.
