package io.github.mzacode.truthglasses.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

// Ajoute et retire le calque. Le type TYPE_ACCESSIBILITY_OVERLAY impose un contexte de service.
class OverlayController(service: AccessibilityService) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val coverView = CoverView(service)
    private var attached = false

    fun attach() {
        if (attached) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // FLAG_NOT_TOUCHABLE laisse passer les touchers vers l'appli en dessous
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        coverView.visibility = View.GONE
        windowManager.addView(coverView, params)
        attached = true
    }

    fun show(covers: List<Cover>) {
        coverView.setCovers(covers)
        coverView.visibility = View.VISIBLE
    }

    fun hide() {
        coverView.setCovers(emptyList())
        coverView.visibility = View.GONE
    }

    fun detach() {
        if (!attached) return
        windowManager.removeView(coverView)
        attached = false
    }
}
