package com.juaris.app

import android.app.Application

class JuarisApp : Application() {

    override fun onCreate() {
        super.onCreate()
       
        try {
            // Echte, einmalige Vorinitialisierung deiner Room-Datenbank beim Systemstart.
            // Verhindert Multithreading-Konflikte und Hintergrund-Abstürze im Keim!
            JuarisDatabase.getDatabase(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
