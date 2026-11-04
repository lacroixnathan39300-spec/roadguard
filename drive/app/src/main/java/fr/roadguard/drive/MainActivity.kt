package fr.roadguard.drive

import fr.roadguard.core.DriveSettings
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.SpeedFix
import fr.roadguard.core.SpeedSource
import fr.roadguard.core.toAlertFix
import fr.roadguard.core.toSpeedFix

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fr.roadguard.core.map.DriveMap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import fr.roadguard.core.media.DriveEqualizer
import fr.roadguard.core.nav.Guidance
import fr.roadguard.core.nav.GuidanceState
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.nav.NavApi
import fr.roadguard.core.nav.Place
import fr.roadguard.core.nav.RecentPlaces
import fr.roadguard.core.nav.SpeedLimitTracker
import fr.roadguard.core.nav.SpeedLimits
import fr.roadguard.core.ui.DayNight
import fr.roadguard.core.ui.SettingsScreen
import fr.roadguard.drive.ui.LinkScreen
import fr.roadguard.drive.ui.PhoneNotificationCard
import fr.roadguard.drive.ui.MirrorScreen
import fr.roadguard.core.nav.SharedDestination
import fr.roadguard.drive.link.LinkClient
import fr.roadguard.drive.link.LinkState
import android.os.SystemClock
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import fr.roadguard.core.nav.Route
import fr.roadguard.core.radar.Fix
import fr.roadguard.core.ui.GuidanceBanner
import fr.roadguard.core.ui.MapButton
import fr.roadguard.core.ui.SearchScreen
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import fr.roadguard.core.media.MediaHub
import fr.roadguard.core.ui.EqualizerPanel
import fr.roadguard.core.ui.MusicCard
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.flowOf
import fr.roadguard.core.radar.AlertEngine
import fr.roadguard.core.radar.AlertVoice
import fr.roadguard.core.radar.AverageSpeedTracker
import fr.roadguard.core.radar.AverageStatus
import fr.roadguard.core.radar.RadarAlert
import fr.roadguard.core.radar.RadarRepository
import fr.roadguard.core.ui.AlertBanners
import fr.roadguard.core.ui.DriveColors
import fr.roadguard.core.ui.Speedometer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Plein écran façon tableau de bord : barres d'Android masquées, un glissement les fait revenir. */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        fr.roadguard.core.net.Net.init(this)
        // MapLibre doit être prêt avant de lui confier le client HTTP (sinon plantage au lancement).
        org.maplibre.android.MapLibre.getInstance(this)
        fr.roadguard.drive.net.LinkInterceptor.install()
        setContent { DriveHome() }
    }
}

