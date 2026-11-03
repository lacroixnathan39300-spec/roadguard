package fr.roadguard.drive.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import fr.roadguard.drive.media.DriveEqualizer
import fr.roadguard.drive.media.NowPlaying
import fr.roadguard.drive.media.frequencyLabel

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
) {
    val context = LocalContext.current
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(DriveColors.Track),
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
                    nowPlaying == null -> Text("Aucune musique", color = DriveColors.TextSecondary, fontSize = 16.sp)
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

/** Égaliseur plein écran : une glissière verticale par bande, activation, remise à zéro. */
@Composable
fun EqualizerPanel(equalizer: DriveEqualizer, onClose: () -> Unit) {
    var bands by remember { mutableStateOf(equalizer.bands()) }
    var enabled by remember { mutableStateOf(equalizer.enabled) }
    Column(
        Modifier.fillMaxSize().background(DriveColors.Background.copy(alpha = 0.97f)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Égaliseur", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (equalizer.available) {
                Switch(checked = enabled, onCheckedChange = { enabled = it; equalizer.enabled = it })
                Spacer(Modifier.width(16.dp))
                PanelButton("Remise à zéro") { equalizer.reset(); bands = equalizer.bands() }
                Spacer(Modifier.width(12.dp))
            }
            PanelButton("Fermer", onClose)
        }
        if (!equalizer.available) {
            Text("Ce poste ne permet pas de régler l'égaliseur depuis une appli.", color = DriveColors.TextSecondary, fontSize = 18.sp)
            return@Column
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            bands.forEachIndexed { i, band ->
                Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${if (band.levelMb > 0) "+" else ""}${band.levelMb / 100} dB", color = Color.White, fontSize = 16.sp)
                    VerticalSlider(
                        value = band.levelMb.toFloat(),
                        range = equalizer.minMb.toFloat()..equalizer.maxMb.toFloat(),
                        onChange = { v ->
                            equalizer.setBand(i, v.toInt())
                            bands = bands.toMutableList().also { it[i] = band.copy(levelMb = v.toInt()) }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(frequencyLabel(band.centerHz), color = DriveColors.TextSecondary, fontSize = 14.sp)
                }
            }
        }
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
