package fr.roadguard.drive

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/** Une application lançable installée sur le poste. */
data class LaunchableApp(val label: String, val packageName: String, val activity: String, val icon: Bitmap?)

/** Liste les applications que l'utilisateur peut ouvrir depuis le lanceur. */
class AppCatalog(private val context: Context) {

    fun load(): List<LaunchableApp> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                LaunchableApp(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    activity = it.activityInfo.name,
                    icon = runCatching { it.loadIcon(pm).toBitmap(96) }.getOrNull(),
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    fun launch(app: LaunchableApp) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.activity)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { context.startActivity(intent) }
    }
}

private fun Drawable.toBitmap(sizePx: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    return bitmap
}
