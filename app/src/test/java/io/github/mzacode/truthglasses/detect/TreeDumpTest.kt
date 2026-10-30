package io.github.mzacode.truthglasses.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeDumpTest {
    private val root = FakeNode(
        Bounds(0, 0, 1000, 2000),
        FakeNode(
            Bounds(0, 0, 1000, 800),
            FakeNode(Bounds(0, 0, 200, 50), text = "Sponsorisé"),
            FakeNode(Bounds(0, 50, 1000, 800), description = "Photo de Camille", visible = false),
            scrollable = true
        )
    )

    @Test
    fun `one indented line per node`() {
        val lines = TreeDump.lines(root, redact = false)

        assertEquals(4, lines.size)
        assertTrue(lines[1], lines[1].startsWith("  ") && lines[1].contains("scrollable"))
        assertTrue(lines[2], lines[2].startsWith("    ") && lines[2].contains("text=\"Sponsorisé\""))
        assertTrue(lines[3], lines[3].contains("invisible") && lines[3].contains("[0,50][1000,800]"))
    }

    @Test
    fun `redacted dump never contains screen content`() {
        val lines = TreeDump.lines(root, redact = true)

        assertEquals(4, lines.size)
        assertFalse(lines.any { it.contains("Sponsorisé") || it.contains("Camille") })
        assertTrue(lines[2], lines[2].contains("text=<10 chars>"))
    }
}
