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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DriveSettings
import fr.roadguard.core.account.AccountManager
import fr.roadguard.core.community.Community
import fr.roadguard.core.net.Net
import org.json.JSONObject

/**
 * Avertissement légal, montré avant tout le reste : au premier lancement, puis chaque fois que le texte change
 * ([VERSION]). Quatre écrans : la loi au volant ; les alertes radars selon les pays (activées à l'acceptation, choix
 * de Nathan, sauf si l'utilisateur les coupe ; dans ce cas une case de responsabilité est exigée : interdites en
 * France, en Suisse…, conséquences assumées) ; les autorisations demandées ; l'engagement (code de la route,
 * téléphone, conditions d'utilisation lues dans l'appli, majorité, « J'accepte » écrit). L'acceptation (date, version, choix pour
 * les radars, appareil, compte) est envoyée au serveur comme preuve. Relisible depuis Réglages → « Infos légales ».
 */
object Legal {
    /** À augmenter quand le texte change : l'avertissement est alors montré de nouveau à tous. */
    const val VERSION = 4
    const val TERMS_URL = "https://roadguard.roadguard-app.workers.dev/conditions"
    const val PRIVACY_URL = "https://roadguard.roadguard-app.workers.dev/confidentialite"
    var forceShow by mutableStateOf(false)
    /** Activation des alertes radars demandée (réglages, accueil) sans la case cochée : confirmation affichée. */
    var askRadars by mutableStateOf(false)
    /** Désactivation du mode Suisse demandée sans la case dédiée cochée : confirmation affichée. */
    var askSwiss by mutableStateOf(false)

    private fun prefs(c: Context) = c.getSharedPreferences("legal", Context.MODE_PRIVATE)
    private fun testBuild(c: Context) =
        c.resources.getIdentifier("rg_emulator_test", "bool", c.packageName).let { it != 0 && c.resources.getBoolean(it) }

    fun accepted(c: Context) = prefs(c).getInt("accepted_version", 0) >= VERSION

    /** Case d'activation des alertes radars cochée par l'utilisateur pour la version en cours du texte. */
    fun radarConsented(c: Context) = testBuild(c) || prefs(c).getInt("radar_consent_version", 0) >= VERSION

    /** Case « je coupe le mode Suisse » cochée par l'utilisateur pour la version en cours du texte. */
    fun swissConsented(c: Context) = prefs(c).getInt("swiss_consent_version", 0) >= VERSION

    /** Phrases écrites par l'utilisateur lui-même (gardées comme preuve), pour la version en cours. */
    fun typedAccept(c: Context) = prefs(c).getString("accept_typed", "") ?: ""
    fun typedRadars(c: Context) = if (radarConsented(c)) prefs(c).getString("radar_typed", "") ?: "" else ""

    /**
     * Acceptation de l'avertissement. [acceptTyped] : « J'accepte » écrit par l'utilisateur ; [radars] : alertes radars gardées
     * (activées à l'acceptation, case de responsabilité cochée ; [radarTyped] : ce qui est gardé comme preuve).
     */
    fun accept(c: Context, radars: Boolean, acceptTyped: String, radarTyped: String) {
        val now = System.currentTimeMillis()
        val e = prefs(c).edit().putInt("accepted_version", VERSION).putLong("accepted_at", now).putBoolean("radars", radars)
            .putString("accept_typed", acceptTyped.trim().take(80))
        // Radars acceptés : consignes légales de la Suisse données en même temps (accord valable aussi pour la Suisse).
        // Le mode Suisse, lui, ne change pas : radars coupés en Suisse jusqu'à ce que l'utilisateur les y rallume (bouton).
        if (radars) e.putInt("radar_consent_version", VERSION).putLong("radar_consent_at", now).putString("radar_typed", radarTyped.trim().take(80))
            .putInt("swiss_consent_version", VERSION).putLong("swiss_consent_at", now).putBoolean("swiss", true)
            .putString("swiss_typed", "avec l'activation des radars")
        else e.remove("radar_consent_version").remove("radar_typed").remove("swiss_consent_version")
        e.commit() // tout de suite : les réglages relisent cet accord juste après
        DriveSettings(c).let { it.changeRadarsOn(radars); if (!radars) it.changeSwissMode(true) }
        syncSoon(c)
    }

    /** Activation des alertes radars après coup : même case, même phrase à écrire, même preuve. */
    fun consentRadars(c: Context, typed: String) {
        val now = System.currentTimeMillis()
        prefs(c).edit().putInt("radar_consent_version", VERSION).putLong("radar_consent_at", now).putBoolean("radars", true)
            .putString("radar_typed", typed.trim().take(80))
            .putInt("swiss_consent_version", VERSION).putLong("swiss_consent_at", now).putBoolean("swiss", true)
            .putString("swiss_typed", "avec l'activation des radars").commit()
        DriveSettings(c).changeRadarsOn(true)
        syncSoon(c)
    }

