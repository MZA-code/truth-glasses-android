# Kit de démarrage — Truth Glasses Android

Ce kit sert à lancer le prototype avec Claude Code.

| Fichier | Rôle |
|---|---|
| `CLAUDE.md` | Les règles du projet, lues automatiquement par Claude Code à chaque session |
| `docs/SPEC.md` | La spécification complète : fonctionnement, architecture, détection, calque, CI, jalons |
| `res-starter/values/messages.xml` | Les messages ironiques en anglais (langue par défaut) |
| `res-starter/values-fr/messages.xml` | Les mêmes en français |
| `KICKOFF.md` | Ce fichier : la mise en place et les prompts à copier-coller |

## Ce qu'il te faut

- Android Studio (pour le SDK, l'émulateur et `adb`), ou au moins le SDK en ligne de commande.
- Un téléphone Android avec le débogage USB activé, ou un émulateur avec Instagram, Reddit ou YouTube installés pour le jalon M5.
- Un dépôt GitHub vide, par exemple `truth-glasses-android`.

## Mise en place

```bash
mkdir truth-glasses-android && cd truth-glasses-android
git init
# copier ici CLAUDE.md, docs/ et res-starter/ depuis le kit
# remplacer <pseudo> par ton pseudo GitHub dans CLAUDE.md et docs/SPEC.md
claude
```

## Les prompts, jalon par jalon

Lance un jalon à la fois et teste avant de passer au suivant. Les jalons sont décrits dans `docs/SPEC.md`, section 8.

### M0 — Squelette et CI

> Lis CLAUDE.md et docs/SPEC.md en entier. Ensuite, réalise uniquement le jalon M0 : crée le projet Android (Kotlin, Gradle KTS, version catalog, Compose, minSdk 28, dernières versions stables que tu vérifies), copie res-starter/ dans app/src/main/res/, ajoute un .gitignore Android (avec *.jks), la licence MIT et le workflow GitHub Actions qui compile l'APK debug et le téléverse comme artefact. L'écran d'accueil peut être un simple texte pour l'instant. Vérifie que `./gradlew assembleDebug` passe, puis résume ce que tu as créé.

### M1 — Service et calque

> Réalise le jalon M1 de docs/SPEC.md : le GlassesService (config XML et description honnête comprises), l'OverlayController avec TYPE_ACCESSIBILITY_OVERLAY et la CoverView qui affiche un bloc de test « OBÉIS » en haut de l'écran, avec le décalage getLocationOnScreen. Ajoute le bouton « Ouvrir les réglages d'accessibilité » sur l'accueil. Dis-moi comment tester sur l'émulateur.

### M2 — Détection dans le bac à sable

> Réalise le jalon M2 : l'interface UiNode et son implémentation, AppProfiles avec le profil du bac à sable, AdDetector selon l'algorithme de la section 4.3, la SandboxActivity (section 4.7), le décalage immédiat au scroll et la planification des scans (section 4.1). Écris les tests unitaires d'AdDetector sur de faux arbres. Pour l'instant, chaque bloc affiche « OBÉIS ».

### M3 — Les messages

> Réalise le jalon M3 : Themes et MessageDecoder (section 4.4, avec le choix stable par hash et le format étroit), TextFitter (section 4.5, dichotomie et cache), la police Anton avec son fichier de licence OFL. Écris les tests de MessageDecoder et de TextFitter. Vérifie dans le bac à sable que chaque thème s'affiche, en FR et en EN.

### M4 — Réglages et tuile

> Réalise le jalon M4 : l'accueil complet de la section 2 (état, guide en 3 étapes avec les paramètres restreints, interrupteur ON/OFF, applis cibles installées, mode debug, ligne de transparence), Prefs avec l'écoute dans le service, GlassesTileService (section 4.9) et le mode debug (section 4.6).

### M5 — Les vraies applis

Pour ce jalon, ouvre l'appli sur un post sponsorisé, active le mode debug et récupère les logs (`adb logcat -s TG > dump.txt`) ou un dump (`adb shell uiautomator dump`). Puis :

> Voici le dump d'un écran Instagram avec un post sponsorisé : [colle ou joins le fichier]. Trouve le libellé exact et vérifie que la remontée vers le conteneur scrollable fonctionne sur cet arbre. Mets à jour le profil Instagram dans AppProfiles avec la date de vérification, et ajoute un test unitaire construit à partir de ce dump (anonymisé).

Refais la même chose pour Reddit et YouTube.

### M6 — Publication

> Réalise le jalon M6 : le workflow de release signée sur tag v* (section 6), le README.md en anglais et le README.fr.md en français (section 10), avec un emplacement pour la bannière, le GIF et les captures, et docs/TESTING.md avec la liste de tests manuels. Explique-moi comment créer le keystore et remplir les secrets GitHub.

## Conseils

- Si Claude Code part trop loin, rappelle-lui « un seul jalon à la fois, voir CLAUDE.md ».
- Le **bac à sable** est ton meilleur ami : tant qu'il ne marche pas parfaitement, inutile d'aller sur Instagram.
- Après une mise à jour d'une appli qui casse la détection : mode debug, nouveau dump, et prompt M5 pour cette appli.
- Pour le GIF du README : `adb shell screenrecord /sdcard/demo.mp4`, puis `ffmpeg -i demo.mp4 -vf "fps=12,scale=360:-1" demo.gif`.
