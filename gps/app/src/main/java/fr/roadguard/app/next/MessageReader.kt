package fr.roadguard.app.next

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateOf

/** Message reçu pendant la conduite (affiché en bandeau, avec réponse à la voix si l'appli le permet). */
data class IncomingMessage(val app: String, val from: String, val text: String, val at: Long, val reply: Notification.Action?)

/**
 * Lecture à voix haute des messages (SMS, WhatsApp, Messenger, Telegram, Signal…) pendant la conduite,
 * et réponse dictée. Autorisation « Accès aux notifications » à donner une fois dans Android.
 */
class MessageReader : NotificationListenerService() {
    companion object {
        val last = mutableStateOf<IncomingMessage?>(null)
        private val apps = mapOf(
            "com.whatsapp" to "WhatsApp", "com.whatsapp.w4b" to "WhatsApp", "com.facebook.orca" to "Messenger",
            "org.telegram.messenger" to "Telegram", "org.thoughtcrime.securesms" to "Signal", "com.snapchat.android" to "Snapchat",
            "com.google.android.apps.messaging" to "SMS", "com.samsung.android.messaging" to "SMS", "com.android.mms" to "SMS",
            "com.android.messaging" to "SMS", "com.instagram.android" to "Instagram",
        )
        private val heard = ArrayDeque<String>()

        fun enabled(context: Context): Boolean =
            android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true

        /** Envoie la réponse dictée par l'action « Répondre » de la notification. */
        fun reply(context: Context, msg: IncomingMessage, text: String): Boolean = runCatching {
            val action = msg.reply ?: return false
            val inputs = action.remoteInputs ?: return false
            val intent = Intent()
            val results = Bundle()
            inputs.forEach { results.putCharSequence(it.resultKey, text) }
            RemoteInput.addResultsToIntent(inputs, intent, results)
            action.actionIntent.send(context, 0, intent)
            true
        }.getOrDefault(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = apps[sbn.packageName] ?: return
        val session = DriveService.session ?: return
        if (!session.settings.speakNotifications) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ex = n.extras
        val from = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() } ?: return
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.takeIf { it.isNotBlank() } ?: return
        val key = "${sbn.packageName}|$from|$text"
        if (key in heard) return
        heard.addLast(key); if (heard.size > 50) heard.removeFirst()
        val reply = n.actions?.firstOrNull { a -> a.remoteInputs?.isNotEmpty() == true }
        last.value = IncomingMessage(app, from, text.take(400), System.currentTimeMillis(), reply)
        session.voice.speak("Message $app de $from : ${text.take(220)}")
    }
}

