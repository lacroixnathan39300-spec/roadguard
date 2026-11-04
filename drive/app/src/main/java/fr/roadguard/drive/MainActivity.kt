package fr.roadguard.drive

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
import fr.roadguard.drive.map.DriveMap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import fr.roadguard.drive.media.DriveEqualizer
import fr.roadguard.drive.nav.Guidance
import fr.roadguard.drive.nav.GuidanceState
import fr.roadguard.drive.nav.LatLon
import fr.roadguard.drive.nav.NavApi
import fr.roadguard.drive.nav.Place
import fr.roadguard.drive.nav.RecentPlaces
import fr.roadguard.drive.nav.SpeedLimitTracker
import fr.roadguard.drive.nav.SpeedLimits
import fr.roadguard.drive.ui.DayNight
import fr.roadguard.drive.nav.Route
import fr.roadguard.drive.radar.Fix
import fr.roadguard.drive.ui.GuidanceBanner
import fr.roadguard.drive.ui.MapButton
import fr.roadguard.drive.ui.SearchScreen
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import fr.roadguard.drive.media.MediaHub
import fr.roadguard.drive.ui.EqualizerPanel
import fr.roadguard.drive.ui.MusicCard
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.flowOf
import fr.roadguard.drive.radar.AlertEngine
import fr.roadguard.drive.radar.AlertVoice
import fr.roadguard.drive.radar.AverageSpeedTracker
import fr.roadguard.drive.radar.AverageStatus
import fr.roadguard.drive.radar.RadarAlert
import fr.roadguard.drive.radar.RadarRepository
import fr.roadguard.drive.ui.AlertBanners
import fr.roadguard.drive.ui.DriveColors
import fr.roadguard.drive.ui.Speedometer
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

    var fix by remember { mutableStateOf<SpeedFix?>(null) }
    var alert by remember { mutableStateOf<RadarAlert?>(null) }
    var average by remember { mutableStateOf<AverageStatus?>(null) }
    var showApps by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    val media = remember { MediaHub(context) }
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

    // Navigation
    val scope = rememberCoroutineScope()
    val recentStore = remember { RecentPlaces(context) }
    var recents by remember { mutableStateOf(recentStore.load()) }
    var showSearch by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf<Route?>(null) }
    var guidance by remember { mutableStateOf<Guidance?>(null) }
    var guidanceState by remember { mutableStateOf<GuidanceState?>(null) }
    var navStatus by remember { mutableStateOf<String?>(null) }
    var navigating by remember { mutableStateOf(false) }
    var recenter by remember { mutableIntStateOf(0) }
    var here by remember { mutableStateOf<Fix?>(null) }
    var rerouting by remember { mutableStateOf(false) }
    var lastRerouteAt by remember { mutableLongStateOf(0L) }
    var pendingDestination by remember { mutableStateOf<LatLon?>(null) }
    var home by remember { mutableStateOf(recentStore.saved("home")) }
    var work by remember { mutableStateOf(recentStore.saved("work")) }

    // Limite de vitesse de la route et thème de nuit.
    val limitTracker = remember { SpeedLimitTracker() }
    var speedLimit by remember { mutableStateOf<Int?>(null) }
    var lastOverWarning by remember { mutableLongStateOf(0L) }
    var night by remember { mutableStateOf(false) }

    fun stopNavigation() {
        navigating = false; route = null; guidance = null; guidanceState = null; navStatus = null
        pendingDestination = null
    }

    fun computeRoute(destination: LatLon, announce: Boolean) {
        val from = here
        if (from == null) { navStatus = "En attente du GPS…"; return }
        rerouting = true
        scope.launch {
            runCatching {
                NavApi.route(LatLon(from.lat, from.lon), from.bearing.takeUnless { it.isNaN() }, destination)
            }.onSuccess { r ->
                if (!navigating) return@onSuccess
                route = r; guidance = Guidance(r); guidanceState = null; navStatus = null
                if (!announce) voice.speak("Nouvel itinéraire")
            }.onFailure {
                if (navigating) navStatus = "Trajet impossible : ${it.message ?: "pas de réseau"}"
            }
            rerouting = false
        }
    }

    fun startNavigation(place: Place) {
        showSearch = false
        recents = recentStore.add(place)
        navigating = true; route = null; guidance = null; guidanceState = null
        pendingDestination = place.point
        navStatus = "Calcul du trajet vers ${place.name}…"
        computeRoute(place.point, announce = true)
    }

    // Une seule boucle GPS : compteur, alertes radars et vitesse moyenne.
    LaunchedEffect(hasLocation) {
        if (!hasLocation) return@LaunchedEffect
        val index = RadarRepository.index(context)
        val alerts = AlertEngine(index)
        val averages = AverageSpeedTracker(index)
        SpeedSource(context).locations().collect { location ->
            fix = location.toSpeedFix()
            val f = location.toAlertFix()
            val newAlert = alerts.update(f)
            if (newAlert?.isNew == true) voice.announce(newAlert)
            alert = newAlert
            val newAverage = averages.update(f)
            if (newAverage?.finished == true && average?.finished != true) voice.announceAverageResult(newAverage)
            average = newAverage

            here = f
            night = DayNight.isNight(f.lat, f.lon, System.currentTimeMillis())
            limitTracker.add(f.lat, f.lon, f.timeMs, f.speedKmh)?.let { trail ->
                scope.launch { SpeedLimits.current(trail)?.let { speedLimit = it } }
            }
            val kmh = fix?.kmh ?: 0
            if (SpeedLimitTracker.isOver(kmh, speedLimit) && f.timeMs - lastOverWarning > 30_000) {
                lastOverWarning = f.timeMs
                voice.speak("Attention, limité à $speedLimit")
            }
            if (navigating && route == null && !rerouting && navStatus?.startsWith("En attente du GPS") == true) {
                pendingDestination?.let { computeRoute(it, announce = true) }
            }
            guidance?.let { g ->
                val state = g.update(f.lat, f.lon, f.speedKmh)
                guidanceState = state
                state.speak?.let(voice::speak)
                // Sortie du trajet : nouveau calcul, au plus toutes les 15 s.
                if (state.offRoute && !rerouting && f.timeMs - lastRerouteAt > 15_000) {
                    lastRerouteAt = f.timeMs
                    computeRoute(g.route.destination, announce = false)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(DriveColors.Background)) {
        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(0.62f).fillMaxHeight()) {
                DriveMap(hasLocation = hasLocation, route = route, recenterRequest = recenter, night = night, modifier = Modifier.fillMaxSize())
                if (navigating) {
                    GuidanceBanner(guidanceState, navStatus, onStop = { stopNavigation() }, modifier = Modifier.align(Alignment.TopStart))
                    AlertBanners(alert, average, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                } else {
                    AlertBanners(alert, average, Modifier.align(Alignment.TopCenter))
                }
                Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MapButton("◎", onClick = { recenter++ })
                    if (!navigating) MapButton("🔍", onClick = { showSearch = true })
                }
            }
            Column(Modifier.weight(0.38f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Clock()
                Panel(Modifier.fillMaxWidth().weight(1f)) {
                    Speedometer(
                        limit = speedLimit,
                        over = SpeedLimitTracker.isOver(fix?.kmh ?: 0, speedLimit),
                        kmh = fix?.kmh,
                        unit = stringResource(R.string.speed_unit),
                        waiting = if (hasLocation) stringResource(R.string.gps_waiting) else stringResource(R.string.location_needed),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Panel(Modifier.fillMaxWidth().height(132.dp)) {
                    MusicCard(
                        nowPlaying = nowPlaying,
                        hasAccess = hasMediaAccess,
                        onPrevious = media::previous,
                        onPlayPause = media::playPause,
                        onNext = media::next,
                        onEqualizer = { showEqualizer = true },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Panel(Modifier.fillMaxWidth().height(64.dp).clickable { showApps = true }) {
                    Text(stringResource(R.string.apps), color = Color.White, fontSize = 20.sp)
                }
            }
        }
        if (showApps) AppDrawer(onClose = { showApps = false })
        if (showSearch) {
            BackHandler { showSearch = false }
            SearchScreen(
                near = here?.let { LatLon(it.lat, it.lon) },
                favorites = recents,
                home = home,
                work = work,
                onSave = { slot, place ->
                    recentStore.save(slot, place)
                    if (slot == "home") home = place else work = place
                },
                onPick = { startNavigation(it) },
                onClose = { showSearch = false },
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
