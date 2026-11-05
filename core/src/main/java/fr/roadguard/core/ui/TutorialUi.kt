package fr.roadguard.core.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tutoriel de bienvenue : montré une fois au premier lancement, puis depuis Réglages → « Revoir le tutoriel ». */
object Tutorial {
    private const val KEY = "tutorial_seen_v1"
    var forceShow by mutableStateOf(false)
    fun seen(context: Context) = context.getSharedPreferences("tutorial", Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markSeen(context: Context) = context.getSharedPreferences("tutorial", Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}

private data class Page(val emoji: String, val title: String, val lines: List<String>)

private fun pages(drive: Boolean): List<Page> = listOf(
    Page("👋", if (drive) "Bienvenue sur Pilote Drive" else "Bienvenue sur Pilote", listOf(
        "GPS, radars et alertes, signalements de la communauté, musique et trafic, réunis dans une seule appli.",
        if (drive) "Pilote Drive transforme ton autoradio en tableau de bord complet." else "Pilote fonctionne seul sur le téléphone, et avec Pilote Drive sur l'autoradio.",
    )),
    Page("📷", "Les radars", listOf(
        "Annonce environ 30 s avant chaque radar, avec des bips de plus en plus rapides jusqu'au radar.",
        "Trop vite à l'approche d'un radar : alarme forte « Ralentis ! ».",
        "Tronçons de vitesse moyenne : ta moyenne en direct, et si elle est bonne ou non.",
        "Réglages → Annonces : voix, bips, tolérance de vitesse, types d'alertes.",
    )),
    Page("🗺", "Radars et cartes hors ligne", listOf(
        "France et Suisse sont déjà incluses.",
        (if (drive) "Réglages → « Cartes hors ligne… »" else "Menu → « Radars par pays »") + " : ajoute d'autres pays.",
        (if (drive) "Réglages → « Cartes hors ligne… »" else "Menu → « Cartes hors ligne »") + " : « Télécharger ma région » pour rouler sans Internet.",
    )),
    Page("⚠️", "Signaler en un appui", listOf(
        "Bouton ⚠ : police, radar mobile, accident, bouchon, travaux, danger, ou un 💬 message visible 24 h.",
        "🎤 « Dire » : signale à la voix, les mains sur le volant.",
        "Les autres confirment ou non : les signalements faux disparaissent tout seuls.",
    )),
    if (drive) Page("✎", "Ton accueil, à ta façon", listOf(
        "Plusieurs pages : passe de l'une à l'autre avec ‹ › en bas.",
        "✎ : déplace, agrandis ou retire les modules, ajoute des widgets Android ou une page.",
        "Bouton « W » sur la carte : ouvre ou ferme la carte Waze en direct (bouchons, police…).",
        "Glisse depuis le bord gauche pour revenir, ou depuis le bord droit pour les applis ; le long du bord droit pour le volume.",
    )) else Page("🚗", "Avec l'autoradio", listOf(
        "Menu → « Pilote Link » : relie le téléphone à l'autoradio (Wi-Fi ou câble).",
        "L'autoradio reçoit ta position, ta musique, tes messages, et Internet par le téléphone.",
        "Menu → « Assistant caméra » : téléphone au pare-brise, distance avec la voiture de devant.",
    )),
    Page("🔒", "Les autorisations", listOf(
        "Position : indispensable pour les radars et le GPS.",
        "Notifications : pour les alertes quand l'appli est en arrière-plan.",
        "Affichage par-dessus les autres applis : pour la bulle sur Waze ou Google Maps.",
        "Tout se règle ensuite dans Réglages. Bonne route ! 🚀",
    )),
)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val drive = context.packageName.contains("drive")
    val list = remember { pages(drive) }
    var i by remember { mutableStateOf(0) }
    val p = list[i]
    // Le fond intercepte les appuis : rien ne passe à l'accueil derrière.
    Box(Modifier.fillMaxSize().background(DriveColors.Background)
        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
        .padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(DriveStyle.panelBrush)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(p.emoji, fontSize = 52.sp)
            Text(p.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            p.lines.forEach { Text("• $it", color = Color(0xFFD5DCE5), fontSize = 18.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                list.indices.forEach { j ->
                    Box(Modifier.size(if (j == i) 12.dp else 8.dp).clip(CircleShape).background(if (j == i) DriveColors.Accent else DriveColors.Track))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (i > 0) BigButton("‹ Retour", DriveColors.Track) { i-- }
                if (i < list.lastIndex) BigButton("Suivant ›", DriveColors.Accent) { i++ }
                else BigButton("C'est parti !", DriveColors.Accent) { Tutorial.markSeen(context); onDone() }
                if (i < list.lastIndex) BigButton("Passer", DriveColors.Panel) { Tutorial.markSeen(context); onDone() }
            }
        }
    }
}
