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

> Étape actuelle (build de développement 1.0.0) : la liseuse ouvre le livre avec les réglages par défaut de Readium, propose une table des matières, mémorise la position de lecture de chaque livre, offre le mode défilement, les zones d'appui, les touches de volume et le mode immersif, et dispose d'un panneau de préférences pour la taille du texte, la police, les espacements, l'alignement, les colonnes et les thèmes, qui peut suivre le mode nuit de l'hôte, importe vos propres polices TTF ou OTF et prend en charge les livres CJK verticaux et de droite à gauche, et affiche les livres à mise en page fixe en simple ou double page, et recherche dans tout le livre. Les signets, la lecture à voix haute, l'entrée autonome depuis le lanceur et l'API de script `epub` sont planifiés dans ROADMAP.md et ne sont pas encore disponibles.

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
- Liens externes : toucher un lien `http` ou `https` affiche l'adresse complète et n'ouvre le navigateur système qu'après confirmation.
- Intégration à l'hôte : menus et dialogues suivent la langue et le mode sombre d'AutoJs6 ; l'enveloppe Explorer Action est validée strictement avant toute ouverture de contenu.
- Multilingue : interface, instructions, README et changelog sont disponibles en 10 langues.

******

### Mode d'emploi

******

1. Téléchargez le dernier APK du plugin depuis la page [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) et installez-le sur votre appareil.
2. Ouvrez le centre de plugins d'AutoJs6 et activez le plugin `Readium EPUB Reader`.
3. Dans le gestionnaire de fichiers d'AutoJs6, touchez un fichier `.epub`, ou ouvrez son menu (autres actions) et choisissez `Lire l'EPUB`.
4. Utilisez le bouton de table des matières de la barre d'outils pour changer de chapitre et le bouton de préférences pour ajuster le texte et le thème ; touchez le tiers gauche ou droit de la page ou appuyez sur les touches de volume pour tourner les pages, et touchez le centre pour masquer ou afficher la barre d'outils ; appuyez sur Retour pour fermer la liseuse, la position est mémorisée.

> Si le plugin n'apparaît pas dans le centre de plugins, mettez d'abord AutoJs6 à jour vers une version récente (build interne 5269 ou ultérieur). Explorer Action v2 prend en charge le bouton principal et le menu contextuel pour un fichier, avec une autorisation temporaire de lecture du document et de son dossier parent.

******

### Formats pris en charge

******

Le plugin reconnaît l'extension suivante, ainsi que les fichiers sans extension explicitement marqués `application/epub+zip` par l'hôte:

```text
epub
```

Seul l'EPUB est pris en charge : livres redistribuables et à mise en page fixe en EPUB 2 ou EPUB 3. Les archives de bandes dessinées (CBZ), les livres audio, les PDF et les livres protégés par LCP sont hors périmètre ; un livre marqué comme chiffré LCP est signalé comme illisible au lieu d'afficher du contenu corrompu.

******

### Questions fréquentes

******

#### Comment ma position de lecture est-elle mémorisée ?

La dernière position de chaque livre est enregistrée dans l'espace privé du plugin sous une empreinte du contenu du fichier, jamais sous son chemin ; rouvrir le même livre reprend là où vous vous étiez arrêté. Choisissez `Reprendre au début` dans le menu pour l'effacer.

#### Puis-je changer la police, la taille du texte ou le thème ?

Oui. Ouvrez le panneau de préférences depuis la barre d'outils pour régler la taille du texte, la police (valeur de l'éditeur, avec ou sans empattement, chasse fixe ou les polices d'accessibilité fournies avec Readium), l'interligne, les marges, les espacements, l'alignement, les colonnes et le thème (clair, sépia, sombre ou suivre l'hôte). Touchez `Importer une police` dans le panneau pour ajouter vos propres fichiers TTF ou OTF ; ils sont stockés en privé dans le plugin et se suppriment via `Gérer les polices`.

#### Ce plugin envoie-t-il mes livres quelque part ?

Non. Le plugin n'a pas de serveur. Le réseau n'est utilisé que lorsqu'un livre référence lui-même des ressources distantes, et pour la vérification manuelle des mises à jour prévue sur la page de réglages autonome.

******

### Autorisations et sécurité

******

Le plugin conserve le comportement par défaut de Readium pour le contenu des livres : les scripts et les ressources distantes d'un livre ne sont ni retirés ni bloqués, y compris les ressources en `http://` non chiffré. N'ouvrez que des livres de confiance.

- Moindre privilège : le plugin ne reçoit que l'autorisation temporaire de lecture du content URI accordée par l'hôte, ne voit jamais de chemin du système de fichiers et n'écrit jamais le livre dans le stockage.
- Enveloppe stricte : la requête Explorer Action doit porter exactement une cible EPUB, son dossier parent, une version de protocole correspondante, un build hôte pris en charge et les deux autorisations de lecture ; tout le reste est rejeté avant l'ouverture du fichier.
- Analyse bornée : un conteneur malformé (pas un ZIP, `container.xml` absent, document de paquet absent, traversée de chemin dans le manifest) se termine par un message d'erreur plutôt qu'un plantage.
- Les liens externes sont affichés en entier et ouverts dans le navigateur système uniquement après confirmation ; les schémas autres que `http` et `https` sont refusés.
- Les données de lecture restent locales : les positions sont indexées par une empreinte du contenu et aucun chemin ni nom de fichier n'est écrit sur le stockage.

