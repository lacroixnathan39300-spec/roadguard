package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.community.Community
import fr.roadguard.core.community.Rank
import fr.roadguard.core.community.ReportType
import fr.roadguard.core.community.VotePrompt
import fr.roadguard.core.radar.Geo
import kotlinx.coroutines.launch

/** Choix du type de signalement : 7 grosses tuiles, un seul appui. */
@Composable
fun ReportSheet(onPick: (ReportType) -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(16.dp).clip(RoundedCornerShape(22.dp)).background(DriveColors.Panel).padding(16.dp).clickable(enabled = false) {},
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            var heard by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            val speech = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { r ->
                val said = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS).orEmpty()
                val type = said.firstNotNullOfOrNull { reportFromSpeech(it) }
                if (type != null) onPick(type) else heard = if (said.isEmpty()) "Rien entendu" else "Pas compris : « ${said.first()} »"
            }
            val speechIntent = androidx.compose.runtime.remember {
                android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                    .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Police, radar, accident, bouchon, travaux, danger…")
            }
            val canSpeak = androidx.compose.runtime.remember { speechIntent.resolveActivity(context.packageManager) != null }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Signaler", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (canSpeak) BigButton("🎤 Dire", DriveColors.Accent) { heard = null; runCatching { speech.launch(speechIntent) } }
            }
            heard?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp) }
            ReportType.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        Column(
                            Modifier.width(104.dp * DriveColors.Scale).clip(RoundedCornerShape(16.dp)).background(Color(t.color))
                                .clickable { onPick(t) }.padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(t.emoji, fontSize = 30.sp)
                            Text(t.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            BigButton("Annuler", DriveColors.Track, onClose)
        }
    }
}

/** « Police toujours là ? » en passant près d'un signalement. */
@Composable
fun VotePromptCard(prompt: VotePrompt, onAnswer: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val r = prompt.report
    Row(
        modifier.padding(12.dp).clip(RoundedCornerShape(18.dp)).background(Color(r.type.color).copy(alpha = 0.96f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 10.dp)) {
            Text("${r.type.emoji} ${r.type.label} toujours là ?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Signalé par ${r.author}" + if (r.yes > 0) " · confirmé ${r.yes} fois" else "", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
        BigButton("👍", Color(0xFF2E7D32)) { onAnswer(true) }
        Spacer(Modifier.width(8.dp))
        BigButton("👎", Color(0xFF424242)) { onAnswer(false) }
    }
}

/** Petit message (coucou reçu, signalement envoyé). */
@Composable
fun CommunityToast(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.padding(12.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xF0263238))
            .clickable(onClick = onClose).padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** Communauté : pseudo, points, partage de position, conducteurs proches, classement. */
@Composable
fun CommunityScreen(community: Community, hereLat: Double?, hereLon: Double?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var nameField by remember { mutableStateOf(community.name.orEmpty()) }
    var message by remember { mutableStateOf<String?>(null) }
    var week by remember { mutableStateOf(true) }
    var ranks by remember { mutableStateOf<List<Rank>?>(null) }
    LaunchedEffect(week, community.available) {
        ranks = null
        if (community.available) ranks = runCatching { community.leaderboard(week) }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Communauté", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        if (!community.available) {
            Text("Le serveur des signalements n'est pas encore en ligne.", color = DriveColors.TextSecondary, fontSize = 17.sp,
                modifier = Modifier.padding(top = 16.dp))
            return@Column
        }
        LazyColumn(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel).padding(14.dp)) {
                    Text(if (community.hasAccount) "${community.name} · ${community.points} points" else "Choisis ton pseudo",
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Ton pseudo apparaît sur tes signalements et dans le classement.", color = DriveColors.TextSecondary, fontSize = 14.sp)
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = nameField, onValueChange = { nameField = it.take(24) }, singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        BigButton(if (community.hasAccount) "Changer" else "Valider", DriveColors.Accent) {
                            scope.launch {
                                message = community.setName(nameField).fold({ "Pseudo enregistré : $it" }, { "Impossible : ${it.message}" })
                            }
                        }
                    }
                    message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel)
                        .clickable { community.changeShare(!community.shareLocation) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Être visible des autres conducteurs", color = Color.White, fontSize = 18.sp)
                        Text("Position arrondie à ~100 m, seulement en conduite. Désactivé = invisible.", color = DriveColors.TextSecondary, fontSize = 13.sp)
                    }
                    Switch(checked = community.shareLocation, onCheckedChange = community::changeShare,
                        colors = SwitchDefaults.colors(checkedTrackColor = DriveColors.Accent))
                }
            }
            if (community.shareLocation) {
                item { Text("Conducteurs ROADGUARD autour", color = DriveColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (community.drivers.isEmpty()) item { Text("Personne de visible pour l'instant.", color = DriveColors.TextSecondary, fontSize = 15.sp) }
                items(community.drivers, key = { it.id }) { d ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val dist = if (hereLat != null && hereLon != null) Geo.distanceMeters(hereLat, hereLon, d.lat, d.lon) else null
                            Text(listOfNotNull(dist?.let { if (it >= 1000) "%.1f km".format(it / 1000) else "${it.toInt()} m" }, d.speed?.let { "$it km/h" })
                                .joinToString(" · "), color = DriveColors.TextSecondary, fontSize = 14.sp)
                        }
                        BigButton("👋", DriveColors.Accent) { community.wave(d) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Classement", color = DriveColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    BigButton("Semaine", if (week) DriveColors.Accent else DriveColors.Panel) { week = true }
                    BigButton("Général", if (!week) DriveColors.Accent else DriveColors.Panel) { week = false }
                }
            }
            val list = ranks
            if (list == null) item { Text("Chargement…", color = DriveColors.TextSecondary, fontSize = 15.sp) }
            else if (list.isEmpty()) item { Text("Pas encore de points : sois le premier à signaler !", color = DriveColors.TextSecondary, fontSize = 15.sp) }
            else items(list, key = { "${it.rank}-${it.name}" }) { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (r.me) DriveColors.Accent.copy(alpha = 0.35f) else DriveColors.Panel)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val medal = when (r.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${r.rank}." }
                    Text(medal, color = Color.White, fontSize = 18.sp, modifier = Modifier.width(44.dp))
                    Text(r.name, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text("${r.points} pts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Text(
                    "Points : +5 par signalement, +2 quand un conducteur le confirme, +1 par réponse « toujours là ? », " +
                        "−3 si ton signalement est démenti. Un signalement confirmé par 5 personnes sur plusieurs jours est gardé à vie.",
                    color = DriveColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Signalement reconnu dans une phrase dite à voix haute (« radar mobile », « y'a les flics », « bouchon »…). */
fun reportFromSpeech(text: String): ReportType? {
    val t = java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    return when {
        "radar fixe" in t -> ReportType.FIXED
        "radar" in t || "flash" in t || "jumelle" in t || "camera" in t -> ReportType.MOBILE
        listOf("police", "flic", "gendarme", "controle", "keuf").any { it in t } -> ReportType.POLICE
        "accident" in t || "accroch" in t -> ReportType.ACCIDENT
        listOf("bouchon", "embouteill", "trafic", "ralenti").any { it in t } -> ReportType.JAM
        "travaux" in t || "chantier" in t -> ReportType.WORKS
        listOf("danger", "obstacle", "objet", "animal", "verglas", "panne").any { it in t } -> ReportType.DANGER
        else -> null
    }
}
