package fr.roadguard.app.next

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.nav.NavApi
import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Appointment(val title: String, val begin: Long, val location: String)

/** Prochain rendez-vous de l'agenda (dans les 4 h, avec un lieu) : itinéraire en un appui. */
@Composable
fun CalendarHint(s: DrivingSession, enabled: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var next by remember { mutableStateOf<Appointment?>(null) }
    var hidden by remember { mutableStateOf<Long?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(enabled) {
        while (enabled) {
            next = if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) null
            else withContext(Dispatchers.IO) { runCatching { query(context) }.getOrNull() }
            kotlinx.coroutines.delay(5 * 60_000L)
        }
    }
    val a = next ?: return
    if (!enabled || hidden == a.begin || s.navigating) return
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date(a.begin))
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Color(0xF0123A6B)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("📅 $time · ${a.title}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(info ?: a.location, color = Color(0xFFCFE3FF), fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BigButton("Y aller", DriveColors.Accent) {
                scope.launch {
                    val near = s.here?.let { LatLon(it.lat, it.lon) }
                    val place = runCatching { NavApi.search(a.location, near).firstOrNull() }.getOrNull()
                    if (place == null) info = "Adresse introuvable : « ${a.location} »" else { s.startNavigation(place); hidden = a.begin }
                }
            }
            BigButton("Plus tard", DriveColors.Panel) { hidden = a.begin }
        }
    }
}

private fun query(context: android.content.Context): Appointment? {
    val now = System.currentTimeMillis()
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, now); ContentUris.appendId(it, now + 4 * 3_600_000L)
    }.build()
    context.contentResolver.query(uri, arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.EVENT_LOCATION),
        null, null, CalendarContract.Instances.BEGIN + " ASC")?.use { c ->
        while (c.moveToNext()) {
            val loc = c.getString(2)?.trim().orEmpty()
            val begin = c.getLong(1)
            if (loc.length >= 4 && begin > now) return Appointment(c.getString(0) ?: "Rendez-vous", begin, loc)
        }
    }
    return null
}
