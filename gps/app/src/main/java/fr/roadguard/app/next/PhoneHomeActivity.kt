package fr.roadguard.app.next

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.roadguard.app.SpotifySessionListener
import fr.roadguard.app.link.LinkActivity
import fr.roadguard.core.DriveSettings
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.SpeedSource
import fr.roadguard.core.map.DriveMap
import fr.roadguard.core.map.OfflineMaps
import fr.roadguard.core.media.DriveEqualizer
import fr.roadguard.core.media.MediaHub
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.net.Net
import fr.roadguard.core.radar.AlertVoice
import fr.roadguard.core.radar.RadarRepository
import fr.roadguard.core.ui.AlertBanners
import fr.roadguard.core.ui.DriveColors
import fr.roadguard.core.ui.EqualizerPanel
import fr.roadguard.core.ui.GuidanceBanner
import fr.roadguard.core.ui.MapButton
import fr.roadguard.core.ui.MusicCard
import fr.roadguard.core.ui.SearchScreen
import fr.roadguard.core.ui.SettingsScreen
import fr.roadguard.core.ui.Speedometer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay
import android.os.Build

/**
 * Nouvel accueil de ROADGUARD GPS sur le téléphone : même cœur que DRIVE (carte native, radars,
 * vitesse moyenne, limite, navigation vocale, musique), présenté pour un écran de téléphone.
 * Les anciennes fonctions restent dans « Plus » (packs de cartes, radars par pays, import…).
 */
class PhoneHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Net.init(this)
        fr.roadguard.core.account.CrashReporter.install(this)
        fr.roadguard.core.map.OfflineRegions.enlargeAmbientCache(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideNavigationBar()
        setContent { fr.roadguard.core.ui.CarFontScale { fr.roadguard.core.ui.AccountGate { PhoneHome() } } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    /**
     * Plein écran : barre du haut (heure, batterie, réseau) et barre du bas (accueil, retour, gestes) masquées ;
     * un glissement depuis le haut ou le bas les fait réapparaître un instant.
     */
    private fun hideNavigationBar() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onStart() { super.onStart(); visible = true }
    override fun onStop() { super.onStop(); visible = false }

    companion object {
        /** L'accueil est-il à l'écran ? Sinon, la bulle prend le relais par-dessus les autres applis. */
        @Volatile var visible = false
    }
}

@Composable
private fun PhoneHome() {
    val context = LocalContext.current
    var hasLocation by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocation = it }
    val notifyAsk = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (!hasLocation) ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    // Le cœur de conduite vit dans un service : il continue écran éteint ou dans une autre appli.
    var session by remember { mutableStateOf(DriveService.session) }
    var restart by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(owner) {
        // Service arrêté depuis la notification puis appli rouverte : on le relance.
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) AutoStart.sync(context)
            if (e == Lifecycle.Event.ON_RESUME && DriveService.session !== session) restart++
        }
        owner.addObserver(obs)
        onDispose { owner.removeObserver(obs) }
    }
    LaunchedEffect(hasLocation, restart) {
        if (!hasLocation) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 33) notifyAsk.launch(Manifest.permission.POST_NOTIFICATIONS)
        DriveService.start(context)
        while (DriveService.session == null) delay(100)
        session = DriveService.session
    }
    val s = session ?: run { Box(Modifier.fillMaxSize().background(DriveColors.Background)); return }
    val settings = s.settings
    // Démarrage automatique en roulant : écoute (ou non) les positions en arrière-plan selon le réglage.
    LaunchedEffect(settings.autoDrive, restart) { AutoStart.sync(context) }

    val media = remember { MediaHub(context, ComponentName(context, SpotifySessionListener::class.java)) }
    var hasMediaAccess by remember { mutableStateOf(media.hasAccess()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) hasMediaAccess = media.hasAccess() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val nowPlayingFlow = remember(hasMediaAccess) { if (hasMediaAccess) media.nowPlaying() else flowOf(null) }
    val nowPlaying by nowPlayingFlow.collectAsState(initial = null)
    val equalizer = remember { DriveEqualizer(context) }
    DisposableEffect(Unit) { onDispose { equalizer.release() } }

    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(false) }
    var offlineTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showCommunity by remember { mutableStateOf(false) }
    var showTrip by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { fr.roadguard.core.map.OfflineRegions.clearCacheOnce(context, 2) } }
    var showEqualizer by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showParking by remember { mutableStateOf(false) }
    var showTrips by remember { mutableStateOf(false) }
    var showNearby by remember { mutableStateOf(false) }
    var showHud by remember { mutableStateOf(false) }
    var showConvoy by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // Dashcam (caméra arrière en boucle) : une seule caméra à la fois avec l'anti-somnolence.
    rememberDashcam(s, settings.dashcam && !settings.drowsyGuard)
    // Commandes vocales : bouton 🎤 de la carte.
    val voiceActions = remember {
        object : VoiceActions {
            override fun openParking() { showParking = true }
            override fun openNearby() { showNearby = true }
            override fun openSearch(query: String?) { searchQuery = query.orEmpty(); showSearch = true }
            override fun openTrips() { showTrips = true }
            override fun openHud() { showHud = true }
        }
    }
    val listen = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
        s.voice.speak(VoiceCommands.handle(text, s, voiceActions))
    }
    fun startListening() = runCatching {
        listen.launch(android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Pilote, je t'écoute…"))
    }
    // Réponse dictée au dernier message lu.
    val incoming = MessageReader.last.value
    val replyTo = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
        val m = MessageReader.last.value ?: return@rememberLauncherForActivityResult
        s.voice.speak(if (MessageReader.reply(context, m, text)) "Réponse envoyée à ${m.from}" else "Impossible de répondre depuis Pilote à ce message")
        MessageReader.last.value = null
    }
    // Anti-somnolence : analyse invisible par la caméra avant pendant que la carte reste affichée.
    val drowsy = rememberDrowsyGuard(s, settings.drowsyGuard)
    var wakeDismissedAt by remember { mutableStateOf(0L) }
    // Mode épuré : en roulant, les panneaux se replient tout seuls après 8 s sans toucher l'écran
    // (reste : vitesse, limite, consigne de guidage) ; un appui sur la vitesse ou la consigne les rouvre.
    var panelOpen by remember { mutableStateOf(true) }
    var touchedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    val moving = (s.fix?.kmh ?: 0) >= 10
    LaunchedEffect(panelOpen, touchedAt, moving) {
        if (panelOpen && moving) { kotlinx.coroutines.delay(8_000); panelOpen = false }
    }
    val openPanel = { panelOpen = true; touchedAt = System.currentTimeMillis() }

    // Carte hors ligne : packs téléchargés dans « Plus → Cartes hors ligne », utilisés sans Internet.
    val packStates by fr.roadguard.core.map.MapPackManager.get(context).states.collectAsState()
    val packs = remember(packStates) { OfflineMaps.packs(java.io.File(context.filesDir, "pmtiles")) }
    val offlinePack = if (Net.hasDirectInternet()) null else OfflineMaps.packFor(packs, s.here?.lat, s.here?.lon)

    Box(Modifier.fillMaxSize().background(DriveColors.Background).pointerInput(Unit) {
        // Tout appui à l'écran repousse le repli automatique (sans gêner la carte ni les boutons).
        awaitPointerEventScope {
            while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial); touchedAt = System.currentTimeMillis() }
        }
    }) {
        DriveMap(
            reports = s.community.reports, drivers = s.community.drivers + s.community.convoy, jams = s.community.jams,
            position = s.here, northUp = settings.northUp, adaptiveZoom = settings.adaptiveZoom, speedKmh = s.fix?.kmh ?: 0,
            alternatives = s.alternatives,
            offlinePack = offlinePack,
            hasLocation = hasLocation, route = s.route, recenterRequest = s.recenter,
            night = s.night, visibleKinds = if (settings.radarsOn && !s.radarsBlockedHere) settings.alertKinds else emptySet(), roadguardStyle = settings.mapStyle == "roadguard", bottomInset = if (panelOpen) 0.30f else 0.14f, modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().displayCutoutPadding().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (s.navigating) {
                GuidanceBanner(s.guidanceState, s.navStatus, onStop = { s.stopNavigation() }, compact = !panelOpen, onTap = openPanel,
                    choices = s.alternatives, current = s.route, onChoose = s::chooseRoute, onDismissChoices = s::dismissAlternatives)
            }
            AlertBanners(s.alert, s.average)
            if (!s.navigating) BubbleSetupCard(settings)
            fr.roadguard.core.ui.TripSummaryCard(s, Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            if (incoming != null && System.currentTimeMillis() - incoming.at < 120_000) {
                // Dernier message lu à voix haute : réponse dictée en un appui.
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color(0xF01B5E20)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💬 ${incoming.app} · ${incoming.from} : ${incoming.text}", color = Color.White, fontSize = 15.sp, maxLines = 2, modifier = Modifier.weight(1f))
                    if (incoming.reply != null) fr.roadguard.core.ui.BigButton("🎤 Répondre", DriveColors.Accent) {
                        runCatching {
                            replyTo.launch(android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                                .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Ta réponse à ${incoming.from}"))
                        }
                    }
                    fr.roadguard.core.ui.BigButton("✕", DriveColors.Panel) { MessageReader.last.value = null }
                }
            }
            CalendarHint(s, settings.calendarHints)
            s.community.prompt?.let { fr.roadguard.core.ui.VotePromptCard(it, s.community::answer) }
            s.community.toast?.let { fr.roadguard.core.ui.CommunityToast(it, s.community::dismissToast) }
        }

        if (!panelOpen) {
            // Panneau replié : la vitesse (avec la limite) et un bouton pour tout rouvrir.
            Row(
                Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(10.dp)
                    .clip(RoundedCornerShape(26.dp)).background(fr.roadguard.core.ui.DriveStyle.panelBrush)
                    .border(1.dp, fr.roadguard.core.ui.DriveStyle.panelBorder, RoundedCornerShape(26.dp))
                    .clickable { openPanel() }.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Speedometer(
                    kmh = s.fix?.kmh, unit = "km/h",
                    waiting = if (hasLocation) "GPS…" else "Position ?",
                    limit = s.speedLimit, over = s.overSpeed,
                    modifier = Modifier.size(104.dp),
                )
                Text("▲", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 12.dp))
            }
        } else
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(10.dp).fillMaxWidth()
                .clip(RoundedCornerShape(26.dp)).background(fr.roadguard.core.ui.DriveStyle.panelBrush)
                .border(1.dp, fr.roadguard.core.ui.DriveStyle.panelBorder, RoundedCornerShape(26.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Speedometer(
                    kmh = s.fix?.kmh, unit = "km/h",
                    waiting = if (hasLocation) "GPS…" else "Position ?",
                    limit = s.speedLimit, over = s.overSpeed,
                    modifier = Modifier.size(140.dp),
                )
                MusicCard(
                    nowPlaying = nowPlaying, hasAccess = hasMediaAccess,
                    onPrevious = media::previous, onPlayPause = media::playPause, onNext = media::next,
                    onEqualizer = { showEqualizer = true },
                    modifier = Modifier.weight(1f).height(132.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("Où aller", Modifier.weight(1.4f), primary = true, icon = Icons.Filled.Search) { showSearch = true }
                Action("Link", Modifier.weight(1f), icon = Icons.Filled.Share) { context.startActivity(Intent(context, LinkActivity::class.java)) }
                Action("", Modifier.weight(0.6f), icon = Icons.Filled.Settings) { showSettings = true }
                Action("Menu", Modifier.weight(0.9f), icon = Icons.Filled.Menu) { showMenu = true }
            }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // Interrupteurs rapides (cachés avec le reste en mode épuré) : radars / annonces de limitation.
            if (panelOpen) {
                fr.roadguard.core.ui.QuickToggle("📷", "Radars", settings.radarsOn, onToggle = {
                    settings.changeRadarsOn(!settings.radarsOn)
                    android.widget.Toast.makeText(context, if (settings.radarsOn) "Alertes radars activées" else "Alertes radars coupées", android.widget.Toast.LENGTH_SHORT).show()
                })
                fr.roadguard.core.ui.QuickToggle("🚦", "Limites", settings.limitVoiceOn, onToggle = {
                    settings.changeLimitVoice(!settings.limitVoiceOn)
                    android.widget.Toast.makeText(context, if (settings.limitVoiceOn) "Annonces de limitation activées" else "Annonces de limitation coupées", android.widget.Toast.LENGTH_SHORT).show()
                })
            }
            if (drowsy != null) {
                // Petit voyant de l'anti-somnolence (vert : visage suivi ; orange : fatigue ou regard ailleurs).
                Text(if (drowsy == Alertness.AWAKE) "👁" else if (drowsy == Alertness.SEARCHING) "👁…" else "😴", fontSize = 22.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(drowsy.color).copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
            MapButton("🎤", onClick = { startListening() })
            MapButton("⚠", onClick = { showReport = true })
            MapButton("◎", onClick = { s.recenter++ })
        }
        if (showReport) {
            BackHandler { showReport = false }
            fr.roadguard.core.ui.ReportSheet(onPick = { t, text -> showReport = false; s.community.report(t, text) }, onClose = { showReport = false })
        }
        if (showTrip) {
            BackHandler { showTrip = false }
            fr.roadguard.core.ui.TripScreen(s.trip, onClose = { showTrip = false }, tracks = s.tracks)
        }
        if (showCommunity) {
            BackHandler { showCommunity = false }
            fr.roadguard.core.ui.CommunityScreen(s.community, s.here?.lat, s.here?.lon, onClose = { showCommunity = false })
        }

        if (showSearch) {
            BackHandler { showSearch = false }
            SearchScreen(
                near = s.here?.let { LatLon(it.lat, it.lon) },
                favorites = s.recents, home = s.home, work = s.work,
                onSave = s::saveSlot,
                onPick = { showSearch = false; s.startNavigation(it) },
                onClose = { showSearch = false; searchQuery = "" },
                starred = s.favorites, onStar = s::toggleFavorite,
                initialQuery = searchQuery,
            )
        }
        if (showMenu) {
            BackHandler { showMenu = false }
            Column(
                Modifier.fillMaxSize().background(DriveColors.Background).statusBarsPadding().navigationBarsPadding().padding(16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text("Menu", color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f))
                    fr.roadguard.core.ui.BigButton("Fermer", DriveColors.Panel) { showMenu = false }
                }
                fun open(tab: Int) { offlineTab = tab; showOffline = true; showMenu = false }
                MenuEntry("📷", "Assistant caméra", "Distance avec la voiture de devant, alerte collision, départ au feu") { showMenu = false; showCamera = true }
                MenuEntry("😴", if (settings.drowsyGuard) "Anti-somnolence : activé" else "Anti-somnolence : désactivé",
                    "Caméra avant, sans rien afficher : alarme si tes yeux se ferment, conseil de pause si tu fatigues") {
                    showMenu = false; settings.changeDrowsyGuard(!settings.drowsyGuard)
                    if (settings.drowsyGuard && settings.dashcam) settings.changeDashcam(false)
                }
                MenuEntry("🅿", "Ma voiture", "Où est garée ta voiture, retour à pied, minuteur de stationnement") { showMenu = false; showParking = true }
                MenuEntry("📍", "À proximité", "Carburants les moins chers, bornes, parkings, aires, toilettes") { showMenu = false; showNearby = true }
                MenuEntry("📒", "Mes trajets", "Historique, éco-conduite, journal km pro/perso, carburant, entretien") { showMenu = false; showTrips = true }
                MenuEntry("🚗", "Convoi et partage", "Rouler à plusieurs, partager ton trajet en direct") { showMenu = false; showConvoy = true }
                MenuEntry("🪞", "Affichage tête haute", "Vitesse et consigne en miroir, pour le pare-brise la nuit") { showMenu = false; showHud = true }
                MenuEntry("📊", "Ordinateur de bord", "Distances, temps, vitesses, chronos") { showMenu = false; showTrip = true }
                MenuEntry("👥", "Communauté", "Pseudo, points, classement, conducteurs autour") { showMenu = false; showCommunity = true }
                MenuEntry("📍", "Mes radars", "Ajouter un radar ici, importer un fichier CSV") { open(2) }
                MenuEntry("🗺", "Cartes hors ligne", "Télécharger les cartes pour rouler sans Internet") { open(0) }
                MenuEntry("🌍", "Radars par pays", "France et Suisse incluses ; autres pays à télécharger") { open(1) }
                MenuEntry("🚗", "ROADGUARD Link", "Relier le téléphone à l'autoradio (Wi-Fi ou câble)") {
                    showMenu = false; context.startActivity(Intent(context, LinkActivity::class.java))
                }
                MenuEntry("⚙", "Réglages", "Voix, alertes, itinéraire, couleurs") { showMenu = false; showSettings = true }
                MenuEntry("🧰", "Ancienne version", "Fonctions de l'ancienne appli, en attendant leur reprise") {
                    showMenu = false; context.startActivity(Intent(context, fr.roadguard.app.MainActivity::class.java))
                }
            }
        }
        if (drowsy == Alertness.ASLEEP && System.currentTimeMillis() - wakeDismissedAt > 3_000) {
            WakeUpOverlay { wakeDismissedAt = System.currentTimeMillis() }
        }
        if (showParking) { BackHandler { showParking = false }; fr.roadguard.core.ui.ParkingScreen(s) { showParking = false } }
        if (showTrips) { BackHandler { showTrips = false }; fr.roadguard.core.ui.TripHistoryScreen(s) { showTrips = false } }
        if (showNearby) { BackHandler { showNearby = false }; fr.roadguard.core.ui.NearbyScreen(s) { showNearby = false } }
        if (showConvoy) { BackHandler { showConvoy = false }; fr.roadguard.core.ui.ConvoyScreen(s) { showConvoy = false } }
        if (showHud) { BackHandler { showHud = false }; fr.roadguard.core.ui.HudScreen(s) { showHud = false } }
        fr.roadguard.core.ui.CrashOverlay(s)
        if (showCamera) {
            BackHandler { showCamera = false }
            CameraAssistScreen(s, onClose = { showCamera = false })
        }
        if (showAdmin) {
            BackHandler { showAdmin = false }
            fr.roadguard.core.ui.AdminScreen(onClose = { showAdmin = false }, community = s.community)
        } else if (showOffline) {
            BackHandler { showOffline = false }
            fr.roadguard.core.ui.OfflineDataScreen(setOf("FR", "CH"), onClose = { showOffline = false },
                here = s.here?.let { it.lat to it.lon }, startTab = offlineTab)
        } else if (showSettings) {
            BackHandler { showSettings = false }
            var radarCount by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { radarCount = RadarRepository.radars(context).size }
            SettingsScreen(settings, radarCount, onClose = { showSettings = false }, onOfflineData = { showOffline = true }, voice = s.voice, updateApp = "gps", onAdmin = { showAdmin = true })
        }
        if (showEqualizer) {
            BackHandler { showEqualizer = false }
            EqualizerPanel(equalizer, onClose = { showEqualizer = false })
        }
    }
}

@Composable
private fun Action(label: String, modifier: Modifier, primary: Boolean = false, icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit) {
    Row(
        modifier.height(54.dp).clip(RoundedCornerShape(16.dp))
            .background(if (primary) DriveColors.Accent else DriveColors.Track)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x1FFFFFFF), Color.Transparent)))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { androidx.compose.material3.Icon(it, null, Modifier.size(22.dp), tint = if (primary) Color(0xFF00131C) else Color.White) }
        if (label.isNotEmpty()) {
            if (icon != null) Spacer(Modifier.width(6.dp))
            Text(label, color = if (primary) Color(0xFF00131C) else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                fontFamily = fr.roadguard.core.ui.DriveStyle.Titles)
        }
    }
}

