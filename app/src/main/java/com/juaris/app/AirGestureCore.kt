package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AirGestureCore(private val context: Context) {
    private val _gestureState = MutableStateFlow("Gesten-Steuerung im Standby")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow<GestureAction>(GestureAction.NONE)
    val lastAction: StateFlow<GestureAction> = _lastAction

    enum class GestureAction { NONE, SWIPE_LEFT, SWIPE_RIGHT }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastXCenter: Float = -1f
    private var lastAnalysisTime: Long = 0L

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onSwipe: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy, onSwipe)
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
                _gestureState.value = "Aktiv: Frontkamera überwacht Luftgesten"
            } catch (e: Exception) {
                _gestureState.value = "Kamera-Fehler: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImage(imageProxy: ImageProxy, onSwipe: (GestureAction) -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < 350) { // Throttling zur Performance-Schonung
            imageProxy.close()
            return
        }
        lastAnalysisTime = currentTime

        try {
            val buffer = imageProxy.planes[0].buffer
            val width = imageProxy.width
            val height = imageProxy.height
            
            var totalXSum = 0L
            var pixelCount = 0L
            
            val step = 20 // Sampling für Echtzeit-Performance
            for (y in 0 until height step step) {
                for (x in 0 until width step step) {
                    val pixelIndex = y * width + x
                    if (pixelIndex < buffer.capacity()) {
                        val luma = buffer.get(pixelIndex).toInt() and 0xFF
                        if (luma > 140) { // Erfassung von Handkontrasten
                            totalXSum += x
                            pixelCount++
                        }
                    }
                }
            }

            if (pixelCount > 40) {
                val currentXCenter = totalXSum.toFloat() / pixelCount
                if (lastXCenter != -1f) {
                    val deltaX = currentXCenter - lastXCenter
                    if (deltaX > 45f) {
                        _lastAction.value = GestureAction.SWIPE_RIGHT
                        _gestureState.value = "Geste erkannt: Nach Rechts wischen ➔"
                        onSwipe(GestureAction.SWIPE_RIGHT)
                    } else if (deltaX < -45f) {
                        _lastAction.value = GestureAction.SWIPE_LEFT
                        _gestureState.value = "Geste erkannt: Nach Links wischen ⬅"
                        onSwipe(GestureAction.SWIPE_LEFT)
                    }
                }
                lastXCenter = currentXCenter
            }
        } catch (e: Exception) {
            // Frame-Ausnahme abfangen
        } finally {
            imageProxy.close()
        }
    }

    fun stop() {
        try {
            cameraExecutor.shutdown()
        } catch (_: Exception) {}
    }
}
