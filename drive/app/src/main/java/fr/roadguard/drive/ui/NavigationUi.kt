package fr.roadguard.drive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.drive.nav.Guidance
import fr.roadguard.drive.nav.GuidanceState
import fr.roadguard.drive.nav.LatLon
import fr.roadguard.drive.nav.Maneuver
import fr.roadguard.drive.nav.NavApi
import fr.roadguard.drive.nav.Place
import kotlinx.coroutines.launch

/** Écran « Où aller ? » : recherche d'adresse ou de lieu, résultats triés autour de la position. */
@Composable
fun SearchScreen(near: LatLon?, favorites: List<Place>, onPick: (Place) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun runSearch() {
        if (query.isBlank() || busy) return
        busy = true
        message = "Recherche…"
        scope.launch {
            runCatching { NavApi.search(query, near) }
                .onSuccess { results = it; message = if (it.isEmpty()) "Aucun résultat" else null }
                .onFailure { message = "Recherche impossible : vérifie la connexion (${it.message})" }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Adresse, ville, lieu…", fontSize = 20.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, color = Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
                    cursorColor = DriveColors.Accent,
                ),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
            Spacer(Modifier.width(12.dp))
            BigButton("Chercher", DriveColors.Accent) { runSearch() }
            Spacer(Modifier.width(12.dp))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp)) }
        val shown = if (results.isEmpty() && query.isBlank()) favorites else results
        if (shown === favorites && favorites.isNotEmpty()) {
            Text("Récents", color = DriveColors.TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown) { place ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
                        .clickable { onPick(place) }.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(place.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (place.detail.isNotBlank()) Text(place.detail, color = DriveColors.TextSecondary, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Bandeau de guidage : flèche, distance, consigne ; en bas, arrivée estimée et bouton Arrêter. */
@Composable
fun GuidanceBanner(state: GuidanceState?, status: String?, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xF00B3D66)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val next = state?.next
            Text(maneuverSymbol(next), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.width(300.dp)) {
                when {
                    status != null -> Text(status, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    state == null -> Text("Calcul du trajet…", color = Color.White, fontSize = 20.sp)
                    state.arrived -> Text("Vous êtes arrivé", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    else -> {
                        Text(Guidance.shortDistance(state.toNextM), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text(next?.instruction.orEmpty(), color = Color(0xFFD6ECFF), fontSize = 17.sp, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (state != null && !state.arrived) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel.copy(alpha = 0.95f))
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${Guidance.shortDistance(state.remainingM)} · ${eta(state.remainingS)}", color = Color.White, fontSize = 17.sp)
                Spacer(Modifier.width(12.dp))
                BigButton("Arrêter", Color(0xFFB71C1C), onStop)
            }
        } else if (state?.arrived == true) {
            Spacer(Modifier.height(8.dp))
            BigButton("Terminer", DriveColors.Accent, onStop)
        }
    }
}

@Composable
fun BigButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(color).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

/** Rond « Où aller ? » posé sur la carte. */
@Composable
fun MapButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(DriveColors.Panel.copy(alpha = 0.92f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 26.sp) }
}

/** Arrivée estimée (heure) et durée restante. */
private fun eta(seconds: Int): String {
    val arrival = java.util.Calendar.getInstance().apply { add(java.util.Calendar.SECOND, seconds) }
    val hm = String.format(java.util.Locale.FRANCE, "%02d:%02d", arrival.get(java.util.Calendar.HOUR_OF_DAY), arrival.get(java.util.Calendar.MINUTE))
    val min = (seconds + 59) / 60
    val duration = if (min >= 60) "${min / 60} h ${String.format(java.util.Locale.FRANCE, "%02d", min % 60)}" else "$min min"
    return "$duration · arrivée $hm"
}

/** Flèche simple selon le type de manœuvre Valhalla. */
fun maneuverSymbol(m: Maneuver?): String = when (m?.type) {
    null -> "↑"
    4, 5, 6 -> "⚑"
    9, 23, 20 -> "↗"
    10, 18, 37 -> "→"
    11 -> "↘"
    12, 13 -> "↶"
    14 -> "↙"
    15, 19, 38 -> "←"
    16, 24, 21 -> "↖"
    26, 27 -> "⟳" + (m.roundaboutExit?.toString() ?: "")
    else -> "↑"
}
