package io.github.mzacode.truthglasses.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.View

data class Cover(val rect: RectF, val message: String)

// Dessine les blocs. Les rectangles sont en coordonnées écran.
class CoverView(context: Context) : View(context) {
    private var covers: List<Cover> = emptyList()
    private val location = IntArray(2)

    private val borderWidth = dp(2f)
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#F2F2F2")
    }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }
    // Taille fixe en attendant TextFitter et la police Anton (jalon M3)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 48f, resources.displayMetrics
        )
    }

    fun setCovers(newCovers: List<Cover>) {
        if (newCovers == covers) return
        covers = newCovers
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Le calque peut ne pas démarrer en (0, 0) : on repasse en coordonnées écran
        getLocationOnScreen(location)
        canvas.translate(-location[0].toFloat(), -location[1].toFloat())
        for (cover in covers) {
            canvas.drawRect(cover.rect, fillPaint)
            // La bordure est tracée à l'intérieur du bloc
            val inset = borderWidth / 2
            canvas.drawRect(
                cover.rect.left + inset,
                cover.rect.top + inset,
                cover.rect.right - inset,
                cover.rect.bottom - inset,
                borderPaint
            )
            val baseline = cover.rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
            canvas.drawText(cover.message, cover.rect.centerX(), baseline, textPaint)
        }
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}
