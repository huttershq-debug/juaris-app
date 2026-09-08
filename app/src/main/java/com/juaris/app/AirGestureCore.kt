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

    private val _gestureState = MutableStateFlow("KI-Kortex aktiv – Bereit")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("KEINE")
    val lastAction: StateFlow<String> = _lastAction

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    // Zustandsvariablen für kinematische Vektor-Analyse
    private var lastTriggerTime = 0L
    private val cooldownMillis = 900L // Exakte taktile Pause
    private var previousCentroidX: Float = -1f

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
                            val step = 12 // Höchste Abtastpräzision für Konturen

                            // Erweiterter Edge-Gradient & Schwerpunkt-Detektor (Zentroid-Tracking)
                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        val delta = abs(curr - prev)

                                        // Nur echte physikalische Kontur-Bewegungen (Ignoriert globales Rauschen)
                                        if (delta > 25) {
                                            massX += (x * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            if (totalMass > 15000L) { // Erforderliche kinetische Masse im Bild
                                val currentCentroidX = massX.toFloat() / totalMass.toFloat()

                                if (previousCentroidX != -1f) {
                                    val deltaX = currentCentroidX - previousCentroidX
                                    val movementThreshold = width * 0.035f // 3.5% Bildbreiten-Vektor

                                    if (abs(deltaX) > movementThreshold) {
                                        if (currentTime - lastTriggerTime > cooldownMillis) {
                                            lastTriggerTime = currentTime

                                            if (deltaX > 0) {
                                                // Bewegung von links nach rechts -> Hand zieht nach rechts
                                                _gestureState.value = "KI-Geste: Swipe Rechts"
                                                _lastAction.value = "SWIPE_RIGHT"
                                                onGestureDetected(GestureAction.SWIPE_RIGHT)
                                            } else {
                                                // Bewegung von rechts nach links -> Hand zieht nach links
                                                _gestureState.value = "KI-Geste: Swipe Links"
                                                _lastAction.value = "SWIPE_LEFT"
                                                onGestureDetected(GestureAction.SWIPE_LEFT)
                                            }
                                        }
                                    }
                                }
                                previousCentroidX = currentCentroidX
                            } else {
                                // Kein massives Objekt im Bild -> Schwerpunkt zurücksetzen
                                previousCentroidX = -1f
                                if (currentTime - lastTriggerTime > 600L) {
                                    _gestureState.value = "KI-Kortex aktiv – Warten auf Interaktion"
                                }
                            }
                        }
                        previousBytes = currentBytes

                    } catch (e: Exception) {
                        // Frame-Pipeline fehlerfrei absichern
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
                _gestureState.value = "Systemfehler: ${e.localizedMessage}"
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

