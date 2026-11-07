package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.account.AccountManager
import fr.roadguard.core.account.AccountStatus
import fr.roadguard.core.account.ManagedUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Porte d'entrée : écran de connexion tant qu'on n'est pas connecté, écran « accès terminé » si l'essai
 * est fini ou le compte bloqué, sinon l'appli. [extra] : boutons en plus sur ces écrans (DRIVE : applis).
 */
@Composable
fun AccountGate(extra: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val account = remember { AccountManager.get(context) }
    // Vérification au démarrage puis toutes les 6 h (en silence ; sans réseau, le dernier état sert).
    LaunchedEffect(account.loggedIn) {
        while (account.loggedIn) { account.refresh(); delay(6 * 3_600_000L) }
    }
    // Messages : nouveaux messages (administrateur) ou réponses, vérifiés toutes les 2 min appli ouverte.
    LaunchedEffect(account.loggedIn) {
        delay(5_000)
        while (account.loggedIn) { InboxNotifier.check(context); delay(120_000) }
    }
    // Avertissement légal avant tout le reste (connexion comprise), puis à chaque nouvelle version du texte.
    var legalOk by remember { mutableStateOf(Legal.accepted(context)) }
    // Preuve d'acceptation : renvoyée une fois connecté (rattachée au compte), ou si l'envoi avait échoué (pas de réseau).
    LaunchedEffect(account.loggedIn, legalOk) { if (legalOk) Legal.syncSoon(context) }
    when {
        account.testBuild -> content()
        !legalOk -> LegalScreen(onDone = { legalOk = true })
        !account.loggedIn -> LoginScreen(account, extra)
        !account.allowed -> AccessEndedScreen(account, account.status!!, extra)
        else -> Box(Modifier.fillMaxSize()) {
            content()
            // Tutoriel au premier lancement (et à la demande depuis les réglages).
            var tutorialDone by remember { mutableStateOf(Tutorial.seen(context)) }
            if (Legal.askRadars) RadarConsentScreen(onDone = { Legal.askRadars = false })
            if (Legal.askSwiss) SwissConsentScreen(onDone = { Legal.askSwiss = false })
            if (Legal.forceShow) LegalScreen(review = true, onDone = { Legal.forceShow = false })
            else if (!tutorialDone || Tutorial.forceShow) TutorialScreen(onDone = { tutorialDone = true; Tutorial.forceShow = false })
            else UpdateBanner()
            // Fin d'essai proche : rappel discret (une fois par jour), avec le contact de l'administrateur.
            val days = account.status?.trialDaysLeft()
            val prefs = remember { context.getSharedPreferences("account", android.content.Context.MODE_PRIVATE) }
            val today = (System.currentTimeMillis() / 86_400_000L).toInt()
            var dismissed by remember { mutableStateOf(prefs.getInt("trial_warned_day", -1) == today) }
            if (days != null && days <= 3 && !dismissed) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(top = 40.dp, start = 16.dp, end = 16.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF0B26A00)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (if (days == 0) "Dernier jour d'essai gratuit." else "Plus que $days jour(s) d'essai gratuit.") +
                            " Pour continuer : " + (account.status?.contact?.ifBlank { null } ?: "contacte Nathan Lacroix") + ".",
                        color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f, fill = false),
                    )
                    BigButton("OK", Color(0x55000000)) { dismissed = true; prefs.edit().putInt("trial_warned_day", today).apply() }
                }
            }
        }
    }
}

@Composable
private fun appName(): String = UpdateNotifier.appKey(LocalContext.current)

@Composable
private fun CenteredCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(DriveStyle.panelBrush)
                .verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, password: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true, label = { Text(label) },
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Ascii, autoCorrectEnabled = false),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
            focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LoginScreen(account: AccountManager, extra: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    CenteredCard {
        Text(if (LocalContext.current.packageName.contains("drive")) "Pilote Drive" else "Pilote", color = DriveColors.Accent, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text(if (creating) "Créer un compte" else "Connexion", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            if (creating) "Essai gratuit d'un mois. Un seul essai par appareil." else "Le même compte sert sur le téléphone et sur l'autoradio.",
            color = DriveColors.TextSecondary, fontSize = 15.sp,
        )
        Field(user, { user = it.take(24) }, "Identifiant")
        Field(pass, { pass = it.take(100) }, "Mot de passe", password = true)
        if (creating) Field(pass2, { pass2 = it.take(100) }, "Mot de passe (encore)", password = true)
        error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 15.sp) }
        BigButton(if (busy) "Patiente…" else if (creating) "Créer mon compte" else "Se connecter", DriveColors.Accent) {
            if (busy) return@BigButton
            if (creating && pass != pass2) { error = "Les deux mots de passe sont différents"; return@BigButton }
            busy = true; error = null
            scope.launch {
                runCatching { if (creating) account.register(user, pass) else account.login(user, pass) }
                    .onFailure { error = it.message ?: "Connexion impossible" }
                busy = false
            }
        }
        BigButton(if (creating) "J'ai déjà un compte" else "Créer un compte", DriveColors.Panel) { creating = !creating; error = null }
        extra()
        // Mise à jour possible même sans être connecté (une correction peut être nécessaire pour se connecter).
        UpdateCard(appName())
    }
}

