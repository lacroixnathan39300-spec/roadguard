package fr.roadguard.core.community

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.core.net.Net
import fr.roadguard.core.radar.Fix
import fr.roadguard.core.radar.Geo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/** Types de signalement (mêmes codes que le serveur). */
enum class ReportType(val code: String, val label: String, val emoji: String, val color: Long) {
    POLICE("police", "Police", "👮", 0xFF1E88E5),
    MOBILE("radar_mobile", "Radar mobile", "📸", 0xFFFFB300),
    ACCIDENT("accident", "Accident", "💥", 0xFFE53935),
    JAM("bouchon", "Bouchon", "🚗", 0xFF8E24AA),
    DANGER("danger", "Danger", "⚠️", 0xFFFB8C00),
    WORKS("travaux", "Travaux", "🚧", 0xFFF4511E),
    FIXED("radar_fixe", "Radar fixe", "📷", 0xFFD32F2F);

    companion object { fun of(code: String) = entries.firstOrNull { it.code == code } }
}

data class Report(
    val id: String, val type: ReportType, val lat: Double, val lon: Double, val author: String,
    val createdAt: Long, val yes: Int, val permanent: Boolean, val mine: Boolean, val voted: Boolean,
)

data class Driver(val id: String, val name: String, val lat: Double, val lon: Double, val speed: Int?)

data class Rank(val rank: Int, val name: String, val points: Int, val me: Boolean)

/** Question posée en passant près d'un signalement : « toujours là ? ». */
data class VotePrompt(val report: Report, val shownAt: Long)

/**
 * Communauté ROADGUARD façon Waze : signalements autour de soi (mis à jour toutes les 30 s),
 * vérification par les conducteurs, points et classement, conducteurs proches et coucous.
 */
class Community(context: Context, private val scope: CoroutineScope, private val speak: (String) -> Unit) {
    private val prefs = context.applicationContext.getSharedPreferences("community", Context.MODE_PRIVATE)

    var reports by mutableStateOf<List<Report>>(emptyList()); private set
    var drivers by mutableStateOf<List<Driver>>(emptyList()); private set
    var prompt by mutableStateOf<VotePrompt?>(null); private set
    /** Message bref à afficher (coucou reçu, signalement envoyé, erreur). */
    var toast by mutableStateOf<String?>(null); private set
    var name by mutableStateOf(prefs.getString("name", null)); private set
    var points by mutableStateOf(prefs.getInt("points", 0)); private set
    var shareLocation by mutableStateOf(prefs.getBoolean("share", false)); private set

