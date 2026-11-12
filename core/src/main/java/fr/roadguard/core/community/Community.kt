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
/** [mapFix] : correction de la carte (pas un événement sur la route) : pas d'annonce, question « c'est juste ? ». */
enum class ReportType(val code: String, val label: String, val emoji: String, val color: Long, val mapFix: Boolean = false) {
    POLICE("police", "Police", "👮", 0xFF1E88E5),
    MOBILE("radar_mobile", "Radar mobile", "📸", 0xFFFFB300),
    ACCIDENT("accident", "Accident", "💥", 0xFFE53935),
    JAM("bouchon", "Bouchon", "🚗", 0xFF8E24AA),
    DANGER("danger", "Danger", "⚠️", 0xFFFB8C00),
    WORKS("travaux", "Travaux", "🚧", 0xFFF4511E),
    FIXED("radar_fixe", "Radar fixe", "📷", 0xFFD32F2F),
    MESSAGE("message", "Message (24 h)", "💬", 0xFF00897B),
    LIMIT("limite", "Limitation fausse", "🔢", 0xFF546E7A, mapFix = true),
    ROUNDABOUT("rond_point", "Rond-point manquant", "🔄", 0xFF00838F, mapFix = true),
    ROAD("route", "Route manquante", "🛣", 0xFF6D4C41, mapFix = true);

    /** Radar ou contrôle : affiché, annoncé et signalable seulement avec les alertes radars activées (et jamais en Suisse). */
    val radarLike: Boolean get() = this == POLICE || this == MOBILE || this == FIXED

    companion object { fun of(code: String) = entries.firstOrNull { it.code == code } }
}

data class Report(
    val id: String, val type: ReportType, val lat: Double, val lon: Double, val author: String,
    val createdAt: Long, val yes: Int, val permanent: Boolean, val mine: Boolean, val voted: Boolean,
    /** Texte d'un signalement « Message » (visible 24 h). */
    val text: String? = null,
    /** Fiabilité de l'auteur (étoiles, null = pas encore noté) et personne de confiance. */
    val authorStars: Double? = null,
    val authorTrusted: Boolean = false,
    /** « À confirmer » : auteur nouveau, signalement pas encore vérifié. */
    val pending: Boolean = false,
    /** Limitation corrigée : portion de route (identifiant OpenStreetMap) où elle s'applique. */
    val way: String? = null,
)

/** Limitation corrigée proposée (« 70|90 » → 70), ou null. */
val Report.limitValue: Int? get() = if (type == ReportType.LIMIT) text?.substringBefore('|')?.toIntOrNull() else null

/** Question posée en passant : « Police toujours là ? », ou pour une correction de carte « Limitation à 70 ici, c'est juste ? ». */
fun voteQuestion(r: Report): String = when (r.type) {
    ReportType.LIMIT -> "Limitation à ${r.limitValue ?: "?"} ici, c'est juste ?"
    ReportType.ROUNDABOUT -> "Rond-point absent de la carte ici, c'est vrai ?"
    ReportType.ROAD -> "Route absente de la carte ici, c'est vrai ?"
    else -> "${r.type.label} toujours là ?"
}

data class Driver(val id: String, val name: String, val lat: Double, val lon: Double, val speed: Int?)

data class Rank(val rank: Int, val name: String, val points: Int, val me: Boolean)

/** Question posée en passant près d'un signalement : « toujours là ? ». */
data class VotePrompt(val report: Report, val shownAt: Long)

/**
 * Communauté ROADGUARD façon Waze : signalements autour de soi (mis à jour toutes les 30 s),
 * vérification par les conducteurs, points et classement, conducteurs proches et coucous.
 */
class Community(context: Context, private val scope: CoroutineScope, private val speak: (String) -> Unit) {
    private val appContext = context.applicationContext
    private val prefs = context.applicationContext.getSharedPreferences("community", Context.MODE_PRIVATE)

