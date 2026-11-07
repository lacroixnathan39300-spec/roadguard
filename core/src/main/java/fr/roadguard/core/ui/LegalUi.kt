package fr.roadguard.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DriveSettings

/**
 * Avertissement légal, montré avant tout le reste : au premier lancement, puis chaque fois que le texte change
 * ([VERSION]). Quatre écrans : la loi au volant, les alertes radars selon les pays (elles restent actives :
 * l'utilisateur choisit de les garder ou de les couper), les autorisations demandées, et l'engagement du conducteur.
 * Relisible depuis Réglages → « Infos légales ».
 */
object Legal {
    /** À augmenter quand le texte change : l'avertissement est alors montré de nouveau à tous. */
    const val VERSION = 1
    const val TERMS_URL = "https://roadguard.roadguard-app.workers.dev/conditions"
    const val PRIVACY_URL = "https://roadguard.roadguard-app.workers.dev/confidentialite"
    var forceShow by mutableStateOf(false)
    private fun prefs(c: Context) = c.getSharedPreferences("legal", Context.MODE_PRIVATE)
    fun accepted(c: Context) = prefs(c).getInt("accepted_version", 0) >= VERSION
    fun accept(c: Context) {
        prefs(c).edit().putInt("accepted_version", VERSION).putLong("accepted_at", System.currentTimeMillis()).apply()
    }
}

private val RedMark = Color(0xFFF87171)
private val RedText = Color(0xFFFCA5A5)
private val RedBg = Color(0x1FEF4444)
private val GreenMark = Color(0xFF4ADE80)
private val GreenText = Color(0xFF86EFAC)
private val GreenBg = Color(0x1F22C55E)
private val BlueText = Color(0xFF7DD3FC)
private val BlueBg = Color(0x1A00B8FF)
private val Body = Color(0xFFE5EAF0)
private val Soft = Color(0xFFC2CBD5)

/** [review] : relu depuis les réglages (déjà accepté) : bouton « Fermer », cases déjà cochées. */
@Composable
fun LegalScreen(review: Boolean = false, onDone: () -> Unit) {
    val context = LocalContext.current
    val drive = context.packageName.contains("drive")
    var page by remember { mutableStateOf(0) }
    val scroll = rememberScrollState()
    LaunchedEffect(page) { scroll.scrollTo(0) }
    BackHandler(enabled = page > 0) { page-- }
    BackHandler(enabled = review && page == 0) { onDone() }
    // Le fond intercepte les appuis : rien ne passe à l'écran derrière.
    Box(
        Modifier.fillMaxSize().background(DriveColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(scroll).padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { j ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (j <= page) DriveColors.Accent else DriveColors.Track))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Étape ${page + 1} sur 4", color = DriveColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (review) BigButton("Fermer", DriveColors.Panel) { onDone() }
            }
            when (page) {
                0 -> LawPage()
                1 -> RadarsPage()
                2 -> PermissionsPage(drive)
                else -> CommitPage(review, onBack = { page-- }, onDone = onDone)
            }
            if (page < 3) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) BigButton("‹ Retour", DriveColors.Track) { page-- }
                PrimaryButton("Suivant", modifier = Modifier.weight(1f)) { page++ }
            }
        }
    }
}

@Composable
private fun LawPage() {
    Header("⚖️", "Avant de prendre la route",
        "Pilote t'aide à conduire, mais c'est toi le conducteur. Voici ce que dit la loi, et ce que tu peux faire à la place.")
    LegalCard("📱", "Le téléphone") {
        Forbidden("Tenir ou toucher le téléphone en roulant, même arrêté au feu. Les écouteurs aussi.", "135 € d'amende et 3 points")
        Allowed("À LA PLACE", "Téléphone fixé sur un support, et tu parles à Pilote : « oui », « non », dictée de tes messages.")
    }
    LegalCard("🖥", "Les écrans") {
        Forbidden("Une vidéo ou une appli qui n'aide pas à conduire, sur le téléphone ou sur l'écran de la voiture (mode miroir compris).",
            "Jusqu'à 1 500 € d'amende et 3 points")
        Allowed("À LA PLACE", "La carte et le guidage sont autorisés. La musique : à la voix ou avec les commandes au volant. Le reste, une fois garé.")
    }
    LegalCard("🚦", "Les limitations") {
        Tinted(BlueBg, "i", BlueText, "C'EST LE PANNEAU QUI COMPTE", BlueText) {
            BodyText("Les limitations affichées par Pilote peuvent être fausses ou pas à jour. En cas de doute, tu suis le panneau.")
            BodyText("Une erreur ? Signale « Limitation fausse » : la carte se corrige pour tout le monde.")
        }
    }
}

