package fr.roadguard.core.net

import java.util.concurrent.atomic.AtomicInteger

/** Compteurs de diagnostic de la carte (affichés quand elle ne se charge pas). */
object NetStats {
    val relayOk = AtomicInteger()
    val relayFail = AtomicInteger()
    @Volatile var lastError: String? = null
}
