package io.github.mzacode.truthglasses.detect

// labelBounds : position du libellé qui a déclenché la détection (contour rouge du mode debug)
data class AdHit(val bounds: Bounds, val adText: String, val packageName: String, val labelBounds: Bounds)

object AdDetector {
    private const val MAX_NODES = 3000
    private const val MAX_DEPTH = 60
    private const val MAX_CLIMB = 20
    private const val MAX_TEXT_LENGTH = 500
    private const val FALLBACK_HEIGHT_RATIO = 0.25

    // minSidePx : taille minimale d'un bloc une fois découpé à la fenêtre (48 dp)
    fun detect(root: UiNode, profile: AppProfile, minSidePx: Int): List<AdHit> {
        val window = root.boundsInScreen()
        val hits = findLabels(root, profile).mapNotNull { label ->
            val container = findContainer(label, root, window) ?: return@mapNotNull null
            val visible = container.boundsInScreen().intersect(window) ?: return@mapNotNull null
            if (visible.width < minSidePx || visible.height < minSidePx) return@mapNotNull null
            AdHit(visible, collectText(container), profile.packageName, label.boundsInScreen())
        }
        return keepOutermost(hits)
    }

    // Parcours en largeur, borné pour rester rapide sur les arbres énormes
    private fun findLabels(root: UiNode, profile: AppProfile): List<UiNode> {
        val labels = mutableListOf<UiNode>()
        val queue = ArrayDeque<Pair<UiNode, Int>>()
        queue.addLast(root to 0)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++
            if (!node.isVisibleToUser) continue
            if (isLabel(node, profile)) labels += node
            if (depth < MAX_DEPTH) node.children.forEach { queue.addLast(it to depth + 1) }
        }
        return labels
    }

    private fun isLabel(node: UiNode, profile: AppProfile): Boolean {
        val candidates = listOfNotNull(node.text, node.contentDescription)
        return candidates.any { candidate ->
            val trimmed = candidate.trim()
            profile.sponsorLabels.any { it.matches(trimmed) }
        }
    }

    // Le post entier est l'enfant direct du parent scrollable
    private fun findContainer(label: UiNode, root: UiNode, window: Bounds): UiNode? {
        var node = label
        repeat(MAX_CLIMB) {
            val parent = node.parent ?: return@repeat
            if (parent.isScrollable) return node
            node = parent
        }
        // Repli sans la racine, qui ferait recouvrir tout l'écran
        val minHeight = window.height * FALLBACK_HEIGHT_RATIO
        return generateSequence(label.parent) { it.parent }
            .takeWhile { it !== root }
            .firstOrNull { it.boundsInScreen().height > minHeight }
    }

    // Ce texte sert uniquement à choisir le thème : ne jamais le stocker ni le logger en release
    private fun collectText(container: UiNode): String {
        val builder = StringBuilder()
        val stack = ArrayDeque<UiNode>()
        stack.addLast(container)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES && builder.length < MAX_TEXT_LENGTH) {
            val node = stack.removeLast()
            visited++
            listOfNotNull(node.text, node.contentDescription).forEach {
                builder.append(it).append(' ')
            }
            node.children.asReversed().forEach { stack.addLast(it) }
        }
        return builder.toString().trim().take(MAX_TEXT_LENGTH).lowercase()
    }

    // Un conteneur inclus dans un autre est retiré, et plusieurs libellés d'un même post comptent pour un
    private fun keepOutermost(hits: List<AdHit>): List<AdHit> {
        val kept = mutableListOf<AdHit>()
        hits.sortedByDescending { it.bounds.area }.forEach { hit ->
            if (kept.none { it.bounds.contains(hit.bounds) }) kept += hit
        }
        return kept
    }
}
