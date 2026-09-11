package com.juaris.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LifecycleOwner

class AirGestureCore(private val context: Context) : SensorEventListener {

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var listener: ((GestureAction) -> Unit)? = null

    private var lastTriggerTime = 0L

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        this.listener = onGestureDetected
        proximitySensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopGestureDetection() {
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            // Ignorieren
        }
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = proximitySensor?.maximumRange ?: 5.0f
            
            // Wenn die Hand nah am oberen Rand des Handys ist (Entfernung < Max-Reichweite)
            if (distance < maxRange) {
                val currentTime = System.currentTimeMillis()
                // 800ms Cooldown, damit ein Winken nicht direkt 5 Tabs auf einmal weiterschaltet
                if (currentTime - lastTriggerTime > 800) {
                    lastTriggerTime = currentTime
                    // Löst den Tab-Wechsel aus
                    listener?.invoke(GestureAction.SWIPE_DOWN)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nicht benötigt
    }
}