@Composable
private fun AccessEndedScreen(account: AccountManager, s: AccountStatus, extra: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    CenteredCard {
        Text(if (s.blocked) "⛔" else "⏳", fontSize = 48.sp)
        Text(s.label, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Compte « ${s.username} ». Pour continuer à utiliser Pilote, demande à Nathan Lacroix d'activer ton compte" +
                (if (s.contact.isNotBlank()) " (${s.contact})" else "") + ", puis touche « Vérifier ».",
            color = DriveColors.TextSecondary, fontSize = 16.sp,
        )
        message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 15.sp) }
        BigButton("↻ Vérifier", DriveColors.Accent) {
            scope.launch {
                message = if (!account.refresh()) "Serveur injoignable : vérifie la connexion Internet."
                    else if (account.allowed) null else "Toujours pas activé."
            }
        }
        BigButton("Changer de compte", DriveColors.Panel) { scope.launch { account.logout() } }
        extra()
        UpdateCard(appName())
    }
}

/** Carte « Mon compte » des réglages (et accès à la gestion des comptes pour l'administrateur). */
@Composable
fun AccountCard(onAdmin: () -> Unit) {
    val context = LocalContext.current
    val account = remember { AccountManager.get(context) }
    val scope = rememberCoroutineScope()
    val s = account.status ?: return
    var changing by remember { mutableStateOf(false) }
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var showContact by remember { mutableStateOf(false) }
    var showInbox by remember { mutableStateOf(false) }
    if (showContact) ContactAdminScreen { showContact = false }
    if (showInbox) AdminMessagesScreen { showInbox = false }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("👤 ${s.username}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(s.label, color = DriveColors.TextSecondary, fontSize = 15.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (s.admin) BigButton("🛡 Gérer les comptes", DriveColors.Accent, onAdmin)
            val unread = InboxNotifier.adminUnread.intValue
            if (s.admin) BigButton(if (unread > 0) "✉ Messages reçus ($unread)" else "✉ Messages reçus", if (unread > 0) Color(0xFFD63031) else DriveColors.Accent) { showInbox = true }
            else {
                val replies = InboxNotifier.repliesUnread.intValue
                BigButton(if (replies > 0) "✉ Écrire à l'administrateur ($replies)" else "✉ Écrire à l'administrateur",
                    if (replies > 0) Color(0xFFD63031) else DriveColors.Accent) { showContact = true }
            }
            BigButton("☁ Sauvegarder mes réglages", DriveColors.Track) {
                scope.launch {
                    message = runCatching { account.saveSettings(fr.roadguard.core.DriveSettings(context).exportAll()) }
                        .fold({ "Réglages sauvegardés dans ton compte ✓" }, { "Sauvegarde impossible : ${it.message}" })
                }
            }
            BigButton("☁ Restaurer", DriveColors.Track) {
                scope.launch {
                    message = runCatching { account.loadSettings() }.fold({ o ->
                        if (o == null) "Aucune sauvegarde" else { fr.roadguard.core.DriveSettings(context).importAll(o); "Réglages restaurés : redémarre l'appli pour les appliquer" }
                    }, { "Restauration impossible : ${it.message}" })
                }
            }
            BigButton("Mot de passe", DriveColors.Track) { changing = !changing; message = null }
            BigButton("Se déconnecter", Color(0xFF7F1D1D)) { scope.launch { account.logout() } }
        }
        if (changing) {
            Field(old, { old = it }, "Ancien mot de passe", password = true)
            Field(new, { new = it }, "Nouveau mot de passe", password = true)
            BigButton("Changer", DriveColors.Accent) {
                scope.launch {
                    message = runCatching { account.changePassword(old, new) }.fold({ changing = false; old = ""; new = ""; "Mot de passe changé" }, { it.message })
                }
            }
        }
        message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp) }
    }
}

