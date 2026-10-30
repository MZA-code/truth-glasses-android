package io.github.mzacode.truthglasses

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mzacode.truthglasses.detect.AppProfiles
import io.github.mzacode.truthglasses.service.GlassesService
import io.github.mzacode.truthglasses.settings.Prefs
import io.github.mzacode.truthglasses.tile.GlassesTileService
import io.github.mzacode.truthglasses.ui.TruthGlassesTheme

class MainActivity : ComponentActivity() {
    private lateinit var prefs: Prefs
    private var state by mutableStateOf(HomeState())
    // Gardé en champ : SharedPreferences n'en conserve qu'une référence faible
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        prefs.registerListener(prefsListener)
        val actions = HomeActions(
            openAppInfo = {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                )
            },
            openAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            addTile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ({ requestAddTile() }) else null,
            setGlassesOn = { prefs.glassesOn = it },
            setAppEnabled = { pkg, enabled -> prefs.setPackageEnabled(pkg, enabled) },
            openSandbox = { startActivity(Intent(this, SandboxActivity::class.java)) },
            setDebugMode = { prefs.debugMode = it }
        )
        setContent {
            TruthGlassesTheme {
                HomeScreen(state, actions)
            }
        }
    }

    // Le service a pu être activé dans les réglages d'accessibilité
    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        prefs.unregisterListener(prefsListener)
        super.onDestroy()
    }

    private fun refresh() {
        val enabled = prefs.enabledPackages
        state = HomeState(
            serviceEnabled = GlassesService.isEnabled(this),
            glassesOn = prefs.glassesOn,
            debugMode = prefs.debugMode,
            apps = AppProfiles.all(packageName).mapNotNull { profile ->
                appLabel(profile.packageName)?.let { label ->
                    TargetApp(profile.packageName, label, profile.verifiedOn != null, profile.packageName in enabled)
                }
            }
        )
    }

    // Nom affiché de l'appli, ou null si elle n'est pas installée (packages déclarés dans <queries>)
    private fun appLabel(targetPackage: String): String? {
        if (targetPackage == packageName) return getString(R.string.sandbox_name)
        return try {
            packageManager.getApplicationInfo(targetPackage, 0).loadLabel(packageManager).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddTile() {
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, GlassesTileService::class.java),
            getString(R.string.tile_label),
            Icon.createWithResource(this, R.drawable.ic_tile_glasses),
            mainExecutor
        ) {}
    }
}
