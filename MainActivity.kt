package com.localshield.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lädt unsere wunderschöne, sichere Startseite
        setContentView(R.layout.activity_main)
    }
}