    var reports by mutableStateOf<List<Report>>(emptyList()); private set
    var drivers by mutableStateOf<List<Driver>>(emptyList()); private set
    /** Bouchons détectés automatiquement (plusieurs conducteurs ROADGUARD au pas au même endroit). */
    var jams by mutableStateOf<List<Driver>>(emptyList()); private set
    var prompt by mutableStateOf<VotePrompt?>(null); private set
    /** Message bref à afficher (coucou reçu, signalement envoyé, erreur). */
    var toast by mutableStateOf<String?>(null); private set
    var name by mutableStateOf(prefs.getString("name", null)); private set
    var points by mutableStateOf(prefs.getInt("points", 0)); private set
    /** Ma fiabilité : étoiles (null tant que pas assez de signalements jugés), niveau, signalements jugés. */
    var myStars by mutableStateOf<Double?>(null); private set
    var myLevel by mutableStateOf("new"); private set
    var myJudged by mutableStateOf(0); private set
    /** Radar déjà connu des bases à cette position (fourni par la session) : radar fixe validé d'office. */
    var knownRadarNear: ((Double, Double) -> Boolean)? = null
    /** Annonce d'un signalement devant soi, selon son type (bouton son : contrôles avec les radars, le reste avec les alertes). */
    var sayReport: ((ReportType, String) -> Unit)? = null
    /** Question « toujours là ? » posée à la voix, avec réponse « oui / non » à la voix. */
    var voiceAnswers = true
    /** Pose une question à la voix puis lance la suite une fois la phrase finie (fourni par la session). */
    var askAloud: ((String, () -> Unit) -> Unit)? = null
    /** Annonce d'un signalement au ton choisi (« Coquin XXL ») : (signalement, mètres) → phrase, ou null pour l'annonce habituelle. */
    var announceAloud: ((Report, Int) -> String?)? = null
    /** Police / radars de la communauté permis : alertes radars activées par l'utilisateur, et jamais en Suisse. */
    var radarAlertsAllowed by mutableStateOf(false)
    /**
     * En Suisse, radars activés : seulement SES PROPRES radars et contrôles (signalements privés, jamais diffusés aux
     * autres : avertir publiquement des contrôles y est puni).
     */
    var ownRadarsOnly by mutableStateOf(false)
    /** Radar ou contrôle de la communauté montrable / annonçable ici. */
    private fun radarVisible(r: Report) = radarAlertsAllowed || (ownRadarsOnly && r.mine)
    /** Radars signalés (mobiles et fixes) annonçables ici, à moins de 3 km : donnés au moteur d'alertes (bips d'approche). */
    fun alertRadars(lat: Double, lon: Double): List<Report> = reports.filter {
        (it.type == ReportType.MOBILE || it.type == ReportType.FIXED) && radarVisible(it) &&
            Geo.distanceMeters(lat, lon, it.lat, it.lon) < 3_000
    }
    /** Signalements à montrer sur la carte : sans radars ni contrôles quand ils ne sont pas permis. */
    val shownReports: List<Report> get() = if (radarAlertsAllowed) reports else reports.filter { !it.type.radarLike || radarVisible(it) }
    /** Portion de route (identifiant OpenStreetMap) et limitation lue sur la carte, fournies par la session. */
    var currentWay: () -> String? = { null }
    var currentLimit: () -> Int? = { null }

