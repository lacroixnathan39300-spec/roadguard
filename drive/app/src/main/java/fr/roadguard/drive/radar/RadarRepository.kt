package fr.roadguard.drive.radar

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

    suspend fun radars(context: Context): List<Radar> = cached ?: lock.withLock {
        cached ?: withContext(Dispatchers.IO) {
            runCatching { RadarCatalog.loadAll(context.applicationContext.assets) }.getOrDefault(emptyList())
        }.also { cached = it }
    }

    suspend fun index(context: Context): RadarIndex =
        cachedIndex ?: RadarIndex(radars(context)).also { cachedIndex = it }
}
