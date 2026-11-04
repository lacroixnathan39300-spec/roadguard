package fr.roadguard.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Téléchargement de gros fichiers (cartes, bases radars) avec reprise :
 * - en direct si l'appareil a Internet (un seul flux, reprise par « Range ») ;
 * - sinon par le téléphone via ROADGUARD Link, en morceaux de 2 Mo.
 * Le fichier est écrit en « .part » puis renommé seulement s'il est complet et vérifié.
 */
object Downloader {
    private const val RELAY_CHUNK = 2L * 1024 * 1024

    /** [onProgress] : octets reçus, taille totale (0 si inconnue). */
    suspend fun download(
        url: String,
        target: File,
        expectedSize: Long,
        sha256: String?,
        onProgress: (Long, Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val part = File(target.path + ".part")
        if (expectedSize > 0 && part.length() > expectedSize) part.delete()
        val needed = (expectedSize - part.length()).coerceAtLeast(0) + 200L * 1024 * 1024
        if (expectedSize > 0 && (target.parentFile?.usableSpace ?: Long.MAX_VALUE) < needed) {
            throw IOException("Espace insuffisant : il faut ${needed / 1_000_000} Mo libres")
        }
        val relay = Net.relay
        if (Net.hasDirectInternet() || relay == null || !relay.isConnected()) direct(url, part, expectedSize, onProgress)
        else relayed(relay, url, part, expectedSize, onProgress)
        ensureActive()
        if (expectedSize > 0 && part.length() != expectedSize) throw IOException("Fichier incomplet, réessaie")
        if (!sha256.isNullOrBlank()) {
            onProgress(part.length(), expectedSize)
            if (!sha256(part).equals(sha256, ignoreCase = true)) { part.delete(); throw IOException("Fichier corrompu (empreinte différente), réessaie") }
        }
        if (target.exists() && !target.delete()) throw IOException("Impossible de remplacer l'ancien fichier")
        if (!part.renameTo(target)) throw IOException("Impossible d'enregistrer le fichier")
        target
    }

    private suspend fun direct(url: String, part: File, total: Long, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        var done = part.length()
        if (total > 0 && done >= total) return@withContext
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "ROADGUARD/2.0")
        if (done > 0) c.setRequestProperty("Range", "bytes=$done-")
        try {
            val code = c.responseCode
            if (code == 200 && done > 0) { done = 0; part.delete() } // le serveur ignore la reprise
            if (code != 200 && code != 206) throw IOException("Téléchargement refusé ($code)")
            FileOutputStream(part, done > 0).use { out ->
                c.inputStream.use { input ->
                    val buf = ByteArray(64 * 1024)
                    var lastReport = 0L
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (done - lastReport > 512 * 1024) { lastReport = done; onProgress(done, total) }
                    }
                }
            }
            onProgress(done, total)
        } finally {
            c.disconnect()
        }
    }

    private suspend fun relayed(relay: NetRelay, url: String, part: File, total: Long, onProgress: (Long, Long) -> Unit) {
        var done = part.length()
        FileOutputStream(part, true).use { out ->
            // Taille inconnue : on avance par morceaux jusqu'à un morceau incomplet (fin du fichier).
            while (total <= 0 || done < total) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val end = (if (total > 0) minOf(done + RELAY_CHUNK, total) else done + RELAY_CHUNK) - 1
                val r = relay.http(url, "GET", null, null, true, "bytes=$done-$end")
                if (r.code == 416 && total <= 0) break
                if (r.code == 200 && done == 0L) { out.write(r.body); done = r.body.size.toLong(); onProgress(done, total); break }
                if (r.code != 206 || r.body.isEmpty()) throw IOException("Téléchargement par le téléphone impossible (${r.code})")
                out.write(r.body)
                done += r.body.size
                onProgress(done, total)
                if (total <= 0 && r.body.size < RELAY_CHUNK) break
            }
        }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(256 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
