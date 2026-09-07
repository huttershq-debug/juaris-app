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

    enum class GestureAction {
        NONE, SWIPE_LEFT, SWIPE_RIGHT
    }

    private val _gestureState = MutableStateFlow("Aktiv: Frontkamera überwacht Luftgesten")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow<GestureAction>(GestureAction.NONE)
    val lastAction: StateFlow<GestureAction> = _lastAction

    private lateinit var cameraExecutor: ExecutorService
    private var lastAverageX: Double = 0.0

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onActionDetected: (GestureAction) -> Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageFrame(imageProxy, onActionDetected)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
                _gestureState.value = "Aktiv: Frontkamera überwacht Luftgesten"
            } catch (exc: Exception) {
                _gestureState.value = "Fehler: ${exc.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageFrame(imageProxy: ImageProxy, onActionDetected: (GestureAction) -> Unit) {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = imageProxy.width
        val height = imageProxy.height

        var totalX = 0L
        var pixelCount = 0L
        val step = 40 // Effizientes Abtastgitter

        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val bufferIndex = y * rowStride + x * pixelStride
                if (bufferIndex < buffer.capacity()) {
                    val pixelValue = buffer.get(bufferIndex).toInt() and 0xFF
                    // Helle Bereiche (z. B. Hand/Haut vor der Kamera)
                    if (pixelValue > 110) {
                        totalX += x
                        pixelCount++
                    }
                }
            }
        }

        if (pixelCount > 25) {
            val currentAverageX = totalX.toDouble() / pixelCount
            if (lastAverageX > 0.0) {
                val diff = currentAverageX - lastAverageX
                val threshold = 8.0 // Feine Schwelle für direkte Wischreaktion

                if (diff > threshold) {
                    _lastAction.value = GestureAction.SWIPE_RIGHT
                    onActionDetected(GestureAction.SWIPE_RIGHT)
                } else if (diff < -threshold) {
                    _lastAction.value = GestureAction.SWIPE_LEFT
                    onActionDetected(GestureAction.SWIPE_LEFT)
                }
            }
            lastAverageX = currentAverageX
        }

        imageProxy.close()
    }

    fun stopGestureDetection() {
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}

