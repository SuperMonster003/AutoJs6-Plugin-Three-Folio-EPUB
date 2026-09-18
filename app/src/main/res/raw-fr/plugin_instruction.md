Utiliser Readium EPUB Reader depuis le gestionnaire de fichiers d'AutoJs6 :

1. Installez et activez le plugin `Readium EPUB Reader`.
2. Touchez un fichier `.epub`, ou ouvrez son menu et choisissez `Lire l'EPUB`.
3. Le livre s'ouvre dans une liseuse basée sur le Readium Kotlin Toolkit.

Le plugin reçoit un accès temporaire en lecture au fichier sélectionné et à son dossier parent via des content URI. Il ne reçoit jamais de chemin brut du système de fichiers, ne copie jamais le livre vers le stockage et lit le conteneur EPUB directement à travers le descripteur de fichier accordé.

Étape actuelle : la liseuse affiche le livre avec les réglages par défaut de Readium et une table des matières. La mémorisation de la position, les signets, les préférences, la recherche, la lecture à voix haute, la mise en page fixe, l'import de polices, l'entrée autonome depuis le lanceur et l'API de script `epub` sont suivis dans ROADMAP.md et arriveront dans des versions ultérieures.

Les livres peuvent contenir des scripts et des ressources distantes ; le plugin conserve le comportement par défaut de Readium et ne les bloque pas, y compris les ressources en `http://` non chiffré. N'ouvrez que des livres de confiance.

Explorer Action v2 prend en charge le bouton principal et le menu d'un seul fichier. AutoJs6 build 5269 ou ultérieur est requis.