    /**
     * Bouton « Suisse » (module radars de l'accueil) : coupe les radars en Suisse, ou les y rallume. Les rallumer
     * demande l'accord des radars (consignes Suisse comprises) s'il n'est pas déjà donné : la confirmation s'affiche
     * ([askSwiss]). Rend le message à montrer, ou null si la confirmation s'affiche.
     */
    fun toggleSwiss(c: Context, s: DriveSettings): String? {
        if (!s.swissMode) { s.changeSwissMode(true); return "Radars coupés en Suisse (mode Suisse)" }
        if (!s.radarsOn || !swissConsented(c)) { askSwiss = true; return null }
        s.changeSwissMode(false)
        return "Radars actifs aussi en Suisse : interdit là-bas, à tes risques"
    }

    fun syncSoon(c: Context) {
        val app = c.applicationContext ?: c
        Thread { runCatching { sync(app) } }.start()
    }

    /** Preuve d'acceptation envoyée au serveur, une fois par version, choix et compte (bloquant : hors fil principal). */
    fun sync(c: Context) {
        val p = prefs(c)
        val version = p.getInt("accepted_version", 0)
        if (version == 0) return
        val radars = p.getBoolean("radars", false)
        val swiss = p.getBoolean("swiss", false) && p.getInt("swiss_consent_version", 0) >= version
        val account = AccountManager.get(c)
        val user = account.status?.username ?: ""
        val key = "$version|$radars|$swiss|$user"
        if (p.getString("proof_sent", null) == key) return
        val at = maxOf(p.getLong("accepted_at", 0), if (radars) p.getLong("radar_consent_at", 0) else 0, if (swiss) p.getLong("swiss_consent_at", 0) else 0)
        val typed = listOfNotNull(
            p.getString("accept_typed", null)?.takeIf { it.isNotBlank() }?.let { "acceptation : « $it »" },
            p.getString("radar_typed", null)?.takeIf { radars && it.isNotBlank() }?.let { "radars : « $it »" },
            p.getString("swiss_typed", null)?.takeIf { swiss && it.isNotBlank() }?.let { "Suisse : « $it »" },
        ).joinToString(" · ")
        val body = JSONObject().put("version", version).put("radars", radars).put("swiss", swiss).put("at", at).put("typed", typed)
            .put("device", account.deviceId()).put("app", if (c.packageName.contains("drive")) "drive" else "gps")
            .put("appVersion", runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull() ?: "?")
        val token = account.sessionToken()
        val r = Net.fetch(Community.DEFAULT_URL + "/v1/legal/accept" + (token?.let { "?session=$it" } ?: ""),
            "POST", body.toString().toByteArray(), "application/json")
        if (r.code in 200..299) p.edit().putString("proof_sent", key).apply()
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

/** Phrases que l'utilisateur doit écrire lui-même (preuve plus forte qu'une case cochée). */
private const val ACCEPT_PHRASE = "J'accepte"
private const val RADAR_PHRASE = "J'active les radars à mes risques"

/** Case obligatoire pour accepter quand les radars restent activés (choix par défaut). */
private const val RADAR_ACK =
    "J'ai compris : les alertes radars sont activées. Elles sont interdites en France, en Suisse (même seulement transportées) " +
        "et dans d'autres pays ; pour y être en règle, je dois les couper. En Suisse, Pilote les coupe automatiquement ; si je les " +
        "y rallume (bouton « Suisse »), c'est aussi à mes risques. Si je les garde, j'en assume seul toutes les conséquences : " +
        "amendes, retrait de points, suspension du permis, confiscation de l'appareil."

/** Phrase écrite reconnue sans tenir compte des majuscules, accents, apostrophes, espaces ni ponctuation. */
internal fun typedMatches(typed: String, phrase: String): Boolean {
    fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").filter { it.isLetterOrDigit() }
    return norm(typed).isNotEmpty() && norm(typed) == norm(phrase)
}


private const val RADAR_CONSENT =
    "J'active les alertes radars. Je sais qu'elles sont interdites en France, en Suisse (même seulement transportées) et dans " +
        "d'autres pays, et que pour y être en règle je dois les laisser coupées. En Suisse, Pilote les coupe automatiquement ; " +
        "si je les y rallume (bouton « Suisse »), c'est aussi à mes risques. J'en assume seul toutes les conséquences : amendes, " +
        "retrait de points, suspension du permis, confiscation de l'appareil."

/** [review] : relu depuis les réglages (déjà accepté) : bouton « Fermer », choix actuels repris. */
@Composable
fun LegalScreen(review: Boolean = false, onDone: () -> Unit) {
    val context = LocalContext.current
    val drive = context.packageName.contains("drive")
    var page by remember { mutableStateOf(0) }
    // Choix de Nathan : radars activés à l'acceptation, sauf si l'utilisateur choisit de les couper (étape 2).
    // L'acceptation exige alors une case de responsabilité dédiée (étape 4) et « J'accepte » écrit.
    var radarsOff by remember { mutableStateOf(review && !DriveSettings(context).radarsOn) }
    val radars = !radarsOff
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
                1 -> RadarsPage(radarsOff) { radarsOff = it }
                2 -> PermissionsPage(drive)
                else -> CommitPage(review, radars, onBack = { page-- }, onDone = onDone)
            }
            if (page < 3) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) BigButton("‹ Retour", DriveColors.Track) { page-- }
                PrimaryButton("Suivant", modifier = Modifier.weight(1f)) { page++ }
            }
        }
    }
}

