# Contribuer à Max player

Merci de votre intérêt pour le projet.

## Signaler un problème

Ouvrez une issue en précisant :

- la version de l’application et d’Android ;
- le modèle d’appareil ;
- les étapes exactes de reproduction ;
- le comportement attendu et le comportement observé ;
- les logs ou captures d’écran utiles.

## Proposer une modification

1. Créez une branche depuis `main` :
   `git switch -c feat/ma-fonctionnalite`
2. Gardez les changements ciblés et respectez le style Kotlin/Compose existant.
3. Vérifiez le build :

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

4. Décrivez clairement la modification dans la pull request.

## Règles

- Pas de publicité, de suivi analytique, de Firebase ou de service de facturation.
- Aucune donnée utilisateur ne doit quitter l’appareil.
- Les nouvelles chaînes visibles doivent être ajoutées dans `values/strings.xml`
  et `values-fr/strings.xml`.
- Ne commitez jamais de keystore, de mot de passe ou de fichier `local.properties`.

## Licence

En contribuant, vous acceptez que vos modifications soient distribuées sous la
licence MIT du projet.
