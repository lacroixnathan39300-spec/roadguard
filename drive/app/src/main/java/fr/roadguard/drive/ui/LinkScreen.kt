package fr.roadguard.drive.ui

import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.drive.link.LinkClient
import fr.roadguard.drive.link.LinkState

/** Écran ROADGUARD Link de l'autoradio : association par code et état de la liaison. */
@Composable
fun LinkScreen(link: LinkClient, onClose: () -> Unit) {
    val state by link.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var code by remember { mutableStateOf<String?>(null) }
    var paired by remember { mutableStateOf(link.isPaired()) }
    if (state is LinkState.Connected) paired = true

    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ROADGUARD Link", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel) { if (code != null && !paired) link.cancelPairing(); onClose() }
        }
        val color = when (state) {
            is LinkState.Connected -> Color(0xFF4CAF50)
            is LinkState.Failed -> Color(0xFFFF7043)
            else -> DriveColors.Accent
        }
        Text(state.label, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (paired && code == null) {
                Text("Téléphone associé : ${link.phoneName ?: "téléphone"}.\nLa liaison se fait toute seule dès que le poste est sur le Wi-Fi du téléphone " +
                    "et que « ROADGUARD Link » est démarré sur le téléphone.", color = DriveColors.TextSecondary, fontSize = 18.sp)
                Text("Le poste utilise alors le GPS du téléphone et affiche sa musique, que tu pilotes depuis l'accueil.",
                    color = DriveColors.TextSecondary, fontSize = 18.sp)
                Text("Par câble : branche le téléphone sur le port USB du poste, la liaison démarre seule (et le téléphone se recharge). " +
                    "La première fois, accepte les deux demandes d'autorisation USB, sur le poste et sur le téléphone, en cochant « toujours ».",
                    color = DriveColors.TextSecondary, fontSize = 18.sp)
                BigButton("Réessayer le câble USB", DriveColors.Panel) {
                    fr.roadguard.drive.link.UsbTransport.resetRejected(context)
                    link.connectForPairing()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigButton("Oublier ce téléphone", Color(0xFFB71C1C)) { link.forget(); paired = false }
                    BigButton("Associer un autre téléphone", DriveColors.Panel) { link.forget(); paired = false; code = link.beginPairing() }
                }
            } else if (code == null) {
                Text("Associe ton téléphone une seule fois : ensuite la liaison est automatique.", color = DriveColors.TextSecondary, fontSize = 18.sp)
                BigButton("Associer un téléphone", DriveColors.Accent) { code = link.beginPairing() }
            } else {
                Text(code!!.chunked(3).joinToString(" "), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Step("1", "Active le partage de connexion sur le téléphone et connecte ce poste à son Wi-Fi.")
                Step("2", "Sur le téléphone, ouvre « ROADGUARD Link », tape ce code et appuie sur « Associer ».")
                Step("3", "Appuie sur « Connecter » ci-dessous.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigButton("Connecter", DriveColors.Accent) { link.connectForPairing() }
                    BigButton("Nouveau code", DriveColors.Panel) { code = link.beginPairing() }
                }
            }
        }
    }
}

@Composable
private fun Step(n: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(n, color = DriveColors.Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 14.dp))
        Text(text, color = Color.White, fontSize = 19.sp)
    }
}
