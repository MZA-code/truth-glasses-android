package io.github.mzacode.truthglasses.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.mzacode.truthglasses.detect.AppProfiles

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    // Par défaut, toutes les applis cibles : celles qui ne sont pas installées ne coûtent rien
    private val allPackages = AppProfiles.all(context.packageName).map { it.packageName }.toSet()

    var glassesOn: Boolean
        get() = prefs.getBoolean(KEY_GLASSES_ON, true)
        set(value) = prefs.edit { putBoolean(KEY_GLASSES_ON, value) }

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DEBUG_MODE, value) }

    val enabledPackages: Set<String>
        get() = prefs.getStringSet(KEY_ENABLED_PACKAGES, null)?.toSet() ?: allPackages

    fun setPackageEnabled(packageName: String, enabled: Boolean) {
        val packages = if (enabled) enabledPackages + packageName else enabledPackages - packageName
        prefs.edit { putStringSet(KEY_ENABLED_PACKAGES, packages) }
    }

    // SharedPreferences ne garde qu'une référence faible : l'appelant doit conserver le listener
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    companion object {
        private const val FILE_NAME = "truth_glasses"
        const val KEY_GLASSES_ON = "glasses_on"
        const val KEY_ENABLED_PACKAGES = "enabled_packages"
        const val KEY_DEBUG_MODE = "debug_mode"
    }
}
