package io.github.mzacode.truthglasses.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMemoryTest {
    @Test
    fun `known ad keeps its first message`() {
        val memory = AdMemory()

        assertEquals("OBÉIS", memory.messageFor(setOf(1, 2)) { "OBÉIS" })
        assertEquals("OBÉIS", memory.messageFor(setOf(2)) { "AUTRE" })
        assertTrue(memory.knows(setOf(2, 3)))
    }

    @Test
    fun `fingerprint grows as more of the ad is seen`() {
        val memory = AdMemory()
        memory.messageFor(setOf(1)) { "OBÉIS" }
        memory.messageFor(setOf(1, 2)) { "AUTRE" }

        assertTrue(memory.knows(setOf(2)))
    }

    @Test
    fun `empty fingerprint is never remembered`() {
        val memory = AdMemory()

        assertEquals("A", memory.messageFor(emptySet()) { "A" })
        assertEquals("B", memory.messageFor(emptySet()) { "B" })
        assertTrue(memory.isEmpty)
        assertFalse(memory.knows(emptySet()))
    }

    @Test
    fun `oldest ad is forgotten beyond capacity`() {
        val memory = AdMemory(capacity = 2)
        memory.messageFor(setOf(1)) { "A" }
        memory.messageFor(setOf(2)) { "B" }
        memory.messageFor(setOf(3)) { "C" }

        assertFalse(memory.knows(setOf(1)))
        assertTrue(memory.knows(setOf(2)) && memory.knows(setOf(3)))
    }

    @Test
    fun `clear forgets everything`() {
        val memory = AdMemory()
        memory.messageFor(setOf(1)) { "A" }
        memory.clear()

        assertTrue(memory.isEmpty)
    }
}
