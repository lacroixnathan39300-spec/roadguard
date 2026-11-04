package fr.roadguard.drive.link

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import fr.roadguard.drive.media.NowPlaying
import fr.roadguard.link.LinkProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** Message ou appel reçu sur le téléphone. [actions] : boutons proposés (vide = pas de bouton). */
data class PhoneNotification(
    val key: String,
    val isCall: Boolean,
    val app: String,
    val title: String,
    val text: String,
    val actions: List<String>,
)

/** État de la liaison avec le téléphone, affiché par DRIVE. */
sealed class LinkState(val label: String) {
    object Off : LinkState("Téléphone non associé")
    object Searching : LinkState("Recherche du téléphone…")
    class Connected(val phone: String) : LinkState("Connecté à $phone")
    class Failed(message: String) : LinkState(message)
}

/**
 * ROADGUARD Link côté autoradio : retrouve le téléphone (passerelle du partage de connexion,
 * ou annonce réseau), s'authentifie, reçoit position et musique, envoie les commandes.
 * Se reconnecte tout seul tant qu'un téléphone est associé.
 */
class LinkClient(private val context: Context) {
    private val prefs = context.getSharedPreferences("link", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var session: LinkProtocol.Session? = null
    @Volatile private var pairingCode: String? = null

    private val _state = MutableStateFlow<LinkState>(if (isPaired()) LinkState.Searching else LinkState.Off)
    val state: StateFlow<LinkState> = _state
    private val _locations = MutableSharedFlow<Location>(extraBufferCapacity = 8)
    /** Positions GPS du téléphone. */
    val locations: SharedFlow<Location> = _locations
    private val _media = MutableStateFlow<NowPlaying?>(null)
    /** Musique en cours sur le téléphone. */
    val media: StateFlow<NowPlaying?> = _media

    private val _notifications = MutableStateFlow<List<PhoneNotification>>(emptyList())
    /** Notifications en cours (la plus récente en premier). */
    val notifications: StateFlow<List<PhoneNotification>> = _notifications
    private val _destinations = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Texte de destination partagé depuis le téléphone. */
    val destinations: SharedFlow<String> = _destinations

    /** Mode miroir : taille de l'image du téléphone, ou null si l'écran n'est pas partagé. */
    private val _mirror = MutableStateFlow<Pair<Int, Int>?>(null)
    val mirror: StateFlow<Pair<Int, Int>?> = _mirror
    /** Reçoit les données vidéo (type, octets) ; branché par l'écran miroir. */
    @Volatile var videoSink: ((Int, ByteArray) -> Unit)? = null

    fun requestKeyFrame() = send(JSONObject().put("t", "mirror_keyframe"))
    fun stopMirror() { send(JSONObject().put("t", "mirror_stop")); _mirror.value = null }
    fun touch(action: String, x: Float, y: Float) =
        send(JSONObject().put("t", "touch").put("a", action).put("x", x.toDouble()).put("y", y.toDouble())
            .put("time", android.os.SystemClock.uptimeMillis()))

    fun dismissNotification(key: String) { _notifications.value = _notifications.value.filterNot { it.key == key } }

    fun notificationAction(key: String, index: Int) {
        send(JSONObject().put("t", "notif_action").put("key", key).put("index", index))
        dismissNotification(key)
    }

    val phoneName: String? get() = prefs.getString("phone_name", null)
    fun isPaired(): Boolean = prefs.getString("secret", null) != null

    private val driveId: String
        get() = prefs.getString("drive_id", null) ?: LinkProtocol.b64(LinkProtocol.random(9)).also {
            prefs.edit().putString("drive_id", it).apply()
        }

    /** Démarre (ou relance) la boucle de connexion si un téléphone est associé. */
    fun start() {
        if (job?.isActive == true || !isPaired() && pairingCode == null) return
        job = scope.launch { loop() }
    }

    /** Nouveau code à afficher ; la prochaine connexion servira à l'association. */
    fun beginPairing(): String {
        val code = LinkProtocol.newPairingCode()
        pairingCode = code
        return code
    }

    /** Lancer la connexion d'association après saisie du code sur le téléphone. */
    fun connectForPairing() {
        job?.cancel()
        closeSession()
        job = scope.launch { loop() }
    }

    fun cancelPairing() { pairingCode = null; if (!isPaired()) { job?.cancel(); _state.value = LinkState.Off } }

    fun forget() {
        job?.cancel()
        closeSession()
        prefs.edit().remove("secret").remove("phone_id").remove("phone_name").apply()
        pairingCode = null
        _media.value = null
        _state.value = LinkState.Off
    }

    fun mediaCommand(cmd: String) = send(JSONObject().put("t", "media_cmd").put("cmd", cmd))

    /** Envois dans l'ordre (le tactile du mode miroir en dépend), sans bloquer l'interface. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val sendDispatcher = Dispatchers.IO.limitedParallelism(1)

    private fun send(m: JSONObject) {
        val s = session ?: return
        scope.launch(sendDispatcher) { runCatching { s.send(m) }.onFailure { closeSession() } }
    }

    private suspend fun loop() {
        var failures = 0
        while (scope.isActive && (isPaired() || pairingCode != null)) {
            _state.value = LinkState.Searching
            val host = findPhone()
            if (host == null) { delay(backoff(failures++)); continue }
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, LinkProtocol.PORT), 5_000)
                    socket.soTimeout = 20_000
                    socket.tcpNoDelay = true
                    val secret = prefs.getString("secret", null)?.let(LinkProtocol::unb64)
                    val code = if (secret == null) pairingCode else null
                    val s = LinkProtocol.clientHandshake(socket.getInputStream(), socket.getOutputStream(),
                        driveId, "ROADGUARD DRIVE (${Build.MODEL})", secret, code)
                    if (secret == null) {
                        prefs.edit().putString("secret", LinkProtocol.b64(s.secret)).putString("phone_id", s.peerId)
                            .putString("phone_name", s.peerName).apply()
                        pairingCode = null
                    }
                    session = s
                    failures = 0
                    _state.value = LinkState.Connected(s.peerName.ifBlank { "téléphone" })
                    val pinger = scope.launch { while (isActive) { delay(5_000); runCatching { s.send(JSONObject().put("t", "ping")) } } }
                    try {
                        while (scope.isActive) {
                            val msg = s.receiveAny()
                            if (msg.json != null) handle(msg.json) else videoSink?.invoke(msg.type, msg.data)
                        }
                    } finally {
                        pinger.cancel()
                    }
                }
            } catch (e: LinkProtocol.AuthException) {
                _state.value = LinkState.Failed(e.message ?: "Association refusée")
                if (pairingCode != null) { pairingCode = null; return }
                delay(backoff(failures++))
            } catch (e: Exception) {
                _state.value = LinkState.Failed("Liaison perdue, nouvelle tentative…")
                delay(backoff(failures++))
            } finally {
                session = null
                _media.value = null
                _notifications.value = emptyList()
                _mirror.value = null
            }
        }
    }

    private fun handle(m: JSONObject) {
        when (m.optString("t")) {
            "loc" -> _locations.tryEmit(Location(LocationManager.GPS_PROVIDER).apply {
                latitude = m.getDouble("lat"); longitude = m.getDouble("lon")
                time = System.currentTimeMillis()
                if (m.has("speed")) speed = m.getDouble("speed").toFloat()
                if (m.has("bearing")) bearing = m.getDouble("bearing").toFloat()
                if (m.has("acc")) accuracy = m.getDouble("acc").toFloat()
            })
            "notif" -> {
                val list = m.optJSONArray("actions")
                val n = PhoneNotification(
                    key = m.getString("key"),
                    isCall = m.optString("kind") == "call",
                    app = m.optString("app"),
                    title = m.optString("title"),
                    text = m.optString("text"),
                    actions = (0 until (list?.length() ?: 0)).map { list!!.optString(it) },
                )
                _notifications.value = (listOf(n) + _notifications.value.filterNot { it.key == n.key }).take(5)
            }
            "mirror_start" -> _mirror.value = m.getInt("w") to m.getInt("h")
            "mirror_stop" -> _mirror.value = null
            "notif_removed" -> dismissNotification(m.optString("key"))
            "dest" -> _destinations.tryEmit(m.optString("text"))
            "media" -> _media.value = if (!m.has("title")) null else NowPlaying(
                title = m.getString("title"),
                artist = m.optString("artist"),
                app = m.optString("app") + " (téléphone)",
                artwork = m.optString("art").takeIf { it.isNotEmpty() }?.let { decodeArt(it) },
                playing = m.optBoolean("playing"),
            )
        }
    }

    private fun decodeArt(b64: String): Bitmap? = runCatching {
        val bytes = LinkProtocol.unb64(b64)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    /** Téléphone : passerelle du Wi-Fi (cas du partage de connexion), sinon annonce réseau. */
    @Suppress("DEPRECATION")
    private suspend fun findPhone(): InetAddress? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val gateway = wifi.dhcpInfo?.gateway ?: 0
        if (gateway != 0) {
            val addr = InetAddress.getByAddress(byteArrayOf(
                (gateway and 0xff).toByte(), (gateway shr 8 and 0xff).toByte(),
                (gateway shr 16 and 0xff).toByte(), (gateway shr 24 and 0xff).toByte(),
            ))
            if (reachable(addr)) return addr
        }
        return discover()
    }

