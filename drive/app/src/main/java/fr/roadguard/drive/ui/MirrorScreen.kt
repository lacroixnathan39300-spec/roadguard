package fr.roadguard.drive.ui

import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fr.roadguard.drive.link.LinkClient
import fr.roadguard.drive.link.VideoDecoder
import kotlinx.coroutines.delay

/** Écran du téléphone en plein écran (mode miroir), avec le tactile renvoyé au téléphone. */
@Composable
fun MirrorScreen(link: LinkClient, width: Int, height: Int, onClose: () -> Unit) {
    val holder = remember(width, height) { arrayOfNulls<VideoDecoder>(1) }
    DisposableEffect(width, height) {
        onDispose {
            link.videoSink = null
            holder[0]?.release()
            holder[0] = null
        }
    }
    // Tant que l'image n'est pas arrivée, redemander la configuration et une image complète.
    LaunchedEffect(width, height) {
        while (holder[0]?.started != true) { link.requestKeyFrame(); delay(1_500) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Box(
            Modifier.aspectRatio(width.toFloat() / height, matchHeightConstraintsFirst = width < height)
                .pointerInput(width, height) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        link.touch("down", down.position.x / size.width, down.position.y / size.height)
                        var lastSent = 0L
                        while (true) {
                            val event = awaitPointerEvent()
                            val c = event.changes.firstOrNull() ?: break
                            val x = c.position.x / size.width
                            val y = c.position.y / size.height
                            if (!c.pressed) { link.touch("up", x, y); break }
                            if (c.uptimeMillis - lastSent > 40) { lastSent = c.uptimeMillis; link.touch("move", x, y) }
                        }
                    }
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        this.holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(h: SurfaceHolder) {
                                val decoder = VideoDecoder(h.surface, width, height)
                                holder[0] = decoder
                                link.videoSink = decoder::feed
                                link.requestKeyFrame()
                            }
                            override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) = Unit
                            override fun surfaceDestroyed(h: SurfaceHolder) {
                                link.videoSink = null
                                holder[0]?.release()
                                holder[0] = null
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            BigButton("✕ Accueil", Color(0xCC121820), onClose)
        }
    }
}