    /**
     * Limitation corrigée par la communauté pour la portion de route où l'on roule : validée (pas « à confirmer »)
     * ou la sienne (appliquée tout de suite pour son auteur). null s'il n'y en a pas.
     */
    fun limitFix(way: String?, lat: Double, lon: Double): Int? {
        val fixes = reports.filter { it.type == ReportType.LIMIT && (it.mine || !it.pending) }
        if (fixes.isEmpty()) return null
        val r = (if (way != null) fixes.filter { it.way == way }.maxByOrNull { it.yes + if (it.mine) 1_000 else 0 } else null)
            ?: fixes.firstOrNull { it.way == null && Geo.distanceMeters(lat, lon, it.lat, it.lon) < 40 }
            ?: return null
        return r.limitValue?.takeIf { it in 5..150 }
    }
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
        val js = o.optJSONArray("jams") ?: JSONArray()
        jams = (0 until js.length()).map { i ->
            val j = js.getJSONObject(i)
            Driver("jam$i", "${j.optInt("n")} conducteurs", j.getDouble("lat"), j.getDouble("lon"), j.optInt("speed"))
        }
        val waves = o.optJSONArray("waves") ?: JSONArray()
        if (waves.length() > 0) {
            val from = (0 until waves.length()).map { waves.getJSONObject(it).optString("from_name") }.distinct()
            show("👋 ${from.joinToString(", ")} te fait signe")
            speak("${from.first()} vous fait signe")
        }
        if (!o.isNull("points") && o.has("points")) o.optInt("points").let { points = it; prefs.edit().putInt("points", it).apply() }
        myStars = if (o.isNull("stars") || !o.has("stars")) null else o.optDouble("stars")
        o.optString("level").takeIf { it.isNotBlank() }?.let { myLevel = it }
        myJudged = o.optInt("judged")
        linkOnce()
    }

    /** Annonce des signalements devant soi et question « toujours là ? » en passant à côté. */
    private fun checkNearby(f: Fix) {
        prompt?.let { if (f.timeMs - it.shownAt > 20_000) prompt = null }
        for (r in reports) {
            val d = Geo.distanceMeters(f.lat, f.lon, r.lat, r.lon)
            // Devant et sur l'axe suivi (couloir étroit, pas un cône) : pas les signalements des routes voisines.
            val off = if (f.bearing.isNaN()) 180.0 else Geo.headingDelta(f.bearing, Geo.bearing(f.lat, f.lon, r.lat, r.lon))
            val ahead = off < 60 && d * kotlin.math.sin(Math.toRadians(off)) < 60 + 0.06 * d
            // Police et radar mobile : coupés avec les alertes radars (interrupteur, ou Suisse où c'est interdit).
            val radarLike = r.type == ReportType.POLICE || r.type == ReportType.MOBILE
            if (radarLike && !radarVisible(r)) continue
            // Radars signalés (mobiles et fixes) : annoncés par le moteur d'alertes, avec les bips d'approche.
            if (d < 1000 && ahead && !r.type.mapFix && announced.add(r.id) && r.type != ReportType.FIXED && r.type != ReportType.MOBILE) {
                val meters = ((d + 25) / 50).toInt() * 50
                val where = if (d > 150) "à $meters mètres" else "tout près"
                val msg = r.text
                val text = if (r.type == ReportType.MESSAGE && !msg.isNullOrBlank()) "Message d'un conducteur, $where : $msg"
                    else announceAloud?.invoke(r, meters.coerceAtLeast(50)) ?: "${r.type.label} signalé $where"
                sayReport?.invoke(r.type, text) ?: speak(text)
            }
            if (d < 120 && !r.mine && !r.voted && prompt == null && asked.add(r.id)) {
                prompt = VotePrompt(r, f.timeMs)
                // Question à la voix, réponse « oui » / « non » à la voix : on confirme sans lâcher le volant.
                if (voiceAnswers && r.type != ReportType.MESSAGE) {
                    val question = "${voteQuestion(r)} Réponds oui ou non."
                    val listen: () -> Unit = { VoiceYesNo.ask(appContext, delayMs = 250) { yes -> if (prompt?.report?.id == r.id) answer(yes) } }
                    val ask = askAloud
                    // L'écoute commence une fois la question dite (sinon le micro entend « oui ou non »).
                    if (ask != null) ask(question, listen)
                    else { speak(question); VoiceYesNo.ask(appContext, delayMs = 3_500) { yes -> if (prompt?.report?.id == r.id) answer(yes) } }
                }
            }
        }
    }

    fun answer(still: Boolean) {
        val p = prompt ?: return
        prompt = null
        scope.launch {
            runCatching { ensureAccount() }
            runCatching { post("/v1/reports/${p.report.id}/vote", JSONObject().put("still", still)) }
                .onSuccess { show(if (still) "Merci, signalement confirmé (+1 point)" else "Merci, c'est noté (+1 point)") }
            runCatching { refresh() }
        }
    }

    fun report(type: ReportType, text0: String? = null) {
        // En Suisse (radars activés) : signalement privé, gardé pour soi seul.
        val priv = type.radarLike && !radarAlertsAllowed && ownRadarsOnly
        if (type.radarLike && !radarAlertsAllowed && !priv) {
            show("Radars et contrôles : signalement impossible (alertes radars coupées, ou mode Suisse actif)")
            return
        }
        val f = here ?: return show("Position GPS indisponible : signalement non envoyé")
        // Limitation corrigée : « nouvelle|ancienne » et la portion de route où l'on roule.
        val text = if (type == ReportType.LIMIT) "${text0?.toIntOrNull() ?: return show("Choisis la bonne limitation")}|${currentLimit() ?: 0}" else text0
        val way = if (type == ReportType.LIMIT) currentWay() else null
        scope.launch {
            // Pas encore de pseudo communauté sur cet appareil : on le crée avec l'identifiant Pilote,
            // au lieu de refuser le signalement (c'était la cause des signalements « perdus »).
            if (!hasAccount) {
                val auto = fr.roadguard.core.account.AccountManager.get(appContext).status?.username?.takeIf { it.length >= 2 } ?: "Pilote"
                if (setName(auto).isFailure) return@launch show("Signalement impossible : pas de connexion au serveur")
            }
            runCatching {
                post("/v1/reports", JSONObject().put("type", type.code).put("lat", f.lat).put("lon", f.lon)
                    .apply {
                        if (!f.bearing.isNaN()) put("heading", f.bearing.toInt()); if (text != null) put("text", text)
                        if (way != null) put("way", way)
                        if (priv) put("private", true)
                        // Radar fixe déjà dans les bases (officielle, MapAtlas, OpenStreetMap) : validé d'office.
                        if (type == ReportType.FIXED && knownRadarNear?.invoke(f.lat, f.lon) == true) put("known", true)
                    })
            }.onSuccess { r ->
                // Affiché tout de suite sur la carte, sans attendre la prochaine mise à jour.
                val id = r.optString("id")
                if (r.optBoolean("created") && id.isNotBlank() && reports.none { it.id == id }) {
                    reports = reports + Report(id = id, type = type, lat = f.lat, lon = f.lon, author = name.orEmpty(),
                        createdAt = System.currentTimeMillis(), yes = 0, permanent = r.optBoolean("permanent"), mine = true, voted = false, text = text, way = way)
                }
                show(when {
                    r.optBoolean("private") -> "${type.label} gardé pour toi seul (en Suisse, il n'est montré à personne d'autre)"
                    type == ReportType.MESSAGE -> "Message publié pour 24 h, merci ! +5 points"
                    type.mapFix && r.optString("status") == "pending" -> "Correction envoyée : publiée quand d'autres conducteurs l'auront confirmée (+5 points)"
                    type.mapFix && r.optBoolean("created") -> "Correction publiée, merci ! " +
                        (if (type == ReportType.LIMIT) "Elle s'applique déjà pour toi. " else "") + "+5 points"
                    type.mapFix -> "Déjà signalée : ta confirmation est comptée"
                    r.optString("status") == "pending" -> "${type.label} signalé, à confirmer par d'autres conducteurs (+5 points)"
                    r.optString("status") == "hidden" -> "${type.label} signalé : il sera visible une fois confirmé"
                    r.optBoolean("permanent") -> "${type.label} signalé, il reste affiché, merci ! +5 points"
                    r.optBoolean("created") -> "${type.label} signalé, merci ! +5 points"
                    else -> "Déjà signalé : ta confirmation est comptée"
                })
            }.onFailure { show("Signalement impossible : ${it.message}"); speak("Signalement non envoyé") }
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

    /** Relie (une fois) le compte communauté au compte Pilote : l'administrateur devient « de confiance ». */
    private fun linkOnce() {
        if (!hasAccount || prefs.getBoolean("linked", false)) return
        val token = fr.roadguard.core.account.AccountManager.get(appContext).sessionToken() ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { request("POST", "/v1/account/link?session=$token", JSONObject(), auth = true) }
                .onSuccess { prefs.edit().putBoolean("linked", true).apply() }
        }
    }

    // ---------- Modération (administrateur) ----------
    suspend fun modPending(): List<JSONObject> = withContext(Dispatchers.IO) {
        val a = request("GET", "/v1/mod/pending", null, auth = true).optJSONArray("reports") ?: JSONArray()
        (0 until a.length()).map { a.getJSONObject(it) }
    }
    suspend fun modReport(id: String, validate: Boolean) = withContext(Dispatchers.IO) {
        request("POST", "/v1/mod/report/$id", JSONObject().put("action", if (validate) "validate" else "reject"), auth = true)
    }
    suspend fun modAccounts(): List<JSONObject> = withContext(Dispatchers.IO) {
        val a = request("GET", "/v1/mod/accounts", null, auth = true).optJSONArray("accounts") ?: JSONArray()
        (0 until a.length()).map { a.getJSONObject(it) }
    }
    suspend fun modAccount(id: String, action: String) = withContext(Dispatchers.IO) {
        request("POST", "/v1/mod/account/$id", JSONObject().put("action", action), auth = true)
    }
    /** Signalements « police » suspendus jusqu'à (ms), 0 sinon (interdiction préfectorale). */
    suspend fun modPoliceUntil(): Long = withContext(Dispatchers.IO) {
        request("GET", "/v1/mod/police", null, auth = true).optLong("until")
    }
    /** Suspend les signalements « police » pour [hours] heures (0 : rétablir). */
    suspend fun modSetPolice(hours: Double): Long = withContext(Dispatchers.IO) {
        request("POST", "/v1/mod/police", JSONObject().put("hours", hours), auth = true).optLong("until")
    }
    suspend fun modLink() { prefs.edit().putBoolean("linked", false).apply(); ensureAccount(); linkOnce() }

    /** Pseudo communauté créé au besoin avec l'identifiant Pilote (convoi, partage). */
    private suspend fun ensureAccount() {
        if (hasAccount) return
        val auto = fr.roadguard.core.account.AccountManager.get(appContext).status?.username?.takeIf { it.length >= 2 } ?: "Pilote"
        setName(auto).getOrThrow()
    }

    // ---------- Convoi entre amis ----------
    var convoyCode by androidx.compose.runtime.mutableStateOf<String?>(prefs.getString("convoy", null)); private set
    var convoy by androidx.compose.runtime.mutableStateOf<List<Driver>>(emptyList()); private set
    private val heardMessages = HashSet<String>()
    private var convoyJob: Job? = null
    private var pendingConvoyMessage: String? = null

    fun joinConvoy(code: String) {
        val c = code.uppercase().filter { it.isLetterOrDigit() }.take(8)
        if (c.length < 4) return show("Code de convoi : 4 à 8 lettres ou chiffres")
        convoyCode = c; prefs.edit().putString("convoy", c).apply()
        startConvoyLoop()
        show("Convoi $c rejoint : partage ce code avec tes amis")
    }
    fun leaveConvoy() {
        val c = convoyCode ?: return
        convoyCode = null; convoy = emptyList(); prefs.edit().remove("convoy").apply()
        convoyJob?.cancel(); convoyJob = null
        scope.launch(Dispatchers.IO) { runCatching { request("POST", "/v1/convoy/leave", JSONObject().put("code", c), auth = true) } }
    }
    fun convoyMessage(text: String) { pendingConvoyMessage = text; show("Message envoyé au convoi") }
    fun startConvoyLoop() {
        if (convoyCode == null || convoyJob?.isActive == true) return
        convoyJob = scope.launch {
            while (convoyCode != null) {
                runCatching {
                    ensureAccount()
                    val f = here
                    val body = JSONObject().put("code", convoyCode)
                    if (f != null) body.put("lat", f.lat).put("lon", f.lon).put("speed", f.speedKmh.toInt())
                    pendingConvoyMessage?.let { body.put("message", it); pendingConvoyMessage = null }
                    val o = withContext(Dispatchers.IO) { request("POST", "/v1/convoy", body, auth = true) }
                    val me = o.optString("me")
                    val a = o.optJSONArray("members") ?: JSONArray()
                    convoy = (0 until a.length()).mapNotNull { i ->
                        val m = a.getJSONObject(i)
                        val id = m.optString("account_id")
                        // Message d'un ami : dit une fois.
                        val msg = m.optString("message").takeIf { it.isNotBlank() && it != "null" }
                        val key = "$id:${m.optLong("message_at")}"
                        if (msg != null && id != me && heardMessages.add(key) && System.currentTimeMillis() - m.optLong("message_at") < 5 * 60_000) {
                            speak("Convoi, ${m.optString("name")} : $msg")
                        }
                        if (id == me || m.isNull("lat")) null
                        else Driver("convoy-$id", "🚗 " + m.optString("name"), m.getDouble("lat"), m.getDouble("lon"), m.optInt("speed").takeIf { it > 0 })
                    }
                }
                delay(10_000)
            }
        }
    }

    // ---------- Partage de trajet en direct ----------
    var shareLink by androidx.compose.runtime.mutableStateOf<String?>(null); private set
    private var shareToken: String? = null
    private var shareJob: Job? = null
    /** Démarre le partage : renvoie le lien à envoyer. [info] donne (destination, arrivée prévue en ms). */
    suspend fun startShare(info: () -> Pair<String?, Long?>): Result<String> = runCatching {
        ensureAccount()
        val o = withContext(Dispatchers.IO) { request("POST", "/v1/share/start", JSONObject(), auth = true) }
        val t = o.getString("token")
        shareToken = t
        val link = baseUrl.trimEnd('/') + "/s/" + t
        shareLink = link
        shareJob?.cancel()
        shareJob = scope.launch {
            while (shareToken == t) {
                runCatching {
                    val f = here
                    val (dest, eta) = info()
                    val body = JSONObject().put("token", t)
                    if (f != null) body.put("lat", f.lat).put("lon", f.lon).put("speed", f.speedKmh.toInt())
                    dest?.let { body.put("dest", it) }; eta?.let { body.put("eta", it) }
                    withContext(Dispatchers.IO) { request("POST", "/v1/share/update", body, auth = true) }
                }
                delay(15_000)
            }
        }
        link
    }
    fun stopShare() {
        val t = shareToken ?: return
        shareToken = null; shareLink = null; shareJob?.cancel()
        scope.launch(Dispatchers.IO) { runCatching { request("POST", "/v1/share/update", JSONObject().put("token", t).put("stop", true), auth = true) } }
    }

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
            text = o.optString("text").takeIf { !o.isNull("text") && it.isNotBlank() },
            authorStars = if (o.isNull("author_stars") || !o.has("author_stars")) null else o.optDouble("author_stars"),
            authorTrusted = o.optBoolean("author_trusted"),
            pending = o.optString("status") == "pending",
            way = o.optString("way").takeIf { !o.isNull("way") && it.isNotBlank() },
        )
    }

    companion object {
        /** Adresse du serveur ROADGUARD (Cloudflare), renseignée après sa mise en ligne. */
        const val DEFAULT_URL = "https://roadguard.roadguard-app.workers.dev"
    }
}
