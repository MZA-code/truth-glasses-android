package io.github.mzacode.truthglasses.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextFitterTest {
    // Mesure simple : largeur = nombre de caractères × taille × 0,55
    private class FakeMeasurer : TextMeasurer {
        var calls = 0

        override fun width(text: String, sizePx: Float): Float {
            calls++
            return text.length * sizePx * 0.55f
        }

        override fun lineHeight(sizePx: Float): Float {
            calls++
            return sizePx * 1.2f
        }
    }

    private val measurer = FakeMeasurer()
    private val fitter = TextFitter(measurer, minSizePx = 8f, maxSizePx = 160f)

    private fun assertFits(fitted: FittedText, width: Float, height: Float) {
        fitted.lines.forEach { assertTrue(it, measurer.width(it, fitted.sizePx) <= width) }
        assertTrue(fitted.lines.size * fitter.lineStep(fitted.sizePx) <= height)
    }

    @Test
    fun `long message fits in a portrait block without breaking words`() {
        val message = "TA VIE EST TROP NULLE POUR RESTER CHEZ TOI"
        val fitted = fitter.fit(message, 300f, 600f)

        assertFits(fitted, 300f, 600f)
        assertEquals(message, fitted.lines.joinToString(" "))
        assertTrue(fitted.sizePx > 8f)
        // Taille maximale : avec un pixel de marge en plus, la recherche ne monte pas plus haut
        val roomier = TextFitter(FakeMeasurer(), 8f, fitted.sizePx + 1).fit(message, 300f, 600f)
        assertEquals(fitted.sizePx, roomier.sizePx)
    }

    @Test
    fun `short message in a wide block uses the largest size`() {
        val fitted = fitter.fit("OBÉIS", 2000f, 1000f)

        assertEquals(160f, fitted.sizePx)
        assertEquals(listOf("OBÉIS"), fitted.lines)
    }

    @Test
    fun `too long word enables word breaking at the smallest size`() {
        val fitted = fitter.fit("ANTICONSTITUTIONNELLEMENT", 50f, 1000f)

        assertEquals(8f, fitted.sizePx)
        assertTrue(fitted.lines.size > 1)
        assertEquals("ANTICONSTITUTIONNELLEMENT", fitted.lines.joinToString(""))
        fitted.lines.forEach { assertTrue(it, measurer.width(it, 8f) <= 50f) }
    }

    @Test
    fun `result is cached by message and size`() {
        val first = fitter.fit("TU N'EN AS PAS BESOIN", 400f, 300f)
        val callsAfterFirst = measurer.calls
        val second = fitter.fit("TU N'EN AS PAS BESOIN", 400f, 300f)

        assertEquals(first, second)
        assertEquals(callsAfterFirst, measurer.calls)

        // Une autre taille de bloc relance le calcul
        fitter.fit("TU N'EN AS PAS BESOIN", 400f, 200f)
        assertTrue(measurer.calls > callsAfterFirst)
    }
}
