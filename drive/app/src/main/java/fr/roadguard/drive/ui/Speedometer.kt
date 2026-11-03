package fr.roadguard.drive.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import fr.roadguard.drive.SpeedMath

/** Compteur rond : arc de fond, arc de vitesse, chiffre au centre. */
@Composable
fun Speedometer(kmh: Int?, unit: String, waiting: String, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
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
                    color = DriveColors.Accent, startAngle = 150f, sweepAngle = SpeedMath.needleSweep(kmh),
                    useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = kmh?.toString() ?: "--",
                color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold,
            )
            Text(text = if (kmh == null) waiting else unit, color = DriveColors.TextSecondary, fontSize = 18.sp)
        }
    }
}
