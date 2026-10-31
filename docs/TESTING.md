# Tests manuels

À dérouler avant chaque release, sur un téléphone ou un émulateur, avec l'APK release signé (`truth-glasses-vX.Y.Z.apk`). Les tests unitaires (`./gradlew testDebugUnitTest`) couvrent déjà la détection, le décodeur, l'ajustement du texte et la mémoire des pubs : cette liste couvre ce qu'ils ne voient pas.

Pour suivre les contours et les logs, activer le **Mode debug** (un build debug affiche aussi les textes qui ressemblent à un libellé de pub dans `adb logcat -s TG`).

## Installation et premier lancement

- [ ] L'APK s'installe. `aapt2 dump permissions` ne liste pas `INTERNET`, `SYSTEM_ALERT_WINDOW` ni `QUERY_ALL_PACKAGES`.
- [ ] Au premier lancement, l'accueil affiche « Service désactivé » et le guide en 3 étapes.
- [ ] Android 13 et plus : le bouton « Ouvrir Infos sur l'appli » ouvre la bonne page, et le service est grisé tant que les paramètres restreints ne sont pas autorisés.
- [ ] « Ouvrir les réglages d'accessibilité » ouvre les réglages. Après activation et retour dans l'appli, l'accueil affiche « Service activé » et l'interrupteur des lunettes.
- [ ] La description du service dans les réglages d'accessibilité est honnête et lisible.

## Bac à sable

- [ ] Onglet **Fil** : les 13 pubs sont recouvertes, les posts normaux ne le sont jamais.
- [ ] La bannière étroite reçoit un message court qui tient dans le bloc.
- [ ] Onglets **Façon Instagram**, **Façon YouTube** et **Façon Reddit** : chaque pub est recouverte en entier, avec un message adapté à son thème.
- [ ] Le message d'une pub ne change pas pendant qu'elle entre à l'écran ni pendant le scroll.
- [ ] Façon Instagram : quand le haut du post (avec le libellé) sort de l'écran, le reste de la pub reste recouvert. En remontant, la pub est recouverte dès que son bas réapparaît.
- [ ] Les blocs suivent le scroll, y compris pendant le geste.
- [ ] Les touchers traversent les blocs : on peut scroller en partant d'un bloc.
- [ ] Rotation de l'écran : les blocs se replacent sur les pubs.
- [ ] Mode sombre : l'appli et le bac à sable restent lisibles, les blocs gardent leur fond gris clair.

## Accueil, tuile et réglages

- [ ] L'interrupteur des lunettes retire et remet les blocs.
- [ ] Désactiver une appli dans « Applis surveillées » retire ses blocs ; la réactiver les remet.
- [ ] Seules les applis installées apparaissent, avec la mention « Détection pas encore vérifiée » tant que leur profil n'est pas vérifié.
- [ ] « Ajouter la tuile Lunettes » (Android 13 et plus) propose d'ajouter la tuile.
- [ ] La tuile passe de active à inactive et inversement, et l'état de l'accueil suit.
- [ ] Service désactivé : toucher la tuile ouvre l'accueil.
- [ ] Mode debug : contour rouge autour des libellés, bleu autour des blocs, arbre de l'écran dans `adb logcat -s TG` une fois le scroll arrêté. Le texte des nœuds est remplacé par sa longueur, sauf en build debug pour ce qui ressemble à un libellé.
- [ ] Mode debug désactivé : aucun log sous le tag `TG`.

## Robustesse

- [ ] En quittant l'appli cible (accueil, autre appli, écran récent), les blocs disparaissent tout de suite.
- [ ] Volet des notifications ou des réglages rapides ouvert par-dessus une appli cible : les blocs se masquent, puis reviennent à la fermeture.
- [ ] Langue de l'appli (Réglages Android → Langue de l'appli) en français puis en anglais : les messages, l'accueil et la tuile suivent.
- [ ] Désactiver le service dans les réglages d'accessibilité retire tous les blocs.
- [ ] Mise à jour par-dessus la version précédente (même keystore) : l'installation passe sans désinstaller, les réglages sont conservés.

## Vraies applis

Pour chaque appli dont le profil est vérifié (`verifiedOn` renseigné dans `AppProfiles.kt`) :

- [ ] Les posts sponsorisés du fil sont recouverts, les posts normaux ne le sont pas.
- [ ] Le libellé détecté (contour rouge en mode debug) est bien celui noté dans le profil.
- [ ] Les champs de saisie (recherche, commentaire) ne déclenchent rien et n'apparaissent pas dans les logs du mode debug.
