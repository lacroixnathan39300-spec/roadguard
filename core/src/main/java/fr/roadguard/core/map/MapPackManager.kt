package fr.roadguard.core.map

import android.content.Context
import fr.roadguard.core.net.Downloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/** Un pack proposé au téléchargement. [unavailable] : raison si la source est défectueuse. */
data class CatalogPack(
    val id: String,
    val name: String,
    val url: String,
    val sha256: String?,
    val size: Long,
    val country: String,
    val unavailable: String?,
)

sealed class PackState {
    object NotInstalled : PackState()
    class Downloading(val done: Long, val total: Long) : PackState()
    class Paused(val done: Long) : PackState()
    object Installed : PackState()
    class Failed(val message: String) : PackState()
}

/**
 * Cartes hors ligne : catalogue (le même que l'ancienne appli), téléchargement avec pause et reprise,
 * vérification de l'empreinte SHA-256, suppression. Les packs vont dans « pmtiles/ » de l'appli.
 */
class MapPackManager private constructor(context: Context) {
    private val dir = File(context.filesDir, "pmtiles")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = HashMap<String, Job>()

    val catalog: List<CatalogPack> = runCatching {
        parseCatalog(context.assets.open("offline_map_catalog.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList())

    private val _states = MutableStateFlow(catalog.associate { it.id to stateOnDisk(it) })
    val states: StateFlow<Map<String, PackState>> = _states

    private fun file(id: String) = File(dir, "$id.pmtiles")

    private fun stateOnDisk(p: CatalogPack): PackState = when {
        file(p.id).isFile -> PackState.Installed
        File(file(p.id).path + ".part").isFile -> PackState.Paused(File(file(p.id).path + ".part").length())
        else -> PackState.NotInstalled
    }

    private fun set(id: String, s: PackState) { _states.value = _states.value + (id to s) }

    fun download(p: CatalogPack) {
        if (p.unavailable != null || jobs[p.id]?.isActive == true) return
        set(p.id, PackState.Downloading(File(file(p.id).path + ".part").length(), p.size))
        jobs[p.id] = scope.launch {
            runCatching {
                Downloader.download(p.url, file(p.id), p.size, p.sha256) { done, total -> set(p.id, PackState.Downloading(done, total)) }
            }.onSuccess { set(p.id, PackState.Installed) }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) set(p.id, stateOnDisk(p))
                    else set(p.id, PackState.Failed(e.message ?: "Erreur de téléchargement"))
                }
        }
    }

    fun pause(p: CatalogPack) { jobs.remove(p.id)?.cancel() }

    fun delete(p: CatalogPack) {
        pause(p)
        file(p.id).delete()
        File(file(p.id).path + ".part").delete()
        set(p.id, PackState.NotInstalled)
    }

    fun installedBytes(): Long = dir.listFiles().orEmpty().filter { it.name.endsWith(".pmtiles") }.sumOf { it.length() }
    fun freeBytes(): Long = (dir.takeIf { it.exists() } ?: dir.parentFile)?.usableSpace ?: 0

    companion object {
        @Volatile private var instance: MapPackManager? = null
        fun get(context: Context): MapPackManager =
            instance ?: synchronized(this) { instance ?: MapPackManager(context.applicationContext).also { instance = it } }

        fun parseCatalog(json: String): List<CatalogPack> {
            val packs = JSONObject(json).getJSONArray("packs")
            return (0 until packs.length()).map { i ->
                val o = packs.getJSONObject(i)
                val ok = o.optString("availability", "ok").let { it.isEmpty() || it == "ok" || it == "available" }
                CatalogPack(
                    id = o.getString("id"), name = o.getString("name"), url = o.getString("url"),
                    sha256 = o.optString("sha256").ifBlank { null }, size = o.optLong("size"),
                    country = o.optString("country_code"),
                    unavailable = if (ok) null else o.optString("unavailable_reason", "Source indisponible"),
                )
            }
        }
    }
}
