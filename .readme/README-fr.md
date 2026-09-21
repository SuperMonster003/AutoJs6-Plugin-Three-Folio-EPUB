<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>Lit les livres EPUB avec navigation, recherche, lecture à voix haute et accès par script</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues (Languages)

******

Le fichier README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### Introduction

******

Lecture en un geste : ouvrez un fichier `.epub` directement depuis le gestionnaire de fichiers d'AutoJs6, avec le bouton principal `Lire l'EPUB` ou depuis le menu contextuel. La liseuse repose sur le [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0, le moteur open source utilisé par de nombreuses liseuses commerciales.

Le plugin lit le livre directement à travers le descripteur de fichier temporaire accordé par l'hôte. Il ne reçoit jamais de chemin du système de fichiers, ne copie jamais le livre et ne l'extrait jamais vers le stockage.

> 1.0.0 est la première version. La liseuse ouvre les livres EPUB 2 et EPUB 3 avec une table des matières, mémorise la position de lecture de chaque livre, offre le mode défilement, les zones d'appui, les touches de volume et le mode immersif, un panneau de préférences (taille du texte, police, espacements, alignement, colonnes et thèmes qui peuvent suivre le mode nuit de l'hôte), les polices TTF / OTF importées, les livres CJK verticaux et de droite à gauche, les livres à mise en page fixe en page simple ou en double page, la recherche plein texte, les signets, les liens dans le livre, les notes et les images, et la lecture à voix haute avec le moteur de synthèse vocale du système. L'icône de l'application ouvre un lanceur avec les livres récents et le sélecteur de documents du système, les autres applications transmettent un EPUB via `ACTION_VIEW`, et la page des paramètres couvre les valeurs par défaut de la liseuse, les données conservées sur l'appareil et une vérification manuelle des mises à jour. L'API de script `epub`, la session de liseuse de l'hôte et trois scripts d'exemple sont livrés avec AutoJs6 6.8.0 (build 5282). Les surlignages, les notes et l'export sont prévus pour 1.1.0 (ROADMAP.md, P9).

******

### Captures d'écran

******

Prises sur un téléphone avec les livres d'exemple générés dans `docs/fixtures` (aucun livre tiers n'est montré) ; l'interface suit la langue d'AutoJs6, ici l'anglais:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>Lecture</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>Table des matières</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>Préférences de lecture</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>Recherche plein texte</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>Signets</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>Lecture à voix haute</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>Thème sombre</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>Thème sépia</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>Japonais vertical</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>Mise en page fixe</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>Livres récents</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>Paramètres</td>
  </tr>
</table>

******

### Points forts

******

- Moteur Readium : les livres EPUB 2 (NCX) et EPUB 3 (NAV) sont rendus par le navigateur Readium avec Readium CSS, y compris les liens internes, les notes et les images.
- Aucune copie : le conteneur EPUB est lu sur place via un descripteur en lecture seule avec des lectures positionnelles, donc même les gros livres s'ouvrent sans fichier cache.
- Table des matières : accédez à n'importe quel chapitre depuis la barre d'outils ; les entrées imbriquées conservent leur niveau.
- Mémoire de la position de lecture : la dernière position de chaque livre est stockée dans l'espace privé du plugin sous une empreinte de son contenu, si bien que le même livre reprend même après un déplacement ou un renommage ; `Reprendre au début` l'efface.
- Interface de la liseuse : titre et chapitre dans la barre d'outils, barre de progression avec position et pourcentage, mode immersif par un appui au centre, zones d'appui et touches de volume pour tourner les pages, mode défilement ou paginé.
- Préférences de lecture : un panneau inférieur règle la taille du texte, la police, l'interligne, les marges, l'espacement des paragraphes, l'alignement, la césure, les styles de l'éditeur, le nombre de colonnes et la mise en page paginée ou défilante ; les changements s'appliquent immédiatement et sont mémorisés pour chaque livre. Thèmes clair, sépia et sombre, ou suivi du mode nuit de l'hôte ; la barre d'outils et les barres système prennent les couleurs du thème.
- Import de polices : choisissez des fichiers TTF ou OTF avec le sélecteur de documents du système ; ils sont validés, stockés en privé dans le plugin (jusqu'à 10 polices de 20 Mo chacune), listés dans le panneau de préférences à côté des polices intégrées, fournis à chaque livre et supprimables depuis le même panneau.
- Livres CJK verticaux et de droite à gauche : le sens de lecture suit la publication, donc les zones d'appui s'inversent pour les livres de droite à gauche ; les livres japonais et chinois à progression de page de droite à gauche s'affichent verticalement, et une préférence `Sens du texte` force le texte horizontal ou vertical. L'interface suit la langue d'AutoJs6 pour son propre sens de mise en page, indépendamment du livre.
- Livres à mise en page fixe : les pages sont comptées `Page x sur N`, le panneau propose un choix `Double page` (auto affiche deux pages côte à côte en paysage) et masque les préférences de texte sans effet ; le zoom par pincement et le déplacement sont ceux de Readium.
- Recherche plein texte : l'entrée `Rechercher` de la barre d'outils trouve chaque occurrence du livre, 50 à la fois (jusqu'à 500), groupées par chapitre avec le texte environnant ; toucher un résultat y mène, la surligne sur la page et propose précédent / suivant au-dessus de la barre de progression.
- Signets : l'icône de la barre d'outils marque la page courante (elle se remplit quand la page a un signet) et l'entrée `Signets` liste chaque signet avec son chapitre, un extrait et l'heure, du plus récent au plus ancien, pour y sauter, le supprimer ou tout effacer ; ils sont conservés par livre (jusqu'à 500) à côté de la position de lecture.
- Gestes, touches et liens : zones de toucher (désactivées, gauche / droite ou haut / bas), touches de volume, clavier physique et menu de sélection de texte (copier, partager, recherche web et applications de traitement de texte) ; les liens internes gardent une pile de retour, les notes s'ouvrent dans une boîte de dialogue, les liens externes s'ouvrent après confirmation ou directement, et une image touchée s'ouvre en plein écran.
- Lecture à voix haute : `Lecture à voix haute` dans le menu déroulant lit le livre depuis la page actuelle avec le moteur de synthèse vocale du système, surligne la phrase lue et tourne les pages ; une barre sous la page et une notification multimédia proposent lecture / pause, phrase précédente / suivante et arrêt, les boutons du casque fonctionnent, la vitesse, la hauteur, la langue et la voix sont réglables, la lecture continue écran éteint et s'arrête à la fermeture de la liseuse sauf si `Continuer en arrière-plan` est activé, et les réglages de lecture à voix haute ajoutent un minuteur de sommeil (15 / 30 / 60 minutes ou la fin du chapitre) et un interrupteur pour garder l'écran allumé.
- Liens externes : toucher un lien `http` ou `https` affiche l'adresse complète et n'ouvre le navigateur système qu'après confirmation.
- Lanceur autonome : l'icône de l'application ouvre une grille des livres récents avec couverture, titre, auteur, progression et dernière lecture, plus un bouton `Ouvrir un EPUB` qui choisit un livre avec le sélecteur de documents du système ; la liseuse est la même que celle ouverte par le gestionnaire de fichiers.
- Ouverture depuis d'autres applications : un gestionnaire de fichiers, un navigateur ou une application de courrier peut confier un EPUB `content://` via `ACTION_VIEW` ; `Ajouter aux livres récents` dans le menu de débordement le garde dans le lanceur lorsque l'expéditeur autorise un accès durable.
- Page de réglages avec le thème, le tournage des pages, les valeurs par défaut de la lecture à voix haute, les liens et la gestion des données, plus l'historique des versions et une vérification manuelle des mises à jour qui n'interroge GitHub qu'au toucher
- Service de script : un service Binder `org.autojs.plugin.EPUB` permet à l'hôte AutoJs6 de lire un livre sans ouvrir le lecteur (métadonnées, table des matières, ordre de lecture, texte des chapitres en texte brut ou Markdown léger, ressources, recherche plein texte et nombre de positions), avec des requêtes bornées, au plus 8 livres ouverts à la fois et un accès réservé à l'hôte ; AutoJs6 6.8.0 l'expose aux scripts sous la forme du module `epub` (voir Scripts ci-dessous).
- Session de lecture pilotée par l'hôte : l'hôte AutoJs6 peut ouvrir le lecteur sur un livre via le service `org.autojs.plugin.EPUB` et le suivre (événements de position, de signet et de fermeture), sauter à un locator, un href ou une progression, tourner les pages ou les chapitres et régler les préférences de lecture ; le lecteur ne démarre que par le lancement explicite de l'hôte avec un jeton de session à usage unique, et fermer la session laisse le lecteur ouvert pour l'utilisateur sauf si l'hôte demande de le terminer.
- Intégration à l'hôte : menus et dialogues suivent la langue et le mode sombre d'AutoJs6 ; l'enveloppe Explorer Action est validée strictement avant toute ouverture de contenu.
- Multilingue : interface, instructions, README et changelog sont disponibles en 10 langues.

