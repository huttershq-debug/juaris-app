package com.localshield.v4

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this)
        textView.text = "🛡️ LocalShield läuft erfolgreich!"
        textView.textSize = 20f
        setContentView(textView)
    }
}
