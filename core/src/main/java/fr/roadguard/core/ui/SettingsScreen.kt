package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DriveSettings
import fr.roadguard.core.ThemeMode
import fr.roadguard.core.radar.AlertText
import fr.roadguard.core.radar.RadarKind

/** Catégories des réglages (l'écran d'accueil des réglages les présente en grandes tuiles). */
private enum class Cat(val emoji: String, val title: String, val hint: String) {
    VOICE("🗣", "Voix", "Ton prénom, voix, débit, langue, bips"),
    TONE("🎭", "Ton et imitations", "Classique, charme, séduction, osé, extrême, Coquin XXL"),
    RADARS("📷", "Alertes radars", "Activer ou couper, types, bips, alarme, Suisse"),
    SPEED("🚦", "Vitesse et limitations", "Dépassement, petit bip, tolérance, pluie, jeune conducteur"),
    ROUTE("🧭", "Trajet et itinéraire", "Péages, autoroutes, étapes, pause"),
    SAFETY("🛡", "Sécurité", "Météo, dangers, accident, somnolence, dashcam"),
    PHONE("💬", "Messages et téléphone", "Messages lus, montre, moto, Bluetooth, batterie, musique"),
    BACKGROUND("📲", "Arrière-plan et démarrage", "Bulle, fermeture de Pilote, démarrage automatique, GPS"),
    MAP("🗺", "Carte et affichage", "Carte, thème, couleurs, boutons, accueil, cartes hors ligne"),
    ACCOUNT("👤", "Compte et aide", "Compte, communauté, tutoriel, infos légales, mise à jour"),
}

/** Tous les réglages, pour la recherche (« péage », « sirène », « prénom »…) : catégorie et intitulé. */
private val INDEX: List<Pair<Cat, String>> = listOf(
    Cat.VOICE to "Ton prénom (Pilote t'appelle par ton prénom)", Cat.VOICE to "Voix (radars, guidage, vitesse moyenne)",
    Cat.VOICE to "Bip d'alerte", Cat.VOICE to "Baisser la musique pendant les annonces", Cat.VOICE to "Choix de la voix",
    Cat.VOICE to "Télécharger d'autres voix", Cat.VOICE to "Débit de la voix : lente, normale, rapide",
    Cat.VOICE to "Langue des annonces : français, anglais, allemand, italien", Cat.VOICE to "Je suis un conducteur / une conductrice",
    Cat.TONE to "Ton classique", Cat.TONE to "Ton charme", Cat.TONE to "Ton séduction", Cat.TONE to "Ton osé",
    Cat.TONE to "Ton extrême", Cat.TONE to "Coquin XXL (imitation, phrases coquines)",
    Cat.RADARS to "Alertes radars : activer ou couper", Cat.RADARS to "Types : radars fixes, feux rouges, vitesse moyenne, mobiles",
    Cat.RADARS to "Bips de plus en plus rapides en approchant (détecteur)", Cat.RADARS to "Alarme forte si trop vite près d'un radar",
    Cat.RADARS to "Mode Suisse", Cat.RADARS to "Infos légales (radars et loi)",
    Cat.SPEED to "Annoncer les changements de limitation", Cat.SPEED to "Me prévenir à la voix si je dépasse (sinon un petit bip)",
    Cat.SPEED to "Tolérance de dépassement", Cat.SPEED to "Alarme forte dès que je dépasse la limitation",
    Cat.SPEED to "Me réprimander si je roule trop vite", Cat.SPEED to "Pluie : limitations abaissées",
    Cat.SPEED to "Jeune conducteur (permis probatoire)", Cat.SPEED to "Vitesse maximale personnelle (remorque, caravane)",
    Cat.ROUTE to "Éviter les péages", Cat.ROUTE to "Éviter les autoroutes", Cat.ROUTE to "Éviter routes étroites, chemins, centres-villes",
    Cat.ROUTE to "Itinéraire économe en carburant", Cat.ROUTE to "Ordre des étapes", Cat.ROUTE to "Rappel de pause après 2 h",
    Cat.ROUTE to "Ma voiture, à proximité, mes trajets, convoi, tête haute",
    Cat.SAFETY to "Météo dangereuse : verglas, brouillard, pluie", Cat.SAFETY to "Passages à niveau, virages, écoles",
    Cat.SAFETY to "Fin de trajet : rien oublié à l'arrière", Cat.SAFETY to "Réponse oui / non à la voix",
    Cat.SAFETY to "Détection d'accident, contact d'urgence, SMS", Cat.SAFETY to "Sirène anti-somnolence", Cat.SAFETY to "Dashcam",
    Cat.PHONE to "Lire les messages à voix haute (SMS, WhatsApp)", Cat.PHONE to "Montre connectée : vibrer à chaque radar",
    Cat.PHONE to "Mode moto", Cat.PHONE to "Bluetooth de la voiture", Cat.PHONE to "Économie de batterie",
    Cat.PHONE to "Agenda : rendez-vous", Cat.PHONE to "Lecteur de musique",
    Cat.BACKGROUND to "Bulle par-dessus Waze, Google Maps", Cat.BACKGROUND to "Continuer quand je ferme Pilote",
    Cat.BACKGROUND to "Démarrer tout seul en roulant", Cat.BACKGROUND to "Position GPS : poste ou téléphone",
    Cat.BACKGROUND to "Ouvrir DRIVE à l'allumage du poste",
    Cat.MAP to "Nord toujours en haut", Cat.MAP to "Zoom selon la vitesse", Cat.MAP to "Style de carte", Cat.MAP to "Thème jour / nuit",
    Cat.MAP to "Couleur de l'appli", Cat.MAP to "Taille des boutons", Cat.MAP to "Cartes hors ligne, radars par pays, mes radars",
    Cat.MAP to "Fond d'écran et logo de démarrage", Cat.MAP to "Disposition de l'accueil", Cat.MAP to "Horloge",
    Cat.MAP to "Météo et adresse", Cat.MAP to "Gestes sur les bords",
    Cat.ACCOUNT to "Mon compte, mot de passe, sauvegarde des réglages", Cat.ACCOUNT to "Communauté : pseudo, points, classement",
    Cat.ACCOUNT to "Revoir le tutoriel", Cat.ACCOUNT to "Infos légales", Cat.ACCOUNT to "Mise à jour", Cat.ACCOUNT to "À propos",
)

