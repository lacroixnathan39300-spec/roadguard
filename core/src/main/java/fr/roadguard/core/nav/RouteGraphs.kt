package fr.roadguard.core.nav

import android.content.Context
import fr.roadguard.core.net.Downloader
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/** Une région de calcul d'itinéraire hors ligne proposée au téléchargement. */
data class GraphRegion(val id: String, val name: String)

/**
 * Graphes routiers hors ligne : fabriqués par GitHub Actions (workflow « Graphes hors ligne »)
 * et publiés dans le dépôt public de données ROADGUARD.
 */
object RouteGraphs {
    const val BASE_URL = "https://github.com/lacroixnathan39300-spec/roadguard-donnees/releases/download/graphs/"

    val regions = listOf(
        GraphRegion("franche-comte", "Franche-Comté (Jura, Doubs…)"),
        GraphRegion("bourgogne", "Bourgogne"),
        GraphRegion("rhone-alpes", "Rhône-Alpes"),
        GraphRegion("alsace", "Alsace"),
        GraphRegion("lorraine", "Lorraine"),
        GraphRegion("switzerland", "Suisse"),
    )

    fun dir(context: Context, id: String) = File(OfflineRouter.graphsDir(context), id)
    fun installed(context: Context, id: String) = File(dir(context, id), "properties").isFile

    /** Télécharge et installe le graphe d'une région (remplacement atomique). */
    suspend fun install(context: Context, id: String, onProgress: (Long) -> Unit) {
        val zip = File(OfflineRouter.graphsDir(context), "graph-$id.zip")
        Downloader.download("$BASE_URL/graph-$id.zip".replace("//graph", "/graph"), zip, 0, null) { d, _ -> onProgress(d) }
        val tmp = File(OfflineRouter.graphsDir(context), "$id.tmp")
        tmp.deleteRecursively()
        tmp.mkdirs()
        try {
            ZipInputStream(zip.inputStream().buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val out = File(tmp, e.name)
                    if (!out.canonicalPath.startsWith(tmp.canonicalPath + File.separator)) throw IOException("Archive invalide")
                    if (e.isDirectory) { out.mkdirs(); continue }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { z.copyTo(it) }
                }
            }
            if (!File(tmp, "properties").isFile) throw IOException("Graphe incomplet")
            val target = dir(context, id)
            target.deleteRecursively()
            if (!tmp.renameTo(target)) throw IOException("Installation impossible")
        } finally {
            zip.delete()
            tmp.deleteRecursively()
        }
    }

    fun remove(context: Context, id: String) { dir(context, id).deleteRecursively() }

    /** Régions livrées dans l'appli (assets « graphs/graph-<id>.zip ») : installées au premier besoin. */
    fun installBundled(context: Context) {
        val bundled = context.assets.list("graphs").orEmpty().filter { it.startsWith("graph-") && it.endsWith(".zip") }
        for (name in bundled) {
            val id = name.removePrefix("graph-").removeSuffix(".zip")
            if (installed(context, id)) continue
            runCatching { unzip(context.assets.open("graphs/$name"), context, id) }
        }
    }

    private fun unzip(input: java.io.InputStream, context: Context, id: String) {
        val tmp = File(OfflineRouter.graphsDir(context), "$id.tmp")
        tmp.deleteRecursively(); tmp.mkdirs()
        try {
            ZipInputStream(input.buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val out = File(tmp, e.name)
                    if (!out.canonicalPath.startsWith(tmp.canonicalPath + File.separator)) throw IOException("Archive invalide")
                    if (e.isDirectory) { out.mkdirs(); continue }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { z.copyTo(it) }
                }
            }
            if (!File(tmp, "properties").isFile) throw IOException("Graphe incomplet")
            val target = dir(context, id)
            target.deleteRecursively()
            if (!tmp.renameTo(target)) throw IOException("Installation impossible")
        } finally {
            tmp.deleteRecursively()
        }
    }
}
