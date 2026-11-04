package fr.roadguard.app;

import android.service.notification.StatusBarNotification;

import fr.roadguard.app.link.LinkService;

/**
 * Accès aux notifications accordé par l'utilisateur. Il sert à :
 * - lire et piloter la musique en cours (sessions média) ;
 * - transmettre à l'autoradio les messages et appels, uniquement si ROADGUARD Link est démarré
 *   ET que l'option « Notifications sur l'autoradio » est activée. Rien n'est stocké.
 */
public final class SpotifySessionListener extends android.service.notification.NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        LinkService service = LinkService.instance;
        if (service != null) service.onNotificationPosted(sbn);
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        LinkService service = LinkService.instance;
        if (service != null) service.onNotificationRemoved(sbn);
    }
}