    val baseUrl: String get() = prefs.getString("url", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_URL
    val available: Boolean get() = baseUrl.isNotBlank()
    val hasAccount: Boolean get() = prefs.getString("id", null) != null

    private var here: Fix? = null
    private var loop: Job? = null
    private val announced = HashSet<String>()
    private val asked = HashSet<String>()

    fun onFix(f: Fix) {
        here = f
        if (loop == null && available) loop = scope.launch { refreshLoop() }
        checkNearby(f)
    }

    private suspend fun refreshLoop() {
        var lastMoving = 0L
        while (scope.isActive) {
            val f = here
            if (f != null && f.speedKmh > 5) lastMoving = System.currentTimeMillis()
            // Une requête par minute en roulant (ou dans les 5 min après l'arrêt), aucune sinon.
            if (f != null && System.currentTimeMillis() - lastMoving < 5 * 60_000 || reports.isEmpty()) runCatching { refresh() }
            delay(60_000)
        }
    }

    private suspend fun refresh() {
        val f = here ?: return
        val body = JSONObject().put("lat", f.lat).put("lon", f.lon).put("radius", 30000)
            .put("speed", f.speedKmh.toInt()).apply { if (!f.bearing.isNaN()) put("heading", f.bearing.toInt()) }
        if (hasAccount) body.put("share", shareLocation)
        val o = withContext(Dispatchers.IO) { request("POST", "/v1/sync", body, auth = hasAccount) }
        val list = o.optJSONArray("reports") ?: JSONArray()
        reports = (0 until list.length()).mapNotNull { i -> parseReport(list.getJSONObject(i)) }
        val ds = o.optJSONArray("drivers") ?: JSONArray()
        drivers = (0 until ds.length()).map { i ->
            val d = ds.getJSONObject(i)
            Driver(d.getString("id"), d.optString("name"), d.getDouble("lat"), d.getDouble("lon"), d.optInt("speed").takeIf { it > 0 })
        }
        val waves = o.optJSONArray("waves") ?: JSONArray()
        if (waves.length() > 0) {
            val from = (0 until waves.length()).map { waves.getJSONObject(it).optString("from_name") }.distinct()
            show("👋 ${from.joinToString(", ")} te fait signe")
            speak("${from.first()} vous fait signe")
        }
        if (!o.isNull("points") && o.has("points")) o.optInt("points").let { points = it; prefs.edit().putInt("points", it).apply() }
    }

    /** Annonce des signalements devant soi et question « toujours là ? » en passant à côté. */
    private fun checkNearby(f: Fix) {
        prompt?.let { if (f.timeMs - it.shownAt > 20_000) prompt = null }
        for (r in reports) {
            val d = Geo.distanceMeters(f.lat, f.lon, r.lat, r.lon)
            val ahead = !f.bearing.isNaN() && Geo.headingDelta(f.bearing, Geo.bearing(f.lat, f.lon, r.lat, r.lon)) < 40
            if (d < 600 && ahead && announced.add(r.id) && r.type != ReportType.FIXED) {
                speak("${r.type.label} signalé ${if (d > 150) "à ${((d + 25) / 50).toInt() * 50} mètres" else "tout près"}")
            }
            if (d < 120 && !r.mine && !r.voted && hasAccount && prompt == null && asked.add(r.id)) {
                prompt = VotePrompt(r, f.timeMs)
            }
        }
    }

    fun answer(still: Boolean) {
        val p = prompt ?: return
        prompt = null
        scope.launch {
            runCatching { post("/v1/reports/${p.report.id}/vote", JSONObject().put("still", still)) }
                .onSuccess { show(if (still) "Merci, signalement confirmé (+1 point)" else "Merci, c'est noté (+1 point)") }
            runCatching { refresh() }
        }
    }

    fun report(type: ReportType) {
        val f = here ?: return show("Position GPS indisponible")
        if (!hasAccount) return show("Choisis d'abord ton pseudo dans Menu → Communauté")
        scope.launch {
            runCatching {
                post("/v1/reports", JSONObject().put("type", type.code).put("lat", f.lat).put("lon", f.lon)
                    .apply { if (!f.bearing.isNaN()) put("heading", f.bearing.toInt()) })
            }.onSuccess { r ->
                show(if (r.optBoolean("created")) "${type.label} signalé, merci ! +5 points" else "Déjà signalé : ta confirmation est comptée")
            }.onFailure { show("Signalement impossible : ${it.message}") }
            runCatching { refresh() }
        }
    }

    /** Crée le compte (ou change le pseudo). */
    suspend fun setName(newName: String): Result<String> = runCatching {
        val clean = newName.trim()
        if (!hasAccount) {
            val o = withContext(Dispatchers.IO) { request("POST", "/v1/account", JSONObject().put("name", clean), auth = false) }
            prefs.edit().putString("id", o.getString("id")).putString("secret", o.getString("secret")).apply()
        } else post("/v1/account/name", JSONObject().put("name", clean))
        prefs.edit().putString("name", clean).apply()
        name = clean
        clean
    }

    fun changeShare(v: Boolean) {
        shareLocation = v
        prefs.edit().putBoolean("share", v).apply()
        if (!v) { drivers = emptyList(); scope.launch { runCatching { post("/v1/presence", JSONObject().put("visible", false)) } } }
        else scope.launch { runCatching { refresh() } }
    }

    fun wave(d: Driver) {
        scope.launch {
            runCatching { post("/v1/wave", JSONObject().put("to", d.id)) }
                .onSuccess { show(if (it.optBoolean("sent")) "👋 Coucou envoyé à ${d.name}" else "Tu as déjà fait signe à ${d.name}") }
        }
    }

    suspend fun leaderboard(week: Boolean): List<Rank> = withContext(Dispatchers.IO) {
        val a = getJson("/v1/leaderboard?period=${if (week) "week" else "all"}").optJSONArray("list") ?: JSONArray()
        (0 until a.length()).map { val o = a.getJSONObject(it); Rank(o.getInt("rank"), o.getString("name"), o.getInt("points"), o.optBoolean("me")) }
    }

    fun dismissToast() { toast = null }

    private fun show(text: String) {
        toast = text
        scope.launch { delay(5000); if (toast == text) toast = null }
    }

    // ---------- HTTP ----------

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) { request("POST", path, body, auth = true) }

    private fun getJson(path: String): JSONObject = request("GET", path, null, auth = true)

    private fun request(method: String, path: String, body: JSONObject?, auth: Boolean): JSONObject {
        val key = if (auth) prefs.getString("id", null)?.let { id -> "$id.${prefs.getString("secret", "")}" } else null
        val sep = if ('?' in path) '&' else '?'
        val url = baseUrl.trimEnd('/') + path + (key?.let { "${sep}key=${URLEncoder.encode(it, "UTF-8")}" } ?: "")
        val r = Net.fetch(url, method, body?.toString()?.toByteArray(), if (body != null) "application/json" else null)
        val o = runCatching { JSONObject(String(r.body)) }.getOrDefault(JSONObject())
        if (r.code !in 200..299) throw IOException(o.optString("error").ifBlank { "serveur indisponible (${r.code})" })
        return o
    }

    private fun parseReport(o: JSONObject): Report? {
        val type = ReportType.of(o.optString("type")) ?: return null
        return Report(
            id = o.getString("id"), type = type, lat = o.getDouble("lat"), lon = o.getDouble("lon"),
            author = o.optString("author"), createdAt = o.optLong("created_at"), yes = o.optInt("yes"),
            permanent = o.optInt("permanent") == 1, mine = o.optInt("mine") == 1, voted = !o.isNull("my_vote") && o.has("my_vote"),
        )
    }

    companion object {
        /** Adresse du serveur ROADGUARD (Cloudflare), renseignée après sa mise en ligne. */
        const val DEFAULT_URL = ""
    }
}
