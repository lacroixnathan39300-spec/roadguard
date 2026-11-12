package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
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
import fr.roadguard.core.nav.Guidance
import fr.roadguard.core.nav.GuidanceState
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.nav.Maneuver
import fr.roadguard.core.nav.NavApi
import fr.roadguard.core.nav.Place
import kotlinx.coroutines.launch

/** Écran « Où aller ? » : recherche d'adresse ou de lieu, résultats triés autour de la position. */
@Composable
fun SearchScreen(
    near: LatLon?,
    favorites: List<Place>,
    home: Place?,
    work: Place?,
    onSave: (slot: String, place: Place) -> Unit,
    onPick: (Place) -> Unit,
    onClose: () -> Unit,
    starred: List<Place> = emptyList(),
    onStar: ((Place) -> Unit)? = null,
    initialQuery: String = "",
    /** Étapes : ajout d'un lieu comme étape, et liste des étapes déjà choisies. */
    stops: List<Place> = emptyList(),
    onAddStop: ((Place) -> Unit)? = null,
    onRemoveStop: ((Place) -> Unit)? = null,
) {
    // Choix de l'adresse « Maison » ou « Travail » : le prochain résultat touché est enregistré.
    var assigning by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    // Une seule recherche à la fois : la plus récente remplace la précédente.
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    suspend fun search(q: String, live: Boolean) {
        if (!live) message = "Recherche…"
        busy = true
        runCatching { NavApi.search(q, near) }
            .onSuccess { results = it; message = if (it.isEmpty() && !live) "Aucun résultat" else null }
            .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                // Sans Internet : communes embarquées.
                val offline = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    fr.roadguard.core.nav.OfflinePlaces.search(context, q)
                }
                results = offline
                message = if (offline.isNotEmpty()) "Sans Internet : villes et villages seulement"
                    else if (live) null else "Recherche impossible : vérifie la connexion (${it.message})"
            }
        busy = false
    }

    fun runSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        job?.cancel()
        job = scope.launch { search(q, live = false) }
    }

    // Suggestions pendant la frappe : dès 3 lettres, après une courte pause (350 ms).
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 3) { if (q.isEmpty()) { results = emptyList(); message = null }; return@LaunchedEffect }
        kotlinx.coroutines.delay(350)
        job?.cancel()
        job = scope.launch { search(q, live = true) }
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
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SlotChip("🏠 Maison", home, assigning == "home",
                onGo = { home?.let(onPick) }, onSet = { assigning = "home"; message = "Cherche puis touche l'adresse de ta maison" })
            SlotChip("💼 Travail", work, assigning == "work",
                onGo = { work?.let(onPick) }, onSet = { assigning = "work"; message = "Cherche puis touche l'adresse de ton travail" })
        }
        message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp)) }
        if (stops.isNotEmpty()) {
            // Étapes choisies : la destination se choisit ensuite comme d'habitude (toucher un lieu).
            Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Étapes :", color = DriveColors.TextSecondary, fontSize = 16.sp)
                stops.forEachIndexed { i, p ->
                    BigButton("${i + 1}. ${p.name.take(18)} ✕", DriveColors.Panel) { onRemoveStop?.invoke(p) }
                }
            }
            Text("Touche maintenant ta destination finale.", color = DriveColors.Accent, fontSize = 15.sp)
        }
        val idle = results.isEmpty() && query.isBlank()
        val shown = if (idle) starred + favorites.filterNot { r -> starred.any { it.name == r.name && it.detail == r.detail } } else results
        if (idle && shown.isNotEmpty()) {
            Text(if (starred.isNotEmpty()) "Favoris ★ puis récents" else "Récents", color = DriveColors.TextSecondary,
                fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown) { place ->
                val isStar = starred.any { it.name == place.name && it.detail == place.detail }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Column(
                    Modifier.weight(1f)
                        .clickable {
                            val slot = assigning
                            if (slot != null) {
                                onSave(slot, place); assigning = null
                                message = "${if (slot == "home") "Maison" else "Travail"} enregistré : ${place.name}"
                            } else onPick(place)
                        }.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(place.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (place.detail.isNotBlank()) Text(place.detail, color = DriveColors.TextSecondary, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (onAddStop != null) {
                    Text("➕ Étape", color = DriveColors.Accent, fontSize = 15.sp,
                        modifier = Modifier.clickable { onAddStop(place); message = "Étape ajoutée : ${place.name}" }.padding(horizontal = 10.dp, vertical = 10.dp))
                }
                if (onStar != null) {
                    Text(if (isStar) "★" else "☆", color = if (isStar) Color(0xFFFFC107) else DriveColors.TextSecondary,
                        fontSize = 30.sp * DriveColors.Scale,
                        modifier = Modifier.clickable { onStar(place) }.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                }
            }
        }
    }
}

/** « Maison » / « Travail » : toucher pour y aller ; « Modifier » pour changer l'adresse. */
@Composable
private fun SlotChip(label: String, place: Place?, active: Boolean, onGo: () -> Unit, onSet: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(if (active) DriveColors.Accent else DriveColors.Panel),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.clickable { if (place != null) onGo() else onSet() }.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(place?.name ?: "Définir", color = DriveColors.TextSecondary, fontSize = 14.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp))
        }
        if (place != null) {
            Text("Modifier", color = DriveColors.Accent, fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onSet).padding(horizontal = 12.dp, vertical = 10.dp))
        }
    }
}

