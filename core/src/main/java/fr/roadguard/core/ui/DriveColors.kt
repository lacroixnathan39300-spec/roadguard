package fr.roadguard.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Couleurs de base, contrastées pour une lecture rapide en conduite. Couleur d'accent et taille réglables. */
object DriveColors {
    val Background = Color(0xFF05070A)
    val Panel = Color(0xFF121820)
    val Track = Color(0xFF26303C)
    var Accent by mutableStateOf(Color(0xFF00B8FF))
    val TextSecondary = Color(0xFF9AA7B4)
    /** Taille des boutons (1 = normale). */
    var Scale by mutableFloatStateOf(1f)

    val accents: List<Pair<String, Long>> = listOf(
        "Bleu" to 0xFF00B8FF, "Vert" to 0xFF22C55E, "Orange" to 0xFFF97316,
        "Rouge" to 0xFFEF4444, "Violet" to 0xFFA855F7, "Jaune" to 0xFFEAB308,
    )
    val scales: List<Pair<String, Float>> = listOf("Normale" to 1f, "Grande" to 1.2f, "Très grande" to 1.4f)
}