******

### Installation

******

1. Depuis le centre de plugins : ouvrez `Plugins` dans AutoJs6, choisissez `Readium EPUB Reader` dans la liste officielle et appuyez sur installer ; le centre de plugins télécharge l'APK signé, l'installe et vous laisse activer le plugin.
2. Depuis GitHub : téléchargez l'APK depuis la page [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) (le nom du fichier porte un CRC32 et `SHA256SUMS` liste la somme de contrôle), installez-le, puis activez le plugin dans le centre de plugins.
3. Prérequis : build interne AutoJs6 5269 ou ultérieur pour l'entrée du gestionnaire de fichiers, AutoJs6 6.8.0 (build 5282) ou ultérieur pour l'API de script `epub`, Android 7.0 ou ultérieur et un WebView système.

******

### Mode d'emploi

******

1. Téléchargez le dernier APK du plugin depuis la page [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) et installez-le sur votre appareil.
2. Ouvrez le centre de plugins d'AutoJs6 et activez le plugin `Readium EPUB Reader`.
3. Dans le gestionnaire de fichiers d'AutoJs6, touchez un fichier `.epub`, ou ouvrez son menu (autres actions) et choisissez `Lire l'EPUB`.
4. Utilisez le bouton de table des matières de la barre d'outils pour changer de chapitre et le bouton de préférences pour ajuster le texte et le thème ; touchez le tiers gauche ou droit de la page ou appuyez sur les touches de volume pour tourner les pages, et touchez le centre pour masquer ou afficher la barre d'outils ; appuyez sur Retour pour fermer la liseuse, la position est mémorisée.
5. Sans le gestionnaire de fichiers, touchez l'icône de l'application : le lanceur liste vos livres récents et `Ouvrir un EPUB` choisit un livre avec le sélecteur de documents du système ; les livres ouverts ainsi restent dans la liste avec leur couverture et leur progression.
6. Depuis une autre application (un gestionnaire de fichiers, les téléchargements d'un navigateur, une pièce jointe), choisissez cette liseuse pour un fichier `.epub` ; le livre s'ouvre de la même façon et `Ajouter aux livres récents` dans le menu de débordement le garde dans la liste du lanceur lorsque l'application expéditrice autorise un accès durable.
7. Ouvrez `Paramètres` depuis le menu du lanceur ou le menu de débordement de la liseuse pour régler le thème, le tournage des pages, les valeurs par défaut de la lecture à voix haute et les liens, effacer les données conservées par le plugin, lire l'historique des versions ou vérifier les mises à jour (la vérification ne contacte GitHub qu'au toucher).
8. Depuis un script : `epub.open(path)` lit un livre (métadonnées, table des matières, texte, recherche) et `epub.read(path)` ouvre cette liseuse et rapporte sa position ; voir la section Scripts ci-dessous et les exemples `Livres numériques` d'AutoJs6.

> Si le plugin n'apparaît pas dans le centre de plugins, mettez d'abord AutoJs6 à jour vers une version récente (build interne 5269 ou ultérieur). Explorer Action v2 prend en charge le bouton principal et le menu contextuel pour un fichier, avec une autorisation temporaire de lecture du document et de son dossier parent.

******

### Scripts

******

AutoJs6 6.8.0 ajoute le module global `epub` (alias `$epub`), que ce plugin sert : lisez un livre sans ouvrir la liseuse, ou ouvrez la liseuse et suivez-la depuis un script. AutoJs6 fournit trois scripts d'exemple sous `Exemples > Livres numériques`, et la référence se trouve dans la [documentation AutoJs6](https://docs.autojs6.com/#/epub):

Métadonnées, table des matières et texte des chapitres:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

Couverture, recherche et fonctions pratiques:

```javascript
let path = './books/lighthouse.epub';
let book = epub.open(path);
try {
    console.log('cover saved to', book.cover(files.cwd(), { overwrite: true }));
} catch (e) {
    if (!(e instanceof epub.EpubError) || e.code !== 'RESOURCE_NOT_FOUND') throw e;
    console.log('this book has no cover');
}
book.search('lighthouse', { limit: 20 }).forEach(hit => console.log(hit.title || hit.href, ':', hit.text));
files.write('./lighthouse.txt', book.textAll({ maxChars: 2 * 1024 * 1024 }));
book.close();
console.log(epub.metadata(path).language); // the convenience functions open and close the book themselves
epub.tocAsync(path).then(toc => console.log(toc.length, 'entries'));
```

Ouvrir la liseuse et suivre la position:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

Les chemins sont relatifs au répertoire de travail du script ou absolus (les URI `content://` ne sont pas acceptés). Chaque appel lève une `EpubError` avec un `code` (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT`, ...) quand le plugin ou le livre ne peut pas être utilisé, `epub.isAvailable()` indique si le plugin est installé et activé, et chaque méthode a un jumeau `*Async` qui renvoie une Promise.