    private fun reachable(addr: InetAddress): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(addr, LinkProtocol.PORT), 1_500) }; true
    }.getOrDefault(false)

    private suspend fun discover(): InetAddress? {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val lock = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("roadguard-link").apply { setReferenceCounted(false); acquire() }
        val found = CompletableDeferred<InetAddress?>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(i: NsdServiceInfo, e: Int) = Unit
                    override fun onServiceResolved(i: NsdServiceInfo) { found.complete(i.host) }
                })
            }
            override fun onStartDiscoveryFailed(t: String, e: Int) { found.complete(null) }
            override fun onStopDiscoveryFailed(t: String, e: Int) = Unit
            override fun onDiscoveryStarted(t: String) = Unit
            override fun onDiscoveryStopped(t: String) = Unit
            override fun onServiceLost(info: NsdServiceInfo) = Unit
        }
        return try {
            nsd.discoverServices(LinkProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
            withTimeoutOrNull(6_000) { found.await() }
        } catch (e: RuntimeException) {
            null
        } finally {
            runCatching { nsd.stopServiceDiscovery(listener) }
            lock.release()
        }
    }

    private fun closeSession() { runCatching { session?.close() }; session = null }

    private fun backoff(failures: Int): Long = (2_000L shl failures.coerceAtMost(3)).coerceAtMost(15_000L)
}
