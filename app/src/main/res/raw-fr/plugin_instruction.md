Utiliser Readium EPUB Reader depuis le gestionnaire de fichiers d'AutoJs6 :

1. Installez et activez le plugin `Readium EPUB Reader`.
2. Touchez un fichier `.epub`, ou ouvrez son menu et choisissez `Lire l'EPUB`.
3. Le livre s'ouvre dans une liseuse basée sur le Readium Kotlin Toolkit.

Touchez le tiers gauche ou droit de la page ou appuyez sur les touches de volume pour tourner les pages ; touchez le centre pour masquer ou afficher la barre d'outils. La position de lecture est enregistrée par livre et restaurée à l'ouverture suivante ; choisissez `Reprendre au début` dans le menu pour l'effacer.

Le plugin reçoit un accès temporaire en lecture au fichier sélectionné et à son dossier parent via des content URI. Il ne reçoit jamais de chemin brut du système de fichiers, ne copie jamais le livre vers le stockage et lit le conteneur EPUB directement à travers le descripteur de fichier accordé.

Étape actuelle : la liseuse affiche le livre avec les réglages par défaut de Readium, propose une table des matières, mémorise la position de lecture de chaque livre, offre le mode défilement, les zones d'appui, les touches de volume et le mode immersif, et dispose d'un panneau de préférences pour la taille du texte, la police, les espacements, l'alignement, les colonnes et les thèmes, importe vos propres polices TTF ou OTF et prend en charge les livres CJK verticaux et de droite à gauche, et affiche les livres à mise en page fixe en simple ou double page, et recherche dans tout le livre, et conserve des signets, et gère les liens internes, les notes et les images avec des zones de toucher configurables et le clavier, et lit à voix haute avec le moteur de synthèse vocale du système. L'icône de l'application ouvre un lanceur autonome avec les livres récents et le sélecteur de documents du système, d'autres applications peuvent confier un EPUB via `ACTION_VIEW`, et la page de réglages couvre les valeurs par défaut de la liseuse, les données conservées sur l'appareil et une vérification manuelle des mises à jour. Un service `org.autojs.plugin.EPUB` fournit à l'hôte AutoJs6 les métadonnées, la table des matières, le texte, les ressources et la recherche, et ouvre une session de lecture pilotée par l'hôte ; l'API de script `epub` est suivie dans ROADMAP.md et arrivera avec le client hôte.

Les livres peuvent contenir des scripts et des ressources distantes ; le plugin conserve le comportement par défaut de Readium et ne les bloque pas, y compris les ressources en `http://` non chiffré. N'ouvrez que des livres de confiance.

Explorer Action v2 prend en charge le bouton principal et le menu d'un seul fichier. AutoJs6 build 5269 ou ultérieur est requis.
