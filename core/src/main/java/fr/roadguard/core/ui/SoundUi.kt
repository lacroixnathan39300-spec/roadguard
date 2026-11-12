package fr.roadguard.core.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.roadguard.core.DriveSettings
import fr.roadguard.core.radar.AlertVoice
import fr.roadguard.core.radar.SoundLevel

/**
 * Bouton son de l'accueil (haut-parleur) : remplace l'ancien feu « Limites » et l'interrupteur « Radars ».
 * Un appui ouvre le choix du niveau (tout, alertes seulement, radars seuls, tout couper) et l'activation des radars.
 */
@Composable
fun SoundButton(settings: DriveSettings, voice: AlertVoice?) {
    var open by remember { mutableStateOf(false) }
    val level = settings.soundLevel
    QuickToggle(SoundLevel.emoji(level), SoundLevel.caption(level), on = level != SoundLevel.MUTE, onToggle = { open = true },
        offCaption = SoundLevel.caption(level), slash = false)
    if (open) SoundPanel(settings, voice, onClose = { open = false })
}

/** Choix du niveau de son et des alertes radars (grands boutons, utilisables en voiture). */
@Composable
fun SoundPanel(settings: DriveSettings, voice: AlertVoice?, onClose: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(16.dp).widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(DriveStyle.panelBrush).border(1.dp, DriveStyle.panelBorder, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🔊 Son des annonces", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            val radars = settings.radarsOn
            SoundLevel.choices(radars).forEach { l ->
                val selected = l == settings.soundLevel || (l == SoundLevel.MUTE && settings.soundLevel == SoundLevel.RADARS && !radars)
                SoundChoice("${SoundLevel.emoji(l)}  ${SoundLevel.title(l)}", SoundLevel.detail(l, radars), selected) {
                    settings.changeSoundLevel(l)
                    voice?.level = l
                    Toast.makeText(context, "${SoundLevel.emoji(l)} ${SoundLevel.title(l)} : ${SoundLevel.detail(l, radars).replaceFirstChar { it.lowercase() }}", Toast.LENGTH_SHORT).show()
                    onClose()
                }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).background(Color(0x22FFFFFF)).padding(top = 1.dp))
            // Radars regroupés ici : activation (avec les consignes légales à accepter) ou arrêt.
            SoundChoice(
                if (radars) "📷  Alertes radars : activées" else "📷  Activer les alertes radars",
                if (radars) "Toucher pour les couper (et les retirer de la carte)" else "Les consignes légales s'affichent pour les accepter",
                selected = radars, accent = if (radars) Color(0xFF2ED573) else Color(0xFFFFB300),
            ) { onClose(); toggleRadars(context, settings, voice) }
            Text("Fermer", color = Color(0xFF9AA4B2), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClose).padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun SoundChoice(title: String, detail: String, selected: Boolean, accent: Color = Color(0xFF2ED573), onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (selected) accent.copy(alpha = 0.16f) else Color(0x14FFFFFF))
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Color(0x22FFFFFF), shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFFB8C0CC), fontSize = 14.sp)
        }
        if (selected) Text("✓", color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** Active ou coupe les alertes radars ; sans accord encore donné, la confirmation légale s'affiche. */
fun toggleRadars(context: Context, settings: DriveSettings, voice: AlertVoice?) {
    val want = !settings.radarsOn
    settings.changeRadarsOn(want)
    if (want && !settings.radarsOn) { Legal.askRadars = true; return }
    // Radars coupés alors que le son était sur « radars seuls » : plus rien à entendre, on l'indique (muet).
    if (!settings.radarsOn && settings.soundLevel == SoundLevel.RADARS) { settings.changeSoundLevel(SoundLevel.MUTE); voice?.level = SoundLevel.MUTE }
    Toast.makeText(context, if (settings.radarsOn) "Alertes radars activées" else "Alertes radars coupées", Toast.LENGTH_SHORT).show()
}
