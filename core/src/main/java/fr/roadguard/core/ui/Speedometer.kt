package fr.roadguard.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.roadguard.core.SpeedMath

/** Compteur rond : arc de fond, arc de vitesse, chiffre au centre. */
@Composable
fun Speedometer(kmh: Int?, unit: String, waiting: String, limit: Int?, over: Boolean, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        // Tailles calculées sur le compteur lui-même : lisible même avec une grande police système.
        val density = androidx.compose.ui.platform.LocalDensity.current
        val side = minOf(maxWidth, maxHeight)
        val digits = if ((kmh ?: 0) >= 100) 0.34f else 0.42f
        val numberSize = with(density) { (side * digits).toSp() }
        val unitSize = with(density) { (side * 0.12f).toSp() }
        val badge = side * 0.36f
        val badgeText = with(density) { (badge * 0.40f).toSp() }
        val accent = DriveColors.Accent
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.065f
            val inset = stroke * 1.6f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val c = center
            val radius = arcSize.width / 2
            // Graduations tous les 20 km/h (plus longues tous les 40), à l'extérieur de l'arc.
            for (v in 0..200 step 10) {
                val a = Math.toRadians((150.0 + 240.0 * v / 200.0))
                val major = v % 40 == 0
                val r1 = radius + stroke * 0.9f
                val r2 = r1 + if (major) stroke * 0.9f else stroke * 0.45f
                val dir = Offset(Math.cos(a).toFloat(), Math.sin(a).toFloat())
                drawLine(if (limit != null && v > limit) Color(0x66FF5252) else Color(0x55FFFFFF),
                    c + dir * r1, c + dir * r2, if (major) stroke * 0.22f else stroke * 0.14f, StrokeCap.Round)
            }
            drawArc(DriveColors.Track, 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Repère de la limite sur l'arc.
            if (limit != null) {
                val a = 150f + SpeedMath.needleSweep(limit)
                drawArc(Color(0xFFD32F2F), a - 1.2f, 2.4f, false, topLeft, arcSize, style = Stroke(stroke * 1.5f))
            }
            if (kmh != null && kmh > 0) {
                val sweep = SpeedMath.needleSweep(kmh)
                val main = if (over) Color(0xFFFF3B30) else accent
                // Halo puis arc en dégradé (plus intense vers la vitesse actuelle).
                drawArc(main.copy(alpha = 0.22f), 150f, sweep, false, topLeft, arcSize, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
                drawArc(
                    Brush.sweepGradient(0f to main.copy(alpha = 0.35f), 0.42f to main, 0.66f to main, 1f to main.copy(alpha = 0.35f), center = c),
                    150f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = kmh?.coerceIn(0, 399)?.toString() ?: "--",
                color = if (over) Color(0xFFFF5252) else Color.White, fontSize = numberSize * 1.12f, fontWeight = FontWeight.Bold,
                fontFamily = DriveStyle.Numbers,
                maxLines = 1, softWrap = false,
            )
            Text(text = if (kmh == null) waiting else unit, color = DriveColors.TextSecondary, fontSize = unitSize, maxLines = 1)
        }
        // Panneau de limitation (rond rouge) en haut à droite du compteur.
        if (limit != null) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = badge * 0.2f, y = -badge * 0.12f).size(badge).clip(CircleShape).background(Color.White)
                    .border(badge * 0.1f, Color(0xFFD32F2F), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(limit.toString(), color = Color.Black, fontSize = badgeText, fontWeight = FontWeight.Bold, maxLines = 1) }
        }
    }
}
