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
    private var consecutiveCount = 0

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
                    // Jedes 2te Frame reicht völlig für flüssige Erkennung ohne Last
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

            // Saubere Rasterung für die vertikale Ausrichtung
            val yStep = (height / 12).coerceAtLeast(1)
            val xStep = (width / 12).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        
                        // Korrekte Zuweisung der vertikalen Koordinate je nach Rotation
                        val verticalCoord = if (isPortrait) y else x
                        val verticalLimit = if (isPortrait) height else width

                        if (verticalCoord < verticalLimit / 2) {
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

            // 700ms Cooldown zwischen Aktionen gegen zu schnelles Durchrauschen
            if (timeSinceLastAction > 700) {
                if (lastBalance != 0.0) {
                    val rawChange = currentBalance - lastBalance

                    // Harten Filter gegen wildes Springen einbauen
                    val balanceChange = rawChange.coerceIn(-0.08, 0.08)

                    if (abs(balanceChange) > 0.008) {
                        // Bei Richtungswechsel sofort Momentum löschen
                        if (gestureMomentum * balanceChange < 0) {
                            gestureMomentum = 0.0
                            consecutiveCount = 0
                        }
                        gestureMomentum = (gestureMomentum + balanceChange).coerceIn(-1.0, 1.0)
                    } else {
                        // Sanftes Abklingen, damit es bei Ruhe sofort stoppt
                        gestureMomentum *= 0.75
                    }

                    // Klare Schwellenwerte für Rauf und Runter
                    if (gestureMomentum < -0.12) {
                        consecutiveCount++
                    } else if (gestureMomentum > 0.12) {
                        consecutiveCount++
                    } else {
                        consecutiveCount = maxOf(0, consecutiveCount - 1)
                    }

                    // Benötigt 2 stabile Frames in dieselbe Richtung -> Kein Zucken/Hüpfen mehr möglich
                    if (consecutiveCount >= 2) {
                        val action = if (gestureMomentum < 0) GestureAction.SWIPE_UP else GestureAction.SWIPE_DOWN
                        
                        lastActionTime = currentTime
                        gestureMomentum = 0.0
                        consecutiveCount = 0
                        currentActionState = action

                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(action)
                        }
                    }
                }
            } else {
                currentActionState = GestureAction.NONE
                gestureMomentum = 0.0
                consecutiveCount = 0
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

