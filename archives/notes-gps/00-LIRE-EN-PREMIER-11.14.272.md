# ROADGUARD 11.14.272 — connexion obligatoire

L'application ouvre l'écran Mon compte tant qu'aucun téléphone n'a été associé et confirmé sur le site. Le bouton « Créer mon compte / Se connecter » prépare un code, ouvre le navigateur pour la connexion au site et la confirmation du téléphone, puis l'application ouvre automatiquement la carte dès que l'association est approuvée.

La carte, le GPS, les commandes de la page principale, l'écran d'import radar et le service de surveillance radar sont bloqués avant la confirmation. Après une première connexion, un téléphone déjà associé conserve l'accès aux cartes téléchargées en absence de réseau. L'application recontrôle la session à son retour au premier plan et périodiquement ; une révocation confirmée par le serveur ferme la carte et arrête la surveillance.

Le numéro est déclaré sur le site, sans vérification par SMS. Cette version ne prouve donc pas l'unicité d'une personne ou d'un numéro.

## Vérification

- JavaScript de l'interface : vérification syntaxique Node réussie.
- Contrôles statiques des chemins de verrouillage : réussis.
- Compilation Android : non exécutée, Gradle 8.11.1 non disponible localement et téléchargement du wrapper inaccessible.
- 🔴 INTERVENTION OBLIGATOIRE : compiler le projet et tester sur le téléphone la première association, la reprise sans réseau d'un compte déjà confirmé, la révocation du téléphone depuis le site, la déconnexion et l'arrêt de la bulle radar.

Le fichier ZIP contient le projet complet. Il ne contient pas un nouvel APK compilé.
