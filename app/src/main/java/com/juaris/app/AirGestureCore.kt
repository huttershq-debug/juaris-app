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

    // Stabilitäts-Zähler gegen das "Sprunghafte"
    private var consecutiveUpFrames = 0
    private var consecutiveDownFrames = 0

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

            val yStep = (effectiveHeight / 12).coerceAtLeast(1)
            val xStep = (effectiveWidth / 12).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        
                        val vCoord = if (isPortrait) x else y
                        val midPoint = effectiveHeight / 2

                        if (vCoord < midPoint) {
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
            
            // 700ms Cooldown für sauberes, kontrolliertes Schalten
            if (currentTime - lastActionTime > 700) {
                val threshold = 4.5 // Strengerer Schwellenwert gegen Zufallszucker

                val isUp = deltaBottom < -threshold && deltaTop > -threshold * 0.6
                val isDown = deltaTop < -threshold && deltaBottom > -threshold * 0.6

                if (isUp) {
                    consecutiveDownFrames = 0
                    consecutiveUpFrames++
                } else if (isDown) {
                    consecutiveUpFrames = 0
                    consecutiveDownFrames++
                } else {
                    // Lässt den Zähler bei Ruhe sanft abklingen
                    consecutiveUpFrames = maxOf(0, consecutiveUpFrames - 1)
                    consecutiveDownFrames = maxOf(0, consecutiveDownFrames - 1)
                }

                // Löst erst aus, wenn die Bewegung über 2 Frames stabil ist -> Kein wildes Springen mehr!
                if (consecutiveUpFrames >= 2) {
                    lastActionTime = currentTime
                    consecutiveUpFrames = 0
                    currentActionState = GestureAction.SWIPE_UP
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_UP)
                    }
                } else if (consecutiveDownFrames >= 2) {
                    lastActionTime = currentTime
                    consecutiveDownFrames = 0
                    currentActionState = GestureAction.SWIPE_DOWN
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_DOWN)
                    }
                }
            }

            // Sanfte Anpassung an den Hintergrund
            baselineTop = baselineTop * 0.90 + avgTop * 0.10
            baselineBottom = baselineBottom * 0.90 + avgBottom * 0.10

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

