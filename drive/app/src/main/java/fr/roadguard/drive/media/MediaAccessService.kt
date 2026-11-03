package fr.roadguard.drive.media

import android.service.notification.NotificationListenerService

/**
 * Accès aux notifications : Android l'exige pour lire le titre en cours et piloter
 * les lecteurs des autres applis (Spotify, Bluetooth du poste, radio…). Aucune notification n'est lue ni stockée.
 */
class MediaAccessService : NotificationListenerService()