/** Texte sans accents ni majuscules, pour une recherche tolérante. */
private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/** Réglages rangés par catégories (Voix, Ton et imitations, Alertes radars…), avec une recherche. Gros boutons, lisibles en voiture. */
@Composable
fun SettingsScreen(settings: DriveSettings, radarCount: Int, onClose: () -> Unit, onOfflineData: (() -> Unit)? = null, showAutostart: Boolean = false,
    onCommunity: (() -> Unit)? = null, onMusicPlayer: (() -> Unit)? = null, onPersonalize: (() -> Unit)? = null,
    voice: fr.roadguard.core.radar.AlertVoice? = null, updateApp: String? = null, onAdmin: (() -> Unit)? = null,
    session: fr.roadguard.core.DrivingSession? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Outils : affichés en plein écran à la place des réglages, dans la même fenêtre. Une fenêtre séparée (Dialog)
    // était décalée sur certains autoradios (réglages visibles à gauche, « Fermer » et « Y aller » coupés à droite).
    val tool = remember { mutableStateOf(0) }
    if (session != null && tool.value != 0) {
        val close = { tool.value = 0 }
        androidx.activity.compose.BackHandler(onBack = close)
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(DriveColors.Background)) {
            when (tool.value) {
                1 -> ParkingScreen(session, close); 2 -> NearbyScreen(session, close); 3 -> TripHistoryScreen(session, close)
                4 -> ConvoyScreen(session, close); else -> HudScreen(session, close)
            }
        }
        return
    }
    var cat by remember { mutableStateOf<Cat?>(null) }
    var query by remember { mutableStateOf("") }
    androidx.activity.compose.BackHandler(enabled = cat != null) { cat = null }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val c = cat
            if (c != null) BigButton("‹ Réglages", DriveColors.Panel) { cat = null }
            Text(if (c == null) "Réglages" else "${c.emoji} ${c.title}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (cat) {
                null -> {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it.take(40) }, singleLine = true,
                        label = { Text("🔍 Chercher un réglage (péage, prénom, bip…)") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 19.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
                            focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary, cursorColor = DriveColors.Accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (query.isNotBlank()) {
                        val words = norm(query).split(' ').filter { it.isNotBlank() }
                        val hits = INDEX.filter { (c, label) -> words.all { w -> norm("$label ${c.title} ${c.hint}").contains(w) } }
                        if (hits.isEmpty()) Text("Aucun réglage trouvé. Essaie un autre mot.", color = DriveColors.TextSecondary, fontSize = 16.sp)
                        hits.forEach { (c, label) ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
                                    .clickable { cat = c; query = "" }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(c.emoji, fontSize = 24.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(label, color = Color.White, fontSize = 18.sp)
                                    Text("dans ${c.title}", color = DriveColors.TextSecondary, fontSize = 14.sp)
                                }
                                Text("›", color = DriveColors.Accent, fontSize = 26.sp)
                            }
                        }
                    } else {
                        AccountCard(onAdmin = { onAdmin?.invoke() })
                        if (session != null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BigButton("🅿 Ma voiture", DriveColors.Panel) { tool.value = 1 }
                            BigButton("📍 À proximité", DriveColors.Panel) { tool.value = 2 }
                            BigButton("📒 Mes trajets", DriveColors.Panel) { tool.value = 3 }
                            BigButton("🚗 Convoi et partage", DriveColors.Panel) { tool.value = 4 }
                            BigButton("🪞 Tête haute", DriveColors.Panel) { tool.value = 5 }
                        }
                        Cat.entries.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { c -> CategoryTile(c, Modifier.weight(1f)) { cat = c } }
                                if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                Cat.VOICE -> VoiceCategory(settings, voice)
                Cat.TONE -> ToneCategory(settings, voice)
                Cat.RADARS -> RadarsCategory(settings)
                Cat.SPEED -> SpeedCategory(settings)
                Cat.ROUTE -> {
                    ToggleRow("Éviter les péages", settings.avoidTolls, settings::changeAvoidTolls)
                    ToggleRow("Éviter les autoroutes", settings.avoidHighways, settings::changeAvoidHighways)
                    ToggleRow("Éviter routes étroites, chemins et centres-villes", settings.routeNarrow, settings::changeRouteNarrow)
                    ToggleRow("Itinéraire le plus économe en carburant (110 km/h au plus)", settings.routeEco, settings::changeRouteEco)
                    ToggleRow("Étapes : dans l'ordre le plus rapide (sinon dans l'ordre choisi)", settings.optimizeStops, settings::changeOptimizeStops)
                    ToggleRow("Rappel de pause après 2 h de conduite", settings.pauseReminder, settings::changePauseReminder)
                    if (session != null) {
                        Section("Outils du trajet")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BigButton("🅿 Ma voiture", DriveColors.Panel) { tool.value = 1 }
                            BigButton("📍 À proximité", DriveColors.Panel) { tool.value = 2 }
                            BigButton("📒 Mes trajets", DriveColors.Panel) { tool.value = 3 }
                            BigButton("🚗 Convoi et partage", DriveColors.Panel) { tool.value = 4 }
                            BigButton("🪞 Tête haute", DriveColors.Panel) { tool.value = 5 }
                        }
                    }
                }
                Cat.SAFETY -> RoadSafetySettings(settings)
                Cat.PHONE -> {
                    ToggleRow("Lire à voix haute les messages du téléphone", settings.readMessages, settings::changeReadMessages)
                    PhoneComfortSettings(settings)
                    if (onMusicPlayer != null) BigButton("🎵 Lecteur de musique (Bluetooth du poste…)", DriveColors.Accent, onMusicPlayer)
                }
                Cat.BACKGROUND -> BackgroundCategory(settings, showAutostart)
                Cat.MAP -> MapCategory(settings, showAutostart, onOfflineData, onPersonalize)
                Cat.ACCOUNT -> {
                    AccountCard(onAdmin = { onAdmin?.invoke() })
                    if (onCommunity != null) BigButton("👥 Communauté : pseudo, points, classement", DriveColors.Accent, onCommunity)
                    BigButton("📖 Revoir le tutoriel", DriveColors.Panel) { Tutorial.forceShow = true; onClose() }
                    BigButton("⚖ Infos légales : la loi, les radars, les autorisations", DriveColors.Panel) { Legal.forceShow = true; onClose() }
                    if (updateApp != null) UpdateCard(updateApp)
                    Section("À propos")
                    Text(
                        "Pilote — © Nathan Lacroix. $radarCount points d'alerte embarqués.\n" +
                            "Cartes © OpenStreetMap (OpenFreeMap, CARTO). Recherche Photon, itinéraires Valhalla (FOSSGIS).\n" +
                            "Les alertes complètent la signalisation : elles ne la remplacent pas.",
                        color = DriveColors.TextSecondary, fontSize = 15.sp,
                    )
                }
            }
            // Marge en bas : le dernier réglage peut remonter au-dessus du bandeau de mise à jour.
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 120.dp))
        }
    }
}

