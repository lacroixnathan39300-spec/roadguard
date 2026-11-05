package fr.roadguard.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import fr.roadguard.core.radar.Geo

/** Outils Waze : carte en direct (page officielle intégrable), appli Waze à côté ou en plein écran. */
object Waze {
    const val PACKAGE = "com.waze"

    fun liveMapUrl(lat: Double, lon: Double, zoom: Int = 14) =
        "https://embed.waze.com/iframe?zoom=$zoom&lat=${"%.5f".format(java.util.Locale.US, lat)}&lon=${"%.5f".format(java.util.Locale.US, lon)}&ct=livemap&pin=1"

    fun installed(context: Context) = context.packageManager.getLaunchIntentForPackage(PACKAGE) != null

    /** Ouvre l'appli Waze, en écran partagé à côté de ROADGUARD si le poste le permet. */
    fun openApp(context: Context, adjacent: Boolean, lat: Double? = null, lon: Double? = null): Boolean {
        val intent = (if (lat != null && lon != null) Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=$lat,$lon&navigate=yes"))
            else context.packageManager.getLaunchIntentForPackage(PACKAGE)) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (adjacent) intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

/**
 * Carte Waze en direct (bouchons, accidents, police, dangers signalés par la communauté Waze), recentrée
 * sur la position quand on s'est déplacé de plus de 400 m. Pas de guidage : c'est une carte d'information.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WazeLiveMap(lat: Double?, lon: Double?, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null) {
    val context = LocalContext.current
    var centered by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(lat, lon, web) {
        val w = web ?: return@LaunchedEffect
        val la = lat ?: 46.6758; val lo = lon ?: 5.5574 // Lons-le-Saunier en attendant le GPS
        val c = centered
        if (c == null || Geo.distanceMeters(c.first, c.second, la, lo) > 400) {
            centered = la to lo
            w.loadUrl(Waze.liveMapUrl(la, lo))
        }
    }
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF1A1F26))) {
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                webViewClient = object : WebViewClient() {
                    // Les liens de la page (ouvrir Waze…) partent vers l'appli ou le navigateur, pas dans la carte.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val u = request.url.toString()
                        if (u.contains("embed.waze.com") || u.contains("waze.com/live-map")) return false
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        return true
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                        if (request.isForMainFrame) failed = true
                    }
                    override fun onPageFinished(view: WebView, url: String) { failed = false }
                }
                web = this
            }
        }, modifier = Modifier.fillMaxSize(), onRelease = { it.destroy() })
        if (failed) Text("Carte Waze indisponible : pas d'Internet sur cet appareil.", color = Color.White, fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center).padding(20.dp))
        Row(Modifier.align(Alignment.TopEnd).padding(10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton("◎", Color(0xCC101820)) { centered = null; val la = lat; val lo = lon; if (la != null && lo != null) { centered = la to lo; web?.loadUrl(Waze.liveMapUrl(la, lo)) } }
            if (Waze.installed(context)) BigButton("Appli Waze ↗", Color(0xCC33CCFF)) { Waze.openApp(context, adjacent = true) }
            if (onClose != null) BigButton("✕ Fermer", Color(0xCC101820), onClose)
        }
    }
}
