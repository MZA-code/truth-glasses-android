# Truth Glasses — Android

Appli Android open source et parodique : elle recouvre les pubs affichées dans les autres applis (Instagram, Reddit, YouTube…) par des messages ironiques sur la consommation (« OBÉIS », « TU N'EN AS PAS BESOIN », « C'EST TOI LE PRODUIT »…). C'est la petite sœur de l'extension Chrome « Lunettes de vérité / Truth Glasses ».

La spec complète est dans `docs/SPEC.md`. Lis-la avant toute tâche importante.

## Contraintes non négociables

- **Distribution GitHub uniquement** (APK dans les Releases). Pas de Play Store, donc aucune contrainte de politique Play à respecter, mais l'appli doit rester honnête et transparente.
- **100 % local** : PAS de permission `INTERNET`, aucune télémétrie, aucune lib d'analytics ou de pub. Rien ne quitte le téléphone.
- **On ne bloque rien, on ne modifie rien** : l'appli dessine un calque par-dessus l'écran. Elle ne clique pas, ne scrolle pas, ne lit pas les champs de saisie et ne stocke aucun contenu lu à l'écran.
- **Service d'accessibilité limité aux applis cibles** : `packageNames` restreint dynamiquement aux applis activées par l'utilisateur.
- **Pas de dépendance tierce** en dehors d'AndroidX et Jetpack Compose (et des libs de test).

## Stack

- Kotlin, Gradle Kotlin DSL avec version catalog (`gradle/libs.versions.toml`).
- Dernières versions stables d'AGP, de Kotlin et de Compose : vérifie-les avant de les fixer, ne les invente pas.
- `minSdk 28`, `targetSdk` et `compileSdk` = dernier niveau d'API stable.
- UI de l'appli en Jetpack Compose (Material 3). Calque dessiné avec une `View` custom (Canvas), pas en Compose.
- Préférences : `SharedPreferences` (suffisant, et facile à écouter depuis le service).

## Commandes

```bash
./gradlew assembleDebug          # APK debug -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # tests unitaires
./gradlew lint                   # lint Android
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s TG                 # logs de l'appli (tag unique "TG")
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml   # arbre de l'écran courant
```

## Conventions

- Package : `io.github.mzacode.truthglasses`.
- Identifiants en anglais, commentaires en français.
- Toute logique de détection ou de décodage passe par l'interface `UiNode` (voir la spec) pour rester testable sans appareil.
- Aucun texte utilisateur en dur : tout dans `res/values` (EN, défaut) et `res/values-fr` (FR). Android choisit la langue tout seul.
- Logs : tag `TG`, uniquement en build debug ou quand le mode debug est activé. Ne jamais logger le contenu lu à l'écran en release.
- Petits commits, un jalon de `docs/SPEC.md` à la fois. Vérifie que `assembleDebug` et les tests passent avant de dire qu'une étape est finie.

## À ne pas faire

- Ne pas ajouter la permission `INTERNET`, `SYSTEM_ALERT_WINDOW` ou `QUERY_ALL_PACKAGES`. Le calque utilise `TYPE_ACCESSIBILITY_OVERLAY`, qui n'a besoin d'aucune permission en plus.
- Ne pas utiliser `performAction` / `dispatchGesture` : l'appli ne fait qu'observer et dessiner.
- Ne pas deviner les libellés « Sponsorisé » des applis réelles : les vérifier avec un dump (voir la spec, section Détection) et les noter dans `AppProfiles.kt` avec la date de vérification.
- Ne jamais committer de keystore ni de mot de passe de signature.
