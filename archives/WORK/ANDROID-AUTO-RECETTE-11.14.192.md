# Recette Android Auto — 11.14.192

Ce tableau distingue les contrôles automatiques de la validation qui exige un hôte connecté. Aucun résultat physique n'est coché par déduction.

| Scénario | Vérification effectuée | Reste à observer sur hôte / véhicule |
|---|---|---|
| Installation / apparition | Classes et manifeste dans l'APK; cible36 | Installation approuvée Play, apparition dans le lanceur |
| Permissions GPS | Message de permission, retrait en session, arrêt propre des services testés | Accepter/refuser depuis le flux Android Auto réel |
| Android16 | Marges barres/encoche/clavier, refus notifications et retour testés | Gestes et rendu sur Xiaomi Android16 |
| Carte / écran | Remplacement, destruction et arrivée tardive de surfaces testés | Carte visible sur écrans petit/grand, format large et tableau de bord |
| Recherche | Réponse du serveur public vérifiée; résultats, erreurs et obsolescence testés | Clavier/dictée gérés par l'hôte, choix d'adresse dans la voiture |
| Favoris | Pagination et lecture du téléphone testées | Manipulation avec commandes du véhicule |
| Guidage | Templates, manœuvres, arrivée, arrêt, recalcul et GPS périmé testés | Trajet réel et synchronisation de la carte avec l'écran voiture |
| Reconnexion | Nouvelle session reprend le trajet enregistré; ressources de l'ancienne libérées | Débrancher/rebrancher USB, coupure liaison sans fil, téléphone verrouillé |
| Voix | Disponibilité TTS, focus audio et erreurs gérés en code; scénarios existants testés | Voix française réelle, musique et appels, coupure/reprise audio |
| Réseau / cache | Cache PNG, conservation après réponse invalide et réutilisation testés | Route connue en coupure réseau puis reconnexion |
| Compte communauté | Cache public testé; recherche publique vérifiée | Association et publication avec un vrai compte, consentements |

Points à consigner : marque/modèle d'écran, version Android Auto, Android du téléphone, version APK, installation utilisée, scénario, résultat et extrait Logcat en cas de panne. Ne pas inclure de jetons de compte.

Si le démarrage d'un service de localisation est refusé par Android lorsque le téléphone est verrouillé, relever le refus exact dans Logcat; les tests Robolectric n'émulent pas toutes les restrictions de service du système et de l'hôte. La vérification de permission en code ne suffit pas à prouver cet accès réel.
