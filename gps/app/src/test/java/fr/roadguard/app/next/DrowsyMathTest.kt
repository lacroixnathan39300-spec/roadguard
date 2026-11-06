package fr.roadguard.app.next

import org.junit.Assert.assertEquals
import org.junit.Test

class DrowsyMathTest {
    @Test fun eyesClosedMoreThanASecondIsAsleep() {
        val m = DrowsyMath()
        assertEquals(Alertness.AWAKE, m.update(true, 0.1f, 0f, 0))
        assertEquals(Alertness.AWAKE, m.update(true, 0.9f, 0f, 100))
        assertEquals(Alertness.AWAKE, m.update(true, 0.9f, 0f, 1_000)) // simple clignement long
        assertEquals(Alertness.ASLEEP, m.update(true, 0.9f, 0f, 1_400))
        assertEquals(Alertness.AWAKE, m.update(true, 0.1f, 0f, 1_500))
    }

    @Test fun threeYawnsMeanTired() {
        val m = DrowsyMath()
        var t = 0L
        repeat(3) {
            m.update(true, 0.1f, 0.8f, t); m.update(true, 0.1f, 0.8f, t + 2_000); m.update(true, 0.1f, 0.1f, t + 2_100)
            t += 60_000
        }
        assertEquals(Alertness.TIRED, m.update(true, 0.1f, 0.1f, t))
    }

    @Test fun noFaceForSecondsIsAway() {
        val m = DrowsyMath()
        m.update(true, 0.1f, 0f, 0)
        assertEquals(Alertness.SEARCHING, m.update(false, 0f, 0f, 1_000))
        assertEquals(Alertness.AWAY, m.update(false, 0f, 0f, 3_500))
    }
}
