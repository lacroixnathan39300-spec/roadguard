package fr.roadguard.core.account

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.core.community.Community
import fr.roadguard.core.net.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/** État du compte tel que donné par le serveur (gardé sur l'appareil pour rouler sans réseau). */
data class AccountStatus(
    val username: String,
    val admin: Boolean,
    /** trial, month, year, unlimited, expired, blocked, admin */
    val plan: String,
    /** Fin de l'accès en ms ; 0 = illimité. */
    val accessUntil: Long,
    val active: Boolean,
    val blocked: Boolean,
    /** Contact de l'administrateur affiché sur l'écran « accès terminé ». */
    val contact: String = "",
) {
    /** Jours d'essai restants (null si ce n'est pas un essai en cours). */
    fun trialDaysLeft(now: Long = System.currentTimeMillis()): Int? =
        if (plan == "trial" && accessUntil > now) ((accessUntil - now) / 86_400_000L).toInt() else null

    fun stillActive(now: Long = System.currentTimeMillis()) = admin || (!blocked && active && (accessUntil == 0L || accessUntil > now))

    val label: String get() = when {
        admin -> "Administrateur"
        blocked -> "Compte bloqué"
        accessUntil == 0L -> "Accès illimité"
        !stillActive() -> if (plan == "trial" || plan == "expired") "Essai gratuit terminé" else "Accès terminé"
        plan == "trial" -> "Essai gratuit jusqu'au ${date(accessUntil)}"
        else -> "Accès jusqu'au ${date(accessUntil)}"
    }

    fun toJson(): JSONObject = JSONObject().put("username", username).put("admin", admin).put("plan", plan)
        .put("accessUntil", accessUntil).put("active", active).put("blocked", blocked).put("contact", contact)

    companion object {
        fun date(ms: Long): String = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.FRANCE).format(java.util.Date(ms))
        fun from(o: JSONObject) = AccountStatus(
            o.optString("username"), o.optBoolean("admin"), o.optString("plan"), o.optLong("accessUntil"),
            o.optBoolean("active"), o.optBoolean("blocked"), o.optString("contact"),
        )
    }
}

/** Compte vu par l'administrateur. */
data class ManagedUser(val status: AccountStatus, val createdAt: Long, val lastSeen: Long, val devices: Int)

/**
 * Connexion ROADGUARD : identifiant + mot de passe, essai gratuit d'un mois (un seul par appareil),
 * puis accès accordé par l'administrateur depuis son appli. Sans réseau, le dernier état connu sert
 * jusqu'à sa date de fin (on ne bloque pas un conducteur parce qu'il n'a pas de réseau).
 */
