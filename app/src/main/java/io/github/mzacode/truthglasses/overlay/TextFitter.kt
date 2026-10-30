package io.github.mzacode.truthglasses.overlay

import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.floor

interface TextMeasurer {
    fun width(text: String, sizePx: Float): Float
    // Hauteur d'une ligne sans interligne
    fun lineHeight(sizePx: Float): Float
}

class PaintTextMeasurer(typeface: Typeface?) : TextMeasurer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface }

    override fun width(text: String, sizePx: Float): Float {
        paint.textSize = sizePx
        return paint.measureText(text)
    }

    override fun lineHeight(sizePx: Float): Float {
        paint.textSize = sizePx
        return paint.fontMetrics.let { it.descent - it.ascent }
    }
}

data class FittedText(val sizePx: Float, val lines: List<String>)

// Cherche la plus grande taille de texte qui tient dans le bloc. Le découpage en lignes est fait
// ici plutôt qu'avec StaticLayout : le dessin reprend exactement ces lignes, et tout reste
// testable sans appareil.
class TextFitter(
    private val measurer: TextMeasurer,
    private val minSizePx: Float,
    private val maxSizePx: Float
) {
    private val cache = object : LinkedHashMap<CacheKey, FittedText>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, FittedText>) =
            size > CACHE_SIZE
    }

    private data class CacheKey(val text: String, val width: Int, val height: Int)

    fun lineStep(sizePx: Float) = measurer.lineHeight(sizePx) * LINE_SPACING

    fun fit(text: String, width: Float, height: Float): FittedText {
        val key = CacheKey(text, width.toInt(), height.toInt())
        return cache.getOrPut(key) { compute(text, key.width.toFloat(), key.height.toFloat()) }
    }

    private fun compute(text: String, width: Float, height: Float): FittedText {
        val words = text.split(' ').filter { it.isNotEmpty() }
        val minFit = layoutAt(words, minSizePx, width, height)
        // Même la plus petite taille ne tient pas : on autorise la coupure des mots
        if (minFit == null) return FittedText(minSizePx, wrap(words, minSizePx, width, breakWords = true))

        var best: FittedText = minFit
        var low = ceil(minSizePx).toInt()
        var high = floor(maxSizePx).toInt()
        while (low < high) {
            val mid = (low + high + 1) / 2
            val fitted = layoutAt(words, mid.toFloat(), width, height)
            if (fitted != null) {
                best = fitted
                low = mid
            } else {
                high = mid - 1
            }
        }
        return best
    }

    // Renvoie null si un mot ne tient pas sur une ligne ou si le texte est trop haut
    private fun layoutAt(words: List<String>, size: Float, width: Float, height: Float): FittedText? {
        if (words.any { measurer.width(it, size) > width }) return null
        val lines = wrap(words, size, width, breakWords = false)
        if (lines.size * lineStep(size) > height) return null
        return FittedText(size, lines)
    }

    private fun wrap(words: List<String>, size: Float, width: Float, breakWords: Boolean): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        val pieces = if (breakWords) words.flatMap { breakWord(it, size, width) } else words
        for (piece in pieces) {
            val candidate = if (current.isEmpty()) piece else "$current $piece"
            if (current.isEmpty() || measurer.width(candidate, size) <= width) {
                current = candidate
            } else {
                lines += current
                current = piece
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    // Coupe un mot trop large en morceaux qui tiennent chacun sur une ligne
    private fun breakWord(word: String, size: Float, width: Float): List<String> {
        if (measurer.width(word, size) <= width) return listOf(word)
        val chunks = mutableListOf<String>()
        var chunk = ""
        for (char in word) {
            if (chunk.isNotEmpty() && measurer.width(chunk + char, size) > width) {
                chunks += chunk
                chunk = ""
            }
            chunk += char
        }
        if (chunk.isNotEmpty()) chunks += chunk
        return chunks
    }

    private companion object {
        const val LINE_SPACING = 0.95f
        const val CACHE_SIZE = 64
    }
}
