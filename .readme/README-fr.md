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

> Étape actuelle (build de développement 1.0.0) : la liseuse ouvre le livre avec les réglages par défaut de Readium et propose une table des matières. La mémorisation de la position, les signets, les préférences, la recherche plein texte, la lecture à voix haute, la mise en page fixe, l'import de polices, l'entrée autonome depuis le lanceur et l'API de script `epub` sont planifiés dans ROADMAP.md et ne sont pas encore disponibles.

******

### Points forts

******

- Moteur Readium : les livres EPUB 2 (NCX) et EPUB 3 (NAV) sont rendus par le navigateur Readium avec Readium CSS, y compris les liens internes, les notes et les images.
- Aucune copie : le conteneur EPUB est lu sur place via un descripteur en lecture seule avec des lectures positionnelles, donc même les gros livres s'ouvrent sans fichier cache.
- Table des matières : accédez à n'importe quel chapitre depuis la barre d'outils ; les entrées imbriquées conservent leur niveau.
- Liens externes : toucher un lien `http` ou `https` affiche l'adresse complète et n'ouvre le navigateur système qu'après confirmation.
- Intégration à l'hôte : menus et dialogues suivent la langue et le mode sombre d'AutoJs6 ; l'enveloppe Explorer Action est validée strictement avant toute ouverture de contenu.
- Multilingue : interface, instructions, README et changelog sont disponibles en 10 langues.

******

### Mode d'emploi

******

1. Téléchargez le dernier APK du plugin depuis la page [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) et installez-le sur votre appareil.
2. Ouvrez le centre de plugins d'AutoJs6 et activez le plugin `Readium EPUB Reader`.
3. Dans le gestionnaire de fichiers d'AutoJs6, touchez un fichier `.epub`, ou ouvrez son menu (autres actions) et choisissez `Lire l'EPUB`.
4. Utilisez le bouton de table des matières de la barre d'outils pour changer de chapitre ; appuyez sur Retour pour fermer la liseuse.

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

#### Pourquoi ma position de lecture n'est-elle pas encore mémorisée ?

La mémorisation de la position et les signets appartiennent au prochain jalon de ROADMAP.md. La version actuelle ouvre toujours le livre au début.

#### Puis-je changer la police, la taille du texte ou le thème ?

Pas encore. Les préférences de lecture (police, taille, interligne, marges, thèmes, mode paginé ou défilement) arrivent avec le jalon des préférences ; la version actuelle utilise les valeurs par défaut de Readium.

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
- `Fonctionnalité` Les livres sont lus sur place à travers le descripteur de fichier accordé, avec des lectures positionnelles ; rien n'est copié ni extrait vers le stockage
- `Fonctionnalité` Interface, instructions, README et changelog en 10 langues
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
