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
    // Throttling auf 400ms erhöht, damit Gesten nicht "doppelt" oder zu schnell hintereinander feuern
    if (currentTime - lastAnalysistime < 400) { 
        imageProxy.close()
        return
    }
    lastAnalysistime = currentTime

    try {
        val buffer = imageProxy.planes[0].buffer
        val width = imageProxy.width
        val height = imageProxy.height

        var totalsum = 0L
        var pixelcount = 0L

        val step = 20 // Sampling-Rate für optimale Performance
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixelIndex = y * width + x
                if (pixelIndex < buffer.capacity()) {
                    val lum = buffer.get(pixelIndex).toInt() and 0xFF
                    if (lum > 140) { // Kontrasterfassung der Hand
                        totalsum += x
                        pixelcount++
                    }
                }
            }
        }

        if (pixelcount > 40) {
            val currentXCenter = totalsum.toFloat() / pixelcount
            if (lastXCenter != -1f) {
                val deltax = currentXCenter - lastXCenter
                
                // Schwellenwert auf 75f erhöht -> macht die Erkennung "feiner" und weniger aggressiv (kein Wackeln)
                if (deltax > 75f) {
                    _lastAction.value = GestureAction.SWIPE_RIGHT
                    _gesturestate.value = "Geste erkannt: Nach Rechts wischen"
                    onSwipe(GestureAction.SWIPE_RIGHT)
                } else if (deltax < -75f) {
                    _lastAction.value = GestureAction.SWIPE_LEFT
                    _gesturestate.value = "Geste erkannt: Nach Links wischen"
                    onSwipe(GestureAction.SWIPE_LEFT)
                }
            }
            lastXCenter = currentXCenter
        }
    } catch (e: Exception) {
        // Frame-Ausnahmen abfangen, damit die App stabil bleibt
    } finally {
        // Sorgt dafür, dass der Kamera-Stream NIEMALS stoppt und permanent aktiv bleibt
        imageProxy.close()
    }
}

