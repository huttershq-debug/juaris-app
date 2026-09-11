package com.juaris.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner

class AirGestureCore(private val context: Context) : SensorEventListener {

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var listener: ((GestureAction) -> Unit)? = null

    private var lastTriggerTime = 0L

    init {
        if (proximitySensor == null) {
            Log.e("AirGestureCore", "❌ FEHLER: Kein Proximity-Sensor auf diesem Gerät gefunden!")
        } else {
            Log.d("AirGestureCore", "✅ Proximity-Sensor gefunden. Max Range: ${proximitySensor.maximumRange}")
        }
    }

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        this.listener = onGestureDetected
        if (proximitySensor != null) {
            // WICHTIG: SENSOR_DELAY_FASTEST verwenden, damit schnelle Bewegungen nicht verschluckt werden
            val success = sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST)
            Log.d("AirGestureCore", "Sensor-Registrierung erfolgreich gestartet: $success")
        } else {
            Log.e("AirGestureCore", "❌ Kann Gestenerkennung nicht starten, da Sensor null ist.")
        }
    }

    fun stopGestureDetection() {
        try {
            sensorManager.unregisterListener(this)
            Log.d("AirGestureCore", "Sensor-Listener erfolgreich unregistriert.")
        } catch (e: Exception) {
            Log.e("AirGestureCore", "Fehler beim Unregistrieren: ${e.message}")
        }
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            Log.d("AirGestureCore", "📡 Sensor Event empfangen! Aktueller Abstand: $distance")

            // Auf dem Galaxy S23: 0.0f bedeutet Hand/Finger ist direkt am oberen Rand
            if (distance == 0.0f || distance < (proximitySensor?.maximumRange ?: 5.0f)) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTriggerTime > 600) { // 600ms Cooldown
                    lastTriggerTime = currentTime
                    Log.d("AirGestureCore", "🎯 GESTE ERKANNT! Liefere SWIPE_DOWN an UI aus.")
                    listener?.invoke(GestureAction.SWIPE_DOWN)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nicht benötigt
    }
}

