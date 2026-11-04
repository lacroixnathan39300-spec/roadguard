package fr.roadguard.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Écrans de conduite : la taille de police du téléphone est prise en compte, mais plafonnée
 * pour que compteur, bandeaux et boutons restent à leur place (ils sont déjà grands).
 */
@Composable
fun CarFontScale(max: Float = 1.15f, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale.coerceAtMost(max)), content = content)
}
