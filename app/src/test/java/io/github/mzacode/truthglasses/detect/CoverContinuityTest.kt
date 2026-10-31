package io.github.mzacode.truthglasses.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverContinuityTest {
    @Test
    fun `block growing from the bottom keeps its message`() {
        // Seul l'en-tête était visible, la pub entre maintenant en entier
        val shown = listOf(Bounds(0, 1800, 1000, 2000) to "SOUMETS-TOI")

        assertEquals("SOUMETS-TOI", CoverContinuity.previousMessage(Bounds(0, 1100, 1000, 2000), shown))
    }

    @Test
    fun `neighbour block does not inherit the message`() {
        val shown = listOf(Bounds(0, 0, 1000, 900) to "OBÉIS")

        assertNull(CoverContinuity.previousMessage(Bounds(0, 880, 1000, 1800), shown))
    }

    @Test
    fun `block in another column does not inherit the message`() {
        val shown = listOf(Bounds(0, 0, 500, 900) to "OBÉIS")

        assertNull(CoverContinuity.previousMessage(Bounds(500, 0, 1000, 900), shown))
    }

    @Test
    fun `largest overlap wins`() {
        val shown = listOf(
            Bounds(0, 0, 1000, 300) to "A",
            Bounds(0, 300, 1000, 1200) to "B"
        )

        assertEquals("B", CoverContinuity.previousMessage(Bounds(0, 200, 1000, 1100), shown))
    }
}
