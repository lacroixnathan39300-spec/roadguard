package fr.roadguard.core.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DriveSettings
import fr.roadguard.core.community.Conversation
import fr.roadguard.core.community.Community
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Proposé une seule fois (première connexion) : le mode conversation. */
object ConversationIntro {
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("account", Context.MODE_PRIVATE)
    fun seen(c: Context) = prefs(c).getBoolean("conv_intro", false)
    fun markSeen(c: Context) = prefs(c).edit().putBoolean("conv_intro", true).apply()
}

/** Écran de présentation du mode conversation, avec choix du personnage. */
@Composable
fun ConversationIntroScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { DriveSettings(context) }
    var persona by remember { mutableStateOf("auto") }
    var gender by remember { mutableStateOf(settings.convGender) }
    val askMic = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    Box(Modifier.fillMaxSize().background(Color(0xE60B0F15)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DriveColors.Panel)
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("💬 Nouveau : le mode conversation", color = DriveColors.Accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Pilote peut t'écouter au micro et te répondre : tu parles, il répond, et la discussion continue. " +
                "Il réagit aussi aux situations (trajet, énervement…). Tout se passe hors ligne.",
                color = Color.White, fontSize = 16.sp)
            Text("Je suis…", color = DriveColors.TextSecondary, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigButton("👨 un homme", if (gender == "homme") DriveColors.Accent else DriveColors.Track) { gender = "homme" }
                BigButton("👩 une femme", if (gender == "femme") DriveColors.Accent else DriveColors.Track) { gender = "femme" }
            }
            Text("Son caractère (le compagnon prend le genre opposé au tien) :", color = DriveColors.TextSecondary, fontSize = 14.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("auto" to "✨ Auto (s'adapte)", "normal" to "🚗 Normal", "ami" to "😊 Ami·e",
                    "amoureux" to "❤️ Amoureux·se", "coquin" to "😏 Coquin·e").forEach { (id, label) ->
                    BigButton(label, if (persona == id) DriveColors.Accent else DriveColors.Track) { persona = id }
                }
            }
            Text("Auto : amical au départ, puis plus tendre ou plus coquin selon ce que tu dis — il s'adapte tout seul.", color = DriveColors.TextSecondary, fontSize = 13.sp)
            Text("Ça utilise le micro en continu (un peu de batterie). Tu pourras le couper ou changer de personnage " +
                "dans Réglages → Voix. Pour le hors-ligne, installe le français « hors connexion » dans Android.",
                color = DriveColors.TextSecondary, fontSize = 13.sp)
            BigButton("Activer et télécharger les phrases", DriveColors.Accent) {
                settings.changeConvGender(gender)
                settings.changeConvPersona(persona)
                settings.changeConversationOn(true)
                if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    askMic.launch(android.Manifest.permission.RECORD_AUDIO)
                scope.launch(Dispatchers.IO) { Conversation.download(context, Community.DEFAULT_URL) }
                ConversationIntro.markSeen(context)
                onDone()
            }
            BigButton("Plus tard", DriveColors.Track) {
                ConversationIntro.markSeen(context)
                onDone()
            }
        }
    }
}