/** Bandeau de guidage : flèche, distance, consigne ; en bas, arrivée estimée et bouton Arrêter. */
@Composable
fun GuidanceBanner(
    state: GuidanceState?, status: String?, onStop: () -> Unit, modifier: Modifier = Modifier,
    choices: List<fr.roadguard.core.nav.Route> = emptyList(), current: fr.roadguard.core.nav.Route? = null,
    onChoose: (fr.roadguard.core.nav.Route) -> Unit = {}, onDismissChoices: () -> Unit = {},
    /** Mode épuré : la ligne « distance · arrivée · Arrêter » est repliée ; l'heure d'arrivée reste en petit. */
    compact: Boolean = false, onTap: (() -> Unit)? = null,
) {
    Column(modifier.padding(12.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xF00B3D66))
                .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        Text(Guidance.shortDistance(state.toNextM), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = DriveStyle.Numbers)
                        Text(next?.instruction.orEmpty(), color = Color(0xFFD6ECFF), fontSize = 17.sp, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        state.then?.let { t ->
                            Text("puis ${maneuverSymbol(t)}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33FFFFFF)).padding(horizontal = 10.dp, vertical = 2.dp))
                        }
                        if (compact) Text("${Guidance.shortDistance(state.remainingM)} · ${eta(state.remainingS)}",
                            color = Color(0xFFB8D8F5), fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        if (state != null && !state.arrived && !compact) {
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
        if (choices.size > 1) {
            Spacer(Modifier.height(8.dp))
            RouteChoices(choices, current, onChoose, onDismissChoices)
        }
    }
}

/** Choix entre le trajet le plus rapide et ses variantes (durée, distance, écart). */
@Composable
fun RouteChoices(
    choices: List<fr.roadguard.core.nav.Route>, current: fr.roadguard.core.nav.Route?,
    onChoose: (fr.roadguard.core.nav.Route) -> Unit, onDismiss: () -> Unit,
) {
    val fastest = choices.minOf { it.durationS }
    Column(
        Modifier.width(360.dp).clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel.copy(alpha = 0.95f)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        choices.forEachIndexed { i, r ->
            val selected = r === current
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (selected) DriveColors.Accent else DriveColors.Track)
                    .clickable { onChoose(r) }.padding(horizontal = 12.dp, vertical = 8.dp * DriveColors.Scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val min = ((r.durationS + 59) / 60).toInt()
                val dur = if (min >= 60) "${min / 60} h ${"%02d".format(min % 60)}" else "$min min"
                Text("${i + 1}. $dur", color = Color.White, fontSize = 18.sp * DriveColors.Scale, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                val extra = ((r.durationS - fastest) / 60).toInt()
                Text("%.0f km".format(r.lengthM / 1000) + (if (extra > 0) " · +$extra min" else " · le plus rapide") +
                    (r.tollEuros?.let { " · péages ~$it €" } ?: " · sans péage"),
                    color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp * DriveColors.Scale)
            }
        }
        BigButton("OK", DriveColors.Panel, onDismiss)
    }
}

@Composable
fun BigButton(label: String, color: Color, onClick: () -> Unit) {
    // Léger relief : reflet en haut du bouton et liseré.
    Box(
        Modifier.clip(RoundedCornerShape(14.dp)).background(color)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x1AFFFFFF), Color.Transparent)))
            .border(1.dp, DriveStyle.panelBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp * DriveColors.Scale, vertical = 12.dp * DriveColors.Scale),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 18.sp * DriveColors.Scale, fontWeight = FontWeight.Bold, fontFamily = DriveStyle.Titles) }
}

/** Rond « Où aller ? » posé sur la carte. */
@Composable
fun MapButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(60.dp * DriveColors.Scale).clip(androidx.compose.foundation.shape.CircleShape).background(DriveStyle.glass)
            .border(1.dp, Color(0x26FFFFFF), androidx.compose.foundation.shape.CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { if (!DriveIcon(label, 28.dp * DriveColors.Scale)) Text(label, color = Color.White, fontSize = 24.sp * DriveColors.Scale) }
}

/**
 * Interrupteur rond de l'accueil (radars, annonces de limitation) : anneau vert quand c'est actif,
 * rouge et barré quand c'est coupé, avec une petite légende dessous.
 */
@Composable
fun QuickToggle(emoji: String, caption: String, on: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier,
                offCaption: String? = null, slash: Boolean = true) {
    val ring = if (on) Color(0xFF2ED573) else Color(0xFFFF4757)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(60.dp * DriveColors.Scale).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (on) DriveStyle.glass else Color(0xCC3A0D12))
                .border(3.dp, ring, androidx.compose.foundation.shape.CircleShape).clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 24.sp * DriveColors.Scale, color = Color.White.copy(alpha = if (on) 1f else 0.55f))
            if (!on && slash) androidx.compose.foundation.Canvas(Modifier.size(40.dp * DriveColors.Scale)) {
                drawLine(ring, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 4.dp.toPx())
            }
        }
        Text(if (on) caption else offCaption ?: "$caption off", color = if (on) Color.White else Color(0xFFFF8A94), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 5.dp))
    }
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
