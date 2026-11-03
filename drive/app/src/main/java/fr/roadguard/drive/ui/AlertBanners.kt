package fr.roadguard.drive.ui

import androidx.compose.foundation.background
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
import fr.roadguard.drive.radar.AlertText
import fr.roadguard.drive.radar.AverageStatus
import fr.roadguard.drive.radar.RadarAlert
import fr.roadguard.drive.radar.RadarKind

/** Bandeaux affichés en haut de la carte : radar devant soi et vitesse moyenne. */
@Composable
fun AlertBanners(alert: RadarAlert?, average: AverageStatus?, modifier: Modifier = Modifier) {
    Column(modifier.padding(12.dp)) {
        alert?.let {
            Banner(AlertText.banner(it), colorFor(it.radar.kind))
        }
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
