package fr.roadguard.core.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.account.AccountManager
import kotlinx.coroutines.launch

/**
 * Prénom du conducteur, gardé dans son compte (le même sur le téléphone et sur l'autoradio) : le GPS l'utilise
 * pour s'adresser à lui (« Nathan, radar fixe dans 300 mètres »). Demandé à la création du compte, une fois aux
 * comptes existants, et modifiable dans Réglages → Voix.
 */
object FirstName {
    private fun prefs(c: Context) = c.getSharedPreferences("account", Context.MODE_PRIVATE)
    fun asked(c: Context) = prefs(c).getBoolean("first_name_asked", false)
    fun markAsked(c: Context) { prefs(c).edit().putBoolean("first_name_asked", true).apply() }
}

/** Champ « Ton prénom » et bouton « Enregistrer » ; [onSaved] une fois enregistré dans le compte. */
@Composable
fun FirstNameEditor(onSaved: (() -> Unit)? = null) {
    val context = LocalContext.current
    val account = remember { AccountManager.get(context) }
    val scope = rememberCoroutineScope()
    val saved = account.status?.firstName.orEmpty()
    var name by remember(saved) { mutableStateOf(saved) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(30); message = null }, singleLine = true, label = { Text("Ton prénom") },
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
                focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary, cursorColor = DriveColors.Accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BigButton(if (busy) "Patiente…" else "Enregistrer", DriveColors.Accent) {
                if (busy) return@BigButton
                busy = true
                scope.launch {
                    runCatching { account.setFirstName(name) }
                        .onSuccess {
                            val n = account.status?.firstName.orEmpty()
                            message = if (n.isBlank()) "Prénom effacé : Pilote ne t'appellera plus par ton prénom." else "C'est noté, $n ✓"
                            FirstName.markAsked(context)
                            onSaved?.invoke()
                        }
                        .onFailure { message = it.message ?: "Enregistrement impossible (pas de réseau ?)" }
                    busy = false
                }
            }
            if (saved.isNotBlank() && name == saved) Text("Enregistré : $saved", color = DriveColors.TextSecondary, fontSize = 15.sp)
        }
        message?.let { Text(it, color = Color(0xFFCFE3FF), fontSize = 15.sp) }
    }
}

/** Question posée une fois aux comptes qui n'ont pas encore de prénom. */
@Composable
fun FirstNameScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier.fillMaxSize().background(Color(0xD9000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp)).background(DriveColors.Panel)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("👋 Comment tu t'appelles ?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Pilote t'appellera par ton prénom dans ses annonces : « Nathan, radar fixe dans 300 mètres », " +
                "« Bonne route, Nathan ». Facultatif, modifiable dans Réglages → Voix.",
                color = DriveColors.TextSecondary, fontSize = 16.sp)
            FirstNameEditor(onSaved = onDone)
            BigButton("Plus tard", DriveColors.Track) { FirstName.markAsked(context); onDone() }
        }
    }
}
