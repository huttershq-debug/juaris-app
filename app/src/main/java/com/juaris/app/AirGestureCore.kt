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
    private var lastBalance = 0.0

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
        try {
            val plane = imageProxy.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = imageProxy.width
            val height = imageProxy.height

            var leftSum = 0.0
            var rightSum = 0.0
            var countLeft = 0
            var countRight = 0

            // Raster-Sampling unter Berücksichtigung von rowStride und pixelStride
            val yStep = (height / 12).coerceAtLeast(1)
            val xStep = (width / 12).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        if (x < width / 2) {
                            leftSum += pixel
                            countLeft++
                        } else {
                            rightSum += pixel
                            countRight++
                        }
                    }
                }
            }

            val avgLeft = if (countLeft > 0) leftSum / countLeft else 0.0
            val avgRight = if (countRight > 0) rightSum / countRight else 0.0

            // Balance misst die Helligkeitsdifferenz zwischen rechter und linker Bildschirmhälfte
            val currentBalance = avgRight - avgLeft
            val currentTime = System.currentTimeMillis()

            if (lastBalance != 0.0) {
                val balanceChange = currentBalance - lastBalance

                // 700ms Sperre, damit Wischgesten sauber einzeln erkannt werden
                if (currentTime - lastActionTime > 700) {
                    if (balanceChange > 12.0) {
                        lastActionTime = currentTime
                        currentActionState = GestureAction.SWIPE_RIGHT
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_RIGHT)
                        }
                    } else if (balanceChange < -12.0) {
                        lastActionTime = currentTime
                        currentActionState = GestureAction.SWIPE_LEFT
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_LEFT)
                        }
                    }
                }
            }

            lastBalance = currentBalance
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}


