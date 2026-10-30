package io.github.mzacode.truthglasses.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.accessibility.AccessibilityEvent
import io.github.mzacode.truthglasses.R
import io.github.mzacode.truthglasses.overlay.Cover
import io.github.mzacode.truthglasses.overlay.OverlayController

class GlassesService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: OverlayController? = null
    private var coverShown = false

    private val targetPackages: Set<String> get() = setOf(packageName)

    // packageNames filtre aussi les événements de sortie : quand l'utilisateur quitte une appli
    // cible, on n'est pas prévenu. On vérifie donc la fenêtre active tant qu'un bloc est affiché.
    // Hors des applis cibles, rootInActiveWindow vaut null : Android ne nous donne pas leur arbre.
    private val leaveCheck = object : Runnable {
        override fun run() {
            val activePackage = rootInActiveWindow?.packageName?.toString()
            if (activePackage in targetPackages) {
                handler.postDelayed(this, LEAVE_CHECK_MS)
            } else {
                hideCover()
            }
        }
    }

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply { packageNames = targetPackages.toTypedArray() }
        overlay = OverlayController(this).also { it.attach() }
        refresh()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        refresh()
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    // Seul le nom du package de la fenêtre active est lu ici
    private fun refresh() {
        val activePackage = rootInActiveWindow?.packageName?.toString() ?: return
        if (activePackage in targetPackages) showTestCover() else hideCover()
    }

    // Bloc de test du jalon M1, remplacé par la détection au jalon M2
    private fun showTestCover() {
        val metrics = resources.displayMetrics
        fun dp(value: Float) =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics)
        val rect = RectF(dp(16f), dp(96f), metrics.widthPixels - dp(16f), dp(256f))
        val message = resources.getStringArray(R.array.msg_generic).first()
        overlay?.show(listOf(Cover(rect, message)))
        if (!coverShown) {
            coverShown = true
            handler.postDelayed(leaveCheck, LEAVE_CHECK_MS)
        }
    }

    private fun hideCover() {
        overlay?.hide()
        coverShown = false
        handler.removeCallbacks(leaveCheck)
    }

    private fun release() {
        handler.removeCallbacks(leaveCheck)
        coverShown = false
        overlay?.detach()
        overlay = null
    }

    private companion object {
        const val LEAVE_CHECK_MS = 500L
    }
}
