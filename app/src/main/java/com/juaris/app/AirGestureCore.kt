package com.juaris.app

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_LEFT, SWIPE_RIGHT
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastActionTime = 0L
    private var previousBrightnessLeft = 0.0
    private var previousBrightnessRight = 0.0

    var currentActionState: GestureAction = GestureAction.NONE
        private set

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy, onGestureDetected)
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImage(imageProxy: ImageProxy, onGestureDetected: (GestureAction) -> Unit) {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width = imageProxy.width
        val height = imageProxy.height

        // Rasteranalyse: Helligkeit in linker und rechter Bildhälfte vergleichen
        var leftSum = 0L
        var rightSum = 0L
        val step = (width * height) / 150 // Optimiert für Performance

        var count = 0
        var i = 0
        while (i < bytes.size && count < 150) {
            val pixel = bytes[i].toInt() and 0xFF
            val xCoord = i % width
            if (xCoord < width / 2) {
                leftSum += pixel
            } else {
                rightSum += pixel
            }
            i += step.coerceAtLeast(1)
            count++
        }

        val currentLeft = if (count > 0) leftSum.toDouble() / (count / 2) else 0.0
        val currentRight = if (count > 0) rightSum.toDouble() / (count / 2) else 0.0

        if (previousBrightnessLeft > 0 && previousBrightnessRight > 0) {
            val diffLeft = currentLeft - previousBrightnessLeft
            val diffRight = currentRight - previousBrightnessRight
            val currentTime = System.currentTimeMillis()

            // Debounce von 800ms, damit Wischgesten sauber einzeln erkannt werden
            if (currentTime - lastActionTime > 800) {
                // Wischbewegung von links nach rechts
                if (diffLeft > 10.0 && diffRight < -10.0) {
                    lastActionTime = currentTime
                    currentActionState = GestureAction.SWIPE_RIGHT
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_RIGHT)
                    }
                } 
                // Wischbewegung von rechts nach links
                else if (diffRight > 10.0 && diffLeft < -10.0) {
                    lastActionTime = currentTime
                    currentActionState = GestureAction.SWIPE_LEFT
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_LEFT)
                    }
                }
            }
        }

        previousBrightnessLeft = currentLeft
        previousBrightnessRight = currentRight

        imageProxy.close()
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            // Ignorieren falls bereits geschlossen
        }
    }
}

