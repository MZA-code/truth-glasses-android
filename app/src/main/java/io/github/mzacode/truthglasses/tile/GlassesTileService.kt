package io.github.mzacode.truthglasses.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.mzacode.truthglasses.MainActivity
import io.github.mzacode.truthglasses.R
import io.github.mzacode.truthglasses.service.GlassesService
import io.github.mzacode.truthglasses.settings.Prefs
import io.github.mzacode.truthglasses.settings.appLocalizedResources

class GlassesTileService : TileService() {
    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        // Sans le service, la tuile ne peut rien faire : on renvoie vers l'accueil et son guide
        if (!GlassesService.isEnabled(this)) {
            openHome()
            return
        }
        val prefs = Prefs(this)
        prefs.glassesOn = !prefs.glassesOn
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val on = GlassesService.isEnabled(this) && Prefs(this).glassesOn
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = appLocalizedResources().getString(R.string.tile_label)
        tile.updateTile()
    }

    // La version avec Intent n'est appelée qu'avant l'API 34, où celle avec PendingIntent n'existe pas
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openHome() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
