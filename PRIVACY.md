# Politique de confidentialité — Max player

Dernière mise à jour : 29 octobre 2026

Max player est un lecteur de musique et de vidéos local. L’application ne
collecte, ne transmet et ne vend aucune donnée personnelle.

## Données traitées

- **Fichiers multimédias** : Max player lit les fichiers audio et vidéo présents
  sur l’appareil via MediaStore. Les fichiers ne sont jamais envoyés sur un
  serveur.
- **Bibliothèque** : les titres, albums, artistes, dossiers, playlists, favoris,
  historiques et paroles sont stockés uniquement dans la base de données locale
  de l’application.
- **Préférences** : les réglages (thème, tri, égaliseur, vitesse, etc.) sont
  enregistrés localement avec DataStore.

## Permissions

- `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` : nécessaire pour afficher la
  bibliothèque et lire les médias.
- `POST_NOTIFICATIONS` : affiche les contrôles de lecture.
- `SYSTEM_ALERT_WINDOW` : uniquement si vous activez les paroles flottantes.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` : proposé pour éviter les coupures de
  lecture en arrière-plan.

Ces permissions sont utilisées exclusivement pour les fonctionnalités décrites
ci-dessus.

## Réseau et services tiers

Max player ne contient ni publicité, ni facturation, ni Firebase, ni Google
Drive, ni outil d’analyse. L’application ne dispose d’aucun accès réseau pour
envoyer vos données.

## Sauvegarde

La fonction d’export crée un fichier JSON à l’emplacement que vous choisissez.
Vous restez seul responsable de ce fichier et de sa destination.

## Contact

Pour toute question, ouvrez une issue sur le dépôt GitHub du projet.
