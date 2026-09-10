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
    private var lastBalance = 0.0
    private var frameCounter = 0
    
    private var gestureMomentum = 0.0

    // NEU: Zähler für die Multi-Frame-Konsistenz (filtert Licht-Störsignale weg)
    private var consecutiveUpCount = 0
    private var consecutiveDownCount = 0

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
                    frameCounter++
                    if (frameCounter % 2 != 0) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
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

            val yStep = (height / 12).coerceAtLeast(1)
            val xStep = (width / 12).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        
                        val verticalCoord = if (isPortrait) x else y
                        val verticalLimit = if (isPortrait) width else height

                        val isTopHalf = if (rotation == 270) {
                            verticalCoord >= verticalLimit / 2
                        } else {
                            verticalCoord < verticalLimit / 2
                        }

                        if (isTopHalf) {
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

            val totalLight = avgTop + avgBottom
            val currentBalance = if (totalLight > 1.0) {
                (avgBottom - avgTop) / totalLight
            } else {
                0.0
            }

            val currentTime = System.currentTimeMillis()
            val timeSinceLastAction = currentTime - lastActionTime

            if (timeSinceLastAction > 800) {
                if (lastBalance != 0.0) {
                    val rawChange = currentBalance - lastBalance

                    // Strenge Begrenzung gegen Erschütterungen
                    val balanceChange = rawChange.coerceIn(-0.12, 0.12)

                    if (abs(balanceChange) > 0.015) {
                        if (gestureMomentum * balanceChange < 0) {
                            gestureMomentum = 0.0
                            consecutiveUpCount = 0
                            consecutiveDownCount = 0
                        }
                        gestureMomentum = (gestureMomentum + balanceChange).coerceIn(-1.0, 1.0)
                    } else {
                        gestureMomentum *= 0.80
                    }

                    // NEU: Erst auslösen, wenn die Richtung über mehrere Frames stabil bleibt (Licht-Filter)
                    if (gestureMomentum < -0.18) {
                        consecutiveUpCount++
                        consecutiveDownCount = 0
                    } else if (gestureMomentum > 0.18) {
                        consecutiveDownCount++
                        consecutiveUpCount = 0
                    } else {
                        consecutiveUpCount = maxOf(0, consecutiveUpCount - 1)
                        consecutiveDownCount = maxOf(0, consecutiveDownCount - 1)
                    }

                    // Benötigt 2 stabile Frames am Stück in dieselbe Richtung -> Kein Licht-Flimmern triggert das mehr!
                    if (consecutiveUpCount >= 2) {
                        lastActionTime = currentTime
                        gestureMomentum = 0.0
                        consecutiveUpCount = 0
                        currentActionState = GestureAction.SWIPE_UP
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_UP)
                        }
                    } else if (consecutiveDownCount >= 2) {
                        lastActionTime = currentTime
                        gestureMomentum = 0.0
                        consecutiveDownCount = 0
                        currentActionState = GestureAction.SWIPE_DOWN
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_DOWN)
                        }
                    }
                }
            } else {
                currentActionState = GestureAction.NONE
                gestureMomentum = 0.0
                consecutiveUpCount = 0
                consecutiveDownCount = 0
            }

            lastBalance = currentBalance
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    fn stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}