******

### Formats pris en charge

******

Le plugin reconnaît l'extension suivante, ainsi que les fichiers sans extension explicitement marqués `application/epub+zip` par l'hôte:

```text
epub
```

Seul l'EPUB est pris en charge : livres redistribuables et à mise en page fixe en EPUB 2 ou EPUB 3. Les archives de bandes dessinées (CBZ), les livres audio, les PDF et les livres protégés par LCP sont hors périmètre ; un livre marqué comme chiffré LCP est signalé comme illisible au lieu d'afficher du contenu corrompu.

******

### Compatibilité

******

Ce dont le plugin a besoin, sur quoi il a été vérifié et ce qui reste hors du périmètre:

- AutoJs6 : build interne 5269 ou ultérieur pour l'entrée du gestionnaire de fichiers (Explorer Action v2) ; l'API de script `epub`, la session de liseuse de l'hôte et les scripts d'exemple exigent AutoJs6 6.8.0 (build 5282), le dernier build de l'hôte audité pour cette version.
- Android 7.0 (API 24) jusqu'à Android 16 (API 37, la cible) ; les pages sont rendues dans le WebView de l'appareil, un Android System WebView ou Chrome à jour est donc attendu. Le plugin n'a aucune bibliothèque native et fonctionne sans changement sur les appareils à pages de 16 KB.
- Vérifié sur AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI) et Xiaomi Pad 6 (Android 15, côté service) ; la matrice appareil x scénario, ses écarts et les versions de WebView sont dans `docs/dev/compatibility-matrix.md`.
- Livres : EPUB 2 et EPUB 3, redistribuables et à mise en page fixe, CJK vertical et de droite à gauche. Les livres protégés par DRM (LCP, Adobe ADEPT) sont signalés comme protégés et jamais rendus ; PDF, MOBI, AZW, CBZ et livres audio sont hors périmètre.
- La lecture à voix haute nécessite un moteur de synthèse vocale avec des données de voix pour la langue du livre (Google Speech Services, le moteur du constructeur ou tout autre moteur installé) ; un appareil sans moteur utilisable le signale après environ 20 secondes au lieu de rester silencieux.
- Taille et performances : l'APK release fait environ 3.3 MB ; un livre de 200 MB s'ouvre en 1 à 3 secondes sur un téléphone de 2017, le calcul des positions et de l'empreinte ne retarde jamais la première page, et les livres de plusieurs milliers de chapitres mettent nettement plus longtemps à s'ouvrir (`docs/dev/performance-baseline.md`).

