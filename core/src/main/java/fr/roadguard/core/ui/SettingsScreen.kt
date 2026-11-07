package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** Réglages : annonces, types d'alertes, limite de vitesse, thème. Gros boutons, lisibles en voiture. */
@Composable
fun SettingsScreen(settings: DriveSettings, radarCount: Int, onClose: () -> Unit, onOfflineData: (() -> Unit)? = null, showAutostart: Boolean = false,
    onCommunity: (() -> Unit)? = null, onMusicPlayer: (() -> Unit)? = null, onPersonalize: (() -> Unit)? = null,
    voice: fr.roadguard.core.radar.AlertVoice? = null, updateApp: String? = null, onAdmin: (() -> Unit)? = null,
    session: fr.roadguard.core.DrivingSession? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Outils : affichés en plein écran à la place des réglages, dans la même fenêtre. Une fenêtre séparée (Dialog)
    // était décalée sur certains autoradios (réglages visibles à gauche, « Fermer » et « Y aller » coupés à droite).
    val tool = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
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
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Réglages", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountCard(onAdmin = { onAdmin?.invoke() })
            if (session != null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("🅿 Ma voiture", DriveColors.Panel) { tool.value = 1 }
                BigButton("📍 À proximité", DriveColors.Panel) { tool.value = 2 }
                BigButton("📒 Mes trajets", DriveColors.Panel) { tool.value = 3 }
                BigButton("🚗 Convoi et partage", DriveColors.Panel) { tool.value = 4 }
                BigButton("🪞 Tête haute", DriveColors.Panel) { tool.value = 5 }
            }
            BigButton("📖 Revoir le tutoriel", DriveColors.Panel) { Tutorial.forceShow = true; onClose() }
            BigButton("⚖ Infos légales : la loi, les radars, les autorisations", DriveColors.Panel) { Legal.forceShow = true; onClose() }
            if (onPersonalize != null) BigButton("🖼 Fond d'écran et logo de démarrage", DriveColors.Accent, onPersonalize)
            if (onMusicPlayer != null) BigButton("🎵 Lecteur de musique (Bluetooth du poste…)", DriveColors.Accent, onMusicPlayer)
            if (onCommunity != null) BigButton("👥 Communauté : pseudo, points, classement", DriveColors.Accent, onCommunity)
            if (onOfflineData != null) {
                BigButton("🗺 Cartes hors ligne, radars par pays et mes radars", DriveColors.Accent, onOfflineData)
            }
            Section("Annonces")
            ToggleRow("Voix (radars, guidage, vitesse moyenne)", settings.voice, settings::changeVoice)
            ToggleRow("Bip d'alerte", settings.beep, settings::changeBeep)
            ToggleRow("Baisser la musique pendant les annonces et les bips", settings.duckMusic, settings::changeDuckMusic)
            ToggleRow("Bips de plus en plus rapides en approchant (détecteur)", settings.proximityBeeps, settings::changeProximityBeeps)
            ToggleRow("Lire à voix haute les messages du téléphone", settings.readMessages, settings::changeReadMessages)
            // Activation : seulement après la case de l'avertissement légal (sinon, la confirmation s'affiche).
            ToggleRow("Alertes radars (interdites en France, en Suisse… : à tes risques)", settings.radarsOn) { v ->
                settings.changeRadarsOn(v)
                if (v && !settings.radarsOn) Legal.askRadars = true
            }
            ToggleRow("Avertir en cas de dépassement de la limite", settings.speedLimitWarning, settings::changeSpeedLimitWarning)
            if (settings.speedLimitWarning) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(-1 to "Tolérance légale", 0 to "Dès 1 km/h", 3 to "+3 km/h", 10 to "+10 km/h").forEach { (v, l) ->
                    BigButton(l, if (settings.overTolerance == v) DriveColors.Accent else DriveColors.Panel) { settings.changeOverTolerance(v) }
                }
            }
            ToggleRow("Annoncer les changements de limitation", settings.announceLimit, settings::changeAnnounceLimit)
            ToggleRow("Alarme forte si je suis trop vite en approchant d'un radar", settings.radarAlarm, settings::changeRadarAlarm)
            Text("🚨 Sirène de police au dernier moment avant un radar", color = Color.White, fontSize = 18.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(2 to "À chaque radar", 1 to "Seulement si trop vite", 0 to "Jamais").forEach { (v, l) ->
                    BigButton(l, if (settings.sirenRadar == v) DriveColors.Accent else DriveColors.Panel) {
                        settings.changeSirenRadar(v)
                        if (v > 0) fr.roadguard.core.radar.PoliceSiren.burst(2) // pour l'entendre
                    }
                }
            }
            ToggleRow("Alarme forte aussi sans radar, dès que je dépasse la limitation", settings.alarmAnywhere, settings::changeAlarmAnywhere)
            ToggleRow("Me réprimander si je roule trop vite trop longtemps ou trop souvent", settings.scold, settings::changeScold)
            SafetySettings(settings)
            ToggleRow("Rappel de pause après 2 h de conduite", settings.pauseReminder, settings::changePauseReminder)

            Section("Itinéraire")
            ToggleRow("Éviter les péages", settings.avoidTolls, settings::changeAvoidTolls)
            ToggleRow("Éviter les autoroutes", settings.avoidHighways, settings::changeAvoidHighways)
            ToggleRow("Éviter routes étroites, chemins et centres-villes", settings.routeNarrow, settings::changeRouteNarrow)
            ToggleRow("Itinéraire le plus économe en carburant (110 km/h au plus)", settings.routeEco, settings::changeRouteEco)
            ToggleRow("Étapes : dans l'ordre le plus rapide (sinon dans l'ordre choisi)", settings.optimizeStops, settings::changeOptimizeStops)

            if (voice != null) {
                Section("Voix")
                val voices = androidx.compose.runtime.remember { voice.frenchVoices() }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Par défaut", if (settings.voiceName == null) DriveColors.Accent else DriveColors.Panel) {
                        settings.changeVoiceName(null); voice.configure(null, settings.speechRate); voice.limitChanged(80)
                    }
                    voices.forEach { (name, label) ->
                        BigButton(label, if (settings.voiceName == name) DriveColors.Accent else DriveColors.Panel) {
                                settings.changeVoiceName(name); voice.configure(name, settings.speechRate); voice.limitChanged(80)
                        }
                    }
                }
                // Autres langues : voix téléchargées dans Android (moteur de synthèse vocale).
                val others = androidx.compose.runtime.remember { voice.otherVoices() }
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
                            settings.changeVoiceName(name); voice.configure(name, settings.speechRate); voice.limitChanged(80)
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0.85f to "Lente", 1f to "Normale", 1.2f to "Rapide").forEach { (r, label) ->
                        BigButton("Débit : $label", if (settings.speechRate == r) DriveColors.Accent else DriveColors.Panel) {
                            settings.changeSpeechRate(r); voice.configure(settings.voiceName, r); voice.speak("Dans 300 mètres, tournez à droite")
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("fr" to "🇫🇷 Français", "en" to "🇬🇧 English", "de" to "🇩🇪 Deutsch", "it" to "🇮🇹 Italiano").forEach { (l, label) ->
                        BigButton(label, if (settings.voiceLang == l) DriveColors.Accent else DriveColors.Panel) {
                            settings.changeVoiceLang(l); voice.configure(settings.voiceName, settings.speechRate, settings.voiceTone, l); voice.limitChanged(80)
                        }
                    }
                }
                if (settings.voiceLang != "fr") Text("Annonces des radars, limitations, réveil et guidage dans cette langue. Les tons coquins restent en français. " +
                    "Choisis une voix de cette langue (« Autres langues » ci-dessus).", color = DriveColors.TextSecondary, fontSize = 14.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(false to "👨 Je suis un conducteur", true to "👩 Je suis une conductrice").forEach { (f, label) ->
                        BigButton(label, if (settings.driverFemale == f) DriveColors.Accent else DriveColors.Panel) {
                            settings.changeDriverFemale(f); voice.pack.feminine = f; voice.limitChanged(80)
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0 to "Ton : classique", 1 to "Ton : charme 💋", 2 to "Ton : séduction 🔥", 3 to "Ton : osé 😈", 4 to "Ton : extrême 🔥😈",
                        fr.roadguard.core.radar.VoicePack.XXL to "Ton : Coquin XXL 🎭").forEach { (c, label) ->
                        BigButton(label, if (settings.voiceTone == c) DriveColors.Accent else DriveColors.Panel) {
                            settings.changeVoiceTone(c)
                            // Le ton garde la voix choisie ci-dessus (aucun changement de voix imposé).
                            voice.configure(settings.voiceName, settings.speechRate, c)
                            voice.limitChanged(80)
                        }
                    }
                }
                Text(if (settings.voiceTone == fr.roadguard.core.radar.VoicePack.XXL)
                        "Coquin XXL : près de 300 phrases (guidage, ronds-points, radars, limitations, bouchons, arrivée…), au masculin ou au féminin. " +
                        "Une phrase n'est dite que si elle correspond à la situation réelle ; la direction, la distance et la limite restent toujours exactes."
                    else "Le ton change les phrases des radars et des limitations, avec la même voix que le guidage.",
                    color = DriveColors.TextSecondary, fontSize = 14.sp)
            }

            Section("Par-dessus les autres applis")
            val bubbleHelper = androidx.compose.runtime.remember { RadarBubble(context) }
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
                val bgState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(bgGranted()) }
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
            }

            Section("Alertes affichées et annoncées")
            listOf(RadarKind.FIXED, RadarKind.REDLIGHT, RadarKind.AVERAGE, RadarKind.MOBILE).forEach { kind ->
                ToggleRow(AlertText.kindLabel(kind), kind in settings.alertKinds) { settings.toggleKind(kind) }
            }

            Section("Carte")
            ToggleRow("Nord toujours en haut", settings.northUp, settings::changeNorthUp)
            ToggleRow("Zoom selon la vitesse", settings.adaptiveZoom, settings::changeAdaptiveZoom)

            Section("Thème de la carte")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("Style ROADGUARD", if (settings.mapStyle == "roadguard") DriveColors.Accent else DriveColors.Panel) { settings.changeMapStyle("roadguard") }
                BigButton("Style classique", if (settings.mapStyle != "roadguard") DriveColors.Accent else DriveColors.Panel) { settings.changeMapStyle("classic") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeMode.entries.forEach { mode ->
                    BigButton(mode.label, if (settings.theme == mode) DriveColors.Accent else DriveColors.Panel) { settings.changeTheme(mode) }
                }
            }

            Section("Apparence")
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
                Section("Position GPS")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("auto" to "Automatique", "unit" to "GPS du poste", "phone" to "GPS du téléphone").forEach { (v, label) ->
                        BigButton(label, if (settings.gpsSource == v) DriveColors.Accent else DriveColors.Panel) { settings.changeGpsSource(v) }
                    }
                }
                Section("Démarrage")
                ToggleRow("Ouvrir DRIVE à l'allumage du poste", settings.autostart, settings::changeAutostart)
            }

            if (updateApp != null) UpdateCard(updateApp)

            Section("À propos")
            Text(
                "Pilote — © Nathan Lacroix. $radarCount points d'alerte embarqués.\n" +
                    "Cartes © OpenStreetMap (OpenFreeMap, CARTO). Recherche Photon, itinéraires Valhalla (FOSSGIS).\n" +
                    "Les alertes complètent la signalisation : elles ne la remplacent pas.",
                color = DriveColors.TextSecondary, fontSize = 15.sp,
            )
            // Marge en bas : le dernier réglage peut remonter au-dessus du bandeau de mise à jour.
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 120.dp))
        }
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
