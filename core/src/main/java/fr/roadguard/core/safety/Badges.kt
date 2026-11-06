package fr.roadguard.core.safety

/** Badge gagné (ou à gagner) et défi du mois, avec progression. */
data class Badge(val emoji: String, val title: String, val detail: String, val done: Boolean, val progress: Float)

/**
 * Badges et défis du mois, calculés d'après tes trajets et ta participation à la communauté
 * (sur le téléphone, rien n'est envoyé).
 */
object Badges {
    fun badges(trips: List<TripRecord>, odometerKm: Double, points: Int, level: String, judged: Int): List<Badge> {
        fun b(e: String, t: String, d: String, value: Double, goal: Double) = Badge(e, t, d, value >= goal, (value / goal).toFloat().coerceIn(0f, 1f))
        val wiseKm = trips.filter { it.overSeconds <= 30 }.sumOf { it.km }
        val eco = trips.count { it.ecoScore >= 85 }
        val night = trips.count { java.util.Calendar.getInstance().apply { timeInMillis = it.start }.get(java.util.Calendar.HOUR_OF_DAY) in 0..4 }
        val longTrip = trips.maxOfOrNull { it.km } ?: 0.0
        return listOf(
            b("📣", "Premier signalement", "Signaler un danger à la communauté", points.toDouble(), 5.0),
            b("📢", "Sentinelle", "100 points de communauté", points.toDouble(), 100.0),
            b("✅", "Vérifié", "5 signalements jugés par les autres", judged.toDouble(), 5.0),
            Badge("⭐", "Fiable", "4 étoiles de fiabilité", level == "reliable" || level == "trusted", if (level == "reliable" || level == "trusted") 1f else 0f),
            b("🛣", "1 000 km", "1 000 km avec Pilote", odometerKm, 1000.0),
            b("🌍", "Grand voyageur", "10 000 km avec Pilote", odometerKm, 10_000.0),
            b("😇", "Conduite sage", "500 km sans excès de vitesse", wiseKm, 500.0),
            b("🌱", "Éco-pilote", "5 trajets notés 85/100 ou plus", eco.toDouble(), 5.0),
            b("🏁", "Longue route", "Un trajet de 300 km", longTrip, 300.0),
            b("🌙", "Oiseau de nuit", "Un trajet entre minuit et 5 h (prudence !)", night.toDouble(), 1.0),
        )
    }

    /** Défis du mois en cours. */
    fun challenges(trips: List<TripRecord>, now: Long = System.currentTimeMillis()): List<Badge> {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val month = trips.filter {
            val t = java.util.Calendar.getInstance().apply { timeInMillis = it.start }
            t.get(java.util.Calendar.MONTH) == c.get(java.util.Calendar.MONTH) && t.get(java.util.Calendar.YEAR) == c.get(java.util.Calendar.YEAR)
        }
        fun b(e: String, t: String, d: String, value: Double, goal: Double) = Badge(e, t, d, value >= goal, (value / goal).toFloat().coerceIn(0f, 1f))
        val wiseKm = month.filter { it.overSeconds <= 30 }.sumOf { it.km }
        val smooth = month.count { it.hardBrakes == 0 && it.km >= 5 }
        val ecoAvg = month.takeIf { it.size >= 3 }?.map { it.ecoScore }?.average() ?: 0.0
        return listOf(
            b("😇", "300 km sans excès ce mois-ci", "${wiseKm.toInt()} / 300 km", wiseKm, 300.0),
            b("🧈", "10 trajets sans freinage brusque", "$smooth / 10 trajets", smooth.toDouble(), 10.0),
            b("🌱", "Note éco moyenne de 80 (3 trajets min.)", "${ecoAvg.toInt()} / 80", ecoAvg, 80.0),
        )
    }
}
