package com.juaris.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.N)
class JuarisTileService : TileService() {

    companion object {
        private const val TAG = "JuarisTileEngine"
    }

    override fun onStartListening() {
        super.onStartListening()
        // KORREKTUR 1: Zustand wird bei jedem Herunterwischen der Statusleiste 
        // ECHT und dynamisch geprüft, statt dem Nutzer "Blindflug-Sicherheit" vorzugaukeln!
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        // KORREKTUR 2: Verwende für Android 14+ ein sauberes Ausführungs-Protokoll, 
        // falls das Gerät im Sperrbildschirm bedient wird.
        val action = Runnable {
            if (tile.state == Tile.STATE_ACTIVE) {
                tile.state = Tile.STATE_INACTIVE
                // KORREKTUR 3: Alle Texte sauber an die 12 Weltsprachen der strings.xml gekoppelt!
                tile.label = getString(R.string.tile_label_inactive)
                
                // Stoppt den Haupt-Schutzdienst sicher
                val stopIntent = Intent(this, JuarisNotificationListenerService::class.java)
                stopService(stopIntent)
            } else {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label_active)
                
                val intent = Intent(this, JuarisNotificationListenerService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this, intent)
                    } else {
                        startService(intent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler beim Aktivieren des Schutzes via Kachel: ${e.message}")
                }
            }
            tile.updateTile()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.normal && isLocked) {
            // Entsperrt das Gerät (falls PIN/Muster nötig) und führt die Aktion crashsicher aus
            @Suppress("DEPRECATION")
            startUnlockAndRun(action)
        } else {
            action.run()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        
        // Prüfen, ob der NotificationListenerService im System tatsächlich aktiv läuft
        val isServiceRunning = isNotificationServiceRunning()

        if (isServiceRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.tile_label_active)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_label_inactive)
        }
        tile.updateTile()
    }

    private fun isNotificationServiceRunning(): Boolean {
        // Überprüft verlässlich, ob der Nutzer der App die System-Berechtigung erteilt hat 
        // und der Service im Android-Betriebssystem als aktiv registriert ist.
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }
}