/** Confirmation demandée quand on active les alertes radars sans avoir coché la case (réglages, accueil). */
@Composable
fun RadarConsentScreen(swiss: Boolean = false, onDone: () -> Unit) {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    FullScreenDialog(onDismissRequest = onDone) {
        Box(Modifier.fillMaxSize().background(DriveColors.Background).padding(horizontal = 16.dp), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (swiss) Header("🇨🇭", "Radars aussi en Suisse ?",
                    "Les alertes radars sont interdites en Suisse (et en France…). Pour les y rallumer, donne d'abord ton accord pour les radars : coche la case et écris la phrase.")
                else Header("📷", "Activer les alertes radars ?",
                    "Elles sont interdites dans plusieurs pays, dont la France et la Suisse. Pour les activer, coche la case et écris la phrase : c'est toi qui choisis, et qui en réponds.")
                FranceCard()
                EuropeCard()
                CheckRow(RADAR_CONSENT, checked, warning = true) { checked = it }
                if (checked) TypedConsent(RADAR_PHRASE, typed) { typed = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BigButton("Annuler", DriveColors.Track, onDone)
                    PrimaryButton(if (swiss) "Activer, Suisse comprise" else "Activer les alertes radars",
                        enabled = checked && typedMatches(typed, RADAR_PHRASE), modifier = Modifier.weight(1f)) {
                        Legal.consentRadars(context, typed)
                        if (swiss) DriveSettings(context).changeSwissMode(false)
                        android.widget.Toast.makeText(context,
                            if (swiss) "Radars actifs, aussi en Suisse, sous ta responsabilité" else "Alertes radars activées, sous ta responsabilité",
                            android.widget.Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                }
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
private fun FranceCard() = LegalCard("🇫🇷", "En France") {
    Forbidden("Utiliser un appareil ou une appli qui indique où se trouvent les radars, fixes ou mobiles.",
        "Jusqu'à 1 500 € d'amende, 6 points, suspension du permis jusqu'à 3 ans et confiscation de l'appareil, pour le conducteur")
    Allowed("POUR ÊTRE EN RÈGLE",
        "Laisse les alertes radars coupées. Pilote annonce quand même la limitation, les dépassements, les accidents, les travaux, les bouchons et les dangers.")
}

@Composable
private fun EuropeCard() = LegalCard("🌍", "Ailleurs en Europe") {
    InfoRow("🇨🇭", "Suisse : coupées automatiquement",
        "Interdit, même seulement transporté (amende, appareil confisqué). Pilote les coupe en Suisse ; tu peux les y rallumer d'un bouton (« Suisse », module radars de l'accueil), à tes risques. Les signalements police et radars de la communauté n'y sont jamais diffusés.", RedBg)
    InfoRow("⛔", "Interdit aussi : à laisser coupées", "Allemagne, Autriche, Liechtenstein, Slovaquie, Irlande.", RedBg)
    InfoRow("✅", "Autorisé", "Belgique, Luxembourg, Italie, Espagne, Pays-Bas, Royaume-Uni.", GreenBg)
    Text("Les lois changent : c'est celle du pays où tu roules qui compte.", color = DriveColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun RadarsPage(off: Boolean, onOff: (Boolean) -> Unit) {
    Header("📷", "Les alertes radars et la loi",
        "Les alertes radars s'activent quand tu acceptes. Elles sont interdites dans plusieurs pays, dont la France : lis bien, et coupe-les si tu veux être en règle.")
    FranceCard()
    EuropeCard()
    LegalCard(null, "Ton choix") {
        CheckRow("Je préfère couper les alertes radars : je suis en règle partout. Pilote annonce quand même limitations, dépassements, accidents, travaux, bouchons et dangers.", off, onChange = onOff)
        Text(
            if (off) "Alertes radars : coupées. Tu es en règle partout."
            else "Alertes radars : activées dès que tu acceptes. Là où elles sont interdites, c'est toi qui en réponds.",
            color = if (off) GreenText else RedText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
    Text("Tu pourras changer d'avis à tout moment : bouton 📷 de la carte, ou Réglages → « Alertes radars ».",
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
private fun CommitPage(review: Boolean, radars: Boolean, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    Header("✍️", "Ton engagement",
        "Pilote est une aide à la conduite : il ne remplace ni ton attention, ni le code de la route. Coche les cases et écris « J'accepte » pour continuer.")
    if (radars) {
        Tinted(RedBg, "!", RedMark, "TON CHOIX : ALERTES RADARS ACTIVÉES", RedText) {
            BodyText("En France, en Suisse et dans les autres pays où elles sont interdites, tu n'es pas en règle tant qu'elles sont activées, et tu en assumes seul les conséquences. Tu peux revenir à l'étape 2 pour les couper.")
        }
    } else {
        Tinted(GreenBg, "✓", GreenMark, "TON CHOIX : ALERTES RADARS COUPÉES", GreenText) {
            BodyText("Tu es en règle partout. Tu pourras les activer plus tard, avec la case et la phrase à écrire.")
        }
    }
    var c1 by remember { mutableStateOf(review) }
    var c2 by remember { mutableStateOf(review) }
    var c3 by remember { mutableStateOf(review) }
    var c4 by remember { mutableStateOf(review) }
    var c5 by remember { mutableStateOf(review) }
    var showTerms by remember { mutableStateOf(false) }
    var acceptTyped by remember { mutableStateOf(if (review) Legal.typedAccept(context) else "") }
    CheckRow("Je respecte le code de la route et les panneaux, même quand Pilote se trompe.", c1) { c1 = it }
    CheckRow("Je ne touche pas au téléphone en roulant : je parle à Pilote, ou je me gare.", c2) { c2 = it }
    if (radars) CheckRow(RADAR_ACK, c5, warning = true) { c5 = it }
    CheckRow("J'ai lu et j'accepte les conditions d'utilisation.", c3) { c3 = it }
    LinkText(if (showTerms) "Masquer les conditions d'utilisation" else "Lire les conditions d'utilisation ici") { showTerms = !showTerms }
    if (showTerms) TermsText()
    CheckRow("Je suis majeur(e).", c4) { c4 = it }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LinkText("Conditions (site)") { openUrl(context, Legal.TERMS_URL) }
        LinkText("Confidentialité") { openUrl(context, Legal.PRIVACY_URL) }
    }
    TypedConsent(ACCEPT_PHRASE, acceptTyped) { acceptTyped = it }
    val all = c1 && c2 && c3 && c4 && (!radars || c5) && typedMatches(acceptTyped, ACCEPT_PHRASE)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BigButton("‹ Retour", DriveColors.Track, onBack)
        PrimaryButton(if (review) "J'ai compris" else "J'accepte et je démarre", enabled = all, modifier = Modifier.weight(1f)) {
            Legal.accept(context, radars, acceptTyped, if (radars) "activés à l'acceptation, case de responsabilité cochée" else ""); onDone()
        }
    }
    if (!review) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, DriveColors.Track, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { activityOf(context)?.finishAffinity() }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Je refuse et je quitte", color = Soft, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
    Text("Ton acceptation (date, version du texte, choix pour les alertes radars, phrase que tu as écrite) est enregistrée comme preuve. " +
        "Ce message revient si les règles changent. À relire quand tu veux dans Réglages → « Infos légales ».",
        color = DriveColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
}

/** Conditions d'utilisation lisibles dans l'appli (même texte que la page /conditions du serveur : les garder identiques). */
private val TERMS = listOf(
    "1. Une aide, pas un pilote automatique" to "Pilote est une aide à la conduite. Il ne remplace ni ton attention, ni le code de la route. Les limitations, radars, itinéraires et signalements peuvent être faux, incomplets ou pas à jour : la signalisation et les règles de circulation passent toujours avant ce qu'affiche ou annonce l'appli.",
    "2. Au volant" to "Ne tiens pas et ne manipule pas ton téléphone en roulant (en France : 135 € d'amende et 3 points, écouteurs compris). Fixe-le sur un support, règle ton trajet avant de partir et utilise la voix. Aucun écran qui n'aide pas à la conduite (vidéo, autres applis, mode miroir) ne doit être visible du conducteur en roulant (en France : jusqu'à 1 500 € d'amende et 3 points).",
    "3. Alertes radars" to "La loi dépend du pays. En France, utiliser un appareil ou une application qui indique l'emplacement des radars fixes ou mobiles est interdit (décret du 3 janvier 2012 : jusqu'à 1 500 € d'amende, 6 points, suspension du permis, confiscation) ; seules les « zones de danger » sont admises. C'est aussi interdit, notamment, en Suisse, en Allemagne, en Autriche, au Liechtenstein, en Slovaquie et en Irlande. Les alertes radars de Pilote s'activent quand tu acceptes ces conditions, en cochant la case qui rappelle cette interdiction et en écrivant « J'accepte » ; tu peux les couper dès l'avertissement, puis à tout moment (bouton 📷, Réglages). Pour être en règle dans ces pays, coupe-les. En Suisse, Pilote les coupe automatiquement (« mode Suisse ») ; ton accord pour les radars couvre aussi la Suisse, et tu peux les y rallumer d'un bouton (« Suisse », module radars de l'accueil), à tes risques ; les signalements « police » et « radar » de la communauté n'y sont jamais diffusés. Si tu les actives, tu le fais en connaissance de cause et sous ta seule responsabilité : amendes, retraits de points, suspensions de permis et confiscations restent à ta charge. Ton choix est enregistré avec la date. Ces informations ne sont pas un conseil juridique.",
    "4. Signalements de contrôles" to "Les signalements « police » de la communauté peuvent être suspendus à tout moment, sans préavis, notamment pour appliquer une interdiction de diffusion décidée par l'autorité administrative.",
    "5. Communauté" to "Signale uniquement ce que tu as vu, honnêtement. Sont interdits : les faux signalements, les messages injurieux, discriminatoires ou illégaux, et toute donnée personnelle sur autrui (nom, plaque, photo). Un compte qui abuse peut être suspendu ou banni.",
    "6. Caméra et dashcam" to "Les images restent sur ton téléphone. Si tu publies une vidéo, floute les visages et les plaques.",
    "7. Compte, âge et accès" to "Tu dois être majeur pour utiliser Pilote. L'accès peut être limité dans le temps (essai), suspendu en cas d'abus ou modifié à tout moment. Pour supprimer ton compte et tes données : Réglages → ton compte → « Écrire à l'administrateur ».",
    "8. Preuve d'acceptation" to "Quand tu acceptes ces conditions (en écrivant « J'accepte ») ou que tu actives plus tard les alertes radars (en écrivant la phrase demandée), Pilote enregistre la date, la version du texte, tes choix, les phrases que tu as écrites, l'identifiant de l'appareil et ton compte, comme preuve.",
    "9. Responsabilité" to "L'appli est fournie telle quelle, sans garantie d'exactitude ni de disponibilité. Dans les limites permises par la loi, l'éditeur n'est pas responsable des conséquences d'une utilisation contraire à ces conditions ou au code de la route, ni des sanctions liées à l'usage des alertes radars.",
)

@Composable
private fun TermsText() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
            .border(1.dp, DriveColors.Track, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Conditions d'utilisation (version ${Legal.VERSION})", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        TERMS.forEach { (title, text) ->
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Soft, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

/** Champ où l'utilisateur écrit lui-même la phrase de confirmation ([phrase]). */
@Composable
private fun TypedConsent(phrase: String, value: String, onValue: (String) -> Unit) {
    val ok = typedMatches(value, phrase)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Pour confirmer, écris toi-même : « $phrase »", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = value, onValueChange = { onValue(it.take(80)) }, singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (ok) GreenMark else DriveColors.Accent, unfocusedBorderColor = if (ok) GreenMark else DriveColors.Track,
                cursorColor = DriveColors.Accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (ok) Text("✓ Confirmé", color = GreenText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
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

/** [warning] : case lourde de conséquences (activation des radars) : liseré rouge une fois cochée. */
@Composable
private fun CheckRow(text: String, checked: Boolean, warning: Boolean = false, onChange: (Boolean) -> Unit) {
    val on = if (warning) RedMark else DriveColors.Accent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
            .border(1.dp, if (checked) on else DriveColors.Track, RoundedCornerShape(14.dp))
            .clickable(role = Role.Checkbox) { onChange(!checked) }.padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(
            checkedColor = on, uncheckedColor = DriveColors.TextSecondary, checkmarkColor = DriveColors.Background))
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
