package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class AirGestureCore(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    fun startGestureDetection(
        lifecycleOwner: LifecycleOwner,
        onGestureDetected: (GestureAction) -> Unit,
        onDebugInfo: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, onGestureDetected, onDebugInfo)
            } catch (e: Exception) {
                onDebugInfo("Hardware-Fehler: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        onGestureDetected: (GestureAction) -> Unit,
        onDebugInfo: (String) -> Unit
    ) {
        val cameraProvider = cameraProvider ?: return
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        var lastLuminance = 0.0
        var coolDownFrames = 0 // <--- DAS HIER VERHINDERT DAS ÜBERSPRINGEN

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            try {
                val buffer = imageProxy.planes[0].buffer
                val data = byteBufferToByteArray(buffer)
                val currentLuminance = calculateAverageLuminance(data)

                if (coolDownFrames > 0) {
                    coolDownFrames-- // Zählt die Sperre herunter
                    onDebugInfo("Warte auf nächsten Wisch...")
                } else if (lastLuminance > 0.0) {
                    val delta = currentLuminance - lastLuminance
                    onDebugInfo("Sensor aktiv | Delta: %.1f".format(delta))

                    if (delta > 14.0) {
                        onGestureDetected(GestureAction.SWIPE_UP)
                        coolDownFrames = 50 // Sperrt den Sensor für ca. 1.5 Sekunden nach dem Auslösen
                    } else if (delta < -14.0) {
                        onGestureDetected(GestureAction.SWIPE_DOWN)
                        coolDownFrames = 50 // Sperrt den Sensor für ca. 1.5 Sekunden nach dem Auslösen
                    }
                }
                lastLuminance = currentLuminance
            } catch (e: Exception) {
                onDebugInfo("Analyzer-Fehler: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            onDebugInfo("Kamera verbunden - Wische vor die Linse!")
        } catch (e: Exception) {
            onDebugInfo("Kamera-Bindung fehlgeschlagen: ${e.message}")
        }
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {}
    }

    private fun byteBufferToByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }

    private fun calculateAverageLuminance(data: ByteArray): Double {
        var sum = 0L
        val step = 50
        var count = 0
        for (i in data.indices step step) {
            sum += (data[i].toInt() and 0xFF)
            count++
        }
        return if (count > 0) sum.toDouble() / count else 0.0
    }
}

