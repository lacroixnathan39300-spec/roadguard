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
     * Barre du bas d'Android (accueil, retour, applis récentes, ou la ligne des gestes) masquée pour gagner
     * de la place ; un glissement depuis le bas la fait réapparaître un instant. La barre du haut reste.
     */
    private fun hideNavigationBar() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
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
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME && DriveService.session !== session) restart++ }
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
            reports = s.community.reports, drivers = s.community.drivers, jams = s.community.jams,
            position = s.here, northUp = settings.northUp, adaptiveZoom = settings.adaptiveZoom, speedKmh = s.fix?.kmh ?: 0,
            alternatives = s.alternatives,
            offlinePack = offlinePack,
            hasLocation = hasLocation, route = s.route, recenterRequest = s.recenter,
            night = s.night, visibleKinds = settings.alertKinds, roadguardStyle = settings.mapStyle == "roadguard", bottomInset = if (panelOpen) 0.30f else 0.14f, modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (s.navigating) {
                GuidanceBanner(s.guidanceState, s.navStatus, onStop = { s.stopNavigation() }, compact = !panelOpen, onTap = openPanel,
                    choices = s.alternatives, current = s.route, onChoose = s::chooseRoute, onDismissChoices = s::dismissAlternatives)
            }
            AlertBanners(s.alert, s.average)
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
            // Interrupteurs rapides : un appui coupe ou rétablit les alertes radars / les annonces de limitation.
            fr.roadguard.core.ui.QuickToggle("📷", "Radars", settings.radarsOn, onToggle = {
                settings.changeRadarsOn(!settings.radarsOn)
                android.widget.Toast.makeText(context, if (settings.radarsOn) "Alertes radars activées" else "Alertes radars coupées", android.widget.Toast.LENGTH_SHORT).show()
            })
            fr.roadguard.core.ui.QuickToggle("🚦", "Limites", settings.limitVoiceOn, onToggle = {
                settings.changeLimitVoice(!settings.limitVoiceOn)
                android.widget.Toast.makeText(context, if (settings.limitVoiceOn) "Annonces de limitation activées" else "Annonces de limitation coupées", android.widget.Toast.LENGTH_SHORT).show()
            })
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
                onClose = { showSearch = false },
                starred = s.favorites, onStar = s::toggleFavorite,
            )
        }
        if (showMenu) {
            BackHandler { showMenu = false }
            Column(
                Modifier.fillMaxSize().background(DriveColors.Background).statusBarsPadding().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text("Menu", color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f))
                    fr.roadguard.core.ui.BigButton("Fermer", DriveColors.Panel) { showMenu = false }
                }
                fun open(tab: Int) { offlineTab = tab; showOffline = true; showMenu = false }
                MenuEntry("📷", "Assistant caméra", "Distance avec la voiture de devant, alerte collision, départ au feu") { showMenu = false; showCamera = true }
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
        if (showCamera) {
            BackHandler { showCamera = false }
            CameraAssistScreen(s, onClose = { showCamera = false })
        }
        if (showAdmin) {
            BackHandler { showAdmin = false }
            fr.roadguard.core.ui.AdminScreen(onClose = { showAdmin = false })
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
