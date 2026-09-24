package com.juaris.app

import android.app.Application

class JuarisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Hier können globale Initialisierungen (z.B. für Room-Datenbank oder lokale Dienste) stattfinden
    }
}
