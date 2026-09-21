Utiliser Readium EPUB Reader depuis le gestionnaire de fichiers d'AutoJs6 :

1. Installez et activez le plugin `Readium EPUB Reader`.
2. Touchez un fichier `.epub`, ou ouvrez son menu et choisissez `Lire l'EPUB`.
3. Le livre s'ouvre dans une liseuse basée sur le Readium Kotlin Toolkit.

Touchez le tiers gauche ou droit de la page ou appuyez sur les touches de volume pour tourner les pages ; touchez le centre pour masquer ou afficher la barre d'outils. La position de lecture est enregistrée par livre et restaurée à l'ouverture suivante ; choisissez `Reprendre au début` dans le menu pour l'effacer.

Le plugin reçoit un accès temporaire en lecture au fichier sélectionné et à son dossier parent via des content URI. Il ne reçoit jamais de chemin brut du système de fichiers, ne copie jamais le livre vers le stockage et lit le conteneur EPUB directement à travers le descripteur de fichier accordé.

1.0.0 est la première version. La liseuse ouvre les livres EPUB 2 et EPUB 3 avec une table des matières, mémorise la position de lecture de chaque livre, offre le mode défilement, les zones d'appui, les touches de volume et le mode immersif, un panneau de préférences (taille du texte, police, espacements, alignement, colonnes et thèmes qui peuvent suivre le mode nuit de l'hôte), les polices TTF / OTF importées, les livres CJK verticaux et de droite à gauche, les livres à mise en page fixe en page simple ou en double page, la recherche plein texte, les signets, les liens dans le livre, les notes et les images, et la lecture à voix haute avec le moteur de synthèse vocale du système. L'icône de l'application ouvre un lanceur avec les livres récents et le sélecteur de documents du système, les autres applications transmettent un EPUB via `ACTION_VIEW`, et la page des paramètres couvre les valeurs par défaut de la liseuse, les données conservées sur l'appareil et une vérification manuelle des mises à jour. L'API de script `epub`, la session de liseuse de l'hôte et trois scripts d'exemple sont livrés avec AutoJs6 6.8.0 (build 5282). Les surlignages, les notes et l'export sont prévus pour 1.1.0 (ROADMAP.md, P9). Le service `org.autojs.plugin.EPUB` qui se trouve derrière répond à l'hôte AutoJs6 avec les métadonnées, la table des matières, le texte, les ressources et la recherche, et ouvre une session de liseuse pilotée par l'hôte (`epub.open(path)`, `epub.read(path)`, exemples sous `Exemples > Livres numériques`).

Les livres peuvent contenir des scripts et des ressources distantes ; le plugin conserve le comportement par défaut de Readium et ne les bloque pas, y compris les ressources en `http://` non chiffré. N'ouvrez que des livres de confiance.

Explorer Action v2 prend en charge le bouton principal et le menu d'un seul fichier. AutoJs6 build 5269 ou ultérieur est requis.
