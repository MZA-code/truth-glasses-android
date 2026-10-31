package io.github.mzacode.truthglasses.detect

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

// Équivalent de android.graphics.Rect, qui n'a aucun comportement dans les tests JVM
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Long get() = width.toLong() * height.toLong()

    fun contains(other: Bounds) =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

    // Renvoie null si les deux zones ne se recouvrent pas
    fun intersect(other: Bounds): Bounds? {
        val result = Bounds(
            maxOf(left, other.left),
            maxOf(top, other.top),
            minOf(right, other.right),
            minOf(bottom, other.bottom)
        )
        return result.takeIf { it.width > 0 && it.height > 0 }
    }
}

interface UiNode {
    val text: CharSequence?
    val contentDescription: CharSequence?
    val className: CharSequence?
    val viewIdResourceName: String?
    val isScrollable: Boolean
    val isVisibleToUser: Boolean
    fun boundsInScreen(): Bounds
    val parent: UiNode?
    val children: List<UiNode>
}

// Enveloppe l'arbre d'un scan. Appeler recycle() à la fin du scan.
class AccessibilityUiTree(root: AccessibilityNodeInfo) {
    private val obtained = mutableListOf(root)

    val root: UiNode = Node(root, null)

    fun recycle() {
        // recycle() ne fait plus rien à partir d'Android 13
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            obtained.forEach { it.recycle() }
        }
        obtained.clear()
    }

    private inner class Node(
        private val info: AccessibilityNodeInfo,
        override val parent: UiNode?
    ) : UiNode {
        // Le texte des champs de saisie n'est jamais lu : ce que tape l'utilisateur ne sort pas
        // de cette enveloppe, ni vers la détection ni vers le mode debug
        override val text: CharSequence?
            get() = if (info.isEditable || info.isPassword) null else info.text
        override val contentDescription: CharSequence? get() = info.contentDescription
        override val className: CharSequence? get() = info.className
        override val viewIdResourceName: String? get() = info.viewIdResourceName
        override val isScrollable: Boolean get() = info.isScrollable
        override val isVisibleToUser: Boolean get() = info.isVisibleToUser

        override fun boundsInScreen(): Bounds {
            val rect = Rect()
            info.getBoundsInScreen(rect)
            return Bounds(rect.left, rect.top, rect.right, rect.bottom)
        }

        override val children: List<UiNode> by lazy {
            (0 until info.childCount).mapNotNull { index ->
                info.getChild(index)?.let { child ->
                    obtained += child
                    Node(child, this)
                }
            }
        }
    }
}
