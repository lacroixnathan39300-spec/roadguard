package fr.roadguard.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Style « tableau de bord » commun aux deux applis. */
object DriveStyle {
    /** Police étroite et grasse des chiffres et des titres (déjà présente sur Android, rien à télécharger). */
    val Numbers: FontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(
        android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)))
    val Titles: FontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(
        android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)))

    /** Panneau : léger relief (plus clair en haut). */
    val panelBrush: Brush get() = Brush.verticalGradient(listOf(Color(0xFF18212B), Color(0xFF0E141B)))
    val panelBorder = Color(0x14FFFFFF)
    /** Bouton de carte façon verre fumé. */
    val glass = Color(0xD9141B23)
}

/** Icônes des boutons ronds : vraies icônes vectorielles à la place des symboles texte. */
@Composable
fun DriveIcon(symbol: String, iconSize: Dp = 28.dp, tint: Color = Color.White): Boolean {
    val m = Modifier.size(iconSize)
    when (symbol) {
        "⚙" -> Icon(Icons.Filled.Settings, "Réglages", m, tint)
        "🔍" -> Icon(Icons.Filled.Search, "Rechercher", m, tint)
        "⚠" -> Icon(Icons.Filled.Warning, "Signaler", m, tint)
        "📲" -> Icon(Icons.Filled.Phone, "Téléphone", m, tint)
        "◎" -> Canvas(m) {
            // Viseur « recentrer ».
            val c = center; val r = size.minDimension * 0.30f; val w = size.minDimension * 0.09f
            drawCircle(tint, r, c, style = Stroke(w))
            drawCircle(tint, r * 0.35f, c)
            listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f)).forEach { d ->
                drawLine(tint, c + d * (r * 1.15f), c + d * (size.minDimension * 0.48f), w, StrokeCap.Round)
            }
        }
        "▦" -> Canvas(m) {
            // Tableau de bord : 4 tuiles.
            val g = size.minDimension * 0.1f; val s = (size.minDimension - g * 3) / 2
            for (i in 0..1) for (j in 0..1) drawRoundRect(tint, Offset(g + i * (s + g), g + j * (s + g)), Size(s, s), CornerRadius(s * 0.25f))
        }
        "🔊", "🔉" -> Canvas(m) {
            // Haut-parleur, avec deux ondes (plus fort) ou une (moins fort).
            val h = size.height; val w = size.width
            val body = Path().apply {
                moveTo(w * 0.12f, h * 0.38f); lineTo(w * 0.30f, h * 0.38f); lineTo(w * 0.52f, h * 0.18f)
                lineTo(w * 0.52f, h * 0.82f); lineTo(w * 0.30f, h * 0.62f); lineTo(w * 0.12f, h * 0.62f); close()
            }
            drawPath(body, tint)
            val sw = Stroke(w * 0.08f, cap = StrokeCap.Round)
            drawArc(tint, -45f, 90f, false, Offset(w * 0.40f, h * 0.30f), Size(w * 0.30f, h * 0.40f), style = sw)
            if (symbol == "🔊") drawArc(tint, -50f, 100f, false, Offset(w * 0.40f, h * 0.15f), Size(w * 0.52f, h * 0.70f), style = sw)
        }
        else -> return false
    }
    return true
}
