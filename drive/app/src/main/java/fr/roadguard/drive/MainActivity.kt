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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.input.pointer.pointerInput
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
        fr.roadguard.core.account.CrashReporter.install(this)
        // MapLibre doit être prêt avant de lui confier le client HTTP (sinon plantage au lancement).
        org.maplibre.android.MapLibre.getInstance(this)
        fr.roadguard.core.map.OfflineRegions.enlargeAmbientCache(this)
        fr.roadguard.drive.net.LinkInterceptor.install()
        setContent {
            fr.roadguard.core.ui.CarFontScale {
                androidx.compose.foundation.layout.Box {
                    // DRIVE est l'accueil du poste : même sans accès, on peut ouvrir les autres applis.
                    // La liaison démarre avant la connexion au compte : le poste passe souvent par le téléphone pour Internet.
                    val link = remember { LinkClient.current ?: LinkClient(applicationContext) }
                    LaunchedEffect(Unit) { link.start() }
                    var apps by remember { mutableStateOf(false) }
                    var linkScreen by remember { mutableStateOf(false) }
                    fr.roadguard.core.ui.AccountGate(extra = {
                        fr.roadguard.core.ui.BigButton("📱 ROADGUARD Link (Internet par le téléphone)", DriveColors.Track) { linkScreen = true }
                        fr.roadguard.core.ui.BigButton("▦ Autres applications", DriveColors.Track) { apps = true }
                    }) { DriveHome() }
                    if (linkScreen) { BackHandler { linkScreen = false }; fr.roadguard.drive.ui.LinkScreen(link, onClose = { linkScreen = false }) }
                    if (apps) AppDrawer(onClose = { apps = false })
                    // Logo au démarrage de l'appli (pas à chaque retour sur l'accueil).
                    if (savedInstanceState == null) BootSplash()
                }
            }
        }
    }
}

