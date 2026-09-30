package com.juaris.app

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class JuarisTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_ACTIVE) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Juaris: Aus"
            
            // Stoppt den Haupt-Schutzdienst sicher, falls vom Nutzer gewünscht
            val stopIntent = Intent(this, JuarisNotificationListenerService::class.java)
            stopService(stopIntent)
        } else {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Juaris: Aktiv"
            
            // KORREKTUR: Weckt den privilegierten Omni-Guard NotificationListenerService.
            // Dieser zieht deine VPN-Firewall im onCreate() ab Android 14 absolut crashsicher mit hoch!
            val intent = Intent(this, JuarisNotificationListenerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        // Startet standardmäßig im aktiven Look als 360° Schutz-Indikator
        tile.state = Tile.STATE_ACTIVE
        tile.label = "Juaris 360° Schutz"
        tile.updateTile()
    }
}

