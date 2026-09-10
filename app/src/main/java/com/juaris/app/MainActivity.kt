package com.juaris.app

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class AirGestureCore(private val context: Context) : ImageAnalysis.Analyzer {

    enum class GestureAction {
        SWIPE_RIGHT, SWIPE_LEFT, NONE
    }

    private val _gestureState = MutableStateFlow("Bereit")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("NONE")
    val lastAction: StateFlow<String> = _lastAction

    private var previousBytesBuffer: ByteArray? = null
    private var isTracking = false
    private var anchorY = -1f
    private var smoothedCentroidY = -1f
    private var gestureStartTime = 0L
    private var lastTriggerTime = 0L

    private var currentCallback: ((GestureAction) -> Unit)? = null

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        currentCallback = onGestureDetected
        _gestureState.value = "Kamera aktiv - Bereit für Gesten"
    }

    fun stopGestureDetection() {
        currentCallback = null
        previousBytesBuffer = null
        isTracking = false
        _gestureState.value = "Gestoppt"
    }

    override fun analyze(image: ImageProxy) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val remaining = buffer.remaining()
            val currBytes = ByteArray(remaining)
            buffer.get(currBytes)

            val width = image.width
            val height = image.height
            val rowStride = planes[0].rowStride

            if (previousBytesBuffer == null || previousBytesBuffer!!.size != remaining) {
                previousBytesBuffer = currBytes
                image.close()
                return
            }

            val currentTime = System.currentTimeMillis()
            // Gut dosierte Pause von 1 Sekunde zwischen Aktionen
            if (currentTime - lastTriggerTime > 1000L) {
                var massY = 0L
                var totalMass = 0L
                var changedPixels = 0L
                var totalSampledPixels = 0L
                val step = 4

                for (y in 0 until height step step) {
                    val rowOffset = y * rowStride
                    for (x in 0 until width step step) {
                        val index = rowOffset + x
                        if (index < remaining) {
                            totalSampledPixels++
                            val curr = currBytes[index].toInt() and 0xFF
                            val prev = previousBytesBuffer!![index].toInt() and 0xFF
                            val delta = abs(curr - prev)

                            if (delta > 12) {
                                changedPixels++
                                massY += (y * delta).toLong()
                                totalMass += delta.toLong()
                            }
                        }
                    }
                }

                // Globaler Belichtungs-Filter: Wenn mehr als 35% des Bildes flackern (Licht/Schatten), ist es keine Handgeste
                if (totalSampledPixels > 0 && (changedPixels.toFloat() / totalSampledPixels.toFloat()) > 0.35f) {
                    System.arraycopy(currBytes, 0, previousBytesBuffer!!, 0, remaining)
                    image.close()
                    return
                }

                val massEnter = 450L
                val massExit = 150L

                if (totalMass > (if (isTracking) massExit else massEnter)) {
                    val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                    smoothedCentroidY = if (smoothedCentroidY == -1f) {
                        rawCentroidY
                    } else {
                        0.3f * rawCentroidY + 0.7f * smoothedCentroidY
                    }

                    if (!isTracking) {
                        anchorY = smoothedCentroidY
                        isTracking = true
                        gestureStartTime = currentTime
                        _gestureState.value = "Hand erkannt..."
                    } else {
                        val totalDisplacement = smoothedCentroidY - anchorY
                        
                        // Asymmetrische Schwellenwerte:
                        // Nach oben (Rauf) erfordert mehr Weg -> Kein automatisches Durchlaufen mehr
                        val thresholdUp = height.toFloat() * 0.18f
                        // Nach unten (Runter) reagiert feinfühliger -> Reagiert sofort flüssig
                        val thresholdDown = height.toFloat() * 0.10f

                        if (currentTime - gestureStartTime > 2000L) {
                            anchorY = smoothedCentroidY
                            gestureStartTime = currentTime
                        }

                        val triggeredAction = if (totalDisplacement < -thresholdUp) {
                            _gestureState.value = "Aktion: Rauf (Nächster Tab)"
                            _lastAction.value = "SWIPE_RIGHT"
                            GestureAction.SWIPE_RIGHT
                        } else if (totalDisplacement > thresholdDown) {
                            _gestureState.value = "Aktion: Runter (Vorheriger Tab)"
                            _lastAction.value = "SWIPE_LEFT"
                            GestureAction.SWIPE_LEFT
                        } else {
                            null
                        }

                        if (triggeredAction != null) {
                            lastTriggerTime = currentTime
                            currentCallback?.let { callback ->
                                ContextCompat.getMainExecutor(context).execute {
                                    callback(triggeredAction)
                                }
                            }

                            isTracking = false
                            smoothedCentroidY = -1f
                            anchorY = -1f
                        }
                    }
                } else {
                    if (totalMass < massExit) {
                        isTracking = false
                        smoothedCentroidY = -1f
                        anchorY = -1f
                        _gestureState.value = "Warte auf Geste..."
                    }
                }
            }

            System.arraycopy(currBytes, 0, previousBytesBuffer!!, 0, remaining)
        } catch (e: Exception) {
            // Frame-Fehler abfangen
        } finally {
            image.close()
        }
    }
}