******

### Questions fréquentes

******

#### Comment ma position de lecture est-elle mémorisée ?

La dernière position de chaque livre est enregistrée dans l'espace privé du plugin sous une empreinte du contenu du fichier, jamais sous son chemin ; rouvrir le même livre reprend là où vous vous étiez arrêté. Choisissez `Reprendre au début` dans le menu pour l'effacer.

#### Puis-je changer la police, la taille du texte ou le thème ?

Oui. Ouvrez le panneau de préférences depuis la barre d'outils pour régler la taille du texte, la police (valeur de l'éditeur, avec ou sans empattement, chasse fixe ou les polices d'accessibilité fournies avec Readium), l'interligne, les marges, les espacements, l'alignement, les colonnes et le thème (clair, sépia, sombre ou suivre l'hôte). Touchez `Importer une police` dans le panneau pour ajouter vos propres fichiers TTF ou OTF ; ils sont stockés en privé dans le plugin et se suppriment via `Gérer les polices`.

#### Ce plugin envoie-t-il mes livres quelque part ?

Non. Le plugin n'a pas de serveur. Le réseau n'est utilisé que lorsqu'un livre référence lui-même des ressources distantes, et pour la vérification manuelle des mises à jour de la page de réglages, qui n'interroge l'API GitHub Releases en HTTPS qu'au toucher et ne télécharge jamais rien.

#### Pourquoi les fichiers PDF, MOBI ou AZW ne sont-ils pas pris en charge ?

La liseuse est construite sur la boîte à outils Readium, qui ne rend que l'EPUB. Le PDF demande un autre moteur de rendu, et MOBI / AZW sont des formats Amazon sans moteur de rendu ouvert ; convertissez-les d'abord en EPUB avec un outil comme Calibre. Les archives de bandes dessinées (CBZ) et les livres audio sont aussi hors périmètre.

#### La lecture à voix haute ne produit aucun son

Le plugin parle via le moteur de synthèse vocale choisi dans les réglages système (`Accessibilité > Sortie de la synthèse vocale`). Vérifiez qu'un moteur avec des données de voix pour la langue du livre est installé, que le volume multimédia est monté et qu'aucune autre application ne détient le focus audio (un appel ou de la musique met la lecture en pause). Un livre dans une langue que le moteur ne sait pas parler utilise la voix par défaut du moteur, et un appareil sans moteur utilisable affiche un message après environ 20 secondes.

#### Une police importée n'apparaît pas dans le livre

Les styles de l'éditeur peuvent imposer leurs propres polices : désactivez `Styles de l'éditeur` dans le panneau des préférences et sélectionnez de nouveau la police importée. Seuls les fichiers TTF et OTF sont acceptés (les collections de polices, `.ttc`, sont refusées avec leur propre message), une police s'applique au corps du texte, et les titres auxquels l'éditeur a attribué une famille précise la conservent.

#### Comment les livres verticaux en japonais ou en chinois sont-ils gérés ?

Un livre dont le spine déclare une progression des pages de droite à gauche et une langue japonaise ou chinoise est rendu verticalement et tourne les pages de droite à gauche ; la préférence `Direction du texte` force le texte horizontal ou vertical pour n'importe quel livre. L'interface garde la direction de la langue d'AutoJs6, si bien qu'une interface en anglais reste de gauche à droite pendant que le livre se lit de droite à gauche.

******

### Autorisations et sécurité

******

Le plugin conserve le comportement par défaut de Readium pour le contenu des livres : les scripts et les ressources distantes d'un livre ne sont ni retirés ni bloqués, y compris les ressources en `http://` non chiffré. N'ouvrez que des livres de confiance.

