package fr.roadguard.core.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.media.DriveEqualizer
import fr.roadguard.core.media.NowPlaying
import fr.roadguard.core.media.frequencyLabel

/** Carte musique de l'accueil : pochette, titre, commandes ; appui long = égaliseur. */
@Composable
fun MusicCard(
    nowPlaying: NowPlaying?,
    hasAccess: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ouvre le lecteur choisi (ou le choix du lecteur) ; null : pas de bouton. */
    onOpenPlayer: (() -> Unit)? = null,
    playerLabel: String? = null,
) {
    val context = LocalContext.current
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(DriveColors.Track)
                    .let { m -> if (onOpenPlayer != null) m.clickable(onClick = onOpenPlayer) else m },
                contentAlignment = Alignment.Center,
            ) {
                nowPlaying?.artwork?.let {
                    Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } ?: Text("♪", color = DriveColors.TextSecondary, fontSize = 26.sp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                when {
                    !hasAccess -> {
                        Text("Musique", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Toucher pour autoriser l'accès",
                            color = DriveColors.Accent, fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                        )
                    }
                    nowPlaying == null -> if (onOpenPlayer != null) Column(Modifier.clickable(onClick = onOpenPlayer)) {
                        Text("Aucune musique", color = DriveColors.TextSecondary, fontSize = 16.sp)
                        Text(playerLabel?.let { "Toucher pour ouvrir $it" } ?: "Toucher pour choisir le lecteur",
                            color = DriveColors.Accent, fontSize = 14.sp)
                    } else Text("Aucune musique", color = DriveColors.TextSecondary, fontSize = 16.sp)
                    else -> {
                        Text(nowPlaying.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(nowPlaying.artist, nowPlaying.app).filter { it.isNotBlank() }.joinToString(" · "),
                            color = DriveColors.TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (hasAccess) {
                ControlButton("⏮", onPrevious)
                ControlButton(if (nowPlaying?.playing == true) "⏸" else "▶", onPlayPause)
                ControlButton("⏭", onNext)
            }
            ControlButton("EQ", onEqualizer)
        }
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 24.sp) }
}

/** Égaliseur pro plein écran : préréglages, 10 bandes, boost de volume, basses, spatialisation, réglages perso. */
@Composable
fun EqualizerPanel(equalizer: DriveEqualizer, onClose: () -> Unit) {
    var gains by remember { mutableStateOf(equalizer.gains) }
    var enabled by remember { mutableStateOf(equalizer.enabled) }
    var preset by remember { mutableStateOf(equalizer.presetName) }
    var boost by remember { mutableStateOf(equalizer.boostDb) }
    var bass by remember { mutableStateOf(equalizer.bassStrength) }
    var spatial by remember { mutableStateOf(equalizer.spatial) }
    var info by remember { mutableStateOf<String?>(null) }
    fun refresh() { gains = equalizer.gains; preset = equalizer.presetName; enabled = equalizer.enabled; boost = equalizer.boostDb; bass = equalizer.bassStrength; spatial = equalizer.spatial }
    Column(
        Modifier.fillMaxSize().background(DriveColors.Background.copy(alpha = 0.98f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Égaliseur", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("$preset · ${equalizer.engine}", color = DriveColors.TextSecondary, fontSize = 13.sp)
            }
            if (equalizer.available) {
                Switch(checked = enabled, onCheckedChange = { enabled = it; equalizer.enabled = it })
                Spacer(Modifier.width(12.dp))
                PanelButton("Zéro") { equalizer.reset(); refresh() }
                Spacer(Modifier.width(8.dp))
            }
            PanelButton("Fermer", onClose)
        }
        if (!equalizer.available) {
            Text("Ce poste ne permet pas de régler l'égaliseur depuis une appli.", color = DriveColors.TextSecondary, fontSize = 18.sp)
            return@Column
        }
        // Préréglages et réglages perso.
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (equalizer.presets + (1..3).mapNotNull { equalizer.custom(it) }).forEach { p ->
                Text(p.name, color = Color.White, fontSize = 15.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (p.name == preset) DriveColors.Accent else DriveColors.Panel)
                        .clickable { equalizer.applyPreset(p); refresh() }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        // 10 bandes.
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            gains.forEachIndexed { i, g ->
                Column(Modifier.fillMaxHeight().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%+.0f".format(java.util.Locale.FRANCE, g), color = if (g == 0f) DriveColors.TextSecondary else Color.White, fontSize = 13.sp)
                    VerticalSlider(
                        value = g, range = equalizer.minDb..equalizer.maxDb,
                        onChange = { v -> val r = Math.round(v * 2) / 2f; equalizer.setGain(i, r); gains = equalizer.gains; preset = equalizer.presetName },
                        modifier = Modifier.weight(1f),
                    )
                    Text(frequencyLabel(equalizer.frequencies[i]), color = DriveColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }
        // Boost, basses, spatialisation.
        @Composable fun Knob(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, available: Boolean, onChange: (Float) -> Unit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.width(150.dp))
                if (available) {
                    Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = DriveColors.Accent, activeTrackColor = DriveColors.Accent))
                    Text(valueText, color = Color.White, fontSize = 15.sp, modifier = Modifier.width(70.dp))
                } else Text("non pris en charge par cet appareil", color = DriveColors.TextSecondary, fontSize = 13.sp)
            }
        }
        Knob("🔊 Boost volume", "+%.0f dB".format(java.util.Locale.FRANCE, boost), boost, 0f..12f, equalizer.boostAvailable) { boost = it; equalizer.setBoost(it) }
        Knob("🥁 Basses", "${bass / 10} %", bass.toFloat(), 0f..1000f, equalizer.bassAvailable) { bass = it.toInt(); equalizer.setBass(it.toInt()) }
        Knob("🎧 Spatialisation", "${spatial / 10} %", spatial.toFloat(), 0f..1000f, equalizer.spatialAvailable) { spatial = it.toInt(); equalizer.setSpatial(it.toInt()) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            (1..3).forEach { slot -> PanelButton("💾 Perso $slot") { equalizer.saveCustom(slot); preset = equalizer.presetName; info = "Réglage enregistré dans Perso $slot" } }
            info?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 13.sp) }
        }
        if (boost > 6f) Text("⚠️ Boost fort : le son peut saturer à volume maximum.", color = Color(0xFFFFB74D), fontSize = 13.sp)
    }
}

@Composable
private fun VerticalSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, modifier: Modifier) {
    // Glissière horizontale tournée de -90° : même comportement tactile, affichage vertical.
    Box(modifier.width(64.dp), contentAlignment = Alignment.Center) {
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = DriveColors.Accent, activeTrackColor = DriveColors.Accent),
            modifier = Modifier.rotateVertical().fillMaxWidth(),
        )
    }
}

@Composable
private fun PanelButton(label: String, onClick: () -> Unit) {
    Text(
        label, color = Color.White, fontSize = 18.sp,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(DriveColors.Panel).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
