******

### Historique des versions

******

# v1.0.0

###### 2026/09/19

* `Note` Version de développement : les phases P0 de la feuille de route (squelette, validation de Readium, jeux d'essai) sont en cours ; la première version publique arrive avec la phase P8
* `Fonctionnalité` Un bouton principal `Lire l'EPUB` et une action de menu pour les fichiers `.epub` dans le gestionnaire de fichiers d'AutoJs6 (ID de plugin `readium-epub-reader`, Explorer Action v2); les fichiers portant l'extension `.epub` que l'hôte signale comme `application/zip` sont aussi acceptés
* `Fonctionnalité` Base de la liseuse : les livres EPUB 2 et EPUB 3 sont rendus par le navigateur Readium, avec une table des matières et des liens externes confirmés
* `Fonctionnalité` Mémoire de la position de lecture : la dernière position de chaque livre est enregistrée sous l'empreinte de son contenu (clé rapide à l'ouverture, puis SHA-256 du fichier complet) et restaurée à l'ouverture suivante ; `Reprendre au début` l'efface
* `Fonctionnalité` Les livres sont lus sur place à travers le descripteur de fichier accordé, avec des lectures positionnelles ; rien n'est copié ni extrait vers le stockage
* `Fonctionnalité` Interface, instructions, README et changelog en 10 langues
* `Dépendance` Ajout de Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
