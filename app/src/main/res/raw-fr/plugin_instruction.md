3-Folio EPUB est le plugin de lecture de livres EPUB d'AutoJs6 et sert aussi de liseuse autonome. Il ouvre les livres EPUB 2 et EPUB 3 avec table des matières, recherche plein texte, signets, surlignages et notes, et lecture à voix haute; les scripts peuvent aussi lire le contenu des livres ou ouvrir la liseuse.

### Prise en main

1. Vérifiez que ce plugin est activé dans le "Centre des plugins" d'AutoJs6.
2. Dans le gestionnaire de fichiers d'AutoJs6, touchez un fichier EPUB pour ouvrir le livre dans la liseuse; vous pouvez aussi choisir "Lire l'EPUB" dans le menu de ce fichier.
3. Touchez le bord gauche ou droit de la page ou appuyez sur une touche de volume pour tourner les pages, et touchez le centre pour afficher ou masquer la barre d'outils; le même livre se rouvre là où vous l'aviez laissé.
4. Pour l'utiliser sans AutoJs6, ouvrez ce plugin depuis son icône et touchez "Ouvrir un EPUB" pour choisir un livre.

### Fonctionnalités principales

- **Table des matières et position de lecture**: Accédez à n'importe quel chapitre depuis "Table des matières"; chaque livre retient votre position, même déplacé ou renommé, et "Reprendre au début" l'efface.
- **Préférences de lecture**: Dans "Préférences de lecture", réglez la taille et la police, les espacements, l'alignement et la pagination, et choisissez "Clair", "Sépia", "Sombre" ou "Suivre l'hôte"; "Importer une police" ajoute des polices TTF ou OTF.
- **Texte vertical et mise en page fixe**: Les livres chinois et japonais verticaux s'affichent à la verticale, et "Sens du texte" peut forcer l'horizontal ou le vertical; les livres à mise en page fixe proposent "Double page" et le zoom à deux doigts.
- **Recherche et signets**: "Rechercher" liste chaque occurrence du livre avec son contexte, et un appui y mène; l'icône de la barre d'outils marque la page actuelle, et "Signets" liste et supprime les signets.
- **Surlignages et notes**: Sélectionnez du texte et choisissez "Surligner" ou "Ajouter une note", avec cinq couleurs et un style souligné; "Surlignages et notes" permet d'y accéder et de les modifier, et "Exporter" les enregistre dans un fichier Markdown.
- **Lecture à voix haute**: "Lecture à voix haute" lit depuis la page actuelle avec le moteur de synthèse vocale du système, surligne la phrase lue et tourne les pages; réglez la vitesse, la voix et le "Minuteur de sommeil", et activez "Continuer en arrière-plan" pour continuer après la fermeture de la liseuse.
- **Liens et images**: Les liens internes permettent de revenir en arrière et les notes de bas de page s'ouvrent dans une fenêtre; par défaut, les liens externes affichent d'abord l'adresse complète et ne passent au navigateur qu'après confirmation; touchez une image pour l'afficher en plein écran.
- **Livres récents et autres applications**: L'icône du plugin ouvre vos livres récents; les EPUB partagés par d'autres applications s'ouvrent directement, et "Ajouter aux livres récents" les garde dans la liste.
- **Apparence et paramètres**: La langue, le mode nuit et "Couleur du thème" suivent AutoJs6 par défaut; "Paramètres" règle aussi les valeurs par défaut de pagination et de lecture à voix haute, et propose "Rechercher des mises à jour" et "Historique des versions".

### Utilisation dans les scripts

Le module `epub` d'AutoJs6 est fourni par ce plugin. Il lit les métadonnées, la table des matières et le texte d'un livre sans ouvrir la liseuse:

```js
let book = epub.open('./book.epub');
console.log(book.metadata.title, book.metadata.authors.join(', '));
book.toc.forEach(entry => console.log(entry.title));
console.log(book.text(0, { maxChars: 500 }));
book.close();
```

`epub.read()` ouvre la liseuse de ce plugin et transmet la progression de lecture au script:

```js
let session = epub.read('./book.epub');
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('error', e => console.warn(e.toString()));
session.on('close', e => console.log('closed:', e.reason));
```

- Les chemins peuvent être absolus ou relatifs au répertoire de travail du script; `content://` et les autres URI ne sont pas acceptés.
- Les erreurs sont levées sous forme de `epub.EpubError`, dont le `code` indique la cause, par exemple `PLUGIN_DISABLED` quand le plugin est désactivé; vérifiez d'abord avec `epub.isAvailable()`.
- La plupart des méthodes ont une forme Async qui renvoie une Promise, par exemple `epub.openAsync()`.
- C'est AutoJs6 qui démarre la liseuse, donc les limites du système sur l'ouverture d'écrans en arrière-plan s'appliquent; lancez les scripts qui appellent `epub.read()` quand AutoJs6 est visible.

### Prérequis

- Android 7.0 ou version ultérieure, avec la WebView du système.
- L'ouverture de livres depuis le gestionnaire de fichiers et le module `epub` nécessitent AutoJs6 6.8.0 ou version ultérieure; l'utilisation autonome depuis l'icône ne nécessite pas AutoJs6.
- La lecture à voix haute nécessite un moteur de synthèse vocale disposant des données vocales de la langue du livre.
- Sur Android 13 et versions ultérieures, les commandes de lecture dans la notification nécessitent l'autorisation de notification; la lecture à voix haute fonctionne même si vous la refusez.

### Questions fréquentes

**Raisons pour lesquelles "Lire l'EPUB" n'apparaît pas dans le gestionnaire de fichiers**

Vérifiez qu'AutoJs6 remplit la condition de version et que ce plugin est activé dans le "Centre des plugins"; une fois les deux réunies, les fichiers EPUB proposent "Lire l'EPUB".

**Formats de livres pris en charge**

Les formats EPUB 2 et EPUB 3 sont pris en charge, y compris les livres à mise en page fixe. PDF, MOBI, AZW, CBZ et les livres audio ne le sont pas; convertissez-les d'abord en EPUB avec un outil comme Calibre. Les livres protégés par DRM affichent un avertissement et ne peuvent pas être lus.

**Cas où la lecture à voix haute reste muette**

Vérifiez que le moteur de synthèse vocale du système dispose des données vocales de la langue du livre, et augmentez le volume multimédia. Si l'appareil n'a aucun moteur utilisable, un avertissement apparaît après environ 20 secondes.

**Cas où ce plugin utilise le réseau**

Ce plugin n'envoie jamais les livres ni les données de lecture; il ne se connecte que lorsqu'un livre fait référence à des ressources distantes ou que vous touchez "Rechercher des mises à jour". Les livres peuvent contenir des scripts, n'ouvrez donc que des livres de confiance.

Consultez la [documentation d'AutoJs6](https://docs.autojs6.com/#/epub) pour l'API de script complète, et la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB) pour plus de détails.
