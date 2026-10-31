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
        val lines = TreeDump.lines(root, reveal = TreeDump.LABEL_HINTS)

        assertEquals(4, lines.size)
        assertTrue(lines[1], lines[1].startsWith("  ") && lines[1].contains("scrollable"))
        assertTrue(lines[2], lines[2].startsWith("    ") && lines[2].contains("text=\"Sponsorisé\""))
        assertTrue(lines[3], lines[3].contains("invisible") && lines[3].contains("[0,50][1000,800]"))
    }

    @Test
    fun `dump never contains screen content by default`() {
        val lines = TreeDump.lines(root)

        assertEquals(4, lines.size)
        assertFalse(lines.any { it.contains("Sponsorisé") || it.contains("Camille") })
        assertTrue(lines[2], lines[2].contains("text=<10 chars>"))
    }

    @Test
    fun `label hints reveal labels but not the rest of the feed`() {
        val lines = TreeDump.lines(root, reveal = TreeDump.LABEL_HINTS)

        assertTrue(lines[2], lines[2].contains("text=\"Sponsorisé\""))
        assertFalse(lines.any { it.contains("Camille") })
        assertTrue(lines[3], lines[3].contains("desc=<16 chars>"))
    }

    @Test
    fun `label hints recognise labels glued to other text`() {
        listOf("Sponsorisée · NovaBank", "u/novabank • Publicité", "Sponsored", "Ad · NovaBank").forEach {
            assertTrue(it, TreeDump.LABEL_HINTS.containsMatchIn(it))
        }
        listOf("Premier café de la journée", "Quelle adresse magnifique").forEach {
            assertFalse(it, TreeDump.LABEL_HINTS.containsMatchIn(it))
        }
    }

    @Test
    fun `revealed text is cut short`() {
        val caption = FakeNode(Bounds(0, 0, 100, 100), text = "Publicité " + "x".repeat(200))

        val line = TreeDump.lines(caption, reveal = TreeDump.LABEL_HINTS).single()

        assertFalse(line, line.contains("x".repeat(100)))
    }

    @Test
    fun `multiline text stays on one line`() {
        val caption = FakeNode(Bounds(0, 0, 100, 100), text = "Ligne 1\nLigne 2\r\nLigne 3")

        val lines = TreeDump.lines(caption, reveal = Regex("Ligne"))

        assertEquals(1, lines.size)
        assertTrue(lines[0], lines[0].contains("text=\"Ligne 1\\nLigne 2\\nLigne 3\""))
    }
}
