package fr.roadguard.drive

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import fr.roadguard.core.ui.DriveColors

/**
 * Appli intégrée dans un module (Waze, Google Maps…) : uniquement dans « Pilote Drive Système » sur un poste
 * signé avec les clés de test d'Android. Utilise la vue système cachée d'Android 8 (« ActivityView ») qui
 * affiche une autre appli dans un écran virtuel et lui transmet le tactile.
 */
object Embedding {
    fun isSystemBuild(context: Context): Boolean =
        context.resources.getIdentifier("rg_system_build", "bool", context.packageName).let { it != 0 && context.resources.getBoolean(it) }

    /** Crée la vue système ; null si Android ne la fournit pas sur ce poste. */
    fun createView(context: Context): View? = runCatching {
        Class.forName("android.app.ActivityView").getConstructor(Context::class.java).newInstance(context) as View
    }.getOrNull()

    /** Lance l'appli dans la vue ; faux tant que la vue n'est pas prête (à réessayer). */
    fun start(view: View, intent: Intent): Result<Unit> = runCatching {
        view.javaClass.getMethod("startActivity", Intent::class.java).invoke(view, intent); Unit
    }

    fun release(view: View) { runCatching { view.javaClass.getMethod("release").invoke(view) } }
}

@Composable
fun EmbeddedAppModule(packageName: String, modifier: Modifier) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val label = remember(packageName) {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)
    }
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF10141B))) {
        if (!Embedding.isSystemBuild(context)) {
            Message("« $label » dans un module nécessite Pilote Drive Système (version pour postes en test-keys).")
        } else {
            AndroidView(
                factory = { ctx ->
                    val v = Embedding.createView(ctx)
                    if (v == null) { error = "Ce poste ne permet pas d'intégrer une appli (vue système absente)."; View(ctx) } else {
                        val launch = ctx.packageManager.getLaunchIntentForPackage(packageName)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        if (launch == null) error = "« $label » n'est pas installée."
                        else {
                            // La vue n'accepte l'appli qu'une fois son écran virtuel prêt : on réessaie quelques secondes.
                            var tries = 0
                            val retry = object : Runnable {
                                override fun run() {
                                    Embedding.start(v, launch).onFailure {
                                        if (++tries < 25) v.postDelayed(this, 300)
                                        else error = "Android a refusé l'intégration : ${it.cause?.message ?: it.message}"
                                    }
                                }
                            }
                            v.postDelayed(retry, 400)
                        }
                        v
                    }
                },
                onRelease = { Embedding.release(it) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        error?.let { Message(it) }
    }
}

@Composable
private fun Message(text: String) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Text(text, color = DriveColors.TextSecondary, fontSize = 15.sp)
    }
}

/** Applis proposées pour un module intégré : celles qui ont un écran de lancement. */
fun embeddableApps(context: Context): List<Pair<String, String>> {
    val pm = context.packageManager
    val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(main, 0).map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
        .filter { it.first != context.packageName }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
}

