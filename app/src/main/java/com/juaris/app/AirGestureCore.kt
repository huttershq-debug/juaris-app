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
                            val step = 16 // Präzisere Abtastung

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousData!!.size) {
                                        val diff = kotlin.math.abs(currentBytes[index].toInt() - previousData!![index].toInt())
                                        if (x < width / 2) {
                                            leftSum += diff
                                        } else {
                                            rightSum += diff
                                        }
                                    }
                                }
                            }

                            val totalSum = leftSum + rightSum
                            val directionalDiff = kotlin.math.abs(leftSum - rightSum)

                            // WICHTIGER FILTER: 
                            // 1. Mindest-Schwelle für echte Bewegung.
                            // 2. Asymmetrie-Prüfung: Der Unterschied zwischen links und rechts muss 
                            // mindestens 35% ausmachen. (Lichtwechsel verändert beide Seiten gleichmäßig -> wird ignoriert!)
                            val minThreshold = 8000L
                            val isAsymmetric = directionalDiff > (totalSum * 0.35)

                            // Prüfen, ob Cooldown abgelaufen ist
                            if (currentTime - lastTriggerTime > cooldownMillis) {
                                if (totalSum > minThreshold && isAsymmetric) {
                                    lastTriggerTime = currentTime
                                    if (leftSum > rightSum) {
                                        _gestureState.value = "Geste erkannt: Nach Rechts"
                                        _lastAction.value = "SWIPE_RIGHT"
                                        onGestureDetected(GestureAction.SWIPE_RIGHT)
                                    } else {
                                        _gestureState.value = "Geste erkannt: Nach Links"
                                        _lastAction.value = "SWIPE_LEFT"
                                        onGestureDetected(GestureAction.SWIPE_LEFT)
                                    }
                                } else {
                                    if (currentTime - lastTriggerTime > 800L) {
                                        _gestureState.value = "Kamera aktiv – Hand bereit"
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

