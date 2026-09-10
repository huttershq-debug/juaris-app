package com.juaris.app

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

enum class GestureAction {
    SWIPE_RIGHT, SWIPE_LEFT
}

class AirGestureCore(
    private val context: Context,
    private val onGestureDetected: (GestureAction) -> Unit
) : ImageAnalysis.Analyzer {

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
            if (currentTime - lastTriggerTime > 1000L) {
                var massY = 0L
                var totalMass = 0L

                // 1. Feinere Abtastung für größere Distanz
                val step = 6

                for (y in 0 until height step step) {
                    val rowOffset = y * rowStride
                    for (x in 0 until width step step) {
                        val index = rowOffset + x
                        if (index < remaining) {
                            val curr = currBytes[index].toInt() and 0xFF
                            val prev = previousBytesBuffer!![index].toInt() and 0xFF
                            val delta = abs(curr - prev)

                            // Filtert Lichtflackern und Geister-Trigger weg
                            if (delta > 15) {
                                massY += (y * delta).toLong()
                                totalMass += delta.toLong()
                            }
                        }
                    }
                }

                // Angepasste Schwellenwerte für mehr Reichweite / Distanz
                val massEnter = 1000L
                val massExit = 400L

                if (totalMass > (if (isTracking) massExit else massEnter)) {
                    val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                    smoothedCentroidY = if (smoothedCentroidY == -1f) {
                        rawCentroidY
                    } else {
                        0.2f * rawCentroidY + 0.8f * smoothedCentroidY
                    }

                    if (!isTracking) {
                        anchorY = smoothedCentroidY
                        isTracking = true
                        gestureStartTime = currentTime
                    } else {
                        val totalDisplacement = smoothedCentroidY - anchorY
                        val swipeThreshold = height.toFloat() * 0.08f

                        if (currentTime - gestureStartTime > 2000L) {
                            anchorY = smoothedCentroidY
                            gestureStartTime = currentTime
                        }

                        if (abs(totalDisplacement) > swipeThreshold) {
                            lastTriggerTime = currentTime

                            val action = if (totalDisplacement < 0f) {
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
                    }
                }
            }

            System.arraycopy(currBytes, 0, previousBytesBuffer!!, 0, remaining)
        } catch (e: Exception) {
            // Ignorieren, um Frame-Abstürze zu verhindern
        } finally {
            image.close()
        }
    }
}

