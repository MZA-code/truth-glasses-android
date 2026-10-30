package io.github.mzacode.truthglasses.detect

// Faux nœud construit à la main pour les tests sans appareil
class FakeNode(
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
    override val viewIdResourceName: String? = null
    override var parent: UiNode? = null
    override val children: List<UiNode> = childNodes.toList()

    init {
        childNodes.forEach { it.parent = this }
    }

    override fun boundsInScreen() = bounds
}
