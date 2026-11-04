package fr.roadguard.core.nav

import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.GraphHopperConfig
import com.graphhopper.config.CHProfile
import com.graphhopper.config.Profile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.zip.ZipInputStream

/** Vérifie le graphe embarqué avec la même configuration que l'appli : un vrai trajet dans le Jura. */
class OfflineGraphTest {
    @Test fun routesLonsToChampagnoleOffline() {
        val zip = File("../../gps/app/src/main/assets/graphs/graph-franche-comte.zip")
        assumeTrue("graphe non téléchargé dans cet environnement", zip.isFile)
        val dir = Files.createTempDirectory("graph").toFile()
        ZipInputStream(zip.inputStream()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val f = File(dir, e.name)
                if (e.isDirectory) f.mkdirs() else { f.parentFile.mkdirs(); f.outputStream().use { z.copyTo(it) } }
            }
        }
        val hopper = GraphHopper()
        hopper.init(GraphHopperConfig()
            .putObject("graph.location", dir.absolutePath)
            .putObject("graph.flag_encoders", "car")
            .putObject("graph.dataaccess", "MMAP")
            .setProfiles(listOf(Profile("car").setVehicle("car").setWeighting("fastest")))
            .setCHProfiles(listOf(CHProfile("car"))))
        hopper.setAllowWrites(false)
        assertTrue("graphe illisible", hopper.load())
        // Lons-le-Saunier → Champagnole.
        val rsp = hopper.route(GHRequest(46.6744, 5.5547, 46.7466, 5.9103).setProfile("car").setLocale(Locale.FRENCH))
        assertFalse(rsp.errors.toString(), rsp.hasErrors())
        val km = rsp.best.distance / 1000
        assertTrue("distance $km km", km in 25.0..55.0)
        assertTrue(rsp.best.instructions.size() > 3)
        val tr = hopper.translationMap.getWithFallBack(Locale.FRENCH)
        println("Hors ligne : %.1f km, %d min, 1re consigne : %s".format(km, rsp.best.time / 60000, rsp.best.instructions[0].getTurnDescription(tr)))
        hopper.close()
    }
}
