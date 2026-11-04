package fr.roadguard.core.radar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Bases radars chargées une seule fois, partagées par la carte et les alertes. */
object RadarRepository {
    private val lock = Mutex()
    @Volatile private var cached: List<Radar>? = null
    @Volatile private var cachedIndex: RadarIndex? = null

    /** Change à chaque ajout ou retrait d'un pays : la carte et les alertes se rechargent. */
    val version = kotlinx.coroutines.flow.MutableStateFlow(0)

    fun extraDir(context: Context) = java.io.File(context.applicationContext.filesDir, "radar_countries")

    suspend fun radars(context: Context): List<Radar> = cached ?: lock.withLock {
        cached ?: withContext(Dispatchers.IO) {
            runCatching { RadarCatalog.loadAll(context.applicationContext.assets, extraDir(context)) }.getOrDefault(emptyList())
        }.also { cached = it }
    }

    fun invalidate() {
        cached = null
        cachedIndex = null
        version.value++
    }

    suspend fun index(context: Context): RadarIndex =
        cachedIndex ?: RadarIndex(radars(context)).also { cachedIndex = it }
}