@Composable
private fun RadarsPage() {
    val context = LocalContext.current
    val settings = remember { DriveSettings(context) }
    Header("📷", "Les alertes radars et la loi",
        "Pilote annonce les radars. Selon le pays où tu roules, c'est permis… ou interdit. À toi de choisir, en connaissance de cause.")
    LegalCard("🇫🇷", "En France") {
        Forbidden("Utiliser un appareil ou une appli qui indique où se trouvent les radars, fixes ou mobiles.",
            "Jusqu'à 1 500 € d'amende et 6 points, pour le conducteur")
        Allowed("CE QUI RESTE AUTORISÉ",
            "La limitation de vitesse, les accidents, les travaux, les bouchons et les dangers. Pilote continue de les annoncer, même radars coupés.")
    }
    LegalCard("🌍", "Ailleurs en Europe") {
        InfoRow("⛔", "Interdit aussi", "Suisse (amende et appareil confisqué), Allemagne, Autriche, Liechtenstein, Slovaquie, Irlande.", RedBg)
        InfoRow("✅", "Autorisé", "Belgique, Luxembourg, Italie, Espagne, Pays-Bas, Royaume-Uni.", GreenBg)
        Text("Les lois changent : c'est celle du pays où tu roules qui compte.", color = DriveColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
    }
    val on = settings.radarsOn
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DriveColors.Panel)
            .border(1.dp, if (on) DriveColors.Accent else DriveColors.Track, RoundedCornerShape(18.dp))
            .clickable(role = Role.Switch) { settings.changeRadarsOn(!on) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Alertes radars", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (on) "Activées. Là où elles sont interdites, c'est toi qui en réponds."
                else "Coupées. Limitations, accidents, travaux et dangers restent annoncés.",
                color = Soft, fontSize = 15.sp, lineHeight = 21.sp,
            )
        }
        Switch(checked = on, onCheckedChange = { settings.changeRadarsOn(it) },
            colors = SwitchDefaults.colors(checkedTrackColor = DriveColors.Accent))
    }
    Text("Tu peux changer d'avis à tout moment : Réglages → « Alertes radars ».",
        color = DriveColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun PermissionsPage(drive: Boolean) {
    val context = LocalContext.current
    Header("🔐", "Ce que Pilote demande, et pourquoi",
        "La position est demandée au démarrage ; le reste, seulement quand tu actives la fonction. Tu peux tout retirer dans les réglages d'Android.")
    LegalCard(null, "Nécessaire") {
        InfoRow("📍", "Position",
            if (drive) "Guidage, limitations et alertes."
            else "Guidage, limitations et alertes. « Toujours autoriser » seulement pour le démarrage automatique en roulant.")
        InfoRow("🔔", "Notifications", "Les alertes et le trajet en cours, même écran éteint.")
    }
    LegalCard(null, "Facultatif") {
        if (drive) {
            InfoRow("🎙", "Micro", "Signaler à la voix et répondre « oui » ou « non » aux questions de Pilote. Le micro s'ouvre seulement quand Pilote attend ta réponse.")
            InfoRow("🎵", "Accès aux notifications", "Afficher et piloter la musique des autres applis. Rien n'est lu ni gardé.")
            InfoRow("🫧", "Affichage par-dessus les applis", "La bulle d'alerte au-dessus des autres applis du poste.")
            InfoRow("📲", "Liste des applis", "L'écran d'accueil du poste : afficher et lancer tes applis.")
        } else {
            InfoRow("🎙", "Micro", "Répondre à la voix (« oui », « non », réponse à un message) et enregistrer ta propre voix pour les alertes. Le micro s'ouvre seulement quand Pilote attend ta réponse.")
            InfoRow("💬", "Accès aux notifications", "Lire tes messages à voix haute et piloter la musique. Rien n'est gardé.")
            InfoRow("🫧", "Affichage par-dessus les applis", "La bulle d'alerte au-dessus d'un autre GPS.")
            InfoRow("📅", "Agenda", "Proposer l'itinéraire vers ton prochain rendez-vous.")
            InfoRow("📸", "Caméra", "Dashcam (les 3 dernières minutes, gardées sur ton téléphone) et assistant de distance, sans Internet. Pour publier une vidéo, floute les visages et les plaques.")
            InfoRow("✉️", "SMS", "Prévenir ton contact d'urgence après un choc violent.")
            InfoRow("🔵", "Bluetooth", "Démarrer Pilote quand le téléphone se connecte à la voiture.")
            InfoRow("🖥", "Écran de la voiture", "Mode miroir : afficher et toucher le téléphone sur l'autoradio (accessibilité et capture d'écran). Pilote ne lit rien de ce qui est affiché.")
        }
    }
    Tinted(BlueBg, "🔒", BlueText, "TES DONNÉES", BlueText) {
        BodyText("Elles ne sont jamais vendues ni utilisées pour la publicité.")
        LinkText("Lire la politique de confidentialité") { openUrl(context, Legal.PRIVACY_URL) }
    }
}

@Composable
private fun CommitPage(review: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    Header("✍️", "Ton engagement",
        "Pilote est une aide à la conduite : il ne remplace ni ton attention, ni le code de la route. Coche les trois points pour continuer.")
    var c1 by remember { mutableStateOf(review) }
    var c2 by remember { mutableStateOf(review) }
    var c3 by remember { mutableStateOf(review) }
    CheckRow("Je respecte le code de la route et les panneaux, même quand Pilote se trompe.", c1) { c1 = it }
    CheckRow("Je ne touche pas au téléphone en roulant : je parle à Pilote, ou je me gare.", c2) { c2 = it }
    CheckRow("J'ai compris : les alertes radars sont interdites dans certains pays, dont la France. Si je les garde, j'en suis seul responsable.", c3) { c3 = it }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LinkText("Conditions d'utilisation") { openUrl(context, Legal.TERMS_URL) }
        LinkText("Confidentialité") { openUrl(context, Legal.PRIVACY_URL) }
    }
    val all = c1 && c2 && c3
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BigButton("‹ Retour", DriveColors.Track, onBack)
        PrimaryButton(if (review) "J'ai compris" else "J'accepte et je démarre", enabled = all, modifier = Modifier.weight(1f)) {
            Legal.accept(context); onDone()
        }
    }
    if (!review) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, DriveColors.Track, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { activityOf(context)?.finishAffinity() }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Je refuse et je quitte", color = Soft, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
    Text("Ce message revient si les règles changent. À relire quand tu veux dans Réglages → « Infos légales ».",
        color = DriveColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun Header(emoji: String, title: String, intro: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(BlueBg), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 28.sp)
        }
        Text(title, color = Color.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Text(intro, color = Soft, fontSize = 17.sp, lineHeight = 24.sp)
    }
}

