package fr.roadguard.core.account

import android.content.Context
import fr.roadguard.core.community.Community
import fr.roadguard.core.net.Net
import org.json.JSONObject
import java.io.File

/**
 * Rapport de plantage : l'erreur est écrite sur l'appareil au moment du plantage, puis envoyée au serveur
 * ROADGUARD au démarrage suivant (visible par l'administrateur dans « Gérer les comptes → Plantages »).
 */
object CrashReporter {
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val file = File(app.filesDir, "crash.txt")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                val trace = android.util.Log.getStackTraceString(e)
                file.writeText("Fil : ${thread.name}\nAndroid ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n$trace".take(6000))
            }
            previous?.uncaughtException(thread, e)
        }
        if (file.isFile) Thread {
            runCatching {
                val version = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull() ?: "?"
                val body = JSONObject().put("app", if (app.packageName.contains("drive")) "drive" else "gps")
                    .put("version", version).put("device", android.os.Build.MODEL).put("text", file.readText())
                val token = app.getSharedPreferences("account", Context.MODE_PRIVATE).getString("token", null)
                val url = Community.DEFAULT_URL + "/v1/auth/crash" + (token?.let { "?session=$it" } ?: "")
                if (Net.fetch(url, "POST", body.toString().toByteArray(), "application/json").code in 200..299) file.delete()
            }
        }.start()
    }
}
