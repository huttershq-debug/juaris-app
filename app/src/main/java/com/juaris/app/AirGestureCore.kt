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

    // Cooldown-Variablen gegen zu schnelles Durchrasten und Feststecken
    private var lastTriggerTime = 0L
    private val cooldownMillis = 1200L // 1,2 Sekunden Pause zwischen Gesten für flüssige Bedienung

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var previousBuffer: ByteArray? = null

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    val plane = imageProxy.planes[0]
                    val buffer = plane.buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)

                    if (previousBuffer != null && data.size == previousBuffer!!.size) {
                        var leftSum = 0L
                        var rightSum = 0L
                        val width = imageProxy.width
                        val height = imageProxy.height
                        val step = 32 // Sampling-Schritt für Performance

                        for (y in 0 until height step step) {
                            for (x in 0 until width step step) {
                                val index = y * width + x
                                if (index < data.size) {
                                    val diff = kotlin.math.abs(data[index].toInt() - previousBuffer!![index].toInt())
                                    if (x < width / 2) {
                                        leftSum += diff
                                    } else {
                                        rightSum += diff
                                    }
                                }
                            }
                        }

                        // Prüfen, ob der Cooldown abgelaufen ist
                        if (currentTime - lastTriggerTime > cooldownMillis) {
                            val threshold = 60000L // Empfindlichkeits-Schwelle
                            if (leftSum > threshold || rightSum > threshold) {
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
                                // Automatischer Reset in den Standby, wenn keine starke Bewegung da ist
                                if (currentTime - lastTriggerTime > 900L) {
                                    _gestureState.value = "Kamera aktiv – Hand bereit"
                                }
                            }
                        }
                    }
                    previousBuffer = data
                    imageProxy.close()
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
            // Ignorieren bei Beendigung
        }
    }
}


