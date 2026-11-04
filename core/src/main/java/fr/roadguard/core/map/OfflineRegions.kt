package fr.roadguard.core.map

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Une zone de carte enregistrée sur l'appareil (téléchargée depuis la carte en ligne). */
data class SavedRegion(val id: Long, val name: String, val done: Boolean, val bytes: Long, val tiles: Long, val total: Long)

/**
 * Zones de carte hors ligne enregistrées par MapLibre lui-même, à partir du style en ligne
 * (OpenFreeMap, OpenStreetMap). Sans Internet, la carte les utilise automatiquement.
 */
object OfflineRegions {
    private val _progress = MutableStateFlow<Pair<Long, Long>?>(null)
    /** Progression du téléchargement en cours (tuiles reçues, total), ou null. */
    val progress: StateFlow<Pair<Long, Long>?> = _progress
    @Volatile private var running: OfflineRegion? = null

    private fun manager(context: Context): OfflineManager {
        MapLibre.getInstance(context.applicationContext)
        return OfflineManager.getInstance(context.applicationContext).also {
            // Limite par défaut (6 000 tuiles) trop basse pour une région : relevée.
            it.setOfflineMapboxTileCountLimit(200_000)
        }
    }

    /** Cache automatique plus grand : ce qui a été vu en ligne reste disponible sans réseau. */
    fun enlargeAmbientCache(context: Context) {
        runCatching { manager(context).setMaximumAmbientCacheSize(1_000_000_000L, null) }
    }

    /** Vide la mémoire automatique de la carte (morceaux abîmés ou erronés). Les zones enregistrées restent. */
    suspend fun clearCache(context: Context) = suspendCancellableCoroutine<Unit> { cont ->
        manager(context).clearAmbientCache(object : OfflineManager.FileSourceCallback {
            override fun onSuccess() { cont.resume(Unit) }
            override fun onError(message: String) { cont.resume(Unit) }
        })
    }

    /** Une fois après une mise à jour qui corrige le cache : on repart d'une mémoire propre. */
    suspend fun clearCacheOnce(context: Context, version: Int) {
        val prefs = context.getSharedPreferences("map_cache", Context.MODE_PRIVATE)
        if (prefs.getInt("cleared", 0) >= version) return
        clearCache(context)
        prefs.edit().putInt("cleared", version).apply()
    }

    suspend fun list(context: Context): List<SavedRegion> {
        val regions = suspendCancellableCoroutine<Array<OfflineRegion>> { cont ->
            manager(context).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) { cont.resume(offlineRegions ?: emptyArray()) }
                override fun onError(error: String) { cont.resume(emptyArray()) }
            })
        }
        return regions.map { r ->
            val status = suspendCancellableCoroutine<OfflineRegionStatus?> { cont ->
                r.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                    override fun onStatus(status: OfflineRegionStatus?) { cont.resume(status) }
                    override fun onError(error: String?) { cont.resume(null) }
                })
            }
            val name = runCatching { JSONObject(String(r.metadata)).optString("name") }.getOrNull().orEmpty().ifBlank { "Zone" }
            SavedRegion(r.id, name, status?.isComplete == true, status?.completedResourceSize ?: 0,
                status?.completedTileCount ?: 0, status?.requiredResourceCount ?: 0)
        }
    }

    /**
     * Télécharge la carte sur [radiusKm] autour d'un point (zooms 5 à 14 ; au-delà, la carte agrandit).
     * Le poste peut passer par le téléphone (même client HTTP que la carte).
     */
    suspend fun download(context: Context, styleUrl: String, lat: Double, lon: Double, radiusKm: Double, name: String) {
        val dLat = radiusKm / 111.0
        val dLon = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)).coerceAtLeast(0.2))
        val bounds = LatLngBounds.Builder().include(LatLng(lat - dLat, lon - dLon)).include(LatLng(lat + dLat, lon + dLon)).build()
        val density = context.resources.displayMetrics.density.coerceAtMost(2f)
        val definition = OfflineTilePyramidRegionDefinition(styleUrl, bounds, 5.0, 14.0, density)
        val metadata = JSONObject().put("name", name).toString().toByteArray()
        val region = suspendCancellableCoroutine<OfflineRegion> { cont ->
            manager(context).createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) { cont.resume(offlineRegion) }
                override fun onError(error: String) { cont.resumeWithException(java.io.IOException(error)) }
            })
        }
        running = region
        _progress.value = 0L to 0L
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: OfflineRegionStatus) {
                        _progress.value = status.completedResourceCount to status.requiredResourceCount
                        if (status.isComplete) {
                            region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            if (cont.isActive) cont.resume(Unit)
                        }
                    }
                    override fun onError(error: OfflineRegionError) { /* nouvel essai automatique par MapLibre */ }
                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        if (cont.isActive) cont.resumeWithException(java.io.IOException("Zone trop grande (limite de $limit tuiles)"))
                    }
                })
                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                cont.invokeOnCancellation { region.setDownloadState(OfflineRegion.STATE_INACTIVE) }
            }
        } finally {
            running = null
            _progress.value = null
        }
    }

    suspend fun delete(context: Context, id: Long) {
        val regions = suspendCancellableCoroutine<Array<OfflineRegion>> { cont ->
            manager(context).listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) { cont.resume(offlineRegions ?: emptyArray()) }
                override fun onError(error: String) { cont.resume(emptyArray()) }
            })
        }
        val r = regions.firstOrNull { it.id == id } ?: return
        suspendCancellableCoroutine<Unit> { cont ->
            r.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() { cont.resume(Unit) }
                override fun onError(error: String) { cont.resume(Unit) }
            })
        }
    }
}
