package io.github.mzacode.truthglasses.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityWindowInfo
import io.github.mzacode.truthglasses.BuildConfig
import io.github.mzacode.truthglasses.decode.MessageCatalog
import io.github.mzacode.truthglasses.decode.MessageDecoder
import io.github.mzacode.truthglasses.detect.AccessibilityUiTree
import io.github.mzacode.truthglasses.detect.AdDetector
import io.github.mzacode.truthglasses.detect.AppProfile
import io.github.mzacode.truthglasses.detect.AppProfiles
import io.github.mzacode.truthglasses.detect.Bounds
import io.github.mzacode.truthglasses.detect.TreeDump
import io.github.mzacode.truthglasses.overlay.Cover
import io.github.mzacode.truthglasses.overlay.OverlayController
import io.github.mzacode.truthglasses.settings.Prefs
import io.github.mzacode.truthglasses.settings.appLocalizedResources

class GlassesService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val handler = Handler(Looper.getMainLooper())
    private var prefs: Prefs? = null
    private var overlay: OverlayController? = null
    private var allProfiles: Map<String, AppProfile> = emptyMap()
    // Profils des applis activées par l'utilisateur
    private var profiles: Map<String, AppProfile> = emptyMap()
    private var catalog: MessageCatalog? = null
    private var coversShown = false
    private var hiddenByPanel = false
    private var watching = false
    private var lastScanAt = 0L
    private var throttledScanPending = false
    private val windowBounds = Rect()

    private val throttledScan = Runnable {
        throttledScanPending = false
        scan()
    }
    private val finalScan = Runnable { scan() }

    // packageNames filtre aussi les événements de sortie : quand l'utilisateur quitte une appli
    // cible, on n'est pas prévenu (hors des applis cibles, rootInActiveWindow vaut null). Le volet
    // des réglages rapides, lui, s'ouvre sans changer la fenêtre active. Tant qu'une appli cible
    // est au premier plan, on vérifie donc régulièrement ces deux cas.
    private val watch = object : Runnable {
        override fun run() {
            if (activeProfile() == null) {
                watching = false
                hideCovers()
                return
            }
            if (systemPanelOpen()) {
                if (coversShown) {
                    hideCovers()
                    hiddenByPanel = true
                }
            } else if (hiddenByPanel) {
                hiddenByPanel = false
                scheduleScan()
            }
            handler.postDelayed(this, WATCH_MS)
        }
    }

    override fun onServiceConnected() {
        val prefs = Prefs(this).also { prefs = it }
        allProfiles = AppProfiles.all(packageName).associateBy { it.packageName }
        catalog = MessageCatalog.from(appLocalizedResources())
        overlay = OverlayController(this).also {
            it.attach()
            it.debug = prefs.debugMode
        }
        applyEnabledPackages(prefs)
        prefs.registerListener(this)
        scheduleScan()
    }

    // La langue a pu changer : on recharge les messages
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        catalog = MessageCatalog.from(appLocalizedResources())
        scheduleScan()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val prefs = prefs ?: return
        when (key) {
            Prefs.KEY_GLASSES_ON -> if (prefs.glassesOn) scheduleScan() else stopCovering()
            Prefs.KEY_ENABLED_PACKAGES -> {
                applyEnabledPackages(prefs)
                scheduleScan()
            }
            Prefs.KEY_DEBUG_MODE -> overlay?.debug = prefs.debugMode
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val prefs = prefs ?: return
        if (!prefs.glassesOn) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Décalage immédiat pour que le scroll reste fluide, le scan corrigera ensuite
            val dx = event.scrollDeltaX.takeUnless { it == SCROLL_DELTA_UNDEFINED } ?: 0
            val dy = event.scrollDeltaY.takeUnless { it == SCROLL_DELTA_UNDEFINED } ?: 0
            if (dx != 0 || dy != 0) overlay?.offsetBy(-dx.toFloat(), -dy.toFloat())
        }
        if (prefs.debugMode && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            logTree()
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

    private fun applyEnabledPackages(prefs: Prefs) {
        val enabled = prefs.enabledPackages
        profiles = allProfiles.filterKeys { it in enabled }
        // Un tableau vide voudrait dire « toutes les applis » : notre propre package reste donc
        // toujours dans la liste. Les événements d'un package désactivé sont ignorés au scan.
        serviceInfo = serviceInfo.apply {
            packageNames = (profiles.keys + packageName).toTypedArray()
        }
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
        val prefs = prefs ?: return
        val messages = catalog ?: return
        if (!prefs.glassesOn) {
            stopCovering()
            return
        }
        // Racine absente : fenêtre en transition, watch masquera les blocs si on est sorti
        val root = rootInActiveWindow ?: return
        val tree = AccessibilityUiTree(root)
        try {
            val profile = profiles[root.packageName?.toString()]
            if (profile == null) {
                hideCovers()
                return
            }
            startWatching()
            val minSidePx = dp(MIN_BLOCK_SIDE_DP).toInt()
            val density = resources.displayMetrics.density
            val covers = AdDetector.detect(tree.root, profile, minSidePx).map {
                val message = MessageDecoder.decode(it.adText, it.packageName, it.bounds.width / density, messages)
                Cover(it.bounds.toRectF(), message, it.labelBounds.toRectF())
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
        if (systemPanelOpen()) {
            hideCovers()
            hiddenByPanel = true
            return
        }
        overlay?.show(covers)
        coversShown = true
    }

    private fun hideCovers() {
        overlay?.hide()
        coversShown = false
    }

    private fun stopCovering() {
        hideCovers()
        hiddenByPanel = false
        watching = false
        handler.removeCallbacks(watch)
    }

    private fun startWatching() {
        if (watching) return
        watching = true
        // La langue de l'appli a pu changer sans que le service soit prévenu
        catalog = MessageCatalog.from(appLocalizedResources())
        handler.postDelayed(watch, WATCH_MS)
    }

    private fun activeProfile(): AppProfile? =
        rootInActiveWindow?.packageName?.toString()?.let { profiles[it] }

    // Volet des réglages rapides ou des notifications : une fenêtre système plus haute que la
    // moitié de l'écran. Seuls le type et la taille des fenêtres sont lus.
    private fun systemPanelOpen(): Boolean {
        val minHeight = resources.displayMetrics.heightPixels / 2
        return windows.any { window ->
            window.getBoundsInScreen(windowBounds)
            window.type == AccessibilityWindowInfo.TYPE_SYSTEM && windowBounds.height() > minHeight
        }
    }

    private fun logTree() {
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() !in profiles) return
        val tree = AccessibilityUiTree(root)
        try {
            // En release, le contenu lu à l'écran n'apparaît jamais dans les logs
            Log.d(TAG, "--- ${root.packageName}")
            TreeDump.lines(tree.root, redact = !BuildConfig.DEBUG).forEach { Log.d(TAG, it) }
        } finally {
            tree.recycle()
        }
    }

    private fun release() {
        handler.removeCallbacksAndMessages(null)
        prefs?.unregisterListener(this)
        prefs = null
        throttledScanPending = false
        coversShown = false
        hiddenByPanel = false
        watching = false
        overlay?.detach()
        overlay = null
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

    private fun Bounds.toRectF() = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    companion object {
        private const val TAG = "TG"
        private const val SCAN_INTERVAL_MS = 100L
        private const val FINAL_SCAN_DELAY_MS = 150L
        private const val WATCH_MS = 300L
        private const val MIN_BLOCK_SIDE_DP = 48f
        // Valeur renvoyée par scrollDeltaX/Y quand l'appli ne les remplit pas
        private const val SCROLL_DELTA_UNDEFINED = -1

        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { info ->
                    val service = info.resolveInfo.serviceInfo
                    service.packageName == context.packageName && service.name == GlassesService::class.java.name
                }
        }
    }
}
