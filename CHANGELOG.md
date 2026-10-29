# Journal des modifications

Toutes les versions notables de Max player sont documentées dans ce fichier.
Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).

## [1.0.0] — 2026-10-29

### Ajouté

- Lecture audio locale via Media3, file d’attente, lecture aléatoire et répétition.
- Bibliothèque Titres, Albums, Artistes, Dossiers, Playlists et Vidéos.
- Synchronisation automatique de la bibliothèque avec les changements MediaStore.
- Mini-player agrandi, gestes vers le haut pour ouvrir le lecteur et vers le bas
  pour revenir à la page précédente.
- Lecteur audio avec pochettes, barre de lecture fine, paroles synchronisées,
  paroles flottantes, égaliseur, minuterie de sommeil et vitesse de lecture.
- Lecteur vidéo avec Picture-in-Picture, pistes audio et sous-titres.
- Favoris, historique, titres les plus écoutés, recherche et tri.
- Widget d’écran d’accueil.
- Thèmes Aurora, Sunset, Ocean, Forest, Candy et Mono.
- Sauvegarde et restauration locales des playlists et réglages.
- Nouvelle identité visuelle Max player et icône dégradé cyan → violet.

### Technique

- Kotlin 2.0.10, Jetpack Compose, Media3 1.4.1, Room, DataStore, Coil et Palette.
- R8 et réduction des ressources activés pour les builds release.
- CI GitHub Actions pour le build, le lint et les tests.