@Composable
private fun CategoryTile(c: Cat, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(c.emoji, fontSize = 30.sp)
        Column(Modifier.weight(1f)) {
            Text(c.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(c.hint, color = DriveColors.TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun VoiceCategory(settings: DriveSettings, voice: fr.roadguard.core.radar.AlertVoice?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (fr.roadguard.core.account.AccountManager.get(context).status != null) {
        Section("Ton prénom")
        Text("Pilote s'adresse à toi par ton prénom : « Nathan, radar fixe dans 300 mètres », « Fais une pause, Nathan ».",
            color = DriveColors.TextSecondary, fontSize = 14.sp)
        FirstNameEditor()
    }
    Section("Annonces")
    ToggleRow("Voix (radars, guidage, vitesse moyenne)", settings.voice, settings::changeVoice)
    ToggleRow("Bip d'alerte", settings.beep, settings::changeBeep)
    ToggleRow("Baisser la musique pendant les annonces et les bips", settings.duckMusic, settings::changeDuckMusic)
    if (voice == null) return
    Section("Voix")
    val voices = remember { voice.frenchVoices() }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton("Par défaut", if (settings.voiceName == null) DriveColors.Accent else DriveColors.Panel) {
            settings.changeVoiceName(null); voice.configure(null, settings.speechRate); voice.preview { limitChanged(80) }
        }
        voices.forEach { (name, label) ->
            BigButton(label, if (settings.voiceName == name) DriveColors.Accent else DriveColors.Panel) {
                settings.changeVoiceName(name); voice.configure(name, settings.speechRate); voice.preview { limitChanged(80) }
            }
        }
    }
    // Autres langues : voix téléchargées dans Android (moteur de synthèse vocale).
    val others = remember { voice.otherVoices() }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton("⬇ Télécharger d'autres voix", DriveColors.Track) {
            val ok = runCatching {
                context.startActivity(android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (!ok) runCatching {
                context.startActivity(android.content.Intent("com.android.settings.TTS_SETTINGS").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        others.forEach { (name, label) ->
            BigButton(label, if (settings.voiceName == name) DriveColors.Accent else DriveColors.Panel) {
                settings.changeVoiceName(name); voice.configure(name, settings.speechRate); voice.preview { limitChanged(80) }
            }
        }
    }
    Section("Débit et langue")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0.85f to "Lente", 1f to "Normale", 1.2f to "Rapide").forEach { (r, label) ->
            BigButton("Débit : $label", if (settings.speechRate == r) DriveColors.Accent else DriveColors.Panel) {
                settings.changeSpeechRate(r); voice.configure(settings.voiceName, r); voice.preview { speak("Dans 300 mètres, tournez à droite") }
            }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("fr" to "🇫🇷 Français", "en" to "🇬🇧 English", "de" to "🇩🇪 Deutsch", "it" to "🇮🇹 Italiano").forEach { (l, label) ->
            BigButton(label, if (settings.voiceLang == l) DriveColors.Accent else DriveColors.Panel) {
                settings.changeVoiceLang(l); voice.configure(settings.voiceName, settings.speechRate, settings.voiceTone, l); voice.preview { limitChanged(80) }
            }
        }
    }
    if (settings.voiceLang != "fr") Text("Annonces des radars, limitations, réveil et guidage dans cette langue. Les tons coquins restent en français. " +
        "Choisis une voix de cette langue (« Autres langues » ci-dessus).", color = DriveColors.TextSecondary, fontSize = 14.sp)
    Section("Conducteur ou conductrice")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(false to "👨 Je suis un conducteur", true to "👩 Je suis une conductrice").forEach { (f, label) ->
            BigButton(label, if (settings.driverFemale == f) DriveColors.Accent else DriveColors.Panel) {
                settings.changeDriverFemale(f); voice.pack.feminine = f; voice.preview { limitChanged(80) }
            }
        }
    }
}

@Composable
private fun ToneCategory(settings: DriveSettings, voice: fr.roadguard.core.radar.AlertVoice?) {
    Text("Le ton change les phrases (radars, limitations, guidage) en gardant la même voix. La direction, la distance et la limite restent toujours exactes.",
        color = DriveColors.TextSecondary, fontSize = 15.sp)
    listOf(0 to "Classique", 1 to "Charme 💋", 2 to "Séduction 🔥", 3 to "Osé 😈", 4 to "Extrême 🔥😈",
        fr.roadguard.core.radar.VoicePack.XXL to "Coquin XXL 🎭 (près de 300 phrases)").forEach { (c, label) ->
        BigButton(if (settings.voiceTone == c) "● $label" else label, if (settings.voiceTone == c) DriveColors.Accent else DriveColors.Panel) {
            settings.changeVoiceTone(c)
            // Le ton garde la voix choisie (aucun changement de voix imposé) ; une phrase pour l'entendre.
            voice?.configure(settings.voiceName, settings.speechRate, c)
            voice?.limitChanged(80)
        }
    }
    if (settings.voiceTone == fr.roadguard.core.radar.VoicePack.XXL) Text(
        "Coquin XXL : guidage, ronds-points, radars, limitations, bouchons, arrivée…, au masculin ou au féminin (Voix → Conducteur ou conductrice). " +
            "Une phrase n'est dite que si elle correspond à la situation réelle.", color = DriveColors.TextSecondary, fontSize = 14.sp)
    if (voice == null) Text("Le choix est appliqué au prochain trajet.", color = DriveColors.TextSecondary, fontSize = 14.sp)
}

@Composable
private fun RadarsCategory(settings: DriveSettings) {
    // Activation : seulement avec l'accord de l'avertissement légal (sinon, la confirmation s'affiche).
    ToggleRow("Alertes radars (interdites en France, en Suisse… : à tes risques)", settings.radarsOn) { v ->
        settings.changeRadarsOn(v)
        if (v && !settings.radarsOn) Legal.askRadars = true
    }
    Section("Types d'alertes affichées et annoncées")
    listOf(RadarKind.FIXED, RadarKind.REDLIGHT, RadarKind.AVERAGE, RadarKind.MOBILE).forEach { kind ->
        ToggleRow(AlertText.kindLabel(kind), kind in settings.alertKinds) { settings.toggleKind(kind) }
    }
    Section("Sons à l'approche")
    ToggleRow("Bips de plus en plus rapides en approchant (détecteur)", settings.proximityBeeps, settings::changeProximityBeeps)
    ToggleRow("Alarme forte si je suis trop vite en approchant d'un radar", settings.radarAlarm, settings::changeRadarAlarm)
    Section("Suisse et loi")
    SwissModeToggle(settings)
    BigButton("⚖ Infos légales : la loi, les radars", DriveColors.Panel) { Legal.forceShow = true }
}

@Composable
private fun SpeedCategory(settings: DriveSettings) {
    ToggleRow("Annoncer les changements de limitation", settings.announceLimit, settings::changeAnnounceLimit)
    ToggleRow("Me prévenir à la voix si je dépasse la limitation", settings.speedLimitWarning, settings::changeSpeedLimitWarning)
    if (settings.speedLimitWarning) {
        Text("Dès que je dépasse :", color = Color.White, fontSize = 17.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(-1 to "Tolérance légale", 0 to "Dès 1 km/h", 3 to "+3 km/h", 10 to "+10 km/h").forEach { (v, l) ->
                BigButton(l, if (settings.overTolerance == v) DriveColors.Accent else DriveColors.Panel) { settings.changeOverTolerance(v) }
            }
        }
        ToggleRow("Alarme forte aussi sans radar, dès que je dépasse la limitation", settings.alarmAnywhere, settings::changeAlarmAnywhere)
        ToggleRow("Me réprimander si je roule trop vite trop longtemps ou trop souvent", settings.scold, settings::changeScold)
    } else {
        Text("Coupé : plus aucune voix pour te dire de ralentir. Juste un tout petit bip discret quand tu dépasses " +
            "(si « Bip d'alerte » est activé dans Voix).", color = DriveColors.TextSecondary, fontSize = 15.sp)
    }
    Section("Limitations adaptées")
    SpeedRulesSettings(settings)
}

@Composable
private fun BackgroundCategory(settings: DriveSettings, showAutostart: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bubbleHelper = remember { RadarBubble(context) }
    ToggleRow("Bulle vitesse et radars sur Waze, Google Maps…", settings.bubble) { v ->
        settings.changeBubble(v)
        if (v && !bubbleHelper.allowed) bubbleHelper.requestPermission()
    }
    if (settings.bubble && !bubbleHelper.allowed) {
        BigButton("Autoriser l'affichage par-dessus les autres applis", DriveColors.Accent) { bubbleHelper.requestPermission() }
    }
    if (!context.packageName.contains("drive")) {
        // Démarrage tout seul dès 20 km/h : il faut la position « Toujours autoriser ».
        fun bgGranted() = android.os.Build.VERSION.SDK_INT < 29 || context.checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val bgState = remember { mutableStateOf(bgGranted()) }
        val bgOk = bgState.value
        val askBg = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
            bgState.value = it || bgGranted()
            // Refus déjà donné : Android n'affiche plus la question, on ouvre la page de l'appli.
            if (!bgState.value) runCatching {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        fun askBackground() {
            if (android.os.Build.VERSION.SDK_INT >= 29) askBg.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        ToggleRow("Continuer quand je ferme Pilote (annonces et bulle en arrière-plan)", settings.keepRunning, settings::changeKeepRunning)
        ToggleRow("Démarrer tout seul en roulant (dès 20 km/h), avec la bulle et les bips", settings.autoDrive) { v ->
            settings.changeAutoDrive(v)
            if (v) {
                if (!settings.bubble) settings.changeBubble(true)
                if (!bgOk) askBackground() else if (!bubbleHelper.allowed) bubbleHelper.requestPermission()
            }
        }
        if (settings.autoDrive && !bgOk) {
            Text("Position : choisis « Toujours autoriser », sinon Pilote ne peut pas voir que tu roules quand il est fermé.",
                color = DriveColors.TextSecondary, fontSize = 15.sp)
            BigButton("Autoriser la position en permanence", DriveColors.Accent) { askBackground() }
        }
        if (settings.autoDrive && bgOk && !bubbleHelper.allowed) {
            BigButton("Autoriser l'affichage par-dessus les autres applis", DriveColors.Accent) { bubbleHelper.requestPermission() }
        }
        Text("Une fois Pilote fermé (retour, ou balayé des applis récentes), plus rien ne le relance tant que tu ne le rouvres pas.",
            color = DriveColors.TextSecondary, fontSize = 14.sp)
    }
    if (showAutostart) {
        Section("Position GPS")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("auto" to "Automatique", "unit" to "GPS du poste", "phone" to "GPS du téléphone").forEach { (v, label) ->
                BigButton(label, if (settings.gpsSource == v) DriveColors.Accent else DriveColors.Panel) { settings.changeGpsSource(v) }
            }
        }
        Section("Démarrage")
        ToggleRow("Ouvrir DRIVE à l'allumage du poste", settings.autostart, settings::changeAutostart)
    }
}

@Composable
private fun MapCategory(settings: DriveSettings, showAutostart: Boolean, onOfflineData: (() -> Unit)?, onPersonalize: (() -> Unit)?) {
    if (onOfflineData != null) BigButton("🗺 Cartes hors ligne, radars par pays et mes radars", DriveColors.Accent, onOfflineData)
    if (onPersonalize != null) BigButton("🖼 Fond d'écran et logo de démarrage", DriveColors.Accent, onPersonalize)
    Section("Carte")
    ToggleRow("Nord toujours en haut", settings.northUp, settings::changeNorthUp)
    ToggleRow("Zoom selon la vitesse", settings.adaptiveZoom, settings::changeAdaptiveZoom)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("Style ROADGUARD", if (settings.mapStyle == "roadguard") DriveColors.Accent else DriveColors.Panel) { settings.changeMapStyle("roadguard") }
        BigButton("Style classique", if (settings.mapStyle != "roadguard") DriveColors.Accent else DriveColors.Panel) { settings.changeMapStyle("classic") }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeMode.entries.forEach { mode ->
            BigButton(mode.label, if (settings.theme == mode) DriveColors.Accent else DriveColors.Panel) { settings.changeTheme(mode) }
        }
    }
    Section("Couleurs et taille")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DriveColors.accents.forEach { (name, argb) ->
            BigButton(if (settings.accent == argb) "● $name" else name, Color(argb)) { settings.changeAccent(argb) }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DriveColors.scales.forEach { (name, v) ->
            BigButton("Boutons : $name", if (settings.scale == v) DriveColors.Accent else DriveColors.Panel) { settings.changeScale(v) }
        }
    }
    if (showAutostart) {
        Section("Disposition de l'accueil")
        ToggleRow("Accueil en pages et modules (✎ en bas à droite de l'accueil pour modifier)", !settings.classicHome) { settings.changeClassicHome(!it) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Carte à gauche", if (!settings.mapRight) DriveColors.Accent else DriveColors.Panel) { settings.changeMapRight(false) }
            BigButton("Carte à droite", if (settings.mapRight) DriveColors.Accent else DriveColors.Panel) { settings.changeMapRight(true) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0.55f to "Carte moyenne", 0.62f to "Carte grande", 0.70f to "Carte très grande").forEach { (v, l) ->
                BigButton(l, if (settings.mapWeight == v) DriveColors.Accent else DriveColors.Panel) { settings.changeMapWeight(v) }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0 to "Horloge 12:34", 1 to "Avec secondes", 2 to "Avec la date").forEach { (v, l) ->
                BigButton(l, if (settings.clockStyle == v) DriveColors.Accent else DriveColors.Panel) { settings.changeClockStyle(v) }
            }
        }
        ToggleRow("Météo et adresse", settings.showWeather, settings::changeShowWeather)
        ToggleRow("Carte musique", settings.showMusic, settings::changeShowMusic)
        ToggleRow("Boutons de volume sur la carte", settings.showVolume, settings::changeShowVolume)
        ToggleRow("Gestes sur les bords : ⟵ retour depuis la gauche, applis depuis la droite, volume en glissant le long du bord droit", settings.edgeGestures, settings::changeEdgeGestures)
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = DriveColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel)
            .clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = DriveColors.Accent),
        )
    }
}
