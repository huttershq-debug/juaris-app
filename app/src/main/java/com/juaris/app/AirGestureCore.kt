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
    private val cooldownMillis = 750L // Saubere Pause gegen Mehrfachtrigger
    
    // Anker- und Zustandsvariablen für saubere Wegmessung statt Zählen
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

                            // Angepasste Masse (3500L), damit Runterwischen nicht am Körper/Kleidung abbricht
                            if (totalMass > 3500L) {
                                val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                                // Sanfte Glättung gegen Zittern
                                val currentCentroidY = if (previousCentroidY == -1f) {
                                    rawCentroidY
                                } else {
                                    0.5f * rawCentroidY + 0.5f * previousCentroidY
                                }

                                if (startY == -1f) {
                                    // Startpunkt des Swipes setzen
                                    startY = currentCentroidY
                                    hasTriggeredThisGesture = false
                                } else if (!hasTriggeredThisGesture) {
                                    // Gesamtweg vom Startpunkt aus messen
                                    val displacement = currentCentroidY - startY
                                    val swipeThreshold = height * 0.12f // 12% der Bildhöhe für einen klaren Wisch

                                    if (abs(displacement) > swipeThreshold) {
                                        if (currentTime - lastTriggerTime > cooldownMillis) {
                                            lastTriggerTime = currentTime
                                            hasTriggeredThisGesture = true // Verhindert sofortiges Mehrfachtriggern (kein Tab-Überspringen mehr)

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
                                // Hand weg oder außerhalb des Erfassungsbereichs -> Anker zurücksetzen
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

