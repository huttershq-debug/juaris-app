package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AirGestureCore(private val context: Context) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    enum class GestureAction {
        SWIPE_UP, SWIPE_DOWN, NONE
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

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var lastLuma = 0.0
                var frameCount = 0

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    frameCount++
                    try {
                        val buffer = imageProxy.planes[0].buffer
                        val data = buffer.toByteArray()
                        val pixels = data.map { it.toInt() and 0xFF }
                        val currentLuma = pixels.average()

                        // Jedes 15. Frame ein Status-Update an den Bildschirm senden
                        if (frameCount % 15 == 0) {
                            onDebugInfo("Luma: ${currentLuma.toInt()} (Aktiv)")
                        }

                        val diff = currentLuma - lastLuma
                        if (diff > 35.0) {
                            onDebugInfo("Geste erkannt: HOCH (UP)")
                            onGestureDetected(GestureAction.SWIPE_UP)
                        } else if (diff < -35.0) {
                            onDebugInfo("Geste erkannt: RUNTER (DOWN)")
                            onGestureDetected(GestureAction.SWIPE_DOWN)
                        }

                        lastLuma = currentLuma
                    } catch (e: Exception) {
                        onDebugInfo("Analyzer-Fehler: ${e.localizedMessage}")
                    } finally {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
                onDebugInfo("Kamera erfolgreich gestartet!")
            } catch (e: Exception) {
                onDebugInfo("Kamera-Startfehler: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            // Ignorieren
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }
}


