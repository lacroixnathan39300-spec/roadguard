package fr.roadguard.core.safety

/**
 * Limitations ajustées selon le code de la route français :
 *  - pluie (ou jeune conducteur) : 130 → 110, 110 → 100, 90 → 80 (les 80 et moins ne changent pas) ;
 *  - vitesse maximale personnelle (remorque, caravane…) : jamais au-dessus.
 * Hors de France, seule la vitesse personnelle s'applique (les règles diffèrent selon le pays).
 */
object RoadRules {
    fun adjust(limit: Int?, country: String?, rain: Boolean, young: Boolean, personalMax: Int): Int? {
        if (limit == null) return null
        val france = country == null || country == "FR"
        var v = limit
        if (france && (rain || young)) v = when (v) { 130 -> 110; 110 -> 100; 90 -> 80; else -> v }
        if (personalMax in 30..v - 1) v = personalMax
        return v
    }
}
