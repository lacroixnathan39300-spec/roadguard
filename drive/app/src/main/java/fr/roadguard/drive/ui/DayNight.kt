package fr.roadguard.drive.ui

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Jour ou nuit selon le lever et le coucher du soleil à la position donnée
 * (algorithme astronomique classique, précision de quelques minutes).
 */
object DayNight {

    /** true s'il fait nuit à [timeMs] (UTC) pour la position. */
    fun isNight(lat: Double, lon: Double, timeMs: Long): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = timeMs }
        val day = cal.get(Calendar.DAY_OF_YEAR)
        val hourUtc = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0
        val rise = sunEventUtc(day, lat, lon, rising = true) ?: return polarNight(lat, day)
        val set = sunEventUtc(day, lat, lon, rising = false) ?: return polarNight(lat, day)
        return if (rise < set) hourUtc < rise || hourUtc > set else hourUtc in set..rise
    }

    /** Heure UTC (décimale) du lever ou du coucher, null si le soleil ne se lève/couche pas. */
    fun sunEventUtc(dayOfYear: Int, lat: Double, lon: Double, rising: Boolean): Double? {
        val zenith = 90.833
        val lngHour = lon / 15.0
        val t = dayOfYear + ((if (rising) 6.0 else 18.0) - lngHour) / 24.0
        val m = 0.9856 * t - 3.289
        var l = m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634
        l = norm(l, 360.0)
        var ra = deg(kotlin.math.atan(0.91764 * tan(rad(l))))
        ra = norm(ra, 360.0)
        ra += Math.floor(l / 90.0) * 90.0 - Math.floor(ra / 90.0) * 90.0
        ra /= 15.0
        val sinDec = 0.39782 * sin(rad(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(rad(zenith)) - sinDec * sin(rad(lat))) / (cosDec * cos(rad(lat)))
        if (cosH > 1 || cosH < -1) return null
        val h = (if (rising) 360.0 - deg(acos(cosH)) else deg(acos(cosH))) / 15.0
        val localT = h + ra - 0.06571 * t - 6.622
        return norm(localT - lngHour, 24.0)
    }

    private fun polarNight(lat: Double, day: Int): Boolean {
        val northernWinter = day < 80 || day > 264
        return if (lat > 0) northernWinter else !northernWinter
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun norm(v: Double, m: Double) = ((v % m) + m) % m
}
