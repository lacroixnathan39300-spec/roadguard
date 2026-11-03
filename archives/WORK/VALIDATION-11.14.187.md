# Validation 11.14.187

- Le cycle de visibilité du service ne dépend plus de commandes `startService` envoyées dans `onStop` ou `onStart`. `MainActivity` notifie directement le service, dans le même processus; il ferme ou affiche l’overlay selon l’écran ouvert, la préférence et la permission Android.
- Le retour des réglages de superposition est revérifié après un court délai. La commande d’arrêt de surveillance désactive également la préférence automatique.
- Compilation et contrôle de signature sont consignés lors de la livraison.
- Un test sur le Xiaomi reste nécessaire pour confirmer la superposition au-dessus de Waze et de l’accueil.
