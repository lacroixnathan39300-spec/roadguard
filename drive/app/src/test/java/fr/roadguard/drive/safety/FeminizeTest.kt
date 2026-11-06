package fr.roadguard.drive.safety

import fr.roadguard.core.radar.VoicePack
import org.junit.Assert.assertEquals
import org.junit.Test

class FeminizeTest {
    @Test fun phrasesAddressAWoman() {
        assertEquals("Doucement, ma belle… tu es fougueuse aujourd'hui.", VoicePack.feminize("Doucement, mon beau… tu es fougueux aujourd'hui."))
        assertEquals("Ralentis, ma chérie ! Vilaine fille, chérie.", VoicePack.feminize("Ralentis, mon chéri ! Vilain garçon, chéri."))
        assertEquals("On n'est pas pressés, toi et moi.", VoicePack.feminize("On n'est pas pressés, toi et moi."))
        assertEquals("Tu es épuisée, ma tigresse.", VoicePack.feminize("Tu es épuisé, mon fauve."))
    }
}
