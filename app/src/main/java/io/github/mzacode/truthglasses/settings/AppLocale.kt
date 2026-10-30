package io.github.mzacode.truthglasses.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build

// Ressources dans la langue choisie pour l'appli (Réglages Android → Langue de l'appli).
// Quand c'est un service qui démarre le processus, ses ressources restent dans la langue du
// système (constaté sur l'API 37) : on applique donc cette langue à la main.
fun Context.appLocalizedResources(): Resources {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return resources
    val locales = getSystemService(LocaleManager::class.java).applicationLocales
    if (locales.isEmpty) return resources
    val config = Configuration(resources.configuration).apply { setLocales(locales) }
    return createConfigurationContext(config).resources
}
