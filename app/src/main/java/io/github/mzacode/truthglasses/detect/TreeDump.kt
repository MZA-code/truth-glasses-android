package io.github.mzacode.truthglasses.detect

// Décrit l'arbre de l'écran, une ligne par nœud, pour le mode debug
object TreeDump {
    private const val MAX_NODES = 3000
    private const val MAX_DEPTH = 60

    // redact : remplace le texte et la contentDescription par leur longueur (builds release)
    fun lines(root: UiNode, redact: Boolean): List<String> {
        val lines = mutableListOf<String>()
        val stack = ArrayDeque<Pair<UiNode, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && lines.size < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            lines += "  ".repeat(depth) + describe(node, redact)
            if (depth < MAX_DEPTH) node.children.asReversed().forEach { stack.addLast(it to depth + 1) }
        }
        return lines
    }

    private fun describe(node: UiNode, redact: Boolean) = buildString {
        append(node.className?.toString()?.substringAfterLast('.') ?: "?")
        appendContent("text", node.text, redact)
        appendContent("desc", node.contentDescription, redact)
        node.viewIdResourceName?.let { append(" id=").append(it) }
        if (node.isScrollable) append(" scrollable")
        if (!node.isVisibleToUser) append(" invisible")
        val bounds = node.boundsInScreen()
        append(" [${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]")
    }

    private fun StringBuilder.appendContent(name: String, value: CharSequence?, redact: Boolean) {
        if (value.isNullOrEmpty()) return
        append(' ').append(name).append('=')
        if (redact) append('<').append(value.length).append(" chars>") else append('"').append(value).append('"')
    }
}
