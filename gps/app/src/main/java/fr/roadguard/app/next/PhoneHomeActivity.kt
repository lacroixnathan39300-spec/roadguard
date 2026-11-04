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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { PhoneHome() }
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
    var showEqualizer by remember { mutableStateOf(false) }

    // Carte hors ligne : packs téléchargés dans « Plus → Cartes hors ligne », utilisés sans Internet.
    val packStates by fr.roadguard.core.map.MapPackManager.get(context).states.collectAsState()
    val packs = remember(packStates) { OfflineMaps.packs(java.io.File(context.filesDir, "pmtiles")) }
    val offlinePack = if (Net.hasDirectInternet()) null else OfflineMaps.packFor(packs, s.here?.lat, s.here?.lon)

    Box(Modifier.fillMaxSize().background(DriveColors.Background)) {
        DriveMap(
            offlinePack = offlinePack,
            hasLocation = hasLocation, route = s.route, recenterRequest = s.recenter,
            night = s.night, visibleKinds = settings.alertKinds, modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (s.navigating) {
                GuidanceBanner(s.guidanceState, s.navStatus, onStop = { s.stopNavigation() })
            }
            AlertBanners(s.alert, s.average)
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(10.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)).background(DriveColors.Panel.copy(alpha = 0.96f)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Speedometer(
                    kmh = s.fix?.kmh, unit = "km/h",
                    waiting = if (hasLocation) "GPS…" else "Position ?",
                    limit = s.speedLimit, over = s.overSpeed,
                    modifier = Modifier.size(132.dp),
                )
                MusicCard(
                    nowPlaying = nowPlaying, hasAccess = hasMediaAccess,
                    onPrevious = media::previous, onPlayPause = media::playPause, onNext = media::next,
                    onEqualizer = { showEqualizer = true },
                    modifier = Modifier.weight(1f).height(132.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action("🔍 Où aller", Modifier.weight(1.4f), primary = true) { showSearch = true }
                Action("🚗 Link", Modifier.weight(1f)) { context.startActivity(Intent(context, LinkActivity::class.java)) }
                Action("⚙", Modifier.weight(0.6f)) { showSettings = true }
                Action("☰ Plus", Modifier.weight(0.9f)) {
                    context.startActivity(Intent(context, fr.roadguard.app.MainActivity::class.java))
                }
            }
        }
        MapButton("◎", onClick = { s.recenter++ },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))

        if (showSearch) {
            BackHandler { showSearch = false }
            SearchScreen(
                near = s.here?.let { LatLon(it.lat, it.lon) },
                favorites = s.recents, home = s.home, work = s.work,
                onSave = s::saveSlot,
                onPick = { showSearch = false; s.startNavigation(it) },
                onClose = { showSearch = false },
            )
        }
        if (showOffline) {
            BackHandler { showOffline = false }
            fr.roadguard.core.ui.OfflineDataScreen(setOf("FR", "CH"), onClose = { showOffline = false })
        } else if (showSettings) {
            BackHandler { showSettings = false }
            var radarCount by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) { radarCount = RadarRepository.radars(context).size }
            SettingsScreen(settings, radarCount, onClose = { showSettings = false }, onOfflineData = { showOffline = true })
        }
        if (showEqualizer) {
            BackHandler { showEqualizer = false }
            EqualizerPanel(equalizer, onClose = { showEqualizer = false })
        }
    }
}

@Composable
private fun Action(label: String, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (primary) DriveColors.Accent else DriveColors.Track).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}
