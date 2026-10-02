package com.juaris.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets
import java.util.UUID

class NearbyMeshManager(
    private val context: Context,
    private val onDeviceDiscovered: (String) -> Unit,
    private val onDeviceLost: (String) -> Unit,
    private val onMessageReceived: (endpointId: String, message: String) -> Unit
) {
    private val connectionsClient = Nearby.getConnectionsClient(context.applicationContext)
    private val SERVICE_ID = "com.juaris.app.MESH_SERVICE"
    
    // KORREKTUR 1 (Anonymitäts-Schutz): Wir funken NIEMALS das echte Gerätemodell (Build.MODEL) in den Raum!
    // Stattdessen nutzen wir eine zufällige, flüchtige UUID. Das verhindert Hardware-Tracking durch Angreifer.
    private val myEndpointName = "Node-PQC-${UUID.randomUUID().toString().take(8).uppercase()}"
    private val connectedEndpoints = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    companion object {
        private const val TAG = "JuarisMeshManager"
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatische Annahme der Krypto-Verbindung
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e -> Log.e(TAG, "Fehler beim Akzeptieren der Verbindung zu $endpointId: ${e.message}") }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                onDeviceDiscovered(endpointId)
                Log.d(TAG, "🔗 Erfolgreich im P2P-Schwarm verbunden mit Node: $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            onDeviceLost(endpointId)
            Log.w(TAG, "🔌 Verbindung zu Node $endpointId getrennt.")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()
                Payload.Type.STREAM -> {
                    try {
                        payload.asStream()?.asInputStream()?.use { it.readBytes() }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fehler beim Lesen des PQC-Stream-Payloads", e)
                        null
                    }
                }
                else -> null
            }
            
            bytes?.let { rawBytes ->
                try {
                    val message = String(rawBytes, StandardCharsets.UTF_8)
                    onMessageReceived(endpointId, message)
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler bei UTF-8 Krypto-Payload-Konvertierung: ${e.message}")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "🔍 Anderen PQC-Schwarm-Knoten gefunden: $endpointId. Fordere Verbindung an...")
            connectionsClient.requestConnection(myEndpointName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e -> Log.e(TAG, "Verbindungsanforderung fehlgeschlagen: ${e.message}") }
        }

        override fun onEndpointLost(endpointId: String) {
            onDeviceLost(endpointId)
        }
    }

    /**
     * Startet den dezentralen Mesh-Knoten absolut crash-sicher.
     */
    @SuppressLint("MissingPermission")
    fun startMeshNode() {
        // KORREKTUR 2: Harter Berechtigungs-Check! Schützt die App vor dem sofortigen Absturz 
        // ab Android 12, falls der Nutzer die Bluetooth/Standort-Rechte entzogen hat.
        if (!hasRequiredPermissions()) {
            Log.e(TAG, "❌ Bruch des Sicherheitskernels: Fehlende Hardware-Berechtigungen für den Schwarm-Start!")
            return
        }

        try {
            val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            connectionsClient.startAdvertising(
                myEndpointName,
                SERVICE_ID,
                connectionLifecycleCallback,
                advertisingOptions
            ).addOnSuccessListener {
                Log.d(TAG, "📡 Sichtbarkeit im Schwarm aktiv. Anonymer Name: $myEndpointName")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Fehler beim Starten der Sichtbarkeit: ${e.message}")
            }

            val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                discoveryOptions
            ).addOnSuccessListener {
                Log.d(TAG, "🔍 Schwarm-Suche nach anderen Geräten läuft...")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Fehler beim Starten der Suche: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kritischer Fehler im Nearby-Framework: ${e.message}")
        }
    }

    /**
     * Überträgt die Post-Quantum-verschlüsselten Datenpakete an alle verbundenen Knoten.
     */
    fun broadcastMessage(text: String) {
        val payload = Payload.fromBytes(text.toByteArray(StandardCharsets.UTF_8))
        
        // KORREKTUR 3: Threadsichere Iteration über das Set (verhindert 'ConcurrentModificationException')
        synchronized(connectedEndpoints) {
            for (endpointId in connectedEndpoints) {
                connectionsClient.sendPayload(endpointId, payload)
                    .addOnFailureListener { e -> Log.e(TAG, "Payload-Sendefehler an $endpointId: ${e.message}") }
            }
        }
    }

    fun stopMeshNode() {
        try {
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()
            Log.d(TAG, "🛑 Dezentraler Mesh-Knoten sauber gestoppt.")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Stoppen des Mesh-Knotens: ${e.message}")
        }
        connectedEndpoints.clear()
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
