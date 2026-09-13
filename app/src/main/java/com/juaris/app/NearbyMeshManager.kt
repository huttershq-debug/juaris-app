import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

class NearbyMeshManager(
    private val context: Context,
    private val onDeviceDiscovered: (String) -> Unit,
    private val onDeviceLost: (String) -> Unit,
    private val onMessageReceived: (endpointId: String, message: String) -> Unit
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.juaris.app.MESH_SERVICE"
    private val myEndpointName = android.os.Build.MODEL
    private val connectedEndpoints = mutableSetOf<String>()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatisch annehmen oder per Dialog bestätigen
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoints.add(endpointId)
                onDeviceDiscovered(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            onDeviceLost(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val message = String(bytes, StandardCharsets.UTF_8)
                onMessageReceived(endpointId, message)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // Gefundenes Gerät direkt verbinden wollen
            connectionsClient.requestConnection(myEndpointName, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            onDeviceLost(endpointId)
        }
    }

    fun startMeshNode() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            myEndpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        )

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        )
    }

    fun broadcastMessage(text: String) {
        val payload = Payload.fromBytes(text.toByteArray(StandardCharsets.UTF_8))
        for (endpointId in connectedEndpoints) {
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    fun stopMeshNode() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
    }
}

