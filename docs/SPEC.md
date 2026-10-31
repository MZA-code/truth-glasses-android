# Truth Glasses Android — Spécification V1

## 1. L'idée

Quand les lunettes sont activées, chaque pub repérée dans une appli cible est recouverte par un bloc gris clair bordé de noir, avec un message ironique en grosses capitales : « OBÉIS », « TU N'EN AS PAS BESOIN », « C'EST TOI LE PRODUIT ». C'est le même esprit que l'extension Chrome, en plus simple : pas de filtre noir et blanc, pas de son, juste les pubs masquées par leur « vrai message ».

### Dans le périmètre de la V1

- Recouvrir les **posts et blocs sponsorisés** dans les fils d'actualité d'Instagram, Reddit et YouTube (d'autres applis pourront suivre).
- Choisir le message **selon le thème de la pub** (crédit, promo, beauté…), sinon un message générique.
- **FR / EN** automatique selon la langue du téléphone.
- Activation et désactivation par une **tuile dans les réglages rapides** et dans l'appli.
- Choix des applis surveillées.
- Un **bac à sable** intégré (faux fil avec de fausses pubs) pour tester sans les vraies applis.
- APK compilé par **GitHub Actions** et publié dans les Releases.

### Hors périmètre (idées pour plus tard)

- Le filtre noir et blanc de tout l'écran.
- Les pubs vidéo en plein écran (pré-roll YouTube, pubs entre les stories Instagram).
- Chrome et les autres navigateurs (le portage de l'extension pour Firefox Android est plus adapté).
- Le décodeur IA et le mode caméra en réalité augmentée.

## 2. Parcours utilisateur

1. L'utilisateur télécharge l'APK depuis les Releases GitHub et l'installe. Android demande d'autoriser l'installation depuis cette source, et Play Protect peut afficher un avertissement.
2. Il ouvre l'appli. L'écran d'accueil montre l'état (« Service désactivé ») et un guide en 3 étapes :
   1. **Android 13 et plus** : *Infos sur l'appli → menu ⋮ → Autoriser les paramètres restreints*. Sans ça, Android grise le service d'accessibilité des applis installées hors store.
   2. Bouton **« Ouvrir les réglages d'accessibilité »** (`Settings.ACTION_ACCESSIBILITY_SETTINGS`), puis activer « Truth Glasses ».
   3. Ajouter la tuile **« Lunettes »** aux réglages rapides (sur Android 13 et plus, proposer `StatusBarManager.requestAddTileService`).
3. Une fois le service actif, l'accueil affiche :
   - un grand interrupteur **Lunettes ON / OFF** ;
   - la liste des applis cibles avec un interrupteur chacune (seules les applis installées sont affichées, voir 4.2) ;
   - un bouton **« Tester dans le bac à sable »** ;
   - un interrupteur **« Mode debug »** (contours rouges sur les libellés détectés et logs de l'arbre).
   - une ligne de transparence : « Aucune connexion Internet. Rien n'est enregistré ni envoyé. »
4. Il ouvre Instagram : les posts sponsorisés sont recouverts par les messages, qui suivent le scroll.
5. Il peut tirer le volet et toucher la tuile **Lunettes** pour les enlever ou les remettre à tout moment.

Les touchers **traversent** le calque : l'utilisateur peut toujours scroller et toucher l'appli en dessous.

## 3. Architecture

```
app/src/main/java/io/github/mzacode/truthglasses/
├── MainActivity.kt              Accueil Compose : état, guide, réglages
├── SandboxActivity.kt           Faux fil de test avec de fausses pubs
├── service/
│   └── GlassesService.kt        AccessibilityService : événements, planification des scans
├── detect/
│   ├── UiNode.kt                Interface d'arbre testable + implémentation AccessibilityNodeInfo
│   ├── AppProfiles.kt           Libellés et réglages par appli cible
│   └── AdDetector.kt            Trouve les blocs pub et leur position
├── decode/
│   ├── Themes.kt                Règles mots-clés -> thème (FR + EN)
│   └── MessageDecoder.kt        Choisit le message (thème, format, choix stable)
├── overlay/
│   ├── OverlayController.kt     Ajoute et retire le calque via WindowManager
│   ├── CoverView.kt             View custom qui dessine les blocs
│   └── TextFitter.kt            Taille de texte maximale qui tient dans le bloc
├── settings/
│   └── Prefs.kt                 SharedPreferences + écoute des changements
└── tile/
    └── GlassesTileService.kt    Tuile des réglages rapides
app/src/main/res/
├── xml/accessibility_service_config.xml
├── values/strings.xml, values-fr/strings.xml        Textes de l'appli
├── values/messages.xml, values-fr/messages.xml      Messages ironiques (fournis dans le kit)
└── font/anton_regular.ttf                           Police (licence OFL dans app/src/main/assets/licenses/Anton-OFL.txt, livrée dans l'APK)
```

Le flux : un événement d'accessibilité arrive → `GlassesService` planifie un scan → `AdDetector` renvoie une liste de `AdHit(bounds, text)` → `MessageDecoder` associe un message à chaque bloc → `CoverView` les dessine.

## 4. Composants

### 4.1 Le service d'accessibilité — `GlassesService`

`res/xml/accessibility_service_config.xml` :

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/a11y_service_description"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewScrolled"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagIncludeNotImportantViews|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100"
    android:isAccessibilityTool="false" />
```

Le manifeste déclare le service avec `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` et l'intent-filter `android.accessibilityservice.AccessibilityService`.

`a11y_service_description` doit être honnête, par exemple : « Lit la structure de l'écran des applis que vous choisissez pour repérer les publicités et les recouvrir d'un message. Rien n'est enregistré ni envoyé : l'appli n'a pas accès à Internet. »

Comportement attendu :

- **`onServiceConnected`** : appliquer `packageNames` à partir des réglages (`serviceInfo = serviceInfo.apply { packageNames = ... }`), créer le calque, et s'abonner aux changements de préférences.
- **Changement des applis activées** : remettre à jour `packageNames` à chaud.
- **`onAccessibilityEvent`** :
  - Sortie d'une appli ciblée → vider les blocs et masquer le calque. `packageNames` filtre les événements des autres applis et `rootInActiveWindow` y vaut `null` (constaté sur l'API 37) : aucun événement ne signale la sortie, donc le service vérifie la fenêtre active toutes les 500 ms tant qu'un bloc est affiché.
  - `TYPE_VIEW_SCROLLED` → **décaler tout de suite** les blocs existants de `-event.scrollDeltaY` (et `-scrollDeltaX`) quand la valeur n'est pas nulle, pour que le scroll reste fluide, puis planifier un scan.
  - Les autres événements → planifier un scan.
- **Planification** : un `Handler` sur le thread principal. Un scan au plus toutes les **100 ms**, plus un scan final **150 ms** après le dernier événement.
- **Lunettes OFF** : ne rien scanner et masquer le calque. Le service reste actif.
- `onInterrupt` : rien. `onUnbind` / `onDestroy` : retirer le calque proprement.

### 4.2 Les profils d'applis — `AppProfiles`

```kotlin
data class AppProfile(
    val packageName: String,
    val displayName: String,
    val sponsorLabels: List<Regex>,   // libellé EXACT d'un nœud (texte ou contentDescription), trim
    val verifiedOn: String?           // date de vérification avec un dump, null = pas encore vérifié
)
```

Packages de départ (à confirmer sur l'appareil) :

| Appli | Package | Libellés probables (à vérifier) |
|---|---|---|
| Instagram | `com.instagram.android` | `Sponsorisé`, `Sponsored` |
| Reddit | `com.reddit.frontpage` | `Promoted`, `Sponsorisé` |
| YouTube | `com.google.android.youtube` | `Sponsorisé`, `Sponsored`, `Annonce`, `Ad` |
| Bac à sable | le package de l'appli elle-même | `Sponsorisé`, `Sponsored` |

**Ces libellés sont des hypothèses.** Pour chaque appli, il faut faire un dump de l'écran sur un post sponsorisé (`adb shell uiautomator dump` ou le mode debug, voir 4.6), relever le texte exact et la `contentDescription`, puis remplir `verifiedOn`. Les applis changent leur interface régulièrement : on garde des regex simples et une correspondance exacte sur le nœud entier (`^sponsoris[ée]e?$`, insensible à la casse). Les libellés courts et ambigus (`Ad`, `Pub`) ne sont utilisés que dans le profil des applis où leur présence a été vérifiée.

Tant qu'un profil n'est pas vérifié, sa liste de libellés reste vide : l'appli apparaît dans l'accueil avec la mention « Détection pas encore vérifiée » et rien n'y est recouvert. L'écran d'accueil n'affiche que les applis installées. Utilise `packageManager.getPackageInfo` avec un bloc `<queries>` listant ces packages dans le manifeste (pas `QUERY_ALL_PACKAGES`).

### 4.3 La détection — `UiNode` et `AdDetector`

Interface testable :

```kotlin
interface UiNode {
    val text: CharSequence?
    val contentDescription: CharSequence?
    val className: CharSequence?
    val viewIdResourceName: String?   // pour le mode debug
    val isScrollable: Boolean
    val isVisibleToUser: Boolean
    fun boundsInScreen(): Bounds
    val parent: UiNode?
    val children: List<UiNode>
}
```

Une implémentation enveloppe `AccessibilityNodeInfo`. Les tests utilisent un faux arbre construit à la main. `Bounds` est un petit type en pur Kotlin, car `android.graphics.Rect` n'a aucun comportement dans les tests JVM.

Algorithme pour un scan :

1. Partir de `rootInActiveWindow`. Si sa racine n'est pas un package ciblé, ne rien renvoyer.
2. Parcourir l'arbre en largeur avec une **limite de 3 000 nœuds** et une **profondeur maximale de 60**. Ignorer les nœuds invisibles.
3. Pour chaque nœud dont le texte ou la `contentDescription` correspond à un `sponsorLabel` du profil, on a un **libellé**.
4. **Remonter jusqu'au conteneur du post** : on remonte les parents jusqu'à trouver le nœud dont le **parent est scrollable** (`isScrollable`). Ce nœud est l'élément de liste, c'est-à-dire le post entier. Cette règle marche pour les `RecyclerView` comme pour les `LazyColumn` en Compose. Au plus 20 niveaux.
   - S'il n'y a pas de parent scrollable, prendre le premier ancêtre (hors racine, qui recouvrirait tout l'écran) dont la hauteur dépasse 25 % de l'écran. Sinon, ignorer le libellé.
5. Découper les limites du conteneur à la zone visible de la fenêtre, et ignorer le bloc si ce qui reste fait moins de 48 dp de haut ou de large.
6. **Texte de la pub** : concaténer le texte et les `contentDescription` du sous-arbre du conteneur, au plus 500 caractères, en minuscules. Ce texte sert uniquement à choisir le thème et n'est jamais stocké ni loggé en release.
7. **Pubs déjà vues** (`AdMemory`) : chaque pub recouverte laisse une empreinte, l'ensemble des hash de ses textes d'au moins 24 caractères (légende, titre ; les textes courts comme « En savoir plus » se retrouvent d'un bloc à l'autre). Les enfants des nœuds scrollables qui partagent un de ces hash sont recouverts eux aussi, même sans libellé : la pub reste recouverte quand son libellé sort de l'écran, et l'est de nouveau quand elle revient. La mémoire garde aussi le message choisi, pour qu'il ne change pas. Elle ne contient que les hash et les messages, jamais le texte lu à l'écran, au plus 32 pubs, et le service la vide quand on quitte l'appli cible ou qu'on enlève les lunettes.
8. Dédoublonner : si un conteneur est inclus dans un autre, ne garder que le plus grand.
9. Résultat : `List<AdHit(bounds: Bounds, adText: String, packageName: String, labelBounds: Bounds?, fingerprint: Set<Int>)>`, avec `labelBounds` null pour une pub reconnue par la mémoire.

Sur Android 13 et plus, ne pas appeler `recycle()` (inutile). Sur les versions plus anciennes, un `recycle()` dans un `finally` évite les fuites. L'enveloppe `UiNode` peut gérer ça.

### 4.4 Le décodeur — `Themes` et `MessageDecoder`

Les messages sont dans `res/values/messages.xml` (EN) et `res/values-fr/messages.xml` (FR), fournis dans ce kit : un `string-array` par thème. Les **mots-clés**, eux, restent en Kotlin et sont **bilingues**, pour qu'une pub en anglais soit reconnue sur un téléphone en français et inversement.

Règles de thème, dans cet ordre (première correspondance gagnante, reprises de l'extension) :

| Thème | `string-array` | Regex (sur le texte de la pub en minuscules) |
|---|---|---|
| Crédit | `msg_credit` | `cr[ée]dit\|pr[êe]t\|financement\|mensualit\|taeg\|\bloan\|financing\|\bapr\b\|mortgage` |
| Promo | `msg_sale` | `soldes\|promo\|r[ée]duction\|-\s?\d+\s?%\|\d+\s?% off\|black friday\|offre limit\|\bsale\b\|discount\|\bdeals?\b\|limited offer` |
| Gratuit | `msg_free` | `gratuit\|offert\|\bfree\b\|essai\|\btrial` |
| Assurance | `msg_insurance` | `assurance\|mutuelle\|s[ée]curit\|alarme\|insurance\|security\|alarm` |
| Jeux d'argent | `msg_gambling` | `\bparis?\b\|casino\|poker\|jackpot\|loto\|lottery\|bonus de bienvenue\|welcome bonus\|\bbet` |
| Beauté | `msg_beauty` | `parfum\|perfume\|beaut[ée]\|beauty\|cr[èe]me\|cream\|anti-[âa]ge\|anti-aging\|maquillage\|makeup\|minceur\|weight loss\|r[ée]gime\|\bdiet` |
| Voiture | `msg_car` | `voiture\|\bcars?\b\|\bsuv\b\|leasing\|\blease\b\|\bauto\b\|\bmoto` |
| Appli | `msg_app` | `t[ée]l[ée]charge\|download\|appli\|\bapp\b\|app store\|google play\|install` |
| Abonnement | `msg_subscription` | `abonne\|newsletter\|inscri\|subscri\|sign up` |
| High-tech | `msg_tech` | `smartphone\|iphone\|galaxy\|ordinateur\|laptop\|\btv\b\|[ée]cran\|screen` |
| Voyage | `msg_travel` | `vacances\|voyage\|s[ée]jour\|h[ôo]tel\|\bvols?\b\|vacation\|holiday\|travel\|flights?\b` |
| Malbouffe | `msg_food` | `fast food\|burger\|pizza\|livr[ée]\|delivery\|soda` |
| Investissement | `msg_invest` | `investi\|crypto\|bourse\|trading\|rendement\|stocks?\b\|returns` |
| Aucun thème | `msg_generic` | — |

Choix du message :

- **Choix stable** : `seed = (packageName + adText).hashCode()` en valeur absolue, puis `messages[seed % messages.size]`. Une même pub garde le même message d'un scan à l'autre (les blocs sont recréés à chaque scan, donc un compteur ne marcherait pas).
- **Format étroit** (largeur du bloc < 200 dp) : ne garder que les messages de 16 caractères au plus dont aucun mot ne dépasse 8 lettres. S'il n'en reste aucun, utiliser `msg_short`.
- Le décodeur est une fonction pure (texte, largeur en dp, `Resources` ou tableau de messages injecté), donc facile à tester.

### 4.5 Le calque — `OverlayController`, `CoverView`, `TextFitter`

Ajout du calque **depuis le service** :

```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.START
    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
}
windowManager.addView(coverView, params)
```

- `FLAG_NOT_TOUCHABLE` laisse passer les touchers vers l'appli en dessous.
- **Décalage des coordonnées** : `boundsInScreen` est en coordonnées écran. Au dessin, soustraire la position du calque obtenue avec `coverView.getLocationOnScreen()`. Sans ça, les blocs sont décalés de la hauteur de la barre d'état sur certains appareils.
- `CoverView` garde une liste de `Cover(rect: RectF, message: String)`, appelle `invalidate()` quand elle change et propose `offsetBy(dx, dy)` pour le décalage immédiat au scroll.

Dessin d'un bloc :

- fond `#F2F2F2`, bordure noire de 2 dp à l'intérieur, coins droits ;
- texte noir en capitales, police **Anton** (Google Fonts, licence OFL : inclure le fichier de licence dans le dépôt), centré horizontalement et verticalement, interligne serré (`lineSpacingMultiplier ≈ 0.95`) ;
- marge intérieure de 8 dp.

`TextFitter` cherche la plus grande taille de texte qui tient : une dichotomie de 8 sp à 160 sp avec un `StaticLayout`. Le texte tient si (a) aucun mot n'est coupé, c'est-à-dire que chaque mot mesuré avec `Paint.measureText` est plus étroit que la largeur utile, et (b) la hauteur du layout est inférieure ou égale à la hauteur utile. Si même 8 sp ne tient pas, autoriser la coupure des mots. **Mettre en cache** le résultat par clé `(message, largeur, hauteur)` : on ne recalcule que quand la taille du bloc change. Dans le code, le découpage en lignes est fait par `TextFitter` lui-même (retour à la ligne glouton mesuré avec `Paint.measureText`) plutôt qu'avec `StaticLayout` : `CoverView` dessine exactement ces lignes, et tout est testable sans appareil.

Petit plus facultatif : une apparition qui clignote (2 ou 3 frames d'alpha, environ 300 ms) la première fois qu'un bloc apparaît, en clin d'œil à l'extension.

### 4.6 Le mode debug

Quand il est activé dans l'appli :

- un contour rouge est dessiné autour de chaque **libellé** détecté, et un contour bleu autour de chaque **conteneur** retenu ;
- après un `TYPE_WINDOW_STATE_CHANGED` ou un scroll, une fois l'écran immobile depuis 0,7 s, l'arbre de l'écran est loggé sous le tag `TG` (classe, texte, `contentDescription`, `viewIdResourceName`, `isScrollable`, limites), avec une indentation par profondeur et une ligne par nœud (les retours à la ligne du texte sont échappés). Le dernier arbre du log est donc celui de l'écran affiché.

C'est l'outil principal pour trouver et vérifier les libellés des vraies applis (section 4.2). En build release, le texte et la `contentDescription` sont remplacés par leur longueur (`text=<12 chars>`) : le contenu de l'écran n'apparaît jamais dans les logs. Pour relever des libellés, utiliser un build debug.

### 4.7 Le bac à sable — `SandboxActivity`

Un faux fil en Compose (`LazyColumn`) avec une vingtaine de posts : des posts normaux (texte, image en dégradé) et, tous les 3 ou 4 posts, un post marqué « Sponsorisé » ou « Sponsored » avec de fausses marques et des textes qui déclenchent chaque thème (« NovaBank — Crédit TAEG 2,9 % », « GLOWÉ — Crème anti-âge », « ROYAL SPIN — Bonus de bienvenue »…). Inclure aussi une bannière étroite pour tester le format fin.

À côté de ce fil générique, trois onglets proposent des maquettes « squelette » qui reprennent la mise en page d'Instagram, de YouTube et de Reddit (code dans `sandbox/`) : des formes grises à la place des logos, des icônes et des compteurs, sans les couleurs ni les noms des marques dans l'écran. Les 13 pubs y sont réparties pour que chaque thème apparaisse. Chaque maquette affiche le libellé relevé dans la vraie appli (`sandbox_label_*` dans les ressources), qui doit aussi figurer dans le profil du bac à sable. Ces maquettes servent aux tests et aux captures du README, à la place de dumps des vraies applis.

Le package de l'appli fait partie des cibles, ce qui permet de tester toute la chaîne sur un émulateur sans Instagram. **C'est la première chose à faire marcher.**

### 4.8 Les réglages — `Prefs`

| Clé | Type | Défaut |
|---|---|---|
| `glasses_on` | Boolean | `true` |
| `enabled_packages` | Set<String> | toutes les applis cibles installées + le bac à sable |
| `debug_mode` | Boolean | `false` |

Le service écoute `OnSharedPreferenceChangeListener` pour réagir tout de suite (ON/OFF, applis, debug).

### 4.9 La tuile — `GlassesTileService`

- Libellé « Lunettes » / « Glasses », icône de lunettes (vector drawable simple).
- État `ACTIVE` ou `INACTIVE` selon `glasses_on`, mis à jour dans `onStartListening`.
- `onClick` : si le service d'accessibilité n'est pas activé, ouvrir l'accueil de l'appli (sur l'API 34 et plus, `startActivityAndCollapse(PendingIntent)` ; avant, la version avec `Intent`). Sinon, inverser `glasses_on` et mettre à jour la tuile.
- Pour savoir si le service est actif : `AccessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` contient notre composant.

## 5. Permissions et manifeste

Seulement :

- le service d'accessibilité (`BIND_ACCESSIBILITY_SERVICE` dans sa déclaration) ;
- la tuile (`BIND_QUICK_SETTINGS_TILE` dans sa déclaration) ;
- un bloc `<queries>` avec les packages cibles.

Rien d'autre : pas d'`INTERNET`, pas de `SYSTEM_ALERT_WINDOW`, pas de `QUERY_ALL_PACKAGES`, pas de notifications. Désactiver les sauvegardes inutiles (`android:allowBackup="false"`).

## 6. Compilation et publication (GitHub Actions)

`.github/workflows/build.yml` :

- **À chaque push et pull request** : `actions/checkout`, `actions/setup-java` (Temurin, JDK 17 ou celui exigé par l'AGP choisi), `gradle/actions/setup-gradle`, puis `./gradlew testDebugUnitTest assembleDebug`, et l'APK debug téléversé comme artefact.
- **Sur un tag `v*`** : `assembleRelease` signé avec un keystore stocké dans les secrets du dépôt (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), puis création d'une Release GitHub avec l'APK (`softprops/action-gh-release`).
- Utiliser les versions majeures actuelles de ces actions (à vérifier au moment d'écrire le workflow).

Pour générer le keystore, en local et une seule fois :
`keytool -genkeypair -v -keystore release.jks -alias truthglasses -keyalg RSA -keysize 2048 -validity 10000`, puis `base64 -w0 release.jks` dans le secret. Le fichier `.jks` est dans `.gitignore`.

Important : garder **le même keystore** pour toutes les versions, sinon les utilisateurs devront désinstaller l'appli avant de mettre à jour.

## 7. Tests

**Unitaires (JVM, sans appareil)** :

- `MessageDecoder` : chaque thème est reconnu en FR et en EN, aucun thème donne un message générique, le choix est stable pour un même texte, le format étroit ne renvoie que des messages courts.
- `AdDetector` sur de faux arbres `UiNode` : libellé trouvé puis conteneur = enfant du parent scrollable ; pas de parent scrollable donne le repli à 25 % ; libellé invisible ignoré ; conteneurs imbriqués dédoublonnés ; limite de nœuds respectée.
- `TextFitter` : en injectant une fonction de mesure simple (largeur = nombre de caractères × taille × 0,55), vérifier qu'un message long tient dans un format portrait, qu'un mot trop long active la coupure, et que le cache est bien utilisé.

**Manuels** (liste à cocher dans `docs/TESTING.md`) :

- Bac à sable : blocs bien placés, suivi du scroll, rotation de l'écran, mode sombre, ON/OFF par la tuile.
- Instagram, Reddit, YouTube : au moins un post sponsorisé recouvert pour chaque appli. Le calque disparaît quand on quitte l'appli et quand le clavier s'ouvre sur un champ de recherche.
- Aucune trace du contenu de l'écran dans `adb logcat` en build release.

## 8. Jalons

| # | Jalon | Terminé quand… |
|---|---|---|
| M0 | Squelette et CI | le projet compile, la CI produit un APK debug téléchargeable |
| M1 | Service et calque | une fois le service activé, un bloc de test « OBÉIS » s'affiche en haut de l'écran dans le bac à sable |
| M2 | Détection | les posts « Sponsorisé » du bac à sable sont recouverts et suivent le scroll ; tests `AdDetector` au vert |
| M3 | Messages | messages par thème, FR/EN, format étroit, texte ajusté ; tests `MessageDecoder` et `TextFitter` au vert |
| M4 | Réglages et tuile | accueil complet avec le guide, tuile fonctionnelle, choix des applis, mode debug |
| M5 | Vraies applis | profils Instagram, Reddit et YouTube vérifiés avec un dump et datés |
| M6 | Publication | README FR/EN avec captures, Release v0.1.0 signée publiée par la CI |

## 9. Risques et points d'attention

- **Les applis changent leurs libellés** : d'où les profils datés, le mode debug et le bac à sable. Les mises à jour d'Instagram ou de YouTube casseront parfois la détection, c'est normal.
- **Volet des réglages rapides** : il s'ouvre sans changer la fenêtre active, et le calque passait par-dessus (constaté au jalon M1). Corrigé au jalon M4 : tant qu'une appli cible est au premier plan, le service lit toutes les 300 ms la liste des fenêtres (`flagRetrieveInteractiveWindows`, seuls le type et la taille sont lus) et masque les blocs quand une fenêtre système dépasse la moitié de l'écran.
- **Langue choisie pour l'appli** (Réglages Android → Langue de l'appli) : quand un service démarre le processus, ses ressources restent dans la langue du système (constaté sur l'API 37). `appLocalizedResources()` applique cette langue à la main pour les messages et la tuile.
- **Pub découverte quand son libellé sort de l'écran** : constaté au jalon M2, corrigé au jalon M5 par la mémoire des pubs déjà vues (4.3, étape 7), vérifié dans la maquette façon Instagram. Restent deux limites : une pub jamais vue qui arrive par le haut reste découverte jusqu'à l'apparition de son libellé, et une pub sans texte d'au moins 24 caractères n'a pas d'empreinte.
- **Léger retard au scroll rapide** : limité par le décalage immédiat (`scrollDeltaY`), mais jamais parfait. Certaines applis ne remplissent pas `scrollDeltaY` : on se contente alors du rescan.
- **Applis qui exposent peu leur arbre** : certaines vues personnalisées ou rendues dans un canvas n'exposent pas de nœuds. Dans ce cas, pas de détection possible : le noter dans le profil.
- **Installation hors store** : Play Protect peut avertir, et sur Android 13 et plus il faut autoriser les paramètres restreints. Le README doit l'expliquer avec des captures.
- **Vérification des développeurs Android** : Google impose progressivement que les applis installées sur les appareils certifiés viennent de développeurs vérifiés. C'est en vigueur depuis le 30 septembre 2026 au Brésil, en Indonésie, à Singapour et en Thaïlande, et l'extension au reste du monde est prévue à partir de 2027. Les solutions : le compte gratuit « étudiants et amateurs » (distribution limitée à 20 appareils), le « parcours avancé » pour les utilisateurs expérimentés, ou l'installation par `adb`, qui reste possible. À revérifier avant la publication sur developer.android.com/developer-verification.
- **Confiance** : un service d'accessibilité fait peur, à juste titre. Code lisible, pas d'Internet, `packageNames` limité, et une section « Vie privée » claire dans le README.

## 10. README du dépôt (au jalon M6)

Même structure que celui de l'extension : bannière, GIF de démo filmé dans le bac à sable (`adb shell screenrecord`, puis conversion en GIF), captures avant/après, installation pas à pas avec les écrans d'Android, section vie privée, limites, licence MIT et mention « projet parodique ». Version anglaise dans `README.md`, version française dans `README.fr.md`. Lien vers l'extension Chrome.
