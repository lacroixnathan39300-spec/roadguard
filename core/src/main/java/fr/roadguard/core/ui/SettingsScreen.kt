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
    voice: fr.roadguard.core.radar.AlertVoice? = null, updateApp: String? = null, onAdmin: (() -> Unit)? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
            BigButton("📖 Revoir le tutoriel", DriveColors.Panel) { Tutorial.forceShow = true; onClose() }
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
            ToggleRow("Avertir en cas de dépassement de la limite", settings.speedLimitWarning, settings::changeSpeedLimitWarning)
            if (settings.speedLimitWarning) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(-1 to "Tolérance légale", 0 to "Dès 1 km/h", 3 to "+3 km/h", 10 to "+10 km/h").forEach { (v, l) ->
                    BigButton(l, if (settings.overTolerance == v) DriveColors.Accent else DriveColors.Panel) { settings.changeOverTolerance(v) }
                }
            }
            ToggleRow("Annoncer les changements de limitation", settings.announceLimit, settings::changeAnnounceLimit)
            ToggleRow("Alarme forte si je suis trop vite en approchant d'un radar", settings.radarAlarm, settings::changeRadarAlarm)
            ToggleRow("Rappel de pause après 2 h de conduite", settings.pauseReminder, settings::changePauseReminder)

            Section("Itinéraire")
            ToggleRow("Éviter les péages", settings.avoidTolls, settings::changeAvoidTolls)
            ToggleRow("Éviter les autoroutes", settings.avoidHighways, settings::changeAvoidHighways)

            if (voice != null) {
                Section("Voix")
                val voices = androidx.compose.runtime.remember { voice.frenchVoices() }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("Par défaut", if (settings.voiceName == null) DriveColors.Accent else DriveColors.Panel) { settings.changeVoiceName(null) }
                    voices.forEach { (name, label) ->
                        BigButton(label, if (settings.voiceName == name) DriveColors.Accent else DriveColors.Panel) {
                            settings.changeVoiceName(name); voice.configure(name, settings.speechRate); voice.speak("Radar fixe, limité à 80")
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
            ToggleRow("Bouton « Trafic Waze en direct » sur la carte", settings.wazeButton, settings::changeWazeButton)
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
                Section("Diagnostic du poste")
                run {
                    val keys = androidx.compose.runtime.remember { fr.roadguard.core.account.DeviceKeys.check(context) }
                    val sent = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    val ok = keys.testKeys || keys.aospCert
                    Text(
                        if (ok) "✅ Poste signé avec les clés de test d'Android : « Pilote Drive Système » (Waze dans un module) est possible."
                        else "❌ Poste signé avec les clés du fabricant : la version système n'est pas possible sans root.",
                        color = if (ok) Color(0xFF81C784) else Color(0xFFFF8A80), fontSize = 17.sp,
                    )
                    Text(keys.text, color = DriveColors.TextSecondary, fontSize = 12.sp)
                    BigButton(if (sent.value) "Envoyé ✔" else "Envoyer le diagnostic à Nathan", DriveColors.Panel) {
                        fr.roadguard.core.account.DeviceKeys.send(context, keys); sent.value = true
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