class AccountManager private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("account", Context.MODE_PRIVATE)
    private val appName = if (app.packageName.contains("drive")) "drive" else "gps"

    var status by mutableStateOf(prefs.getString("status", null)?.let { runCatching { AccountStatus.from(JSONObject(it)) }.getOrNull() }); private set
    // Jeton observable lui aussi : sinon l'écran de connexion ne voyait pas qu'on venait de se connecter
    // (« token != null && … » ne lisait jamais l'état du compte tant que le jeton était vide).
    private var token by mutableStateOf(prefs.getString("token", null))
    val loggedIn: Boolean get() { val s = status; val t = token; return t != null && s != null }

    @SuppressLint("HardwareIds")
    private val device: String = (Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID) ?: "")
        .takeIf { it.length >= 8 } ?: prefs.getString("device", null) ?: java.util.UUID.randomUUID().toString().replace("-", "").also {
            prefs.edit().putString("device", it).apply()
        }

    /** Accès autorisé : connecté et accès en cours (vérifié par le serveur, ou dernier état connu). */
    val allowed: Boolean get() = testBuild || (loggedIn && status?.stillActive() == true)

    /** Appli des essais automatiques (émulateur) : pas de compte demandé. Jamais vrai dans les applis publiées. */
    val testBuild: Boolean = app.resources.getIdentifier("rg_emulator_test", "bool", app.packageName).let { it != 0 && app.resources.getBoolean(it) }

    suspend fun register(username: String, password: String) = auth("/v1/auth/register", username, password)
    suspend fun login(username: String, password: String) = auth("/v1/auth/login", username, password)

    private suspend fun auth(path: String, username: String, password: String) = withContext(Dispatchers.IO) {
        val o = request("POST", path, JSONObject().put("username", username.trim()).put("password", password).put("device", device).put("app", appName), withToken = false)
        val t = o.getString("token")
        prefs.edit().putString("token", t).apply()
        save(AccountStatus.from(o))
        token = t
    }

    /** Nouvelle vérification auprès du serveur. Faux si le serveur n'a pas pu être joint. */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        if (token == null) return@withContext false
        try {
            save(AccountStatus.from(request("GET", "/v1/auth/status", null)))
            true
        } catch (e: SessionExpired) {
            clear(); true
        } catch (e: Exception) { false }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { request("POST", "/v1/auth/logout", JSONObject()) }
        clear()
    }

    suspend fun changePassword(old: String, new: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/auth/password", JSONObject().put("old", old).put("password", new))
    }

    suspend fun users(): List<ManagedUser> = withContext(Dispatchers.IO) {
        val a: JSONArray = request("GET", "/v1/admin/users", null).optJSONArray("users") ?: JSONArray()
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            ManagedUser(AccountStatus.from(o), o.optLong("createdAt"), o.optLong("lastSeen"), o.optInt("devices"))
        }
    }

    /** unlimited, month, year, trial, expire, block, unblock, devices, delete */
    suspend fun manage(username: String, action: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/admin/users/" + URLEncoder.encode(username, "UTF-8"), JSONObject().put("action", action))
    }

    /** Administrateur : nouveau mot de passe pour un compte (oubli). */
    suspend fun resetPassword(username: String, password: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/admin/users/" + URLEncoder.encode(username, "UTF-8"), JSONObject().put("action", "password").put("password", password))
    }

    suspend fun setContact(contact: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/admin/config", JSONObject().put("contact", contact))
    }

    /** Jeton de session (liaison du compte communauté au compte Pilote). */
    fun sessionToken(): String? = token
    /** Identifiant de l'appareil (preuve d'acceptation de l'avertissement légal). */
    fun deviceId(): String = device

    // ---------- Sauvegarde des réglages dans le compte ----------
    suspend fun saveSettings(data: JSONObject) = withContext(Dispatchers.IO) { request("POST", "/v1/auth/settings", data) }
    suspend fun loadSettings(): JSONObject? = withContext(Dispatchers.IO) { request("GET", "/v1/auth/settings", null).optJSONObject("settings") }

    // ---------- Messagerie avec l'administrateur ----------
    suspend fun sendMessage(text: String, app: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/auth/message", JSONObject().put("text", text).put("app", app))
    }
    suspend fun myMessages(): List<JSONObject> = withContext(Dispatchers.IO) {
        val a = request("GET", "/v1/auth/messages", null).optJSONArray("messages") ?: JSONArray()
        (0 until a.length()).map { a.getJSONObject(it) }
    }
    suspend fun markRepliesSeen() = withContext(Dispatchers.IO) { request("POST", "/v1/auth/messages/seen", JSONObject()) }
    /** Compteurs : réponses non lues, messages non lus (administrateur), dernier message. */
    suspend fun inbox(): JSONObject = withContext(Dispatchers.IO) { request("GET", "/v1/auth/inbox", null) }
    suspend fun adminMessages(): List<JSONObject> = withContext(Dispatchers.IO) {
        val a = request("GET", "/v1/admin/messages", null).optJSONArray("messages") ?: JSONArray()
        (0 until a.length()).map { a.getJSONObject(it) }
    }
    suspend fun markAdminRead() = withContext(Dispatchers.IO) { request("POST", "/v1/admin/messages/read", JSONObject()) }
    suspend fun replyTo(id: Long, text: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/admin/messages/$id/reply", JSONObject().put("text", text))
    }
    suspend fun clearMessages() = withContext(Dispatchers.IO) { request("POST", "/v1/admin/messages/clear", JSONObject()) }

    /** Derniers plantages remontés (administrateur). */
    suspend fun crashes(): List<JSONObject> = withContext(Dispatchers.IO) {
        val a = request("GET", "/v1/admin/crashes", null).optJSONArray("crashes") ?: JSONArray()
        (0 until a.length()).map { a.getJSONObject(it) }
    }

    suspend fun clearCrashes() = withContext(Dispatchers.IO) { request("POST", "/v1/admin/crashes/clear", JSONObject()) }

    private fun save(s: AccountStatus) {
        status = s
        prefs.edit().putString("status", s.toJson().toString()).putLong("checked_at", System.currentTimeMillis()).apply()
    }

    private fun clear() {
        status = null
        token = null
        prefs.edit().remove("token").remove("status").apply()
    }

    private class SessionExpired : IOException("Session expirée")

    private fun request(method: String, path: String, body: JSONObject?, withToken: Boolean = true): JSONObject {
        val t = if (withToken) token ?: throw SessionExpired() else null
        // Jeton dans l'adresse : les requêtes relayées par le téléphone (Link) n'ont pas d'en-têtes personnalisés.
        val url = Community.DEFAULT_URL + path + (t?.let { "?session=$it" } ?: "")
        val r = Net.fetch(url, method, body?.toString()?.toByteArray(), if (body != null) "application/json" else null)
        val o = runCatching { JSONObject(String(r.body)) }.getOrDefault(JSONObject())
        if (r.code == 401 && withToken) throw SessionExpired()
        if (r.code !in 200..299) throw IOException(o.optString("error").ifBlank { "Serveur ROADGUARD indisponible (${r.code})" })
        return o
    }

    companion object {
        @Volatile private var instance: AccountManager? = null
        fun get(context: Context): AccountManager =
            instance ?: synchronized(this) { instance ?: AccountManager(context).also { instance = it } }
    }
}