/** Gestion des comptes (administrateur) : chiffres, contact, plantages, et par compte : accès, mot de passe, blocage. */
@Composable
fun AdminScreen(onClose: () -> Unit, community: fr.roadguard.core.community.Community? = null) {
    val context = LocalContext.current
    val account = remember { AccountManager.get(context) }
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<ManagedUser>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var passwordFor by remember { mutableStateOf<String?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf(account.status?.contact ?: "") }
    var crashes by remember { mutableStateOf<List<org.json.JSONObject>?>(null) }
    var moderation by remember { mutableStateOf(false) }
    if (moderation && community != null) ModerationScreen(community) { moderation = false }
    fun reload() = scope.launch { runCatching { account.users() }.fold({ users = it; error = null }, { error = it.message }) }
    LaunchedEffect(Unit) { reload() }
    fun act(u: String, action: String) = scope.launch {
        runCatching { account.manage(u, action) }.onFailure { error = it.message }
        reload()
    }
    val crashList = crashes
    if (crashList != null) {
        Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Plantages (${crashList.size})", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                BigButton("Tout effacer", Color(0xFF7F1D1D)) { scope.launch { runCatching { account.clearCrashes() }; crashes = emptyList() } }
                BigButton("Retour", DriveColors.Panel) { crashes = null }
            }
            if (crashList.isEmpty()) Text("Aucun plantage remonté. 👍", color = DriveColors.TextSecondary, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp))
            LazyColumn(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(crashList) { c ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp)) {
                        Text("${c.optString("app").uppercase()} ${c.optString("version")} · ${c.optString("device")} · ${c.optString("username").ifBlank { "non connecté" }} · " +
                            java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.FRANCE).format(java.util.Date(c.optLong("at"))),
                            color = DriveColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(c.optString("text").take(1500), color = Color.White, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comptes ROADGUARD", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("↻", DriveColors.Panel) { reload() }
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        val list = users
        val now = System.currentTimeMillis()
        Text(
            if (list == null) "Chargement…" else "${list.size} comptes · ${list.count { it.status.stillActive() }} actifs · " +
                "${list.count { it.status.plan == "trial" && it.status.stillActive() }} en essai · " +
                "${list.count { now - it.lastSeen < 86_400_000L }} vus aujourd'hui · ${list.count { now - it.createdAt < 7 * 86_400_000L }} nouveaux en 7 jours",
            color = DriveColors.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (community != null) BigButton("🛡 Communauté (à valider, comptes)", DriveColors.Accent) { moderation = true }
            BigButton("🐞 Plantages", DriveColors.Track) { scope.launch { runCatching { account.crashes() }.fold({ crashes = it }, { error = it.message }) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Field(contact, { contact = it.take(120) }, "Ton contact affiché aux utilisateurs (tél. ou e-mail)") }
            BigButton("Enregistrer", DriveColors.Accent) {
                scope.launch { info = runCatching { account.setContact(contact); account.refresh() }.fold({ "Contact enregistré" }, { it.message }) }
            }
        }
        info?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp) }
        Field(filter, { filter = it }, "Rechercher un identifiant")
        error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 15.sp) }
        LazyColumn(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list.orEmpty().filter { filter.isBlank() || filter.trim().lowercase() in it.status.username }, key = { it.status.username }) { u ->
                val s = u.status
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.username, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${s.label} · ${u.devices} appareil(s) · créé le ${AccountStatus.date(u.createdAt)}" +
                            (if (u.lastSeen > 0) " · vu le ${AccountStatus.date(u.lastSeen)}" else ""),
                        color = if (s.stillActive()) Color(0xFF81C784) else Color(0xFFFF8A80), fontSize = 14.sp,
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!s.admin) {
                            BigButton("∞ Illimité", DriveColors.Accent) { act(s.username, "unlimited") }
                            BigButton("+1 mois", DriveColors.Track) { act(s.username, "month") }
                            BigButton("+1 an", DriveColors.Track) { act(s.username, "year") }
                            BigButton("Arrêter l'accès", DriveColors.Track) { act(s.username, "expire") }
                            if (s.blocked) BigButton("Débloquer", Color(0xFF2E7D32)) { act(s.username, "unblock") }
                            else BigButton("Bloquer", Color(0xFF7F1D1D)) { act(s.username, "block") }
                        }
                        BigButton("🔑 Mot de passe", DriveColors.Track) { passwordFor = if (passwordFor == s.username) null else s.username; newPassword = "" }
                        BigButton("Déconnecter ses appareils", DriveColors.Track) { act(s.username, "devices") }
                        if (!s.admin) {
                            if (confirmDelete == s.username) BigButton("Confirmer la suppression", Color(0xFFB71C1C)) { confirmDelete = null; act(s.username, "delete") }
                            else BigButton("Supprimer", Color(0xFF7F1D1D)) { confirmDelete = s.username }
                        }
                    }
                    if (passwordFor == s.username) Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { Field(newPassword, { newPassword = it.take(100) }, "Nouveau mot de passe (6 caractères min.)") }
                        BigButton("Valider", DriveColors.Accent) {
                            scope.launch {
                                info = runCatching { account.resetPassword(s.username, newPassword) }
                                    .fold({ passwordFor = null; "Mot de passe de ${s.username} changé : donne-le-lui. Ses appareils sont déconnectés." }, { it.message })
                            }
                        }
                    }
                }
            }
        }
    }
}
