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
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.07f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = DriveColors.Track, startAngle = 150f, sweepAngle = 240f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (kmh != null && kmh > 0) {
                drawArc(
                    color = if (over) Color(0xFFE53935) else DriveColors.Accent, startAngle = 150f, sweepAngle = SpeedMath.needleSweep(kmh),
                    useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = kmh?.toString() ?: "--",
                color = if (over) Color(0xFFFF5252) else Color.White, fontSize = numberSize, fontWeight = FontWeight.Bold,
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
