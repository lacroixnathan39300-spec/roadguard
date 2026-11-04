package fr.roadguard.drive

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lecteur de musique du poste (appli Bluetooth du constructeur, Spotify…) : choisi une fois,
 * ouvert d'un appui depuis la carte musique et, si on le veut, lancé tout seul au démarrage.
 */
object MusicPlayer {
    private fun prefs(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun chosen(c: Context): Pair<String, String>? {
        val p = prefs(c)
        val pkg = p.getString("player_pkg", null) ?: return null
        return pkg to (p.getString("player_label", pkg) ?: pkg)
    }

    fun autoStart(c: Context) = prefs(c).getBoolean("player_autostart", true)

    fun choose(c: Context, app: LaunchableApp) {
        prefs(c).edit().putString("player_pkg", app.packageName).putString("player_activity", app.activity)
            .putString("player_label", app.label).apply()
    }

    fun setAutoStart(c: Context, v: Boolean) = prefs(c).edit().putBoolean("player_autostart", v).apply()

    /** Ouvre le lecteur ; [comeBack] : DRIVE revient devant au bout de quelques secondes. */
    fun open(c: Context, comeBack: Boolean) {
        val p = prefs(c)
        val pkg = p.getString("player_pkg", null) ?: return
        val activity = p.getString("player_activity", null)
        val intent = (if (activity != null) Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(pkg, activity)
            else c.packageManager.getLaunchIntentForPackage(pkg)) ?: return
        runCatching { c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        if (comeBack) android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching {
                c.startActivity(Intent(c, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            }
        }, 3500)
    }

    /** Applis qui ressemblent à un lecteur de musique, proposées en premier. */
    fun looksLikePlayer(app: LaunchableApp): Boolean {
        val t = (app.label + " " + app.packageName).lowercase()
        return listOf("music", "musique", "bluetooth", "bt", "audio", "media", "média", "player", "lecteur", "spotify",
            "deezer", "radio", "podcast", "youtube", "amazon", "tidal", "soundcloud", "a2dp").any { it in t }
    }
}

@Composable
fun MusicPlayerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    var chosen by remember { mutableStateOf(MusicPlayer.chosen(context)) }
    var auto by remember { mutableStateOf(MusicPlayer.autoStart(context)) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { AppCatalog(context).load() }
            .sortedByDescending { MusicPlayer.looksLikePlayer(it) }
    }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lecteur de musique", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Row(
            Modifier.padding(vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
                .clickable { auto = !auto; MusicPlayer.setAutoStart(context, auto) }.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ouvrir ce lecteur au démarrage du poste (puis revenir sur DRIVE)", color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Switch(checked = auto, onCheckedChange = { auto = it; MusicPlayer.setAutoStart(context, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = DriveColors.Accent))
        }
        Text("Choisis l'appli qui lit ta musique (souvent l'appli Bluetooth du poste) :", color = DriveColors.TextSecondary, fontSize = 15.sp)
        LazyColumn(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps.orEmpty(), key = { it.packageName + it.activity }) { app ->
                val selected = chosen?.first == app.packageName
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (selected) DriveColors.Accent.copy(alpha = 0.4f) else DriveColors.Panel)
                        .clickable { MusicPlayer.choose(context, app); chosen = app.packageName to app.label }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    app.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(44.dp)) }
                    Text(app.label, color = Color.White, fontSize = 19.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
                    if (selected) Text("✓", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
