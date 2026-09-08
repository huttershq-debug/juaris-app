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
import kotlin.math.abs

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_LEFT, SWIPE_RIGHT
    }

    private val _gestureState = MutableStateFlow("Juaris Kortex Bereit")
    val gestureState: StateFlow<String> = _gestureState

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastTriggerTime = 0L
    private val cooldownMillis = 500L // Genug Puffer, um doppelte Auslösungen zu verhindern
    
    private var previousCentroidX: Float = -1f
    private var accumulatedDeltaX = 0f

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var previousBytes: ByteArray? = null

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

                        if (previousBytes != null && previousBytes!!.size == currentBytes.size) {
                            var massX = 0L
                            var totalMass = 0L
                            val step = 16 // Etwas gröberer Step eliminiert Mikro-Rauschen und Zittern

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        val delta = abs(curr - prev)
                                        
                                        // Höherer Rauschfilter, damit Umgebungslicht ignoriert wird
                                        if (delta > 25) {
                                            massX += (x * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            // Erst ab einer echten Handmasse reagieren (verhindert wildes Herpringen bei leerem Bild)
                            if (totalMass > 8000L) {
                                val currentCentroidX = massX.toFloat() / totalMass.toFloat()

                                if (previousCentroidX != -1f) {
                                    val deltaX = currentCentroidX - previousCentroidX
                                    accumulatedDeltaX += deltaX

                                    // Benötigt einen klaren Wischweg (ca. 6% der Bildbreite), um auszulösen
                                    val swipeThreshold = width * 0.06f

                                    if (abs(accumulatedDeltaX) > swipeThreshold) {
                                        if (currentTime - lastTriggerTime > cooldownMillis) {
                                            lastTriggerTime = currentTime

                                            val action = if (accumulatedDeltaX > 0) {
                                                _gestureState.value = "Swipe Rechts erkannt"
                                                GestureAction.SWIPE_RIGHT
                                            } else {
                                                _gestureState.value = "Swipe Links erkannt"
                                                GestureAction.SWIPE_LEFT
                                            }

                                            // Direkt auf dem Main-Thread an die UI übergeben
                                            ContextCompat.getMainExecutor(context).execute {
                                                onGestureDetected(action)
                                            }
                                        }
                                        // Puffer nach Auslösung sofort komplett leeren
                                        accumulatedDeltaX = 0f
                                    }
                                }
                                previousCentroidX = currentCentroidX
                            } else {
                                // Hand aus dem Bild -> Puffer sanft zurücksetzen
                                previousCentroidX = -1f
                                accumulatedDeltaX = 0f
                            }
                        }
                        previousBytes = currentBytes

                    } catch (e: Exception) {
                        // Frame abfangen
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


