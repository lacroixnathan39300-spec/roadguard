package fr.roadguard.drive.ui

import fr.roadguard.core.ui.typedMatches
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phrase de confirmation écrite par l'utilisateur (avertissement légal). */
class TypedConsentTest {
    @Test
    fun acceptsTheSentenceWrittenLoosely() {
        assertTrue(typedMatches("J'accepte", "J'accepte"))
        assertTrue(typedMatches("  j’accepte ", "J'accepte"))
        assertTrue(typedMatches("JACCEPTE", "J'accepte"))
        assertTrue(typedMatches("j'active les radars a mes risques", "J'active les radars à mes risques"))
    }

    @Test
    fun refusesAnythingElse() {
        assertFalse(typedMatches("", "J'accepte"))
        assertFalse(typedMatches("ok", "J'accepte"))
        assertFalse(typedMatches("J'accepte pas", "J'accepte"))
        assertFalse(typedMatches("j'active les radars", "J'active les radars à mes risques"))
    }
}
