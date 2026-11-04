package fr.roadguard.core.update

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import fr.roadguard.core.community.Community
import fr.roadguard.core.net.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

/** Nouvelle version disponible sur le serveur ROADGUARD. */
data class AppRelease(val app: String, val version: Int, val build: Int, val size: Long, val sha256: String, val chunks: Int)

/**
 * Mise à jour des applis depuis le serveur ROADGUARD (sans Play Store) : vérification, téléchargement
 * par morceaux (aussi à travers le téléphone pour le poste), contrôle d'intégrité, puis installation Android.
 */
object AppUpdater {
    private fun installed(context: Context): Int = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
    }.getOrDefault(0)

    /** Version proposée si elle est plus récente que celle installée, sinon null. */
    suspend fun check(context: Context, app: String): AppRelease? = withContext(Dispatchers.IO) {
        val r = Net.fetch(Community.DEFAULT_URL + "/v1/apps")
        if (r.code !in 200..299) throw IOException("Serveur indisponible (${r.code})")
        val o = JSONObject(String(r.body)).optJSONObject(app) ?: return@withContext null
        val rel = AppRelease(app, o.getInt("version"), o.getInt("build"), o.getLong("size"), o.getString("sha256"), o.getInt("chunks"))
        rel.takeIf { it.version > installed(context) }
    }

    fun file(context: Context) = File(context.cacheDir, "roadguard-update.apk")

    /** Télécharge et vérifie l'APK, puis ouvre l'installation Android. */
    suspend fun downloadAndInstall(context: Context, rel: AppRelease, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val out = file(context)
        val digest = MessageDigest.getInstance("SHA-256")
        var done = 0L
        out.outputStream().use { o ->
            for (i in 0 until rel.chunks) {
                var bytes: ByteArray? = null
                for (attempt in 1..3) {
                    val r = runCatching { Net.fetch("${Community.DEFAULT_URL}/v1/apps/${rel.app}/${rel.build}/$i") }.getOrNull()
                    if (r != null && r.code in 200..299) { bytes = r.body; break }
                }
                val b = bytes ?: throw IOException("Téléchargement interrompu (morceau ${i + 1}/${rel.chunks})")
                o.write(b); digest.update(b); done += b.size
                onProgress(done, rel.size)
            }
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        if (sha != rel.sha256) { out.delete(); throw IOException("Fichier abîmé, réessaie") }
        val uri = Uri.parse("content://${context.packageName}.roadguard.updates/apk")
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        withContext(Dispatchers.Main) { context.startActivity(intent) }
    }
}

/** Donne l'APK téléchargé à l'installeur Android (content://<appli>.roadguard.updates/apk). */
class UpdateProvider : ContentProvider() {
    private fun f(u: Uri): File {
        if (u.path != "/apk") throw FileNotFoundException()
        return AppUpdater.file(context!!).takeIf { it.isFile } ?: throw FileNotFoundException()
    }
    override fun onCreate() = true
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(f(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    override fun getType(uri: Uri) = "application/vnd.android.package-archive"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? = runCatching {
        val file = f(uri)
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        MatrixCursor(cols).apply { addRow(cols.map { if (it == OpenableColumns.DISPLAY_NAME) file.name else if (it == OpenableColumns.SIZE) file.length() else null }) }
    }.getOrNull()
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