- Moindre privilège : le plugin ne reçoit que l'autorisation temporaire de lecture du content URI accordée par l'hôte, ne voit jamais de chemin du système de fichiers et n'écrit jamais le livre dans le stockage.
- Enveloppe stricte : la requête Explorer Action doit porter exactement une cible EPUB, son dossier parent, une version de protocole correspondante, un build hôte pris en charge et les deux autorisations de lecture ; tout le reste est rejeté avant l'ouverture du fichier.
- Entrée distincte pour les autres applications : `ACTION_VIEW` est pris en charge par sa propre activité exportée, qui n'accepte que des documents `content://` avec autorisation de lecture (jamais `file://`, jamais un dossier), tandis que l'activité Explorer Action reste protégée par l'autorisation de plugin AutoJs6 ; l'autorisation de l'application expéditrice n'est conservée que si vous choisissez `Ajouter aux livres récents`.
- Service de script protégé : le service `org.autojs.plugin.EPUB` est exporté derrière la permission du plugin, ne sert que le paquet hôte AutoJs6 avec une signature correspondante, vérifie chaque requête contre des plafonds fixes (longueur du href, fenêtre de texte, pages de recherche, taille des options, 8 livres ouverts, 64 Mo par ressource) et ne lance jamais le lecteur depuis l'arrière-plan : une session de lecture ne remet à l'hôte qu'un jeton à usage unique, l'hôte démarre lui-même l'Activity du lecteur, une session non réclamée se ferme après 60 s et un mauvais jeton n'ouvre rien.
- Vérification des mises à jour à la demande uniquement : la page de réglages n'interroge l'API GitHub Releases en HTTPS que lorsque vous touchez `Rechercher des mises à jour` (au plus une fois par jour, sans redirection, réponse plafonnée), affiche le résultat et ouvre la page de la version dans votre navigateur ; le plugin ne télécharge ni n'installe jamais rien de lui-même.
- Analyse bornée : un conteneur malformé (pas un ZIP, `container.xml` absent, document de paquet absent, traversée de chemin dans le manifest) se termine par un message d'erreur plutôt qu'un plantage.
- Les liens externes sont affichés en entier et ouverts dans le navigateur système uniquement après confirmation ; les schémas autres que `http` et `https` sont refusés.
- Les données de lecture restent locales : les positions sont indexées par une empreinte du contenu et aucun chemin ni nom de fichier n'est écrit sur le stockage.
- La lecture à voix haute s'exécute dans un service de lecture multimédia non exporté qui n'existe que pendant la lecture et s'arrête quand vous l'arrêtez, à la fin du livre ou à la fermeture de la liseuse (ou, si `Continuer en arrière-plan` est activé, quand vous l'arrêtez depuis la notification) ; le texte est confié au moteur de synthèse vocale choisi dans les réglages du système, et le plugin ne détient aucun verrou de réveil.

Le manifeste demande l'autorisation réseau, l'autorisation de plugin AutoJs6 et, pour la lecture à voix haute, les autorisations de service de premier plan (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) ainsi que `POST_NOTIFICATIONS` sur Android 13+, demandée une seule fois au démarrage de la lecture et refusable (la lecture continue alors sans les commandes de la notification). AndroidX ajoute aussi une autorisation de signature limitée au paquet qui protège les récepteurs dynamiques non exportés ; elle ne donne aucun accès aux données de l'appareil. Aucune autorisation de stockage, média, caméra, localisation, accessibilité ou superposition n'est demandée.

******

### Interface du plugin

******

Les informations suivantes s'adressent aux développeurs; l'hôte découvre et exécute le plugin avec ces identités:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

Explorer Action v2 prend en charge le bouton principal et le menu contextuel pour un fichier, avec une autorisation temporaire de lecture du document et de son dossier parent. AutoJs6 build 5269 ou ultérieur est requis.

- [Voir la matrice de compatibilité Explorer Action](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### Feuille de route

******

ROADMAP.md suit chaque jalon sous forme de liste à cocher avec critères d'acceptation et preuves : P0 à P8 (la liseuse, préférences et polices, recherche et signets, lecture à voix haute, l'entrée autonome, le contrat de l'hôte, l'API de script `epub`, la robustesse et la porte de publication 1.0.0) sont cochés ; P9 (surlignages, notes et export, 1.1.0) est prévu. Les éléments non cochés décrivent des plans et non des capacités livrées. Les retours via Issues sont les bienvenus.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/09/21_

- `Note` Première version : 1.0.0 clôt les phases P0 à P8 de la feuille de route (liseuse, préférences et polices, recherche et signets, lecture à voix haute, entrée autonome, contrat de l'hôte, API de script `epub`, robustesse et porte de publication) ; l'API de script `epub` et les scripts d'exemple sont livrés avec AutoJs6 6.8.0 (build 5282) ; les surlignages, les notes et l'export suivront dans 1.1.0 (feuille de route P9)
- `Fonctionnalité` Un bouton principal `Lire l'EPUB` et une action de menu pour les fichiers `.epub` dans le gestionnaire de fichiers d'AutoJs6 (ID de plugin `readium-epub-reader`, Explorer Action v2); les fichiers portant l'extension `.epub` que l'hôte signale comme `application/zip` sont aussi acceptés
- `Fonctionnalité` Base de la liseuse : les livres EPUB 2 et EPUB 3 sont rendus par le navigateur Readium, avec une table des matières et des liens externes confirmés
- `Fonctionnalité` Mémoire de la position de lecture : la dernière position de chaque livre est enregistrée sous l'empreinte de son contenu (clé rapide à l'ouverture, puis SHA-256 du fichier complet) et restaurée à l'ouverture suivante ; `Reprendre au début` l'efface
- `Fonctionnalité` Interface de la liseuse : titre du livre et chapitre courant dans la barre d'outils, barre de progression avec position synthétique et pourcentage, mode immersif par un appui au centre, zones d'appui et touches de volume pour tourner les pages, bascule du mode défilement
- `Fonctionnalité` Panneau de préférences de lecture : taille du texte, police, interligne, marges, espacement des paragraphes, alignement, césure, styles de l'éditeur, nombre de colonnes et mise en page paginée ou défilante s'appliquent immédiatement et sont mémorisés pour tous les livres ; thèmes clair, sépia et sombre plus `Suivre l'hôte`, la barre d'outils et les barres système prenant les couleurs du thème
- `Fonctionnalité` Import de polices : les fichiers TTF et OTF choisis avec le sélecteur de documents du système sont validés (signature SFNT, table `name`, 20 Mo par fichier, 10 polices), stockés en privé sous `files/fonts/<sha256>` et fournis au navigateur Readium sous forme de déclarations `@font-face` ; les polices importées apparaissent dans le panneau de préférences à côté des polices intégrées et peuvent y être supprimées
- `Fonctionnalité` Livres CJK verticaux et de droite à gauche : le sens de lecture suit la publication (les zones d'appui s'inversent pour les livres de droite à gauche), les livres japonais / chinois à progression de page de droite à gauche s'affichent verticalement via Readium CSS, une préférence `Sens du texte` force le texte horizontal ou vertical, et le sens de mise en page de l'interface reste indépendant du livre
- `Fonctionnalité` Livres à mise en page fixe : `Page x sur N` dans la barre de progression, une préférence `Double page` (auto = deux pages en paysage, une page, deux pages) avec les préférences de texte masquées, et le zoom par pincement de Readium
- `Fonctionnalité` Recherche plein texte : l'entrée `Rechercher` de la barre d'outils ouvre un panneau de résultats chargés 50 par 50 (jusqu'à 500), groupés par chapitre avec leur contexte ; toucher un résultat y mène et la surligne sur la page, avec précédent / suivant dans une barre au-dessus de la progression
- `Fonctionnalité` Signets : une icône de la barre d'outils ajoute ou retire un signet pour la page courante (avec le chapitre et un extrait du texte), et un panneau `Signets` les liste du plus récent au plus ancien avec saut, suppression et tout effacer ; ils sont conservés par livre (jusqu'à 500) à côté de la position de lecture
- `Fonctionnalité` Commandes de lecture : les zones de toucher peuvent être désactivées ou réglées sur gauche / droite ou haut / bas, les claviers physiques tournent les pages avec les flèches, les touches page et l'espace, et le texte sélectionné propose copier, partager, recherche web et les applications de traitement de texte du système
- `Fonctionnalité` Liens : les liens internes au livre s'ouvrent dans la liseuse et la touche retour ramène à l'endroit précédent, les notes de bas de page et de fin s'ouvrent dans une boîte de dialogue, et les liens externes s'ouvrent après confirmation ou, au choix, directement dans le navigateur ; les liens d'autres schémas sont refusés
- `Fonctionnalité` Images : toucher une image l'ouvre en plein écran avec sa légende
- `Fonctionnalité` Lecture à voix haute : le menu déroulant lit le livre depuis la page actuelle avec le moteur de synthèse vocale du système, surligne la phrase lue et tourne les pages ; une barre sous la page et une notification multimédia proposent lecture / pause, phrase précédente / suivante et arrêt, les boutons du casque fonctionnent, la vitesse, la hauteur, la langue et la voix sont réglables, la lecture continue écran éteint et s'arrête à la fermeture de la liseuse
- `Fonctionnalité` Minuteur de sommeil pour la lecture à voix haute (15 / 30 / 60 minutes ou la fin du chapitre), interrupteur pour garder l'écran allumé et `Continuer en arrière-plan` (désactivé par défaut) : une fois activé, la voix continue après la fermeture de la liseuse jusqu'à la fin du livre ou du minuteur, la notification permet de mettre en pause, d'arrêter ou de rouvrir le livre à la phrase lue, et rouvrir le même livre reprend la voix là où elle en est ; la position de lecture est enregistrée quand une voix en arrière-plan s'arrête
- `Fonctionnalité` Les livres sont lus sur place à travers le descripteur de fichier accordé, avec des lectures positionnelles ; rien n'est copié ni extrait vers le stockage
- `Fonctionnalité` Interface, instructions, README et changelog en 10 langues
- `Fonctionnalité` Lanceur autonome : l'icône de l'application ouvre une grille des livres récents (couverture, titre, auteur, progression et dernière lecture, jusqu'à 100) et un bouton `Ouvrir un EPUB` qui choisit un livre avec le sélecteur de documents du système ; les livres choisis conservent une autorisation de lecture persistante et se rouvrent depuis la grille, un livre dont le fichier a disparu est marqué indisponible, et un appui long retire un livre et libère son autorisation
- `Fonctionnalité` Ouverture depuis d'autres applications : les gestionnaires de fichiers, navigateurs et applications de courrier peuvent confier un EPUB `content://` à la liseuse via `ACTION_VIEW` ; le livre s'ouvre comme les autres mais n'apparaît pas dans le lanceur, sauf si `Ajouter aux livres récents` dans le menu de débordement parvient à conserver l'accès accordé par l'expéditeur (il refuse sinon) ; les chemins `file://`, les requêtes sans autorisation de lecture et les dossiers sont rejetés
- `Fonctionnalité` Page de réglages et historique des versions : le menu du lanceur et le menu de débordement de la liseuse ouvrent une page de réglages pour le thème, les zones de toucher, les touches de volume, la vitesse, la hauteur et le minuteur de sommeil par défaut de la lecture à voix haute, les liens externes et les données conservées par le plugin (positions de lecture, livres récents, polices importées, préférences, chacune effacée après confirmation), avec une section À propos, l'historique des versions intégré et une vérification manuelle des mises à jour qui n'interroge GitHub qu'au toucher et ouvre la page de la version dans le navigateur (aucun téléchargement, `Ignorer cette version` mémorisé)
- `Fonctionnalité` Service de capacités EPUB pour l'hôte AutoJs6 (feuille de route P5.2) : le service Binder `org.autojs.plugin.EPUB` ouvre un livre à partir du descripteur en lecture seule de l'hôte et répond avec les métadonnées, la table des matières, l'ordre de lecture, le texte des chapitres (texte brut ou Markdown léger, paginé), les ressources via un tube, la recherche plein texte et le nombre de positions ; au plus 8 livres sont ouverts à la fois, un livre inactif se ferme après 5 minutes, chaque requête est bornée et seul l'hôte AutoJs6 peut appeler le service
- `Fonctionnalité` Session de lecture pilotée par l'hôte sur le contrat EPUB (feuille de route P5.3) : `openReader` ouvre le livre, émet un jeton de session à usage unique et laisse le lancement à l'hôte, qui démarre explicitement l'Activity du lecteur avec ce jeton ; la session signale ensuite les événements `open`, `progress` (au plus toutes les 500 ms), `bookmark`, `error` et `close` avec une seule génération et une séquence strictement croissante, accepte `goTo` (locator, href ou progression), `navigate` (page ou chapitre), `setPreferences` (le sous-ensemble de préférences du contrat ; les clés inconnues sont signalées, pas appliquées), `getBookmarks` et `getState`, remplace une session antérieure, se ferme après 60 s si aucun lecteur ne la réclame, et un `close` de l'hôte laisse le lecteur ouvert sauf demande explicite
- `Correctif` Avertissements de lecture SDK XML v4 avec AGP 9.1 et contrôles d'alignement natif des APK déclenchés par erreur lors de l'assemblage des tests unitaires JVM, avec les plugins de compilation partagés 1.8.3
- `Correctif` Un échec d'écriture de la progression (dossier du livre supprimé, stockage non inscriptible) ne fait plus planter le lecteur ; cet enregistrement est perdu et la lecture continue
- `Correctif` Le lecteur ne meurt plus avec l'hôte lorsque AutoJs6 est arrêté ou mis à jour pendant la lecture de son fournisseur de réglages ; cette lecture échoue simplement et la langue / le mode nuit de l'hôte ne sont pas appliqués
- `Correctif` Une session de l'hôte dont l'intent de lancement atteint un lecteur déjà au sommet de sa tâche (livraison single-top, par exemple après qu'un script a laissé le lecteur ouvert) s'ouvre désormais dans un nouveau lecteur au lieu d'attendre sans être réclamée jusqu'au délai de 60 s ; le lecteur précédent se termine comme s'il était remplacé (feuille de route P6.2)
- `Correctif` Un livre dont le XML NCX / OPF est tronqué ou malformé échoue désormais de façon sûre : le service répond `PARSE_FAILED` et le lecteur affiche son panneau d'échec d'ouverture, au lieu du code `INTERNAL` ou d'un plantage dû à l'`AssertionError` levée par l'analyseur XML de Readium (feuille de route P7.1)
- `Correctif` Une page de `search` est limitée à 50 s côté plugin et répond `TIMEOUT` lorsque la requête ne correspond qu'à la fin d'un livre énorme (50 000 ressources), de sorte que le thread Binder ne reste plus occupé au-delà du délai d'appel de 60 s de l'hôte lui-même (feuille de route P7.1)
- `Correctif` Chaque WebView de page que Readium crée dans le lecteur porte désormais une frontière en plus des réglages propres de Readium : aucun accès au système de fichiers ni aux fournisseurs de contenu et les deux commutateurs d'origine croisée pour les file URL désactivés, tandis que JavaScript reste activé pour Readium (feuille de route D6) ; la revue des frontières du WebView, du conteneur et des composants est consignée dans `docs/dev/security-boundaries.md` (feuille de route P7.2)
- `Correctif` Si le processus du lecteur meurt d'une exception non interceptée, la position de lecture courante est d'abord écrite sur le disque, de façon synchrone, avant que la gestion de plantage du système n'intervienne ; le plugin lui-même n'écrit aucun journal et ne plante aucun arbre Timber, si bien qu'aucun titre, chemin ou texte de livre n'atteint jamais logcat (feuille de route P7.7)
- `Correctif` Choisir une collection TrueType ou OpenType (`.ttc` / `.otc`) comme police de lecture signale désormais que les collections de polices ne sont pas prises en charge, au lieu de déclarer que le fichier n'est pas une police ; trouvé en exécutant la matrice de compatibilité appareil x scénario consignée dans `docs/dev/compatibility-matrix.md` (feuille de route P7.3)
- `Correctif` Accessibilité : les quatre curseurs du panneau des préférences de lecture (taille du texte, marges de page, hauteur de ligne, espacement des paragraphes) portent désormais des libellés qu'un lecteur d'écran peut énoncer, et la barre d'outils du lecteur grandit avec les grandes tailles de police du système au lieu de rogner le sous-titre du chapitre ; un audit instrumentation des libellés, des cibles tactiles de 48 dp, de l'échelle de police 1.3x, du mode nuit, du RTL forcé, de la pagination au clavier et du mode paysage l'étaye (feuille de route P7.6)
- `Correctif` La lecture à voix haute n'attend plus indéfiniment un moteur vocal qui ne termine jamais son initialisation (le Google TTS sans données vocales de l'émulateur API 24 fait exactement cela) : après 20 secondes, le lecteur signale qu'aucun moteur n'est utilisable et revient au repos, et une session qui arriverait plus tard est fermée (feuille de route P7.3)
- `Amélioration` Taille de l'APK release : le lecteur DiViNa que Readium embarque dans les assets du navigateur (427 Ko, jamais utilisé par un lecteur EPUB) est exclu des assets fusionnés et la règle keep globale du paquet du plugin disparaît, si bien que R8 réduit aussi les classes propres du plugin ; l'APK release passe de 3 922 786 o après P5 à 3 328 220 o (feuille de route P7.5, détails dans `docs/dev/release-size.md`)
- `Dépendance` Ajout de Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
- `Dépendance` Ajout de `androidx.media3:media3-session` 1.11.0 (déjà apporté par `readium-navigator-media-tts` ; déclaré directement pour le service de premier plan de lecture à voix haute)
- `Dépendance` Ajout de `org.jsoup:jsoup` 1.23.2 (déjà apporté par `readium-shared` ; déclaré directement pour l'extraction du texte des chapitres du service EPUB)

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilation Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Les paramètres de compilation proviennent de `version.properties`. Le SDK minimum actuel est 24 et le SDK cible est 37.

******

### Localisation et génération des documents

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` localise les métadonnées du plugin et l'interface de la liseuse, tandis que `plugin_instruction.md` fournit les instructions affichées par l'hôte. Pour le README et le changelog, modifiez toujours les sources JSON sous `.readme/` et `.changelog/`, puis exécutez `py .python/generate_markdown.py` pour tout régénérer; les fichiers générés ne sont jamais modifiés à la main. Exécutez `py .python/generate_markdown.py --check` pour vérifier que sources et fichiers générés sont synchronisés.

******

### Licence et mentions tierces

******

Le plugin est publié sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE). Le Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3 et Jsoup, les bibliothèques de contrat AutoJs6 et les autres composants livrés dans l'APK sont listés avec leurs versions, sommes de contrôle et licences dans les [mentions tierces](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Documentation AutoJs6: https://docs.autojs6.com
- Référence de l'API de script `epub`: https://docs.autojs6.com/#/epub
- Spécification EPUB 3.3: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- Mentions tierces: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md
- Alignement des pages de 16 KB et vérification du build: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md
