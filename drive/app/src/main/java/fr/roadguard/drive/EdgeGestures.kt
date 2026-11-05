package fr.roadguard.drive

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Gestes sur les bords de l'écran (comme Car Launcher) :
 *  - depuis le bord gauche vers la droite : retour (ferme l'écran ouvert) ;
 *  - depuis le bord droit vers la gauche : toutes les applis ;
 *  - en glissant le long du bord droit : volume plus / moins.
 * Les bandes sont fines (16 dp) pour ne pas gêner les boutons.
 */
@Composable
fun BoxScope.EdgeGestures(onBack: () -> Unit, onApps: () -> Unit, onVolume: (up: Boolean) -> Unit) {
    val density = LocalDensity.current
    val trigger = with(density) { 70.dp.toPx() }
    val volumeStep = with(density) { 36.dp.toPx() }

    Box(Modifier.align(Alignment.CenterStart).width(16.dp).fillMaxHeight().pointerInput(Unit) {
        var dx = 0f; var done = false
        detectDragGestures(
            onDragStart = { dx = 0f; done = false },
            onDrag = { change, drag ->
                change.consume(); dx += drag.x
                if (!done && dx > trigger) { done = true; onBack() }
            },
        )
    })

    Box(Modifier.align(Alignment.CenterEnd).width(16.dp).fillMaxHeight().pointerInput(Unit) {
        var dx = 0f; var dy = 0f; var done = false; var vertical = false
        detectDragGestures(
            onDragStart = { dx = 0f; dy = 0f; done = false; vertical = false },
            onDrag = { change, drag ->
                change.consume(); dx += drag.x; dy += drag.y
                if (done) return@detectDragGestures
                if (!vertical && -dx > trigger && -dx > kotlin.math.abs(dy)) { done = true; onApps() }
                else if (kotlin.math.abs(dy) > volumeStep && kotlin.math.abs(dy) > -dx) {
                    vertical = true
                    onVolume(dy < 0); dy = 0f
                }
            },
        )
    })
}