/** Ligne du menu : icône, titre, explication courte. */
@Composable
private fun MenuEntry(icon: String, title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Text(icon, fontSize = 28.sp)
        Column(Modifier.padding(start = 14.dp)) {
            androidx.compose.material3.Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = 19.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            androidx.compose.material3.Text(detail, color = DriveColors.TextSecondary, fontSize = 14.sp)
        }
    }
}

/**
 * Carte d'accueil pour la bulle par-dessus Waze : l'active en un appui (démarrage tout seul dès 20 km/h)
 * et enchaîne les autorisations nécessaires. Disparaît quand tout est prêt ou après « Plus tard ».
 */
@Composable
private fun BubbleSetupCard(settings: fr.roadguard.core.DriveSettings) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("bubble_card", android.content.Context.MODE_PRIVATE) }
    var hidden by remember { mutableStateOf(prefs.getBoolean("later", false)) }
    var tick by remember { mutableStateOf(0) }
    // Revérifie les autorisations au retour des écrans d'Android.
    val owner = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.addObserver(obs); onDispose { owner.removeObserver(obs) }
    }
    val overlayOk = remember(tick) { android.provider.Settings.canDrawOverlays(context) }
    val bgOk = remember(tick) { AutoStart.hasBackgroundLocation(context) }
    fun openOverlay() = runCatching {
        context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}")))
    }
    fun openAppSettings() = runCatching {
        context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
    }
    val askBg = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (!ok && !AutoStart.hasBackgroundLocation(context)) openAppSettings() else if (!android.provider.Settings.canDrawOverlays(context)) openOverlay()
    }
    val ready = settings.autoDrive && settings.bubble && overlayOk && bgOk
    if (ready || (hidden && !settings.autoDrive)) return
    val (title, detail, button) = when {
        !settings.autoDrive || !settings.bubble -> Triple("🫧 Bulle par-dessus Waze",
            "Pilote démarre tout seul dès 20 km/h : bulle vitesse et radars, bips et alarme, même sans ouvrir l'appli.", "Activer")
        !bgOk -> Triple("🫧 Bulle : étape 1 sur 2", "Position : choisis « Toujours autoriser ».", "Autoriser")
        else -> Triple("🫧 Bulle : étape 2 sur 2", "Active « Afficher par-dessus les autres applis » pour Pilote.", "Autoriser")
    }
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 6.dp).widthIn(max = 560.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(Color(0xF0123A6B)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = Color(0xFFCFE3FF), fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action(button, Modifier.weight(1f), primary = true) {
                if (!settings.autoDrive) settings.changeAutoDrive(true)
                if (!settings.bubble) settings.changeBubble(true)
                when {
                    !AutoStart.hasBackgroundLocation(context) -> if (Build.VERSION.SDK_INT >= 29) askBg.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    !android.provider.Settings.canDrawOverlays(context) -> openOverlay()
                }
                tick++
            }
            Action("Plus tard", Modifier.weight(1f)) {
                prefs.edit().putBoolean("later", true).apply(); hidden = true
                if (!ready) settings.changeAutoDrive(false)
            }
        }
    }
}