@Composable
private fun LegalCard(emoji: String?, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DriveColors.Panel)
            .border(1.dp, DriveColors.Track, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (emoji != null) Text(emoji, fontSize = 22.sp)
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        content()
    }
}

@Composable
private fun Tinted(bg: Color, mark: String, markColor: Color, label: String, labelColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(mark, color = markColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = labelColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            content()
        }
    }
}

@Composable
private fun Forbidden(text: String, fine: String?) = Tinted(RedBg, "✕", RedMark, "INTERDIT", RedText) {
    BodyText(text)
    if (fine != null) Text(fine, color = RedText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Allowed(label: String, text: String) = Tinted(GreenBg, "✓", GreenMark, label, GreenText) { BodyText(text) }

@Composable
private fun BodyText(text: String) = Text(text, color = Body, fontSize = 16.sp, lineHeight = 22.sp)

@Composable
private fun LinkText(text: String, onClick: () -> Unit) = Text(
    text, color = BlueText, fontSize = 15.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline,
    modifier = Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
)

@Composable
private fun InfoRow(emoji: String, title: String, text: String, tint: Color = DriveColors.Track) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(tint), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Soft, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
            .border(1.dp, if (checked) DriveColors.Accent else DriveColors.Track, RoundedCornerShape(14.dp))
            .clickable(role = Role.Checkbox) { onChange(!checked) }.padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(
            checkedColor = DriveColors.Accent, uncheckedColor = DriveColors.TextSecondary, checkmarkColor = DriveColors.Background))
        Text(text, color = Body, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (enabled) DriveColors.Accent else DriveColors.Track)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) DriveColors.Background else DriveColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        android.widget.Toast.makeText(context, "Aucun navigateur : $url", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun activityOf(context: Context): Activity? =
    generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
