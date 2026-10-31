package io.github.mzacode.truthglasses.detect

// Décrit l'arbre de l'écran, une ligne par nœud, pour le mode debug
object TreeDump {
    private const val MAX_NODES = 3000
    private const val MAX_DEPTH = 60
    private const val MAX_REVEALED_LENGTH = 80

    // Ce qui ressemble à un libellé de pub, même collé à un autre texte : sert à relever les
    // libellés des vraies applis sans faire sortir le contenu du fil
    val LABEL_HINTS = Regex("sponsor|publicit|annonce|promot|\\bad\\b|\\bpub\\b", RegexOption.IGNORE_CASE)

    // Le texte et la contentDescription sont remplacés par leur longueur, sauf ceux qui
    // correspondent à reveal (jamais en release), coupés à 80 caractères
    fun lines(root: UiNode, reveal: Regex? = null): List<String> {
        val lines = mutableListOf<String>()
        val stack = ArrayDeque<Pair<UiNode, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty() && lines.size < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            lines += "  ".repeat(depth) + describe(node, reveal)
            if (depth < MAX_DEPTH) node.children.asReversed().forEach { stack.addLast(it to depth + 1) }
        }
        return lines
    }

    private fun describe(node: UiNode, reveal: Regex?) = buildString {
        append(node.className?.toString()?.substringAfterLast('.') ?: "?")
        appendContent("text", node.text, reveal)
        appendContent("desc", node.contentDescription, reveal)
        node.viewIdResourceName?.let { append(" id=").append(it) }
        if (node.isScrollable) append(" scrollable")
        if (!node.isVisibleToUser) append(" invisible")
        val bounds = node.boundsInScreen()
        append(" [${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]")
    }

    private fun StringBuilder.appendContent(name: String, value: CharSequence?, reveal: Regex?) {
        if (value.isNullOrEmpty()) return
        append(' ').append(name).append('=')
        if (reveal?.containsMatchIn(value) == true) {
            // Un retour à la ligne couperait le nœud en plusieurs lignes de log
            append('"').append(value.toString().take(MAX_REVEALED_LENGTH).replace(LINE_BREAK, "\\\\n")).append('"')
        } else {
            append('<').append(value.length).append(" chars>")
        }
    }

    private val LINE_BREAK = Regex("\r\n|\r|\n")
}
