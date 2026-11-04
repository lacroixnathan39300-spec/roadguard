package fr.roadguard.core.nav

import android.content.Context
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.GraphHopperConfig
import com.graphhopper.config.CHProfile
import com.graphhopper.config.Profile
import com.graphhopper.util.Instruction
import com.graphhopper.util.RoundaboutInstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Calcul d'itinéraire sans Internet : graphe routier OpenStreetMap précalculé (GraphHopper 5.3, voiture),
 * avec sens uniques, interdictions et vitesses. Un graphe par région, dans « graphs/<région>/ ».
 * La configuration doit rester celle de tools/graphs/config.yml.
 */
object OfflineRouter {
    private val lock = Mutex()
    private var loaded: Pair<File, GraphHopper>? = null

    fun graphsDir(context: Context) = File(context.applicationContext.filesDir, "graphs")

    /** Régions installées (dossiers contenant un graphe complet). */
    fun installed(context: Context): List<File> =
        graphsDir(context).listFiles { f -> f.isDirectory && File(f, "properties").isFile }.orEmpty().toList()

    /** Itinéraire hors ligne, ou null si aucun graphe installé ne couvre le départ et l'arrivée. */
    suspend fun route(context: Context, from: LatLon, headingDeg: Double?, to: LatLon): Route? = withContext(Dispatchers.IO) {
        lock.withLock {
            for (dir in installed(context)) {
                val hopper = open(dir) ?: continue
                val bounds = hopper.graphHopperStorage.bounds
                if (!bounds.contains(from.lat, from.lon) || !bounds.contains(to.lat, to.lon)) continue
                val req = GHRequest(from.lat, from.lon, to.lat, to.lon).setProfile("car").setLocale(Locale.FRENCH)
                if (headingDeg != null && !headingDeg.isNaN()) req.setHeadings(listOf(headingDeg, Double.NaN))
                val rsp = hopper.route(req)
                if (rsp.hasErrors()) continue
                return@withContext toRoute(hopper, rsp.best, to)
            }
            null
        }
    }

    private fun open(dir: File): GraphHopper? {
        loaded?.let { (d, h) -> if (d == dir) return h else { h.close(); loaded = null } }
        return runCatching {
            val cfg = GraphHopperConfig()
                .putObject("graph.location", dir.absolutePath)
                .putObject("graph.flag_encoders", "car")
                // Lecture sur disque (mémoire vive limitée des autoradios).
                .putObject("graph.dataaccess", "MMAP")
                .setProfiles(listOf(Profile("car").setVehicle("car").setWeighting("fastest")))
                .setCHProfiles(listOf(CHProfile("car")))
            val hopper = GraphHopper()
            hopper.init(cfg)
            hopper.setAllowWrites(false)
            if (!hopper.load()) null else hopper
        }.getOrNull()?.also { loaded = dir to it }
    }

    private fun toRoute(hopper: GraphHopper, path: com.graphhopper.ResponsePath, to: LatLon): Route {
        val pts = path.points
        val points = (0 until pts.size()).map { LatLon(pts.getLat(it), pts.getLon(it)) }
        val tr = hopper.translationMap.getWithFallBack(Locale.FRENCH)
        var index = 0
        val maneuvers = path.instructions.map { ins ->
            val text = ins.getTurnDescription(tr).replaceFirstChar { it.uppercase() }
            val m = Maneuver(
                type = valhallaType(ins),
                instruction = text,
                verbalAlert = text,
                verbalNow = text,
                shapeIndex = index.coerceAtMost((points.size - 1).coerceAtLeast(0)),
                street = ins.name.orEmpty(),
                roundaboutExit = (ins as? RoundaboutInstruction)?.exitNumber?.takeIf { it > 0 },
            )
            index += (ins.points.size() - 1).coerceAtLeast(1)
            m
        }
        return Route(points, maneuvers, path.distance, path.time / 1000.0, to)
    }

    /** Signes GraphHopper → types de manœuvre (mêmes flèches que le guidage en ligne). */
    private fun valhallaType(ins: Instruction): Int = when (ins.sign) {
        Instruction.FINISH, Instruction.REACHED_VIA -> 4
        Instruction.TURN_SLIGHT_RIGHT, Instruction.KEEP_RIGHT -> 9
        Instruction.TURN_RIGHT -> 10
        Instruction.TURN_SHARP_RIGHT -> 11
        Instruction.U_TURN_RIGHT, Instruction.U_TURN_UNKNOWN -> 12
        Instruction.U_TURN_LEFT -> 13
        Instruction.TURN_SHARP_LEFT -> 14
        Instruction.TURN_LEFT -> 15
        Instruction.TURN_SLIGHT_LEFT, Instruction.KEEP_LEFT -> 16
        Instruction.USE_ROUNDABOUT -> 26
        else -> 8
    }
}
