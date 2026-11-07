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
    private const val KEY = "tutorial_seen_v3" // v2 : nouveautés, montré à nouveau une fois
    var forceShow by mutableStateOf(false)
    fun seen(context: Context) = context.getSharedPreferences("tutorial", Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun markSeen(context: Context) = context.getSharedPreferences("tutorial", Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
}

private data class Page(val emoji: String, val title: String, val lines: List<String>)

private fun pages(drive: Boolean): List<Page> = listOfNotNull(
    Page("👋", if (drive) "Bienvenue sur Pilote Drive" else "Bienvenue sur Pilote", listOf(
        "GPS, radars, limitations, signalements de la communauté, musique et trafic, réunis dans une seule appli.",
        if (drive) "Pilote Drive transforme ton autoradio en tableau de bord complet." else "Pilote fonctionne seul sur le téléphone, et avec Pilote Drive sur l'autoradio.",
        "Ce petit tour prend une minute. Tu peux le revoir dans Réglages → « Revoir le tutoriel ».",
    )),
    Page("📷", "Les radars", listOf(
        "Annonce à 1 km, puis des bips de plus en plus rapides jusqu'au radar, qui s'arrêtent pile au radar.",
        "Seulement le radar qui est sur TA route : avec un itinéraire, ceux du tracé ; sinon, ceux de ton couloir qui se rapprochent vraiment.",
        "Trop vite à l'approche : alarme stridente « Ralentis ! ». Tronçons : ta vitesse moyenne en direct.",
        if (drive) "Réglages → Annonces : couper les radars, les bips, l'alarme." else "Bouton rond 📷 à droite de la carte : coupe ou remet les radars (et leurs points sur la carte).",
    )),
    Page("🚦", "Limitations de vitesse", listOf(
        "La limitation de la route s'affiche à côté de ta vitesse, et change en quelques secondes avec la zone.",
        if (drive) "Réglages → Annonces : annonces « Limitation 50 » et avertissement de dépassement." else "Bouton 🚦 : coupe ou remet les annonces « Limitation 50 » et de dépassement.",
        "Option : alarme forte dès que tu dépasses, même sans radar. Et si tu insistes, elle te réprimande !",
    )),
    Page("💋", "La voix et ses tons", listOf(
        "Réglages → Voix : choisis la voix (♀ / ♂), le débit, et le ton : classique, charme 💋, séduction 🔥, osé 😈, extrême 🔥😈.",
        "Le ton s'applique à tout : radars, limitations, réprimandes, et même le guidage (« tournez à droite, mon chéri »).",
        "Plus le ton monte, plus il reprend aussi les phrases des tons d'en dessous : jamais deux fois la même chose.",
        "« ⬇ Télécharger d'autres voix » : ajoute des voix d'Android, y compris d'autres langues.",
    )),
    Page("⚠️", "Signaler en un appui", listOf(
        "Bouton ⚠ : police, radar mobile, accident, bouchon, travaux, danger, ou un 💬 message visible 24 h.",
        "🎤 « Dire » : signale à la voix, les mains sur le volant. Ton signalement apparaît tout de suite sur la carte.",
        "Les signalements devant toi sont annoncés à 1 km. En passant, « toujours là ? » : réponds oui ou non à la voix.",
        "⭐ Ta fiabilité monte quand les autres confirment tes signalements : les tiens sont alors validés tout de suite.",
        "🔢 Limitation fausse, 🔄 rond-point ou 🛣 route absents de la carte : signale-les. Une fois confirmées, les corrections " +
            "s'appliquent pour tous et partent à OpenStreetMap pour corriger la carte.",
    )),
    if (drive) null else Page("🧭", "Plus de place pour la carte", listOf(
        "Plein écran : glisse depuis le haut ou le bas pour voir les barres d'Android un instant.",
        "En roulant, sans toucher l'écran 8 s, les panneaux se replient : il reste la vitesse et la consigne.",
        "Touche la pastille de vitesse ou la consigne pour tout faire revenir.",
        "La recherche propose des résultats dès 3 lettres, pendant que tu tapes.",
    )),
    if (drive) null else Page("🫧", "La bulle par-dessus Waze", listOf(
        "Carte « 🫧 Bulle » de l'accueil → Activer : Pilote démarre tout seul dès 20 km/h, même fermé.",
        "La bulle (vitesse, limite, radar) s'affiche sur Waze ou Maps, avec les bips ; rouge en alerte, cachée à l'arrêt.",
        "Un appui sur la bulle ouvre Pilote. Arrêt automatique après 10 min à l'arrêt.",
        "Xiaomi : infos de l'appli → « Démarrage automatique » et batterie « Aucune restriction ».",
    )),
    if (drive) null else Page("😴", "Anti-somnolence et caméra", listOf(
        "Menu → « Anti-somnolence » : la caméra avant surveille tes yeux, sans rien afficher (voyant 👁 sur la carte).",
        "Yeux qui se ferment : écran rouge, sirène et « Réveille-toi ! ». Fatigue, bâillements : conseil de pause.",
        "Menu → « Assistant caméra » : téléphone au pare-brise, distance avec la voiture de devant.",
        "Tout est analysé sur le téléphone : aucune image enregistrée ni envoyée.",
    )),
    Page("🗺", "Radars et cartes hors ligne", listOf(
        "France, Suisse, Belgique, Allemagne, Italie, Espagne et Luxembourg sont déjà inclus.",
        (if (drive) "Réglages → « Cartes hors ligne… »" else "Menu → « Radars par pays »") + " : plus de 100 autres pays à télécharger.",
        (if (drive) "Réglages → « Cartes hors ligne… »" else "Menu → « Cartes hors ligne »") + " : « Télécharger ma région » pour rouler sans Internet.",
    )),
    if (drive) Page("✎", "Ton accueil, à ta façon", listOf(
        "Plusieurs pages : passe de l'une à l'autre avec ‹ › en bas.",
        "✎ : déplace, agrandis ou retire les modules, ajoute des widgets Android ou une page.",
        "Glisse depuis le bord gauche pour revenir, ou depuis le bord droit pour les applis ; le long du bord droit pour le volume.",
    )) else Page("🚗", "Avec l'autoradio", listOf(
        "Menu → « Pilote Link » : relie le téléphone à l'autoradio (Wi-Fi ou câble).",
        "L'autoradio reçoit ta position, ta musique, tes messages, et Internet par le téléphone.",
    )),
    Page("🛡", "Sécurité automatique", listOf(
        "Suisse : alertes radars coupées toutes seules (interdites là-bas), rétablies en France.",
        "Pluie : limitations abaissées (130→110, 110→100, 90→80). Jeune conducteur et vitesse max perso dans Réglages.",
        "Météo dangereuse, passages à niveau, virages, écoles annoncés. Radars suivis même dans les tunnels.",
        if (drive) "Fin de trajet : voiture garée mémorisée, et rappel « rien oublié ? » si tu l'actives." else "Détection d'accident : choc + arrêt → « Tout va bien ? », puis SMS à ton contact d'urgence.",
    )),
    Page("🧰", "Les outils", listOf(
        (if (drive) "Réglages" else "Menu") + " → 🅿 Ma voiture (retour à pied, minuteur), 📍 À proximité (carburants les moins chers, bornes, parkings, aires).",
        "📒 Mes trajets : historique, éco-conduite, journal km pro/perso à exporter, carnet de carburant, entretien.",
        "🚗 Convoi entre amis et partage de trajet en direct ; 🪞 affichage tête haute pour le pare-brise.",
        "Recherche : « ➕ Étape » pour passer par plusieurs endroits avant la destination. 📒 Mes trajets → 🏆 Badges et défis du mois.",
        if (drive) "☁ Réglages sauvegardés dans ton compte : tu les retrouves sur un autre appareil."
        else "🎤 sur la carte : « Pilote, signale la police », « coupe les radars », « rentre à la maison »… Messages lus à voix haute, dashcam, widget.",
    )),
    Page("✉", "Une question ? Écris-moi", listOf(
        "Réglages → ton compte → « ✉ Écrire à l'administrateur » : bug, idée, question.",
        "La réponse arrive dans la même conversation, avec une notification.",
        "Les mises à jour sont signalées par un bandeau et une notification : un appui pour installer.",
    )),
    Page("🔒", "Les autorisations", listOf(
        "Position (« Toujours autoriser » pour la bulle automatique) : indispensable pour les radars et le GPS.",
        "Notifications : alertes, messages et mises à jour quand l'appli est en arrière-plan.",
        if (drive) "Micro : pour signaler à la voix." else "Affichage par-dessus les autres applis : pour la bulle. Caméra : seulement si tu actives l'anti-somnolence ou l'assistant.",
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