Le manifeste ne demande que l'autorisation réseau et l'autorisation de plugin AutoJs6. AndroidX ajoute aussi une autorisation de signature limitée au paquet qui protège les récepteurs dynamiques non exportés ; elle ne donne aucun accès aux données de l'appareil. Aucune autorisation de stockage, média, caméra, localisation, accessibilité ou superposition n'est demandée.

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

Les capacités prévues et leur état d'avancement sont suivis dans ROADMAP.md sous forme de liste à cocher, organisée par jalons avec critères d'acceptation : mémorisation de la position et signets, préférences et import de polices, recherche plein texte, lecture à voix haute, mise en page fixe, entrée d'application autonome, contrat hôte et API de script `epub`. Les éléments non cochés décrivent des plans et non des capacités livrées. Les retours via Issues sont bienvenus.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.0.0

_2026/09/19_

- `Note` Version de développement : les phases P0 de la feuille de route (squelette, validation de Readium, jeux d'essai) sont en cours ; la première version publique arrive avec la phase P8
- `Fonctionnalité` Un bouton principal `Lire l'EPUB` et une action de menu pour les fichiers `.epub` dans le gestionnaire de fichiers d'AutoJs6 (ID de plugin `readium-epub-reader`, Explorer Action v2); les fichiers portant l'extension `.epub` que l'hôte signale comme `application/zip` sont aussi acceptés
- `Fonctionnalité` Base de la liseuse : les livres EPUB 2 et EPUB 3 sont rendus par le navigateur Readium, avec une table des matières et des liens externes confirmés
- `Fonctionnalité` Mémoire de la position de lecture : la dernière position de chaque livre est enregistrée sous l'empreinte de son contenu (clé rapide à l'ouverture, puis SHA-256 du fichier complet) et restaurée à l'ouverture suivante ; `Reprendre au début` l'efface
- `Fonctionnalité` Interface de la liseuse : titre du livre et chapitre courant dans la barre d'outils, barre de progression avec position synthétique et pourcentage, mode immersif par un appui au centre, zones d'appui et touches de volume pour tourner les pages, bascule du mode défilement
- `Fonctionnalité` Panneau de préférences de lecture : taille du texte, police, interligne, marges, espacement des paragraphes, alignement, césure, styles de l'éditeur, nombre de colonnes et mise en page paginée ou défilante s'appliquent immédiatement et sont mémorisés pour tous les livres ; thèmes clair, sépia et sombre plus `Suivre l'hôte`, la barre d'outils et les barres système prenant les couleurs du thème
- `Fonctionnalité` Import de polices : les fichiers TTF et OTF choisis avec le sélecteur de documents du système sont validés (signature SFNT, table `name`, 20 Mo par fichier, 10 polices), stockés en privé sous `files/fonts/<sha256>` et fournis au navigateur Readium sous forme de déclarations `@font-face` ; les polices importées apparaissent dans le panneau de préférences à côté des polices intégrées et peuvent y être supprimées
- `Fonctionnalité` Livres CJK verticaux et de droite à gauche : le sens de lecture suit la publication (les zones d'appui s'inversent pour les livres de droite à gauche), les livres japonais / chinois à progression de page de droite à gauche s'affichent verticalement via Readium CSS, une préférence `Sens du texte` force le texte horizontal ou vertical, et le sens de mise en page de l'interface reste indépendant du livre
- `Fonctionnalité` Livres à mise en page fixe : `Page x sur N` dans la barre de progression, une préférence `Double page` (auto = deux pages en paysage, une page, deux pages) avec les préférences de texte masquées, et le zoom par pincement de Readium
- `Fonctionnalité` Recherche plein texte : l'entrée `Rechercher` de la barre d'outils ouvre un panneau de résultats chargés 50 par 50 (jusqu'à 500), groupés par chapitre avec leur contexte ; toucher un résultat y mène et la surligne sur la page, avec précédent / suivant dans une barre au-dessus de la progression
- `Fonctionnalité` Les livres sont lus sur place à travers le descripteur de fichier accordé, avec des lectures positionnelles ; rien n'est copié ni extrait vers le stockage
- `Fonctionnalité` Interface, instructions, README et changelog en 10 langues
- `Correctif` Avertissements de lecture SDK XML v4 avec AGP 9.1 et contrôles d'alignement natif des APK déclenchés par erreur lors de l'assemblage des tests unitaires JVM, avec les plugins de compilation partagés 1.8.3
- `Correctif` Un échec d'écriture de la progression (dossier du livre supprimé, stockage non inscriptible) ne fait plus planter le lecteur ; cet enregistrement est perdu et la lecture continue
- `Dépendance` Ajout de Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

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

### Liens

******

- Documentation AutoJs6: https://docs.autojs6.com
- Spécification EPUB 3.3: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
