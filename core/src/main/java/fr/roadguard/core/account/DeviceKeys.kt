package fr.roadguard.core.account

import android.content.Context
import android.os.Build
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Diagnostic du poste : le système est-il signé avec les clés de test publiques d'Android (« test-keys ») ?
 * Si oui, une version de Pilote Drive signée avec la clé « platform » publique serait reconnue comme appli
 * système (écran virtuel pour afficher Waze dans un module, etc.).
 */
object DeviceKeys {
    data class Report(val testKeys: Boolean, val aospCert: Boolean, val text: String)

    fun check(context: Context): Report {
        val cert = runCatching {
            @Suppress("DEPRECATION")
            val sig = context.packageManager.getPackageInfo("android", android.content.pm.PackageManager.GET_SIGNATURES).signatures!!.first()
            CertificateFactory.getInstance("X.509").generateCertificate(sig.toByteArray().inputStream()) as X509Certificate
        }.getOrNull()
        val subject = cert?.subjectX500Principal?.name.orEmpty()
        val sha = cert?.let { MessageDigest.getInstance("SHA-256").digest(it.encoded).joinToString(":") { b -> "%02X".format(b) } }.orEmpty()
        // Certificat « platform » de test d'AOSP : émis au nom d'Android, android@android.com, Mountain View.
        val aosp = subject.contains("android@android.com", true) && subject.contains("Mountain View", true)
        val tags = Build.TAGS.orEmpty()
        val text = buildString {
            appendLine("Modèle : ${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE}) · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Type : ${Build.TYPE} · Étiquettes : $tags")
            appendLine("Empreinte : ${Build.FINGERPRINT}")
            appendLine("Certificat système : $subject")
            appendLine("SHA-256 : $sha")
        }
        return Report(tags.contains("test-keys"), aosp, text)
    }

    /** Envoie le diagnostic au serveur (visible par l'administrateur dans « Plantages »). */
    fun send(context: Context, report: Report) {
        val app = context.applicationContext
        Thread {
            runCatching {
                val body = org.json.JSONObject().put("app", "diag").put("version", Build.MODEL).put("device", Build.MODEL)
                    .put("text", "DIAGNOSTIC DU POSTE\n" + report.text)
                val token = app.getSharedPreferences("account", Context.MODE_PRIVATE).getString("token", null)
                fr.roadguard.core.net.Net.fetch(fr.roadguard.core.community.Community.DEFAULT_URL + "/v1/auth/crash" + (token?.let { "?session=$it" } ?: ""),
                    "POST", body.toString().toByteArray(), "application/json")
            }
        }.start()
    }
}
