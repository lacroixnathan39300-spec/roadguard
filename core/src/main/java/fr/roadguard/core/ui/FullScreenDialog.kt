package fr.roadguard.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Fenêtre en plein écran (messagerie, modération) : elle occupe tout l'écran, barres d'Android masquées comme l'appli.
 * Une fenêtre Android ordinaire était décalée sur certains autoradios (barre système sur le côté) :
 * l'écran d'en dessous dépassait à gauche et les boutons étaient coupés à droite.
 */
@Composable
fun FullScreenDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { w ->
                w.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                w.setGravity(android.view.Gravity.TOP or android.view.Gravity.START)
                w.attributes = w.attributes.apply {
                    x = 0; y = 0
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
                androidx.core.view.WindowInsetsControllerCompat(w, w.decorView).apply {
                    hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
        content()
    }
}
