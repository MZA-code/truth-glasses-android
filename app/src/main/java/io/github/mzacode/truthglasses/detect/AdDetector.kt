package io.github.mzacode.truthglasses.detect

// labelBounds : position du libellé qui a déclenché la détection (contour rouge du mode debug),
// null pour une pub reconnue par AdMemory alors que son libellé n'est plus à l'écran.
// fingerprint : empreinte du bloc pour AdMemory.
data class AdHit(
    val bounds: Bounds,
    val adText: String,
    val packageName: String,
    val labelBounds: Bounds?,
    val fingerprint: Set<Int>
)

object AdDetector {
    private const val MAX_NODES = 3000
    private const val MAX_DEPTH = 60
    private const val MAX_CLIMB = 20
    private const val MAX_TEXT_LENGTH = 500
    private const val FALLBACK_HEIGHT_RATIO = 0.25
    // Les textes courts (« En savoir plus », pseudo, libellé) se retrouvent d'un bloc à l'autre :
    // seuls les textes longs (légende, titre) identifient une pub
    private const val MIN_FINGERPRINT_TEXT = 24

    private class Scan(val labels: List<UiNode>, val scrollables: List<UiNode>)

    // minSidePx : taille minimale d'un bloc une fois découpé à la fenêtre (48 dp).
    // memory : pubs déjà vues, recouvertes même quand leur libellé a quitté l'écran.
    fun detect(root: UiNode, profile: AppProfile, minSidePx: Int, memory: AdMemory? = null): List<AdHit> {
        val window = root.boundsInScreen()
        val scan = scanTree(root, profile)
        val labeled = scan.labels.mapNotNull { label ->
            val container = findContainer(label, root, window) ?: return@mapNotNull null
            hitFor(container, window, minSidePx, profile, label.boundsInScreen())
        }
        val remembered = if (memory == null || memory.isEmpty) {
            emptyList()
        } else {
            scan.scrollables
                .flatMap { it.children }
                .filter { it.isVisibleToUser && memory.knows(fingerprint(it)) }
                .mapNotNull { hitFor(it, window, minSidePx, profile, labelBounds = null) }
        }
        return keepOutermost(labeled + remembered)
    }

    private fun hitFor(container: UiNode, window: Bounds, minSidePx: Int, profile: AppProfile, labelBounds: Bounds?): AdHit? {
        val visible = container.boundsInScreen().intersect(window) ?: return null
        if (visible.width < minSidePx || visible.height < minSidePx) return null
        return AdHit(visible, collectText(container), profile.packageName, labelBounds, fingerprint(container))
    }

    // Parcours en largeur, borné pour rester rapide sur les arbres énormes
    private fun scanTree(root: UiNode, profile: AppProfile): Scan {
        val labels = mutableListOf<UiNode>()
        val scrollables = mutableListOf<UiNode>()
        val queue = ArrayDeque<Pair<UiNode, Int>>()
        queue.addLast(root to 0)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++
            if (!node.isVisibleToUser) continue
            if (isLabel(node, profile)) labels += node
            if (node.isScrollable) scrollables += node
            if (depth < MAX_DEPTH) node.children.forEach { queue.addLast(it to depth + 1) }
        }
        return Scan(labels, scrollables)
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
        walk(container) { text ->
            builder.append(text).append(' ')
            builder.length < MAX_TEXT_LENGTH
        }
        return builder.toString().trim().take(MAX_TEXT_LENGTH).lowercase()
    }

    // Hash des textes longs du bloc, y compris ceux des nœuds hors écran encore présents.
    // Les listes imbriquées sont exclues : une page qui contient tout le fil n'a pas d'empreinte,
    // sinon elle serait prise pour la pub qu'elle contient et le fil entier serait recouvert.
    private fun fingerprint(container: UiNode): Set<Int> {
        val hashes = mutableSetOf<Int>()
        val stack = ArrayDeque<UiNode>()
        stack.addLast(container)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++
            listOfNotNull(node.text, node.contentDescription).forEach {
                val normalized = it.toString().trim().lowercase()
                if (normalized.length >= MIN_FINGERPRINT_TEXT) hashes += normalized.hashCode()
            }
            node.children.filterNot { it.isScrollable }.forEach { stack.addLast(it) }
        }
        return hashes
    }

    // Parcours en profondeur des textes et contentDescription ; onText renvoie false pour arrêter
    private fun walk(container: UiNode, onText: (CharSequence) -> Boolean) {
        val stack = ArrayDeque<UiNode>()
        stack.addLast(container)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++
            listOfNotNull(node.text, node.contentDescription).forEach {
                if (!onText(it)) return
            }
            node.children.asReversed().forEach { stack.addLast(it) }
        }
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
