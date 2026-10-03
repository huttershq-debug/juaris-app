package com.juaris.app

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.N)
class JuarisTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_ACTIVE
        // KORREKTUR: Nutzt existierende Kern-Ressourcen statt fehlender Sonder-Labels
        tile.label = getString(R.string.status_filters_active)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val intent = Intent(this, JuarisNotificationListenerService::class.java)
        
        if (tile.state == Tile.STATE_ACTIVE) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.status_paused)
            stopService(intent)
        } else {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.status_filters_active)
            
            try {
                ContextCompat.startForegroundService(this, intent)
            } catch (e: Exception) {
                // KORREKTUR: Android 14/15 konforme Überlagerung bei gesperrtem Bildschirm
                unlockAndRun {
                    try { startService(intent) } catch (ex: Exception) {}
                }
            }
        }
        tile.updateTile()
    }
}