@Composable
private fun DriveHome() {
    val context = LocalContext.current
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocation = it }
    LaunchedEffect(Unit) { if (!hasLocation) askLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    var showApps by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val media = remember { MediaHub(context, android.content.ComponentName(context, fr.roadguard.drive.media.MediaAccessService::class.java)) }
    var hasMediaAccess by remember { mutableStateOf(media.hasAccess()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        // L'accès se donne dans les réglages Android : on revérifie au retour sur l'accueil.
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) hasMediaAccess = media.hasAccess() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val nowPlayingFlow = remember(hasMediaAccess) { if (hasMediaAccess) media.nowPlaying() else flowOf(null) }
    val nowPlaying by nowPlayingFlow.collectAsState(initial = null)
    val equalizer = remember { DriveEqualizer(context) }
    DisposableEffect(Unit) { onDispose { equalizer.release() } }
    val voice = remember { AlertVoice(context) }
    DisposableEffect(Unit) { onDispose { voice.release() } }
    val settings = remember { DriveSettings(context) }
    voice.voiceEnabled = settings.voice
    voice.beepEnabled = settings.beep
    val scope = rememberCoroutineScope()
    // Cœur de conduite commun avec l'appli GPS du téléphone.
    val session = remember { DrivingSession(context, scope, voice, settings) }
    var showSettings by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(false) }

    // ROADGUARD Link : liaison avec le téléphone.
    val link = remember { LinkClient(context.applicationContext) }
    LaunchedEffect(Unit) { link.start() }
    val linkState by link.state.collectAsState()
    val phoneMedia by link.media.collectAsState()
    var showLink by remember { mutableStateOf(false) }
    val phoneNotifications by link.notifications.collectAsState()
    val mirror by link.mirror.collectAsState()
    var mirrorHidden by remember { mutableStateOf(false) }
    LaunchedEffect(mirror) { if (mirror != null) mirrorHidden = false }

    var showSearch by remember { mutableStateOf(false) }
    fun startNavigation(place: Place) { showSearch = false; session.startNavigation(place) }

    // Messages et appels du téléphone : annonce vocale à l'arrivée.
    var announcedKeys by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(phoneNotifications) {
        phoneNotifications.firstOrNull()?.let { n ->
            if (n.key in announcedKeys) return@let
            announcedKeys = announcedKeys + n.key
            if (n.isCall) voice.speak("Appel de ${n.title}")
            else if (settings.readMessages) voice.speak("Message de ${n.title} : ${n.text.take(200)}")
        }
    }
    // Destination envoyée depuis le téléphone (Partager → ROADGUARD Link).
    LaunchedEffect(Unit) {
        link.destinations.collect { text ->
            val place = runCatching { SharedDestination.resolve(text, session.here?.let { LatLon(it.lat, it.lon) }) }.getOrNull()
            if (place != null) { voice.speak("Destination reçue du téléphone"); startNavigation(place) }
            else voice.speak("Destination du téléphone introuvable")
        }
    }

    // Une seule boucle GPS : compteur, alertes radars et vitesse moyenne.
    LaunchedEffect(hasLocation) {
        if (!hasLocation) return@LaunchedEffect
        session.prepare()
        // GPS du téléphone (ROADGUARD Link) en priorité ; GPS du poste s'il se tait plus de 3 s.
        var lastPhoneFixAt = 0L
        merge(
            SpeedSource(context).locations().filter { SystemClock.elapsedRealtime() - lastPhoneFixAt > 3_000 },
            link.locations.onEach { lastPhoneFixAt = SystemClock.elapsedRealtime() },
        ).collect { session.onLocation(it) }
    }

    // Carte hors ligne si le poste n'a Internet ni par lui-même ni par le téléphone.
    val packStates by fr.roadguard.core.map.MapPackManager.get(context).states.collectAsState()
    val packs = remember(packStates) { fr.roadguard.core.map.OfflineMaps.packs(java.io.File(context.filesDir, "pmtiles")) }
    val offlinePack = if (fr.roadguard.core.net.Net.hasDirectInternet() || linkState is LinkState.Connected) null
        else fr.roadguard.core.map.OfflineMaps.packFor(packs, session.here?.lat, session.here?.lon)

    Box(Modifier.fillMaxSize().background(DriveColors.Background)) {
        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(0.62f).fillMaxHeight()) {
                DriveMap(alternatives = session.alternatives, offlinePack = offlinePack, hasLocation = hasLocation, route = session.route, recenterRequest = session.recenter, night = session.night, visibleKinds = settings.alertKinds, modifier = Modifier.fillMaxSize())
                if (session.navigating) {
                    GuidanceBanner(session.guidanceState, session.navStatus, onStop = { session.stopNavigation() }, modifier = Modifier.align(Alignment.TopStart),
                        choices = session.alternatives, current = session.route, onChoose = session::chooseRoute, onDismissChoices = session::dismissAlternatives)
                    AlertBanners(session.alert, session.average, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                } else {
                    AlertBanners(session.alert, session.average, Modifier.align(Alignment.TopCenter))
                }
                Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MapButton("⚙", onClick = { showSettings = true })
                    MapButton("◎", onClick = { session.recenter++ })
                    if (!session.navigating) MapButton("🔍", onClick = { showSearch = true })
                }
            }
            Column(Modifier.weight(0.38f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Clock()
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (linkState is LinkState.Connected) "📱 Connecté" else "📱",
                        color = if (linkState is LinkState.Connected) Color(0xFF4CAF50) else DriveColors.TextSecondary,
                        fontSize = 20.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showLink = true }.padding(10.dp),
                    )
                }
                Panel(Modifier.fillMaxWidth().weight(1f)) {
                    Speedometer(
                        limit = session.speedLimit,
                        over = session.overSpeed,
                        kmh = session.fix?.kmh,
                        unit = stringResource(R.string.speed_unit),
                        waiting = if (hasLocation) stringResource(R.string.gps_waiting) else stringResource(R.string.location_needed),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Panel(Modifier.fillMaxWidth().height(132.dp)) {
                    // Musique du téléphone si la liaison en fournit, sinon celle du poste.
                    val fromPhone = phoneMedia != null
                    MusicCard(
                        nowPlaying = phoneMedia ?: nowPlaying,
                        hasAccess = hasMediaAccess || fromPhone,
                        onPrevious = { if (fromPhone) link.mediaCommand("previous") else media.previous() },
                        onPlayPause = {
                            if (fromPhone) link.mediaCommand(if (phoneMedia?.playing == true) "pause" else "play") else media.playPause()
                        },
                        onNext = { if (fromPhone) link.mediaCommand("next") else media.next() },
                        onEqualizer = { showEqualizer = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Panel(Modifier.fillMaxWidth().height(64.dp).clickable { showApps = true }) {
                    Text(stringResource(R.string.apps), color = Color.White, fontSize = 20.sp)
                }
            }
        }
        phoneNotifications.firstOrNull()?.let { n ->
            // Les messages s'effacent seuls après 12 s ; les appels restent jusqu'à leur fin.
            if (!n.isCall) LaunchedEffect(n.key) { delay(12_000); link.dismissNotification(n.key) }
            PhoneNotificationCard(
                n,
                onAction = { link.notificationAction(n.key, it) },
                onDismiss = { link.dismissNotification(n.key) },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            )
        }
        // Mode miroir : l'écran du téléphone s'affiche dès que le téléphone le partage.
        val m = mirror
        if (m != null && !mirrorHidden) {
            BackHandler { mirrorHidden = true }
            MirrorScreen(link, m.first, m.second, onClose = { mirrorHidden = true })
        } else if (m != null) {
            MapButton("📲", onClick = { mirrorHidden = false }, modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 48.dp))
        }
        if (showApps) AppDrawer(onClose = { showApps = false })
        if (showLink) {
            BackHandler { showLink = false }
            LinkScreen(link, onClose = { showLink = false })
        }
        if (showOffline) {
            BackHandler { showOffline = false }
            fr.roadguard.core.ui.OfflineDataScreen(setOf("FR", "CH"), onClose = { showOffline = false }, here = session.here?.let { it.lat to it.lon })
        } else if (showSettings) {
            BackHandler { showSettings = false }
            var radarCount by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { radarCount = RadarRepository.radars(context).size }
            SettingsScreen(settings, radarCount, onClose = { showSettings = false }, onOfflineData = { showOffline = true }, showAutostart = true)
        }
        if (showSearch) {
            BackHandler { showSearch = false }
            SearchScreen(
                near = session.here?.let { LatLon(it.lat, it.lon) },
                favorites = session.recents,
                home = session.home,
                work = session.work,
                onSave = session::saveSlot,
                onPick = { startNavigation(it) },
                onClose = { showSearch = false },
                starred = session.favorites, onStar = session::toggleFavorite,
            )
        }
        if (showEqualizer) {
            BackHandler { showEqualizer = false }
            EqualizerPanel(equalizer, onClose = { showEqualizer = false })
        }
    }
}

@Composable
private fun Panel(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(DriveColors.Panel),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1_000) } }
    val format = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    Text(format.format(now), color = Color.White, fontSize = 40.sp, modifier = Modifier.padding(start = 8.dp))
}

@Composable
private fun AppDrawer(onClose: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember { AppCatalog(context) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { catalog.load() } }
    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(DriveColors.Background.copy(alpha = 0.97f)).clickable(onClick = onClose)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(apps, key = { it.packageName + "/" + it.activity }) { app ->
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).clickable { catalog.launch(app); onClose() }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    app.icon?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp)) }
                        ?: Spacer(Modifier.size(64.dp))
                    Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
