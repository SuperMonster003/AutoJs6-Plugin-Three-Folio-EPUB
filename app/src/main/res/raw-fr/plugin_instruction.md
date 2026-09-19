Utiliser Readium EPUB Reader depuis le gestionnaire de fichiers d'AutoJs6 :

1. Installez et activez le plugin `Readium EPUB Reader`.
2. Touchez un fichier `.epub`, ou ouvrez son menu et choisissez `Lire l'EPUB`.
3. Le livre s'ouvre dans une liseuse basée sur le Readium Kotlin Toolkit.

Touchez le tiers gauche ou droit de la page ou appuyez sur les touches de volume pour tourner les pages ; touchez le centre pour masquer ou afficher la barre d'outils. La position de lecture est enregistrée par livre et restaurée à l'ouverture suivante ; choisissez `Reprendre au début` dans le menu pour l'effacer.

Le plugin reçoit un accès temporaire en lecture au fichier sélectionné et à son dossier parent via des content URI. Il ne reçoit jamais de chemin brut du système de fichiers, ne copie jamais le livre vers le stockage et lit le conteneur EPUB directement à travers le descripteur de fichier accordé.

Étape actuelle : la liseuse affiche le livre avec les réglages par défaut de Readium, propose une table des matières, mémorise la position de lecture de chaque livre, offre le mode défilement, les zones d'appui, les touches de volume et le mode immersif, et dispose d'un panneau de préférences pour la taille du texte, la police, les espacements, l'alignement, les colonnes et les thèmes, importe vos propres polices TTF ou OTF et prend en charge les livres CJK verticaux et de droite à gauche, et affiche les livres à mise en page fixe en simple ou double page, et recherche dans tout le livre. Les signets, la lecture à voix haute, l'entrée autonome depuis le lanceur et l'API de script `epub` sont suivis dans ROADMAP.md et arriveront dans des builds ultérieurs.

Les livres peuvent contenir des scripts et des ressources distantes ; le plugin conserve le comportement par défaut de Readium et ne les bloque pas, y compris les ressources en `http://` non chiffré. N'ouvrez que des livres de confiance.

Explorer Action v2 prend en charge le bouton principal et le menu d'un seul fichier. AutoJs6 build 5269 ou ultérieur est requis.
