package com.juaris.app

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    private var listener: ((GestureAction) -> Unit)? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var lastTriggerTime = 0L
    private var lastAverageBrightness = -1.0

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        this.listener = onGestureDetected
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner)
                Log.d("AirGestureCore", "✅ CameraX Frontkamera-Scanner erfolgreich gestartet.")
            } catch (e: Exception) {
                Log.e("AirGestureCore", "❌ Fehler beim Starten von CameraX: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return

        // Frontkamera anfordern
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        // Bildanalyse-Stream einrichten (leichtgewichtiger Stream für Performance)
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("AirGestureCore", "❌ Bindung an Lifecycle fehlgeschlagen: ${e.message}")
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // Durchschnittliche Helligkeit (Luminanz) des Kamerabildes berechnen
        var sum = 0L
        for (byte in data) {
            sum += (byte.toInt() and 0xFF)
        }
        val averageBrightness = sum.toDouble() / data.size

        if (lastAverageBrightness != -1.0) {
            val diff = lastAverageBrightness - averageBrightness
            
            // Wenn es plötzlich deutlich dunkler wird (Hand wird vor die Frontkamera geführt)
            if (diff > 35.0) { 
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTriggerTime > 800) { // 800ms Cooldown gegen Mehrfach-Trigger
                    lastTriggerTime = currentTime
                    Log.d("AirGestureCore", "🎯 GESTE ERKANNT! (Luminanz-Drop: $diff)")
                    listener?.invoke(GestureAction.SWIPE_DOWN)
                }
            }
        }
        lastAverageBrightness = averageBrightness
        imageProxy.close() // Wichtig, um den Stream für das nächste Bild freizugeben
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
            Log.d("AirGestureCore", "🛑 CameraX Scanner gestoppt.")
        } catch (e: Exception) {
            Log.e("AirGestureCore", "Fehler beim Stoppen: ${e.message}")
        }
        listener = null
    }
}


