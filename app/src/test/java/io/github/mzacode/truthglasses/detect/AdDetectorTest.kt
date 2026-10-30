package io.github.mzacode.truthglasses.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdDetectorTest {
    private val window = Bounds(0, 0, 1000, 2000)
    private val profile = AppProfile(
        packageName = "com.example",
        displayName = "Example",
        sponsorLabels = listOf(Regex("^sponsoris[ée]e?$", RegexOption.IGNORE_CASE)),
        verifiedOn = null
    )

    private fun detect(root: UiNode) = AdDetector.detect(root, profile, minSidePx = 100)

    private fun label(bounds: Bounds = Bounds(0, 100, 200, 150), visible: Boolean = true) =
        FakeNode(bounds, text = "Sponsorisé", visible = visible)

    @Test
    fun `container is the child of the scrollable parent`() {
        val post = FakeNode(
            Bounds(0, 100, 1000, 900),
            FakeNode(Bounds(0, 100, 1000, 200), label(), FakeNode(Bounds(0, 200, 1000, 800)))
        )
        val other = FakeNode(Bounds(0, 900, 1000, 1500), FakeNode(window, text = "Post"))
        val root = FakeNode(window, FakeNode(window, post, other, scrollable = true))

        assertEquals(listOf(Bounds(0, 100, 1000, 900)), detect(root).map { it.bounds })
    }

    @Test
    fun `label matches on content description`() {
        val post = FakeNode(
            Bounds(0, 0, 1000, 800),
            FakeNode(Bounds(0, 0, 200, 50), description = "  sponsorisée ")
        )
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertEquals(1, detect(root).size)
    }

    @Test
    fun `label must match the whole node`() {
        val post = FakeNode(
            Bounds(0, 0, 1000, 800),
            FakeNode(Bounds(0, 0, 200, 50), text = "Contenu sponsorisé par personne")
        )
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertTrue(detect(root).isEmpty())
    }

    @Test
    fun `without scrollable parent falls back on first ancestor above a quarter of the screen`() {
        val big = FakeNode(
            Bounds(0, 100, 1000, 700),
            FakeNode(Bounds(0, 100, 1000, 200), label())
        )
        val root = FakeNode(window, big)

        assertEquals(listOf(Bounds(0, 100, 1000, 700)), detect(root).map { it.bounds })
    }

    @Test
    fun `fallback never covers the whole window`() {
        val small = FakeNode(Bounds(0, 100, 1000, 200), label())
        val root = FakeNode(window, small)

        assertTrue(detect(root).isEmpty())
    }

    @Test
    fun `invisible label is ignored`() {
        val post = FakeNode(Bounds(0, 0, 1000, 800), label(visible = false))
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertTrue(detect(root).isEmpty())
    }

    @Test
    fun `nested containers keep only the largest`() {
        val inner = FakeNode(Bounds(100, 200, 900, 600), label())
        val carousel = FakeNode(Bounds(0, 100, 1000, 700), inner, scrollable = true)
        val post = FakeNode(Bounds(0, 0, 1000, 800), label(), carousel)
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertEquals(listOf(Bounds(0, 0, 1000, 800)), detect(root).map { it.bounds })
    }

    @Test
    fun `two separate ads give two hits`() {
        val first = FakeNode(Bounds(0, 0, 1000, 800), label())
        val second = FakeNode(Bounds(0, 800, 1000, 1600), label(Bounds(0, 800, 200, 850)))
        val root = FakeNode(window, FakeNode(window, first, second, scrollable = true))

        assertEquals(2, detect(root).size)
    }

    @Test
    fun `container is clipped to the window`() {
        val post = FakeNode(Bounds(0, 1500, 1000, 2600), label(Bounds(0, 1500, 200, 1550)))
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertEquals(listOf(Bounds(0, 1500, 1000, 2000)), detect(root).map { it.bounds })
    }

    @Test
    fun `block too small once clipped is ignored`() {
        val post = FakeNode(Bounds(0, 1950, 1000, 2600), label(Bounds(0, 1950, 200, 2000)))
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertTrue(detect(root).isEmpty())
    }

    @Test
    fun `ad text is lowercase, gathers the subtree and is capped`() {
        val post = FakeNode(
            Bounds(0, 0, 1000, 800),
            label(),
            FakeNode(Bounds(0, 50, 1000, 100), text = "NovaBank"),
            FakeNode(Bounds(0, 100, 1000, 200), description = "Crédit " + "x".repeat(600))
        )
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        val text = detect(root).single().adText
        assertTrue(text.startsWith("sponsorisé novabank crédit x"))
        assertEquals(500, text.length)
    }

    @Test
    fun `result carries the package name of the profile`() {
        val post = FakeNode(Bounds(0, 0, 1000, 800), label())
        val root = FakeNode(window, FakeNode(window, post, scrollable = true))

        assertEquals("com.example", detect(root).single().packageName)
    }

    @Test
    fun `node limit is respected`() {
        // Le libellé est placé après 3 000 nœuds : il ne doit pas être atteint
        val filler = List(3000) { FakeNode(Bounds(0, 0, 10, 10)) }
        val post = FakeNode(Bounds(0, 0, 1000, 800), label())
        val list = FakeNode(window, *(filler + post).toTypedArray(), scrollable = true)
        val root = FakeNode(window, list)
        assertTrue(detect(root).isEmpty())

        // Témoin : avec moins de nœuds, le même libellé est trouvé
        val shortFiller = List(100) { FakeNode(Bounds(0, 0, 10, 10)) }
        val shortPost = FakeNode(Bounds(0, 0, 1000, 800), label())
        val shortList = FakeNode(window, *(shortFiller + shortPost).toTypedArray(), scrollable = true)
        assertFalse(detect(FakeNode(window, shortList)).isEmpty())
    }

    @Test
    fun `depth limit is respected`() {
        var deep: FakeNode = label()
        repeat(70) { deep = FakeNode(Bounds(0, 0, 1000, 800), deep) }
        val root = FakeNode(window, FakeNode(window, deep, scrollable = true))

        assertTrue(detect(root).isEmpty())
    }
}

private class FakeNode(
    private val bounds: Bounds,
    vararg childNodes: FakeNode,
    override val text: CharSequence? = null,
    description: CharSequence? = null,
    scrollable: Boolean = false,
    visible: Boolean = true
) : UiNode {
    override val contentDescription: CharSequence? = description
    override val isScrollable: Boolean = scrollable
    override val isVisibleToUser: Boolean = visible
    override val className: CharSequence? = null
    override var parent: UiNode? = null
    override val children: List<UiNode> = childNodes.toList()

    init {
        childNodes.forEach { it.parent = this }
    }

    override fun boundsInScreen() = bounds
}
