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
import kotlin.math.abs

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastActionTime = 0L

    private var baselineTop = -1.0
    private var baselineBottom = -1.0

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
            val rotation = imageProxy.imageInfo.rotationDegrees

            var topSum = 0.0
            var bottomSum = 0.0
            var countTop = 0
            var countBottom = 0

            val isPortrait = rotation == 90 || rotation == 270
            val effectiveHeight = if (isPortrait) width else height
            val effectiveWidth = if (isPortrait) height else width

            val yStep = (effectiveHeight / 10).coerceAtLeast(1)
            val xStep = (effectiveWidth / 10).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        val vCoord = if (isPortrait) x else y
                        if (vCoord < effectiveHeight / 2) {
                            topSum += pixel
                            countTop++
                        } else {
                            bottomSum += pixel
                            countBottom++
                        }
                    }
                }
            }

            val avgTop = if (countTop > 0) topSum / countTop else 0.0
            val avgBottom = if (countBottom > 0) bottomSum / countBottom else 0.0

            if (baselineTop < 0.0) {
                baselineTop = avgTop
                baselineBottom = avgBottom
                return
            }

            val deltaTop = avgTop - baselineTop
            val deltaBottom = avgBottom - baselineBottom

            val currentTime = System.currentTimeMillis()
            val threshold = 3.8 // Perfekter Mittelweg für knackige Erkennung ohne Fehlzündungen

            val isMoving = abs(deltaTop) > threshold || abs(deltaBottom) > threshold

            // 650ms Cooldown, damit eine Geste exakt einen Tab weiterschaltet
            if (currentTime - lastActionTime > 650) {
                // Rauf wischen (Hand zieht von unten nach oben) -> Nächster Tab (Rechts)
                if (deltaBottom < -threshold && deltaTop > -threshold * 0.5) {
                    lastActionTime = currentTime
                    currentActionState = GestureAction.SWIPE_UP
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_UP)
                    }
                }
                // Runter wischen (Hand zieht von oben nach unten) -> Vorheriger Tab (Links)
                else if (deltaTop < -threshold && deltaBottom > -threshold * 0.5) {
                    lastActionTime = currentTime
                    currentActionState = GestureAction.SWIPE_DOWN
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_DOWN)
                    }
                }
            }

            // Baseline passt sich nur an, wenn keine Bewegung stattfindet
            if (!isMoving) {
                baselineTop = baselineTop * 0.93 + avgTop * 0.07
                baselineBottom = baselineBottom * 0.93 + avgBottom * 0.07
            }

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

