package com.juaris.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JuarisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Datenbank im Hintergrund oeffnen: Keystore, native Bibliothek und Schema-Pruefung
        // blockieren so nie den App-Start.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                JuarisDatabase.getDatabase(this@JuarisApp)
            } catch (t: Throwable) {
                Log.e("JuarisApp", "DB-Vorinitialisierung fehlgeschlagen: ${t.javaClass.simpleName}")
            }
        }
    }
}