/** Le lecteur n'est lancé qu'une fois par démarrage du processus (pas à chaque retour sur l'accueil). */
private var playerStartedThisBoot = false

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
    var showReport by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }
    var showTrip by remember { mutableStateOf(false) }
    /** Page affichée : 0 = carte, 1 = tableau de bord (la carte reste chargée dessous). */
    var page by remember { mutableStateOf(0) }
    var showPersonalize by remember { mutableStateOf(false) }
    var wallpaperVersion by remember { mutableStateOf(0) }
    // Mémoire de la carte remise à zéro une fois (des morceaux abîmés affichaient une image à la place de la carte).
    LaunchedEffect(Unit) { runCatching { fr.roadguard.core.map.OfflineRegions.clearCacheOnce(context, 2) } }
    // Bulle par-dessus les autres applis (Waze…) quand DRIVE n'est plus à l'écran.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val bubble = remember { fr.roadguard.core.ui.RadarBubble(context) }
    LaunchedEffect(Unit) {
        while (true) {
            val foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
            if (settings.bubble && !foreground && bubble.allowed) bubble.show(session) else bubble.hide()
            delay(1000)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { bubble.hide() } }
    var lastTouch by remember { mutableStateOf(System.currentTimeMillis()) }
    // Lecteur de musique choisi : ouvert une fois au démarrage, puis retour sur DRIVE.
    LaunchedEffect(Unit) {
        if (MusicPlayer.autoStart(context) && MusicPlayer.chosen(context) != null && !playerStartedThisBoot) {
            playerStartedThisBoot = true
            kotlinx.coroutines.delay(1500)
            MusicPlayer.open(context, comeBack = true)
        }
    }
    var showCommunity by remember { mutableStateOf(false) }
    var showOffline by remember { mutableStateOf(false) }

    // ROADGUARD Link : liaison avec le téléphone.
    val link = remember { LinkClient.current ?: LinkClient(context.applicationContext) }
    LaunchedEffect(Unit) { link.start() }
    val linkState by link.state.collectAsState()
    val phoneMedia by link.media.collectAsState()
    var showLink by remember { mutableStateOf(false) }
    val phoneNotifications by link.notifications.collectAsState()
    val mirror by link.mirror.collectAsState()
    var mirrorHidden by remember { mutableStateOf(false) }
    LaunchedEffect(mirror) { if (mirror != null) mirrorHidden = false }

    var showSearch by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var launcherMapVisible by remember { mutableStateOf(true) }
    var showWaze by remember { mutableStateOf(false) }
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
        // Choix de la meilleure position : le GPS du poste arrive sans délai, celui du téléphone passe par
        // la liaison. Automatique : le poste s'il capte bien (précision ≤ 30 m), sinon le téléphone.
        var unitAt = 0L; var unitAcc = 999f
        var phoneAt = 0L; var phoneAcc = 999f
        merge(
            SpeedSource(context).locations().onEach { unitAt = SystemClock.elapsedRealtime(); unitAcc = if (it.hasAccuracy()) it.accuracy else 50f }
                .filter { settings.gpsSource != "phone" || SystemClock.elapsedRealtime() - phoneAt > 3_000 }
                .filter { settings.gpsSource == "unit" || unitAcc <= 30f || SystemClock.elapsedRealtime() - phoneAt > 3_000 || phoneAcc >= unitAcc },
            link.locations.onEach { phoneAt = SystemClock.elapsedRealtime(); phoneAcc = if (it.hasAccuracy()) it.accuracy else 50f }
                .filter { settings.gpsSource != "unit" || SystemClock.elapsedRealtime() - unitAt > 3_000 }
                .filter { settings.gpsSource == "phone" || SystemClock.elapsedRealtime() - unitAt > 2_500 || (unitAcc > 30f && phoneAcc < unitAcc) },
        ).collect { session.onLocation(it) }
    }

    // Carte hors ligne si le poste n'a Internet ni par lui-même ni par le téléphone.
    val packStates by fr.roadguard.core.map.MapPackManager.get(context).states.collectAsState()
    val packs = remember(packStates) { fr.roadguard.core.map.OfflineMaps.packs(java.io.File(context.filesDir, "pmtiles")) }
    val offlinePack = if (fr.roadguard.core.net.Net.hasDirectInternet() || linkState is LinkState.Connected) null
        else fr.roadguard.core.map.OfflineMaps.packFor(packs, session.here?.lat, session.here?.lon)

    Box(Modifier.fillMaxSize().background(DriveColors.Background).pointerInput(Unit) {
        // Tout appui repousse l'économiseur d'écran (l'évènement continue vers les boutons).
        awaitPointerEventScope { while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial); lastTouch = System.currentTimeMillis() } }
    }) {
        WallpaperBackground(wallpaperVersion)
        // Contenu des modules, partagé entre l'accueil en pages (façon Car Launcher) et la disposition classique.
        val mapContent: @Composable BoxScope.() -> Unit = {
                DriveMap(reports = session.community.reports, drivers = session.community.drivers, jams = session.community.jams, position = session.here, northUp = settings.northUp, adaptiveZoom = settings.adaptiveZoom, speedKmh = session.fix?.kmh ?: 0, alternatives = session.alternatives, offlinePack = offlinePack, hasLocation = hasLocation, route = session.route, recenterRequest = session.recenter, night = session.night, visibleKinds = settings.alertKinds, roadguardStyle = settings.mapStyle == "roadguard",
                    // Avec Waze ouvert : carte Pilote à gauche, carte Waze à droite (les deux en même temps).
                    modifier = if (showWaze) Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterStart) else Modifier.fillMaxSize())
                // Carte Waze en direct par-dessus la carte ROADGUARD : bouton « W » pour l'ouvrir ou la fermer.
                if (showWaze) fr.roadguard.core.ui.WazeLiveMap(session.here?.lat, session.here?.lon,
                    Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd).padding(start = 4.dp), onClose = { showWaze = false })
                if (session.navigating) {
                    GuidanceBanner(session.guidanceState, session.navStatus, onStop = { session.stopNavigation() }, modifier = Modifier.align(Alignment.TopStart),
                        choices = session.alternatives, current = session.route, onChoose = session::chooseRoute, onDismissChoices = session::dismissAlternatives)
                    AlertBanners(session.alert, session.average, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                } else {
                    AlertBanners(session.alert, session.average, Modifier.align(Alignment.TopCenter))
                }
                Column(Modifier.align(Alignment.BottomStart)) {
                    session.community.prompt?.let { fr.roadguard.core.ui.VotePromptCard(it, session.community::answer) }
                    session.community.toast?.let { fr.roadguard.core.ui.CommunityToast(it, session.community::dismissToast) }
                }
                // Boutons de la carte : défilent si la carte est petite (module d'accueil réduit).
                Column(Modifier.align(Alignment.BottomEnd).padding(12.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MapButton("▦", onClick = { page = 1 })
                    MapButton("⚠", onClick = { showReport = true })
                    MapButton("⚙", onClick = { showSettings = true })
                    if (settings.wazeButton) MapButton(if (showWaze) "🗺" else "W", onClick = { showWaze = !showWaze })
                    MapButton("◎", onClick = { session.recenter++ })
                    if (!session.navigating) MapButton("🔍", onClick = { showSearch = true })
                    if (settings.showVolume) {
                        val audio = remember { context.getSystemService(android.media.AudioManager::class.java) }
                        MapButton("🔊", onClick = { audio?.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI) })
                        MapButton("🔉", onClick = { audio?.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI) })
                    }
                }
        }
        val clockContent: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Clock(settings.clockStyle)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (linkState is LinkState.Connected) "📱 Connecté" else "📱",
                        color = if (linkState is LinkState.Connected) Color(0xFF4CAF50) else DriveColors.TextSecondary,
                        fontSize = 20.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showLink = true }.padding(10.dp),
                    )
                }
                if (settings.showWeather) fr.roadguard.core.ui.WeatherAddressLine(session.localInfo, Modifier.padding(start = 4.dp))
        }
        val speedContent: @Composable () -> Unit = {
                    Speedometer(
                        limit = session.speedLimit,
                        over = session.overSpeed,
                        kmh = session.fix?.kmh,
                        unit = stringResource(R.string.speed_unit),
                        waiting = if (hasLocation) stringResource(R.string.gps_waiting) else stringResource(R.string.location_needed),
                        modifier = Modifier.padding(16.dp),
                    )
                        }
        val musicContent: @Composable () -> Unit = {
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
                        onOpenPlayer = { if (MusicPlayer.chosen(context) != null) MusicPlayer.open(context, comeBack = false) else showPlayer = true },
                        playerLabel = MusicPlayer.chosen(context)?.second,
                        modifier = Modifier.fillMaxSize(),
                    )
                        }
        if (settings.classicHome) Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Disposition réglable : carte à gauche ou à droite, largeur de la carte.
            val mapPart: @Composable () -> Unit = { Panel(Modifier.weight(settings.mapWeight).fillMaxHeight(), mapContent) }
            val sidePart: @Composable () -> Unit = {
            Column(Modifier.weight(1f - settings.mapWeight).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column { clockContent() }
                Panel(Modifier.fillMaxWidth().weight(1f).heightIn(min = 150.dp).clickable { showTrip = true }) { speedContent() }
                if (settings.showMusic) Panel(Modifier.fillMaxWidth().height(132.dp)) { musicContent() }
                FavoritesDock(refresh = showApps, onAll = { showApps = true })
            }
            }
            if (settings.mapRight) { sidePart(); mapPart() } else { mapPart(); sidePart() }
        } else {
            val launcher = remember { LauncherLayout(context) }
            LauncherPages(launcher, Modifier.fillMaxSize().padding(12.dp), onPage = { p ->
                launcherMapVisible = launcher.pages.getOrNull(p)?.any { it.type == ModuleType.MAP } == true
            }) { tile, m ->
                when (tile.type) {
                    ModuleType.MAP -> Panel(m, mapContent)
                    ModuleType.CLOCK -> Panel(m) { Column(Modifier.align(Alignment.CenterStart).padding(8.dp)) { clockContent() } }
                    ModuleType.SPEED -> Panel(m.clickable { showTrip = true }) { speedContent() }
                    ModuleType.MUSIC -> Panel(m) { musicContent() }
                    ModuleType.DOCK -> Box(m, contentAlignment = Alignment.Center) { FavoritesDock(refresh = showApps, onAll = { showApps = true }) }
                    ModuleType.TRIP -> Panel(m.clickable { showTrip = true }) { TripModule(session.trip, Modifier.align(Alignment.Center)) }
                    ModuleType.SHORTCUTS -> Panel(m) {
                        Row(Modifier.align(Alignment.Center).horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MapButton("⚠", onClick = { showReport = true })
                            MapButton("🔍", onClick = { showSearch = true })
                            MapButton("▦", onClick = { page = 1 })
                            MapButton("⚙", onClick = { showSettings = true })
                            MapButton("📱", onClick = { showLink = true })
                        }
                    }
                    ModuleType.WIDGET -> AndroidWidget(tile.widgetId, m)
                    ModuleType.WAZE -> fr.roadguard.core.ui.WazeLiveMap(session.here?.lat, session.here?.lon, m)
                    ModuleType.APP_EMBED -> EmbeddedAppModule(tile.pkg.ifBlank { "com.waze" }, m)
                    ModuleType.WAZE_APP -> Panel(m.clickable {
                        if (!fr.roadguard.core.ui.Waze.openApp(context, adjacent = true)) showApps = true
                    }) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧭", fontSize = 34.sp)
                            Text(if (fr.roadguard.core.ui.Waze.installed(context)) "Waze à côté" else "Waze non installé", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
            // Alertes radars toujours visibles, même sur une page sans la carte.
            if (!launcherMapVisible) AlertBanners(session.alert, session.average, Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
        }
        if (page == 1) {
            BackHandler { page = 0 }
            val fromPhone = phoneMedia != null
            DashboardPage(
                session, music = phoneMedia ?: nowPlaying,
                onPlayPause = { if (fromPhone) link.mediaCommand(if (phoneMedia?.playing == true) "pause" else "play") else media.playPause() },
                onNext = { if (fromPhone) link.mediaCommand("next") else media.next() },
                onPrevious = { if (fromPhone) link.mediaCommand("previous") else media.previous() },
                onMap = { page = 0 }, onTrip = { showTrip = true },
            )
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
        if (showReport) {
            BackHandler { showReport = false }
            fr.roadguard.core.ui.ReportSheet(onPick = { t, text -> showReport = false; session.community.report(t, text) }, onClose = { showReport = false })
        }
        if (showAdmin) {
            BackHandler { showAdmin = false }
            fr.roadguard.core.ui.AdminScreen(onClose = { showAdmin = false })
        } else if (showOffline) {
            BackHandler { showOffline = false }
            fr.roadguard.core.ui.OfflineDataScreen(setOf("FR", "CH"), onClose = { showOffline = false }, here = session.here?.let { it.lat to it.lon })
        } else if (showPersonalize) {
            BackHandler { showPersonalize = false }
            PersonalizeScreen(onChanged = { wallpaperVersion++ }, onClose = { showPersonalize = false })
        } else if (showPlayer) {
            BackHandler { showPlayer = false }
            MusicPlayerScreen(onClose = { showPlayer = false })
        } else if (showCommunity) {
            BackHandler { showCommunity = false }
            fr.roadguard.core.ui.CommunityScreen(session.community, session.here?.lat, session.here?.lon, onClose = { showCommunity = false })
        } else if (showSettings) {
            BackHandler { showSettings = false }
            var radarCount by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { radarCount = RadarRepository.radars(context).size }
            SettingsScreen(settings, radarCount, onClose = { showSettings = false }, onOfflineData = { showOffline = true }, showAutostart = true,
                onCommunity = { showCommunity = true }, onMusicPlayer = { showPlayer = true },
                onPersonalize = { showPersonalize = true }, voice = session.voice, updateApp = fr.roadguard.core.ui.UpdateNotifier.appKey(context), onAdmin = { showAdmin = true })
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
        if (showTrip) {
            BackHandler { showTrip = false }
            fr.roadguard.core.ui.TripScreen(session.trip, onClose = { showTrip = false }, tracks = session.tracks)
        }
        fr.roadguard.core.ui.ScreenSaver(
            stopped = (session.fix?.kmh ?: 0) < 3 && !session.navigating, lastTouch = lastTouch, night = session.night,
            onDismiss = { lastTouch = System.currentTimeMillis() },
        )
        if (showEqualizer) {
            BackHandler { showEqualizer = false }
            EqualizerPanel(equalizer, onClose = { showEqualizer = false })
        }
        if (settings.edgeGestures) {
            val dispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            val audio = remember { context.getSystemService(android.media.AudioManager::class.java) }
            EdgeGestures(
                // Retour seulement si un écran est ouvert (DRIVE est l'accueil : il ne doit pas se fermer).
                onBack = { if (dispatcher?.hasEnabledCallbacks() == true) dispatcher.onBackPressed() },
                onApps = { showApps = true },
                onVolume = { up ->
                    audio?.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                        if (up) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                },
            )
        }
    }
}

@Composable
private fun Panel(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(22.dp)).background(fr.roadguard.core.ui.DriveStyle.panelBrush)
            .border(1.dp, fr.roadguard.core.ui.DriveStyle.panelBorder, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Clock(style: Int = 0) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1_000) } }
    val format = remember(style) { SimpleDateFormat(if (style == 1) "HH:mm:ss" else "HH:mm", Locale.FRANCE) }
    val dateFormat = remember { SimpleDateFormat("EEE d MMM", Locale.FRANCE) }
    Column(Modifier.padding(start = 8.dp)) {
        Text(format.format(now), color = Color.White, fontSize = if (style == 1) 40.sp else 46.sp, fontFamily = fr.roadguard.core.ui.DriveStyle.Numbers)
        if (style == 2) Text(dateFormat.format(now).replaceFirstChar { it.titlecase(Locale.FRANCE) }, color = DriveColors.TextSecondary, fontSize = 16.sp)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppDrawer(onClose: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember { AppCatalog(context) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var favorites by remember { mutableStateOf(catalog.favorites()) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { catalog.load() } }
    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(DriveColors.Background.copy(alpha = 0.97f)).clickable(onClick = onClose)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text("Appui long sur une appli : l'ajouter ou la retirer des favoris de l'accueil (★).",
                    color = DriveColors.TextSecondary, fontSize = 15.sp)
            }
            items(apps, key = { it.packageName + "/" + it.activity }) { app ->
                val fav = app.packageName + "/" + app.activity in favorites
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .combinedClickable(onClick = { catalog.launch(app); onClose() }, onLongClick = { favorites = catalog.toggleFavorite(app) })
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (fav) Text("★", color = Color(0xFFFFC107), fontSize = 16.sp)
                    app.icon?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp)) }
                        ?: Spacer(Modifier.size(64.dp))
                    Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Raccourcis favoris sur l'accueil (choisis par appui long dans la liste des applis) + « Toutes ». */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FavoritesDock(refresh: Boolean, onAll: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember { AppCatalog(context) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    LaunchedEffect(refresh) {
        if (refresh) return@LaunchedEffect
        val favs = catalog.favorites()
        apps = withContext(Dispatchers.IO) { catalog.load() }.filter { it.packageName + "/" + it.activity in favs }
            .sortedBy { favs.indexOf(it.packageName + "/" + it.activity) }
    }
    Row(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp)).background(DriveColors.Panel).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        apps.take(4).forEach { app ->
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).clickable { catalog.launch(app) }, contentAlignment = Alignment.Center) {
                app.icon?.let { Image(it.asImageBitmap(), contentDescription = app.label, modifier = Modifier.size(44.dp)) }
            }
        }
        Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onAll), contentAlignment = Alignment.Center) {
            Text(if (apps.isEmpty()) stringResource(R.string.apps) else "⋯", color = Color.White, fontSize = 20.sp)
        }
    }
}
