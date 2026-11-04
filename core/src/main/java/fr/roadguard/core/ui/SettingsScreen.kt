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
fun SettingsScreen(settings: DriveSettings, radarCount: Int, onClose: () -> Unit, onOfflineData: (() -> Unit)? = null, showAutostart: Boolean = false) {
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Réglages", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onOfflineData != null) {
                BigButton("🗺 Cartes hors ligne, radars par pays et mes radars", DriveColors.Accent, onOfflineData)
            }
            Section("Annonces")
            ToggleRow("Voix (radars, guidage, vitesse moyenne)", settings.voice, settings::changeVoice)
            ToggleRow("Bip d'alerte", settings.beep, settings::changeBeep)
            ToggleRow("Bips de plus en plus rapides en approchant (détecteur)", settings.proximityBeeps, settings::changeProximityBeeps)
            ToggleRow("Lire à voix haute les messages du téléphone", settings.readMessages, settings::changeReadMessages)
            ToggleRow("Avertir en cas de dépassement de la limite", settings.speedLimitWarning, settings::changeSpeedLimitWarning)

            Section("Itinéraire")
            ToggleRow("Éviter les péages", settings.avoidTolls, settings::changeAvoidTolls)
            ToggleRow("Éviter les autoroutes", settings.avoidHighways, settings::changeAvoidHighways)

            Section("Alertes affichées et annoncées")
            listOf(RadarKind.FIXED, RadarKind.REDLIGHT, RadarKind.AVERAGE, RadarKind.MOBILE).forEach { kind ->
                ToggleRow(AlertText.kindLabel(kind), kind in settings.alertKinds) { settings.toggleKind(kind) }
            }

            Section("Carte")
            ToggleRow("Nord toujours en haut", settings.northUp, settings::changeNorthUp)
            ToggleRow("Zoom selon la vitesse", settings.adaptiveZoom, settings::changeAdaptiveZoom)

            Section("Thème de la carte")
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
                Section("Démarrage")
                ToggleRow("Ouvrir DRIVE à l'allumage du poste", settings.autostart, settings::changeAutostart)
            }

            Section("À propos")
            Text(
                "ROADGUARD DRIVE 2 — © Nathan Lacroix. $radarCount points d'alerte embarqués.\n" +
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
