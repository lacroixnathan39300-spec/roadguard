package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.radar.AlertText
import fr.roadguard.core.radar.AverageStatus
import fr.roadguard.core.radar.RadarAlert
import fr.roadguard.core.radar.RadarKind

/** Bandeaux affichés en haut de la carte : radar devant soi et vitesse moyenne. */
@Composable
fun AlertBanners(alert: RadarAlert?, average: AverageStatus?, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp)) {
        alert?.let { RadarBanner(it) }
        average?.let {
            val color = when {
                it.limitKmh == null -> Color(0xFF3A4654)
                it.over -> Color(0xFFD32F2F)
                else -> Color(0xFF2E7D32)
            }
            Banner(AlertText.average(it), color, Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * Alerte radar : pastille « limitation » façon panneau, type et distance en gros chiffres,
 * barre de couleur qui pulse doucement sous 300 m.
 */
@Composable
private fun RadarBanner(a: RadarAlert) {
    val color = colorFor(a.radar.kind)
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "radar")
    val glow by pulse.animateFloat(
        initialValue = 0.55f, targetValue = 1f, label = "glow",
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(if (a.distanceM < 300) 450 else 900),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
    )
    androidx.compose.foundation.layout.Row(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xF00E141B))
            .border(2.dp, color.copy(alpha = glow), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        // Panneau de limitation (ou triangle si la vitesse n'est pas connue).
        a.radar.speedKmh?.let { v ->
            androidx.compose.foundation.layout.Box(
                Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White)
                    .border(5.dp, Color(0xFFD32F2F), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { Text("$v", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = DriveStyle.Numbers) }
        } ?: DriveIcon("⚠", 40.dp, color)
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
        Column {
            Text(AlertText.kindLabel(a.radar.kind), color = color.copy(alpha = 0.6f + 0.4f * glow), fontSize = 16.sp,
                fontWeight = FontWeight.Bold, fontFamily = DriveStyle.Titles)
            Text("${AlertText.roundedDistance(a.distanceM)} m", color = Color.White, fontSize = 30.sp,
                fontWeight = FontWeight.Bold, fontFamily = DriveStyle.Numbers)
        }
    }
}

@Composable
private fun Banner(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.94f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

private fun colorFor(kind: RadarKind): Color = when (kind) {
    RadarKind.REDLIGHT -> Color(0xFFE65100)
    RadarKind.AVERAGE, RadarKind.SECTION -> Color(0xFF7B1FA2)
    RadarKind.MOBILE -> Color(0xFFF9A825)
    else -> Color(0xFFC62828)
}
