package fr.roadguard.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Réponse HTTP minimale. */
class NetResponse(val code: Int, val body: ByteArray, val contentType: String?) {
    val text: String get() = String(body, Charsets.UTF_8)
}

/**
 * Accès Internet de DRIVE : directement si le poste a Internet (Wi-Fi du téléphone…),
 * sinon par le téléphone à travers ROADGUARD Link (câble USB, Wi-Fi sans Internet), comme Android Auto.
 */
/** Passage par le téléphone (fourni par ROADGUARD Link sur l'autoradio). */
interface NetRelay {
    fun isConnected(): Boolean
    suspend fun http(url: String, method: String, body: ByteArray?, contentType: String?, follow: Boolean, range: String? = null): NetResponse
}

object Net {
    /** Relais Internet par le téléphone, ou null (appli du téléphone elle-même). */
    @Volatile var relay: NetRelay? = null
    private const val USER_AGENT = "ROADGUARD-DRIVE/2.0 (usage personnel)"
    @Volatile private var appContext: Context? = null

    fun init(context: Context) { appContext = context.applicationContext }

    /** Le poste a-t-il un accès Internet vérifié par Android ? */
    fun hasDirectInternet(): Boolean {
        val cm = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Requête bloquante (à appeler hors du fil principal). */
    fun fetch(
        url: String,
        method: String = "GET",
        body: ByteArray? = null,
        contentType: String? = null,
        followRedirects: Boolean = true,
    ): NetResponse {
        val link = relay
        if (!hasDirectInternet() && link != null && link.isConnected()) {
            return runBlocking { link.http(url, method, body, contentType, followRedirects) }
        }
        return direct(url, method, body, contentType, followRedirects)
    }

    /**
     * Téléchargement avec progression : [onBytes] reçoit les octets au fur et à mesure (barre de progression).
     * Par le relais du téléphone (autoradio sans Internet), le morceau arrive d'un coup.
     */
    fun fetchWithProgress(url: String, onBytes: (Int) -> Unit): NetResponse {
        val link = relay
        if (!hasDirectInternet() && link != null && link.isConnected()) {
            val r = fetch(url)
            if (r.code in 200..299) onBytes(r.body.size)
            return r
        }
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 60_000
        c.setRequestProperty("User-Agent", USER_AGENT)
        try {
            val code = c.responseCode
            if (code !in 200..299) return NetResponse(code, c.errorStream?.use { it.readBytes() } ?: ByteArray(0), c.contentType)
            val out = java.io.ByteArrayOutputStream(maxOf(c.contentLength, 0))
            val buf = ByteArray(64 * 1024)
            c.inputStream.use { input ->
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n); onBytes(n)
                }
            }
            return NetResponse(code, out.toByteArray(), c.contentType)
        } catch (e: IOException) {
            throw IOException(if (relay?.isConnected() == true) e.message else "Pas d'accès Internet", e)
        } finally {
            c.disconnect()
        }
    }

    private fun direct(url: String, method: String, body: ByteArray?, contentType: String?, follow: Boolean): NetResponse {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = follow
        c.requestMethod = method
        c.setRequestProperty("User-Agent", USER_AGENT)
        if (contentType != null) c.setRequestProperty("Content-Type", contentType)
        try {
            if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body) } }
            val code = c.responseCode
            if (code in 300..399 && !follow) {
                return NetResponse(code, (c.getHeaderField("Location") ?: "").toByteArray(), "text/x-location")
            }
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            return NetResponse(code, bytes, c.contentType)
        } catch (e: IOException) {
            throw IOException(if (relay?.isConnected() == true) e.message else "Pas d'accès Internet", e)
        } finally {
            c.disconnect()
        }
    }
}
