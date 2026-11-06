package fr.roadguard.core.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import fr.roadguard.core.account.AccountManager
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Notifications des messages : nouveau message (administrateur) ou réponse reçue (utilisateur). */
object InboxNotifier {
    private const val CHANNEL = "pilote_messages"
    /** Nombre de messages non lus pour l'administrateur (affiché sur le bouton Administration). */
    var adminUnread = androidx.compose.runtime.mutableIntStateOf(0)
    var repliesUnread = androidx.compose.runtime.mutableIntStateOf(0)

    suspend fun check(context: Context) {
        val account = AccountManager.get(context)
        if (!account.loggedIn) return
        val o = runCatching { account.inbox() }.getOrNull() ?: return
        val prefs = context.getSharedPreferences("inbox", Context.MODE_PRIVATE)
        val replies = o.optInt("replies"); val admin = o.optInt("admin"); val last = o.optLong("adminLast")
        repliesUnread.intValue = replies; adminUnread.intValue = admin
        if (replies > 0 && prefs.getInt("replies_notified", 0) != replies) {
            prefs.edit().putInt("replies_notified", replies).apply()
            notify(context, 4301, "Réponse de l'administrateur", "Tu as $replies réponse(s) : Réglages → Écrire à l'administrateur.")
        }
        if (replies == 0) prefs.edit().putInt("replies_notified", 0).apply()
        if (admin > 0 && last > prefs.getLong("admin_last_notified", 0)) {
            prefs.edit().putLong("admin_last_notified", last).apply()
            notify(context, 4302, "Nouveau message pour l'administrateur", "$admin message(s) non lu(s) : Réglages → Administration → Messages.")
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String) = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@runCatching
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH))
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        nm.notify(id, Notification.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true).apply { open?.let { setContentIntent(it) } }.build())
    }
}

private fun time(ms: Long) = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.FRANCE).format(java.util.Date(ms))

@Composable
private fun Bubble(text: String, mine: Boolean, meta: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(16.dp))
            .background(if (mine) Color(0xFF1565C0) else DriveColors.Panel).padding(12.dp)) {
            Text(text, color = Color.White, fontSize = 17.sp)
            Text(meta, color = Color(0xB3FFFFFF), fontSize = 12.sp)
        }
    }
}

@Composable
private fun MessageField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier = modifier, label = { Text(label) }, maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
            focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary))
}

/** « Écrire à l'administrateur » : conversation (mes messages, ses réponses) et envoi. */
@Composable
fun ContactAdminScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val account = remember { AccountManager.get(context) }
    val scope = rememberCoroutineScope()
    val list = remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    val draft = remember { mutableStateOf("") }
    val info = remember { mutableStateOf<String?>(null) }
    suspend fun load() {
        runCatching { account.myMessages() }.onSuccess { list.value = it.reversed() }.onFailure { info.value = it.message }
        runCatching { account.markRepliesSeen() }; InboxNotifier.repliesUnread.intValue = 0
    }
    LaunchedEffect(Unit) { load() }
    val app = UpdateNotifier.appKey(context)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✉ Écrire à l'administrateur", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                BigButton("Fermer", DriveColors.Panel, onClose)
            }
            Text("Une question, un bug, une idée ? Ta réponse arrive ici, avec une notification.", color = DriveColors.TextSecondary, fontSize = 15.sp)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.value.isEmpty()) item { Text("Aucun message pour l'instant.", color = DriveColors.TextSecondary, fontSize = 16.sp) }
                items(list.value) { m ->
                    Bubble(m.optString("text"), mine = true, meta = "Moi · ${time(m.optLong("at"))}")
                    val reply = m.optString("reply").takeIf { it.isNotBlank() && it != "null" }
                    if (reply != null) Bubble(reply, mine = false, meta = "Administrateur · ${time(m.optLong("reply_at"))}")
                }
            }
            info.value?.let { Text(it, color = DriveColors.Accent, fontSize = 15.sp) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageField(draft.value, { draft.value = it.take(2000) }, "Ton message", Modifier.weight(1f))
                BigButton("Envoyer", DriveColors.Accent) {
                    val t = draft.value.trim()
                    if (t.isEmpty()) return@BigButton
                    scope.launch {
                        runCatching { account.sendMessage(t, app) }
                            .onSuccess { draft.value = ""; info.value = "Message envoyé ✓"; load() }
                            .onFailure { info.value = "Envoi impossible : ${it.message}" }
                    }
                }
            }
        }
    }
}

/** Messages reçus par l'administrateur, avec réponse à chacun. */
@Composable
fun AdminMessagesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val account = remember { AccountManager.get(context) }
    val scope = rememberCoroutineScope()
    val list = remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    val drafts = remember { mutableStateOf(mapOf<Long, String>()) }
    val info = remember { mutableStateOf<String?>(null) }
    suspend fun load() {
        runCatching { account.adminMessages() }.onSuccess { list.value = it }.onFailure { info.value = it.message }
        runCatching { account.markAdminRead() }; InboxNotifier.adminUnread.intValue = 0
    }
    LaunchedEffect(Unit) { load() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✉ Messages (${list.value.size})", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                BigButton("Tout effacer", Color(0xFF7F1D1D)) { scope.launch { runCatching { account.clearMessages() }; load() } }
                BigButton("Fermer", DriveColors.Panel, onClose)
            }
            info.value?.let { Text(it, color = DriveColors.Accent, fontSize = 15.sp) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (list.value.isEmpty()) item { Text("Aucun message reçu.", color = DriveColors.TextSecondary, fontSize = 16.sp) }
                items(list.value, key = { it.optLong("id") }) { m ->
                    val id = m.optLong("id")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(DriveColors.Panel).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${if (m.optInt("read_admin") == 0) "🔵 " else ""}${m.optString("username")} · ${m.optString("app").uppercase()} · ${time(m.optLong("at"))}",
                            color = DriveColors.TextSecondary, fontSize = 13.sp)
                        Text(m.optString("text"), color = Color.White, fontSize = 17.sp)
                        val reply = m.optString("reply").takeIf { it.isNotBlank() && it != "null" }
                        if (reply != null) Text("↳ Ta réponse : $reply", color = Color(0xFF9FD3FF), fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MessageField(drafts.value[id].orEmpty(), { drafts.value = drafts.value + (id to it.take(2000)) },
                                if (reply == null) "Répondre" else "Nouvelle réponse", Modifier.weight(1f))
                            BigButton("Envoyer", DriveColors.Accent) {
                                val t = drafts.value[id].orEmpty().trim()
                                if (t.isEmpty()) return@BigButton
                                scope.launch {
                                    runCatching { account.replyTo(id, t) }
                                        .onSuccess { drafts.value = drafts.value - id; info.value = "Réponse envoyée ✓"; load() }
                                        .onFailure { info.value = "Envoi impossible : ${it.message}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
