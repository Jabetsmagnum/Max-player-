<p align="center">
  <img src="art/icon-512.png" width="120" alt="Icône Max player" />
</p>

<h1 align="center">Max player</h1>

<p align="center">
  Lecteur de musique et de vidéos local, rapide et sans compte.<br />
  Kotlin · Jetpack Compose · Media3 · Room · DataStore
</p>

---

## Présentation

**Max player** est un lecteur Android natif qui lit les fichiers audio et vidéo
déjà présents sur l’appareil. Aucun fichier n’est envoyé sur Internet, aucune
publicité, aucun compte et aucun service de facturation ne sont utilisés.

L’interface est pensée pour un usage quotidien : démarrage direct sur **Titres**,
navigation par balayage entre les sections, mini-player toujours accessible et
lecteur plein écran ouvert d’un glissement vers le haut.

## Fonctionnalités

- **Bibliothèque complète** : Titres, Albums, Artistes, Dossiers, Playlists et
  Vidéos dans un carrousel horizontal.
- **Synchronisation automatique** : les ajouts, suppressions et modifications de
  fichiers sont détectés via un `ContentObserver` MediaStore, avec rafraîchissement
  débouncé, sérialisé et relancé au retour au premier plan.
- **Lecture audio** : file d’attente, lecture aléatoire, répétition, vitesse,
  silence ignoré, gapless, fondu enchaîné et reprise après branchement d’un casque.
- **Gestes** : balayage gauche/droite pour changer de titre, balayage haut sur le
  mini-player pour ouvrir le lecteur, balayage bas pour revenir.
- **Paroles** : fichiers LRC/SRT, paroles synchronisées, import local et overlay
  flottant au-dessus des autres applications.
- **Égaliseur** : presets, bandes, bass boost, virtualizer et réverbération.
- **Vidéo** : lecture, Picture-in-Picture, pistes audio, sous-titres et gestes de
  volume/luminosité/recherche.
- **Organisation** : favoris, historique, titres les plus écoutés, recherche,
  tri, fichiers et dossiers masqués.
- **Personnalisation** : thèmes Aurora, Sunset, Ocean, Forest, Candy et Mono,
  couleur d’accentuation et mode sombre/clair.
- **Widget** : mini-lecteur RemoteViews sur l’écran d’accueil.
- **Sauvegarde** : export et restauration locales des playlists et réglages.

## Captures et identité visuelle

L’icône officielle combine un triangle « play » arrondi avec un dégradé cyan →
violet sur fond blanc. Elle est utilisée comme icône adaptative, icône ronde,
icône monochrome Android 13+ et logo d’en-tête dans l’application.

Le fichier source 512 × 512 est disponible dans [`art/icon-512.png`](art/icon-512.png).

## Stack technique

| Composant | Version |
| --- | --- |
| Android Gradle Plugin | 8.6.0 |
| Gradle | 8.7 |
| Kotlin | 2.0.10 |
| Jetpack Compose | 1.7.0-rc01 |
| Material 3 | 1.3.0-rc01 |
| Media3 | 1.4.1 |
| Room / KSP | 2.6.1 |
| DataStore | 1.1.1 |
| Coil | 2.6.0 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |

## Prérequis

- JDK 17
- Android SDK Platform 35 et Build-Tools 35
- Un appareil ou un émulateur Android 8.0 (API 26) minimum

Le chemin du SDK peut être défini dans `local.properties` :

```properties
sdk.dir=C\:\\Android\\Sdk
```

## Compiler le projet

```bash
# APK debug
./gradlew assembleDebug

# Vérifications
./gradlew lintDebug testDebugUnitTest

# APK release minifié
./gradlew assembleRelease

# Bundle Play Store
./gradlew bundleRelease
```

Sur Windows :

```powershell
.\gradlew.bat assembleDebug
```

Sorties :

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk` (signé si `keystore.properties` est configuré)
- `app/build/outputs/bundle/release/app-release.aab`

Sans `keystore.properties`, Gradle produit `app-release-unsigned.apk` et un AAB
non signé. Ces fichiers ne sont pas installables ou publiables en l’état : ils
servent uniquement à vérifier la compilation release et R8.

## Signature de production

1. Créer le dossier `keystore/` et générer une clé (à conserver précieusement) :

```bash
keytool -genkeypair -v \
  -keystore keystore/max-player-release.jks \
  -alias max-player \
  -keyalg RSA -keysize 4096 -validity 10000
```

2. Copier `keystore.properties.example` vers `keystore.properties` et renseigner
   les mots de passe :

```properties
storeFile=keystore/max-player-release.jks
storePassword=...
keyAlias=max-player
keyPassword=...
```

3. Lancer `./gradlew assembleRelease` ou `./gradlew bundleRelease`.

`keystore.properties`, les fichiers `.jks` et le dossier `keystore/` sont exclus
par `.gitignore`. Ne les ajoutez jamais au dépôt.

## Intégration continue

Le workflow [`.github/workflows/android.yml`](.github/workflows/android.yml)
compile l’APK debug, exécute le lint et les tests à chaque push ou pull request
sur `main`, puis publie l’APK en artefact.

Le workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) se
déclenche sur un tag `v*` et publie l’APK et l’AAB signés dans une release GitHub.
Configurez ces secrets dans **Settings → Secrets and variables → Actions** :

| Secret | Contenu |
| --- | --- |
| `SIGNING_KEY` | Keystore `.jks` encodé en base64 |
| `KEYSTORE_PASSWORD` | Mot de passe du keystore |
| `KEY_ALIAS` | Alias de la clé |
| `KEY_PASSWORD` | Mot de passe de la clé |

Exemple PowerShell pour encoder le keystore :

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore/max-player-release.jks"))
```

Publier une version :

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Structure du projet

```text
app/src/main/kotlin/com/larkplaye/player/
├── core/          # MediaStore, préférences, base Room, sauvegarde, paroles
├── playback/      # MediaSessionService, MediaController, état de lecture
├── ui/            # Compose : bibliothèque, lecteur, vidéos, réglages, thèmes
├── widget/        # Widget d’écran d’accueil
├── AppContainer.kt
├── LarkPlayApplication.kt
└── MainActivity.kt
```

> L’identifiant technique `com.larkplaye.player` est conservé pour assurer la
> continuité des mises à jour. Le nom affiché, l’icône et l’interface utilisent
> l’identité **Max player**.

## Confidentialité

Max player ne collecte aucune donnée et ne dispose d’aucun service réseau pour
envoyer la bibliothèque. La sauvegarde cloud Android et la migration automatique
d’appareil à appareil sont désactivées ; utilisez l’export/import manuel. La
politique complète est disponible dans [`PRIVACY.md`](PRIVACY.md).

## Contribuer

Les contributions sont bienvenues. Consultez [`CONTRIBUTING.md`](CONTRIBUTING.md)
et ouvrez une issue ou une pull request. Avant de proposer du code, lancez :

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

## Licence

Distribué sous licence MIT. Voir [`LICENSE`](LICENSE).

## Avertissement

Max player est un projet indépendant. Il n’est ni affilié, ni associé, ni
approuvé par LarkPlayer ou ses éditeurs. Les marques et composants tiers
appartiennent à leurs propriétaires respectifs.
