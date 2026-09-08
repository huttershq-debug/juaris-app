package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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

    private val _gestureState = MutableStateFlow("Kamera aktiv – Hand bewegen")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("KEINE")
    val lastAction: StateFlow<String> = _lastAction

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    // Cooldown und angepasste Sensibilität
    private var lastTriggerTime = 0L
    private val cooldownMillis = 1000L // 1 Sekunde Pause für saubere Erkennung

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var previousData: ByteArray? = null

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val currentTime = System.currentTimeMillis()
                        val plane = imageProxy.planes[0]
                        val buffer = plane.buffer
                        val rowStride = plane.rowStride
                        val width = imageProxy.width
                        val height = imageProxy.height

                        val currentBytes = ByteArray(buffer.remaining())
                        buffer.get(currentBytes)

                         if (previousData != null && previousData!!.size == currentBytes.size) {
                            var leftSum = 0L
                            var rightSum = 0L
                            var totalBrightness = 0L
                            val step = 16
                            val totalPixels = (width / step) * (height / step)

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousData!!.size) {
                                        val currentVal = currentBytes[index].toInt() and 0xFF
                                        totalBrightness += currentVal
                                        
                                        val diff = kotlin.math.abs(currentVal - (previousData!![index].toInt() and 0xFF))
                                        if (x < width / 2) {
                                            leftSum += diff
                                        } else {
                                            rightSum += diff
                                        }
                                    }
                                }
                            }

                            val averageBrightness = if (totalPixels > 0) totalBrightness / totalPixels else 128L
                            val totalSum = leftSum + rightSum
                            val directionalDiff = kotlin.math.abs(leftSum - rightSum)

                            // DYNAMISCHER SCHWELLENWERT: Passt sich automatisch an die Dunkelheit an!
                            // Je dunkler der Raum (averageBrightness niedrig), desto sensibler reagiert der Algorithmus.
                            val adaptiveThreshold = if (averageBrightness < 50) 3000L else 8000L
                            
                            val isAsymmetric = directionalDiff > (totalSum * 0.30)

                            if (totalSum > adaptiveThreshold && isAsymmetric) {
                                if (leftSum > rightSum) {
                                    consecutiveLeft++
                                    consecutiveRight = 0
                                } else {
                                    consecutiveRight++
                                    consecutiveLeft = 0
                                }
                            } else {
                                consecutiveLeft = maxOf(0, consecutiveLeft - 1)
                                consecutiveRight = maxOf(0, consecutiveRight - 1)
                            }

                            if (currentTime - lastTriggerTime > cooldownMillis) {
                                if (consecutiveLeft >= 2) {
                                    lastTriggerTime = currentTime
                                    consecutiveLeft = 0
                                    _gestureState.value = "Geste erkannt: Nach Links"
                                    _lastAction.value = "SWIPE_LEFT"
                                    onGestureDetected(GestureAction.SWIPE_LEFT)
                                } else if (consecutiveRight >= 2) {
                                    lastTriggerTime = currentTime
                                    consecutiveRight = 0
                                    _gestureState.value = "Geste erkannt: Nach Rechts"
                                    _lastAction.value = "SWIPE_RIGHT"
                                    onGestureDetected(GestureAction.SWIPE_RIGHT)
                                } else {
                                    if (currentTime - lastTriggerTime > 800L) {
                                        _gestureState.value = "Kamera aktiv (Auto-Light)"
                                    }
                                }
                            }
                        }
                        previousData = currentBytes




                    } catch (e: Exception) {
                        // Frame-Fehler abfangen
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )

            } catch (e: Exception) {
                _gestureState.value = "Fehler: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}

