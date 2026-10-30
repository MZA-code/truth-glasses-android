package io.github.mzacode.truthglasses.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.accessibility.AccessibilityEvent
import io.github.mzacode.truthglasses.R
import io.github.mzacode.truthglasses.detect.AccessibilityUiTree
import io.github.mzacode.truthglasses.detect.AdDetector
import io.github.mzacode.truthglasses.detect.AppProfile
import io.github.mzacode.truthglasses.detect.AppProfiles
import io.github.mzacode.truthglasses.overlay.Cover
import io.github.mzacode.truthglasses.overlay.OverlayController

class GlassesService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: OverlayController? = null
    private var profiles: Map<String, AppProfile> = emptyMap()
    private var coversShown = false
    private var lastScanAt = 0L
    private var throttledScanPending = false

    private val throttledScan = Runnable {
        throttledScanPending = false
        scan()
    }
    private val finalScan = Runnable { scan() }

    // packageNames filtre aussi les événements de sortie : quand l'utilisateur quitte une appli
    // cible, on n'est pas prévenu. On vérifie donc la fenêtre active tant qu'un bloc est affiché.
    // Hors des applis cibles, rootInActiveWindow vaut null : Android ne nous donne pas leur arbre.
    private val leaveCheck = object : Runnable {
        override fun run() {
            val activePackage = rootInActiveWindow?.packageName?.toString()
            if (activePackage in profiles) {
                handler.postDelayed(this, LEAVE_CHECK_MS)
            } else {
                hideCovers()
            }
        }
    }

    override fun onServiceConnected() {
        profiles = AppProfiles.all(packageName).associateBy { it.packageName }
        serviceInfo = serviceInfo.apply { packageNames = profiles.keys.toTypedArray() }
        overlay = OverlayController(this).also { it.attach() }
        scheduleScan()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Décalage immédiat pour que le scroll reste fluide, le scan corrigera ensuite
            val dx = event.scrollDeltaX.takeUnless { it == SCROLL_DELTA_UNDEFINED } ?: 0
            val dy = event.scrollDeltaY.takeUnless { it == SCROLL_DELTA_UNDEFINED } ?: 0
            if (dx != 0 || dy != 0) overlay?.offsetBy(-dx.toFloat(), -dy.toFloat())
        }
        scheduleScan()
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

    // Un scan au plus toutes les 100 ms, plus un scan final 150 ms après le dernier événement
    private fun scheduleScan() {
        handler.removeCallbacks(finalScan)
        handler.postDelayed(finalScan, FINAL_SCAN_DELAY_MS)
        if (throttledScanPending) return
        throttledScanPending = true
        val elapsed = SystemClock.uptimeMillis() - lastScanAt
        handler.postDelayed(throttledScan, (SCAN_INTERVAL_MS - elapsed).coerceAtLeast(0))
    }

    private fun scan() {
        lastScanAt = SystemClock.uptimeMillis()
        // Racine absente : fenêtre en transition, leaveCheck masquera les blocs si on est sorti
        val root = rootInActiveWindow ?: return
        val tree = AccessibilityUiTree(root)
        try {
            val profile = profiles[root.packageName?.toString()]
            if (profile == null) {
                hideCovers()
                return
            }
            val minSidePx = dp(MIN_BLOCK_SIDE_DP).toInt()
            val hits = AdDetector.detect(tree.root, profile, minSidePx)
            // Message fixe en attendant le décodeur (jalon M3)
            val message = resources.getStringArray(R.array.msg_generic).first()
            val covers = hits.map {
                val bounds = it.bounds
                Cover(
                    RectF(
                        bounds.left.toFloat(),
                        bounds.top.toFloat(),
                        bounds.right.toFloat(),
                        bounds.bottom.toFloat()
                    ),
                    message
                )
            }
            showCovers(covers)
        } finally {
            tree.recycle()
        }
    }

    private fun showCovers(covers: List<Cover>) {
        if (covers.isEmpty()) {
            hideCovers()
            return
        }
        overlay?.show(covers)
        if (!coversShown) {
            coversShown = true
            handler.postDelayed(leaveCheck, LEAVE_CHECK_MS)
        }
    }

    private fun hideCovers() {
        overlay?.hide()
        coversShown = false
        handler.removeCallbacks(leaveCheck)
    }

    private fun release() {
        handler.removeCallbacksAndMessages(null)
        throttledScanPending = false
        coversShown = false
        overlay?.detach()
        overlay = null
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private companion object {
        const val SCAN_INTERVAL_MS = 100L
        const val FINAL_SCAN_DELAY_MS = 150L
        const val LEAVE_CHECK_MS = 500L
        const val MIN_BLOCK_SIDE_DP = 48f
        // Valeur renvoyée par scrollDeltaX/Y quand l'appli ne les remplit pas
        const val SCROLL_DELTA_UNDEFINED = -1
    }
}
