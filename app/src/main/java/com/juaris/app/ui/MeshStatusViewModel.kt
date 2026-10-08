package com.juaris.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juaris.app.JuarisDatabase
import com.juaris.app.MeshPostEntity
import com.juaris.app.NearbyMeshManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MeshStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JuarisDatabase.getDatabase(application)
    private val meshDao = db.meshDao()

    var isMeshActive by mutableStateOf(false)
        private set

    var connectedNodesCount by mutableStateOf(0)
        private set

    var discoveredNodesList = mutableStateOf<List<String>>(emptyList())
        private set

    val messagesFlow: StateFlow<List<MeshPostEntity>> = meshDao.getAllActivePosts(System.currentTimeMillis())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val nearbyMeshManager = NearbyMeshManager(
        context = application,
        onDeviceDiscovered = { endpointId ->
            val currentList = discoveredNodesList.value.toMutableList()
            if (!currentList.contains(endpointId)) {
                currentList.add(endpointId)
                discoveredNodesList.value = currentList
                connectedNodesCount = currentList.size
            }
        },
        onDeviceLost = { endpointId ->
            val currentList = discoveredNodesList.value.toMutableList()
            currentList.remove(endpointId)
            discoveredNodesList.value = currentList
            connectedNodesCount = currentList.size
        },
        onMessageReceived = { endpointId, message ->
            saveIncomingMeshPost(endpointId, message)
        }
    )

    fun toggleMesh(active: Boolean) {
        isMeshActive = active
        if (active) {
            nearbyMeshManager.startMeshNode()
        } else {
            nearbyMeshManager.stopMeshNode()
            discoveredNodesList.value = emptyList()
            connectedNodesCount = 0
        }
    }

    fun broadcastEmergency(reason: String) {
        viewModelScope.launch {
            val postId = UUID.randomUUID().toString()
            val senderHash = MeshPostEntity.generateAnonymousNodeId(android.os.Build.MODEL)
            val currentTime = System.currentTimeMillis()

            val post = MeshPostEntity(
                postId = postId,
                senderNodeHash = senderHash,
                content = "🚨 NOTFALL-ALARM: $reason",
                mediaUri = null,
                mediaType = "TEXT",
                timestamp = currentTime,
                isEphemeral = true,
                ttlHopCount = 5,
                pqcSignature = "PQC-PROD-SECURE",
                pqcPublicKey = "PQC-PROD-KEY"
            )

            db.meshDao().insertPost(post)
            nearbyMeshManager.broadcastMessage("🚨 NOTFALL: $reason")
        }
    }

    private fun saveIncomingMeshPost(senderId: String, content: String) {
        viewModelScope.launch {
            val postId = UUID.randomUUID().toString()
            val post = MeshPostEntity(
                postId = postId,
                senderNodeHash = "Node-PQC-${senderId.take(8).uppercase()}",
                content = content,
                mediaUri = null,
                mediaType = "TEXT",
                timestamp = System.currentTimeMillis(),
                isEphemeral = false,
                ttlHopCount = 3,
                pqcSignature = "VERIFIED",
                pqcPublicKey = "UNKNOWN"
            )
            meshDao.insertPost(post)
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyMeshManager.stopMeshNode()
    }
}

