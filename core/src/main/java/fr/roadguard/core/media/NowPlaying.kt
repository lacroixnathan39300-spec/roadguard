package fr.roadguard.core.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Morceau en cours, quelle que soit l'appli qui le joue. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val app: String,
    val artwork: Bitmap?,
    val playing: Boolean,
)

/** Lecture des sessions média actives et commandes (précédent, lecture/pause, suivant). */
class MediaHub(private val context: Context, private val listener: ComponentName) {
    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var current: MediaController? = null

    fun hasAccess(): Boolean =
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.contains(context.packageName) == true

    fun nowPlaying(): Flow<NowPlaying?> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        var watched: MediaController? = null
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) { trySend(describe(watched)) }
            override fun onPlaybackStateChanged(state: PlaybackState?) { trySend(describe(watched)) }
            override fun onSessionDestroyed() { trySend(null) }
        }
        fun pick(controllers: List<MediaController>?) {
            watched?.unregisterCallback(callback)
            watched = choose(controllers.orEmpty())
            current = watched
            watched?.registerCallback(callback, handler)
            trySend(describe(watched))
        }
        val sessions = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessions, listener, handler)
            pick(manager.getActiveSessions(listener))
        }.onFailure { trySend(null) }
        awaitClose {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessions) }
            watched?.unregisterCallback(callback)
        }
    }

    fun previous() { current?.transportControls?.skipToPrevious() }
    fun next() { current?.transportControls?.skipToNext() }
    fun playPause() {
        val c = current ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    /** Priorité à la session qui joue ; sinon la plus récente. */
    private fun choose(controllers: List<MediaController>): MediaController? =
        controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()

    private fun describe(c: MediaController?): NowPlaying? {
        val m = c?.metadata ?: return null
        val title = m.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isBlank()) return null
        val app = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault(c.packageName)
        return NowPlaying(
            title = title,
            artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            app = app,
            artwork = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
        )
    }
}
