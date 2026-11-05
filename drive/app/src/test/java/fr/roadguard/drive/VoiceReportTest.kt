package fr.roadguard.drive

import fr.roadguard.core.community.ReportType
import fr.roadguard.core.ui.reportFromSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceReportTest {
    @Test fun understandsCommonPhrases() {
        assertEquals(ReportType.POLICE, reportFromSpeech("Il y a un contrôle de gendarmerie"))
        assertEquals(ReportType.MOBILE, reportFromSpeech("radar mobile"))
        assertEquals(ReportType.FIXED, reportFromSpeech("Radar fixe"))
        assertEquals(ReportType.JAM, reportFromSpeech("gros embouteillage"))
        assertEquals(ReportType.WORKS, reportFromSpeech("Travaux"))
        assertEquals(ReportType.ACCIDENT, reportFromSpeech("un accident"))
        assertEquals(ReportType.DANGER, reportFromSpeech("objet sur la route"))
        assertNull(reportFromSpeech("bonjour"))
    }
}
