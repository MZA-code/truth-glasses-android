package io.github.mzacode.truthglasses.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import io.github.mzacode.truthglasses.R

// labelRect : position du libellé détecté, dessinée seulement en mode debug
data class Cover(val rect: RectF, val message: String, val labelRect: RectF? = null)

// Dessine les blocs. Les rectangles sont en coordonnées écran.
class CoverView(context: Context) : View(context) {
    private var covers: List<Cover> = emptyList()
    private val location = IntArray(2)
    private val inner = RectF()

    private val borderWidth = dp(2f)
    private val padding = dp(8f)
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#F2F2F2")
    }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }
    private val typeface = resources.getFont(R.font.anton_regular)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = this@CoverView.typeface
    }
    private val fitter = TextFitter(PaintTextMeasurer(typeface), sp(8f), sp(160f))
    private val containerDebugPaint = debugPaint(Color.BLUE)
    private val labelDebugPaint = debugPaint(Color.RED)

    // Mode debug : contour bleu autour des conteneurs, rouge autour des libellés
    var debug = false
        set(value) {
            field = value
            invalidate()
        }

    fun setCovers(newCovers: List<Cover>) {
        if (newCovers == covers) return
        covers = newCovers
        invalidate()
    }

    // Décalage immédiat au scroll, sans attendre le prochain scan
    fun offsetBy(dx: Float, dy: Float) {
        if (covers.isEmpty()) return
        covers = covers.map { cover ->
            cover.copy(
                rect = RectF(cover.rect).apply { offset(dx, dy) },
                labelRect = cover.labelRect?.let { RectF(it).apply { offset(dx, dy) } }
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Le calque peut ne pas démarrer en (0, 0) : on repasse en coordonnées écran
        getLocationOnScreen(location)
        canvas.translate(-location[0].toFloat(), -location[1].toFloat())
        covers.forEach { drawCover(canvas, it) }
        if (debug) {
            covers.forEach { cover ->
                canvas.drawRect(cover.rect, containerDebugPaint)
                cover.labelRect?.let { canvas.drawRect(it, labelDebugPaint) }
            }
        }
    }

    private fun drawCover(canvas: Canvas, cover: Cover) {
        val rect = cover.rect
        canvas.drawRect(rect, fillPaint)
        // La bordure est tracée à l'intérieur du bloc
        val half = borderWidth / 2
        canvas.drawRect(rect.left + half, rect.top + half, rect.right - half, rect.bottom - half, borderPaint)

        val inset = borderWidth + padding
        inner.set(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        if (inner.width() <= 0 || inner.height() <= 0) return
        val fitted = fitter.fit(cover.message, inner.width(), inner.height())
        textPaint.textSize = fitted.sizePx
        val step = fitter.lineStep(fitted.sizePx)
        val top = inner.centerY() - fitted.lines.size * step / 2
        // Centre chaque ligne verticalement dans sa tranche de hauteur step
        val baselineOffset = step / 2 - (textPaint.ascent() + textPaint.descent()) / 2

        canvas.save()
        canvas.clipRect(inner)
        fitted.lines.forEachIndexed { index, line ->
            canvas.drawText(line, inner.centerX(), top + index * step + baselineOffset, textPaint)
        }
        canvas.restore()
    }

    private fun debugPaint(debugColor: Int) = Paint().apply {
        color = debugColor
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun sp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
