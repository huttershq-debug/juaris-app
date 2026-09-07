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
import kotlin.math.abs

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_LEFT, SWIPE_RIGHT
    }

    private val _gestureState = MutableStateFlow("Initialisiere Kamera...")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow<GestureAction>(GestureAction.NONE)
    val lastAction: StateFlow<GestureAction> = _lastAction

    private lateinit var cameraExecutor: ExecutorService
    private var prevLeftAvg: Double = 0.0
    private var prevRightAvg: Double = 0.0

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
                _gestureState.value = "Kamera scharf – Hand bewegen"
            } catch (exc: Exception) {
                _gestureState.value = "Fehler: ${exc.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageFrame(imageProxy: ImageProxy, onActionDetected: (GestureAction) -> Unit) {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride

        var leftSum = 0L
        var rightSum = 0L
        var count = 0L

        val step = 32 // Schnell und ressourcenschonend
        val midX = width / 2

        // Zonen-Scan: Trennt linke und rechte Bildhälfte
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val index = y * rowStride + x
                if (index < buffer.capacity()) {
                    val pixel = buffer.get(index).toInt() and 0xFF
                    if (x < midX) {
                        leftSum += pixel
                    } else {
                        rightSum += pixel
                    }
                    count++
                }
            }
        }

        if (count > 0) {
            val currentLeftAvg = leftSum.toDouble() / (count / 2)
            val currentRightAvg = rightSum.toDouble() / (count / 2)

            if (prevLeftAvg > 0.0 && prevRightAvg > 0.0) {
                val leftDelta = currentLeftAvg - prevLeftAvg
                val rightDelta = currentRightAvg - prevRightAvg

                // Live-Werte direkt auf dem Display sichtbar machen
                _gestureState.value = "Aktiv | L: ${String.format("%.1f", leftDelta)} R: ${String.format("%.1f", rightDelta)}"

                val threshold = 3.5 // Extrem feinfühlig

                // Richtungs-Erkennung über Energieverschiebung
                if (leftDelta > threshold && rightDelta < -threshold) {
                    _lastAction.value = GestureAction.SWIPE_RIGHT
                    onActionDetected(GestureAction.SWIPE_RIGHT)
                } else if (leftDelta < -threshold && rightDelta > threshold) {
                    _lastAction.value = GestureAction.SWIPE_LEFT
                    onActionDetected(GestureAction.SWIPE_LEFT)
                }
            }

            prevLeftAvg = currentLeftAvg
            prevRightAvg = currentRightAvg
        }

        imageProxy.close()
    }

    fun stopGestureDetection() {
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}

