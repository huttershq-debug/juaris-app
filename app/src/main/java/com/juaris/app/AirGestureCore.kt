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

    private val _gestureState = MutableStateFlow("Juaris Kortex Aktiv")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("KEINE")
    val lastAction: StateFlow<String> = _lastAction

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastTriggerTime = 0L
    private val cooldownMillis = 600L 
    
    // Variablen für das saubere Anker-Modell (verhindert Springen und Mehrfachtrigger)
    private var previousCentroidY: Float = -1f
    private var startY: Float = -1f
    private var hasTriggeredThisGesture = false

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        if (cameraProvider != null) return

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
                            var massY = 0L
                            var totalMass = 0L
                            val step = 8

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        val delta = abs(curr - prev)
                                        
                                        if (delta > 10) {
                                            massY += (y * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            // Niedrige Masse (1800L): Verhindert, dass Runterwischen am Körper abbricht
                            if (totalMass > 1800L) {
                                val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                                val currentCentroidY = if (previousCentroidY == -1f) {
                                    rawCentroidY
                                } else {
                                    0.4f * rawCentroidY + 0.6f * previousCentroidY
                                }

                                if (startY == -1f) {
                                    // Startpunkt (Anker) für diesen Wisch setzen
                                    startY = currentCentroidY
                                    hasTriggeredThisGesture = false
                                } else if (!hasTriggeredThisGesture) {
                                    // Gesamtweg exakt vom Startpunkt aus messen (kein Aufaddieren!)
                                    val displacement = currentCentroidY - startY
                                    val swipeThreshold = height.toFloat() * 0.10f // 10% der Bildhöhe

                                    if (abs(displacement) > swipeThreshold) {
                                        if (currentTime - lastTriggerTime > cooldownMillis) {
                                            lastTriggerTime = currentTime
                                            hasTriggeredThisGesture = true // Sofortige Sperre: Verhindert 100%iges Tab-Überspringen

                                            // Negatives displacement = Rauf = Rechts (Nächster Tab)
                                            // Positives displacement = Runter = Links (Vorheriger Tab)
                                            val action = if (displacement < 0f) {
                                                _gestureState.value = "Swipe: Rauf (Rechts)"
                                                _lastAction.value = "SWIPE_RIGHT"
                                                GestureAction.SWIPE_RIGHT
                                            } else {
                                                _gestureState.value = "Swipe: Runter (Links)"
                                                _lastAction.value = "SWIPE_LEFT"
                                                GestureAction.SWIPE_LEFT
                                            }

                                            ContextCompat.getMainExecutor(context).execute {
                                                onGestureDetected(action)
                                            }
                                        }
                                    }
                                }
                                previousCentroidY = currentCentroidY
                            } else {
                                // Hand weg oder außerhalb -> Anker komplett zurücksetzen
                                startY = -1f
                                previousCentroidY = -1f
                                hasTriggeredThisGesture = false
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
            cameraProvider = null
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}


