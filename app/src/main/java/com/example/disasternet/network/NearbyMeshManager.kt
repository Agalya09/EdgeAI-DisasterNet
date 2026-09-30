package com.example.disasternet.network

import android.content.Context
import android.util.Log
import com.example.disasternet.model.NearbyPacket
import com.example.disasternet.model.PayloadKind
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.LinkedHashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages offline nearby peer-to-peer communication with adaptive multi-hop forwarding
 * using Google Nearby Connections (Strategy.P2P_CLUSTER).
 *
 * Transports: Bluetooth, BLE, and Wi-Fi Direct/Hotspot.
 * Limit: Local mesh neighborhood with a strict maximum of 4 relay hops.
 */
class NearbyMeshManager(
    private val context: Context,
    val deviceNodeId: String,
    var deviceDisplayName: String
) {
    private val TAG = "NearbyMeshManager"
    val SERVICE_ID = "com.example.disasternet.mesh.v3"
    private val STRATEGY = Strategy.P2P_CLUSTER

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Active connected endpoints: endpointId -> endpointName
    private val connectedEndpoints = ConcurrentHashMap<String, String>()
    // In-flight connection attempts: endpointId -> timestamp
    private val connectingEndpoints = ConcurrentHashMap<String, Long>()

    private val _connectedPeerCount = MutableStateFlow(0)
    val connectedPeerCount: StateFlow<Int> = _connectedPeerCount.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Outbox for pending emergency packets created while no peers were connected
    private val pendingOutbox = ConcurrentLinkedQueue<NearbyPacket>()

    // Duplicate suppression bounded cache (last 500 packet IDs)
    private val processedPacketIds = Collections.synchronizedSet(object : LinkedHashSet<String>() {
        override fun add(element: String): Boolean {
            if (size > 500) {
                val iterator = iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            return super.add(element)
        }
    })

    // Packet listeners
    var onEmergencyReceived: ((NearbyPacket) -> Unit)? = null
    var onStatusReceived: ((NearbyPacket) -> Unit)? = null
    var onAckReceived: ((NearbyPacket) -> Unit)? = null
    var onTeamLocationReceived: ((NearbyPacket) -> Unit)? = null
    var onCommunityAlertReceived: ((NearbyPacket) -> Unit)? = null
    var onPeerConnected: ((String, String) -> Unit)? = null
    var onPeerDisconnected: ((String) -> Unit)? = null

    private fun getAdvertisedName(): String = "$deviceNodeId|$deviceDisplayName"

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val json = String(bytes, StandardCharsets.UTF_8)
                Log.d(TAG, "[onPayloadReceived] from $endpointId: ${bytes.size} bytes: $json")
                handleIncomingJson(json, endpointId)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                Log.d(TAG, "[onPayloadTransferUpdate] Payload ${update.payloadId} successfully sent to $endpointId")
            } else if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                Log.w(TAG, "[onPayloadTransferUpdate] Payload ${update.payloadId} failed for $endpointId")
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.i(TAG, "[onConnectionInitiated] from: $endpointId (${connectionInfo.endpointName})")
            // Automatically accept incoming connections to form peer-to-peer ad-hoc network
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Log.i(TAG, "[acceptConnection] Sent acceptance for $endpointId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "[acceptConnection] Failed accepting connection from $endpointId", e)
                    connectingEndpoints.remove(endpointId)
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            connectingEndpoints.remove(endpointId)
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.i(TAG, "[onConnectionResult] CONNECTED SUCCESSFULLY to: $endpointId")
                    connectedEndpoints[endpointId] = endpointId
                    _connectedPeerCount.value = connectedEndpoints.size
                    onPeerConnected?.invoke(endpointId, endpointId)

                    // Flush any pending packets queued while offline
                    flushPendingOutbox(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "[onConnectionResult] Connection rejected by: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                    Log.i(TAG, "[onConnectionResult] Already connected to: $endpointId")
                    connectedEndpoints[endpointId] = endpointId
                    _connectedPeerCount.value = connectedEndpoints.size
                }
                else -> {
                    Log.e(TAG, "[onConnectionResult] Error with $endpointId (code: ${resolution.status.statusCode}, msg: ${resolution.status.statusMessage})")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "[onDisconnected] Disconnected from: $endpointId")
            connectedEndpoints.remove(endpointId)
            connectingEndpoints.remove(endpointId)
            _connectedPeerCount.value = connectedEndpoints.size
            onPeerDisconnected?.invoke(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val remoteName = info.endpointName
            Log.i(TAG, "[onEndpointFound] Discovered endpoint: $endpointId ($remoteName, service: ${info.serviceId})")

            if (info.serviceId != SERVICE_ID) return
            if (connectedEndpoints.containsKey(endpointId)) {
                Log.d(TAG, "Already connected to $endpointId, skipping.")
                return
            }
            if (connectingEndpoints.containsKey(endpointId)) {
                Log.d(TAG, "Connection already in-flight for $endpointId, skipping.")
                return
            }

            // Tie-breaker to prevent simultaneous connection collisions between two discovering peers
            val remoteNodeId = if (remoteName.contains("|")) remoteName.substringBefore("|") else endpointId

            if (deviceNodeId > remoteNodeId) {
                Log.i(TAG, "Tie-breaker: Local ($deviceNodeId) > Remote ($remoteNodeId). Initiating connection to $endpointId")
                initiateConnection(endpointId)
            } else {
                Log.i(TAG, "Tie-breaker: Local ($deviceNodeId) < Remote ($remoteNodeId). Waiting for remote to initiate.")
                // Fallback: If remote doesn't initiate within 5 seconds, attempt connection as fallback
                scope.launch {
                    delay(5000)
                    if (!connectedEndpoints.containsKey(endpointId) && !connectingEndpoints.containsKey(endpointId)) {
                        Log.i(TAG, "Fallback timeout: Remote did not initiate. Initiating fallback connection to $endpointId")
                        initiateConnection(endpointId)
                    }
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "[onEndpointLost] Lost discovered endpoint: $endpointId")
            connectingEndpoints.remove(endpointId)
        }
    }

    private fun initiateConnection(endpointId: String) {
        if (connectedEndpoints.containsKey(endpointId) || connectingEndpoints.containsKey(endpointId)) return
        connectingEndpoints[endpointId] = System.currentTimeMillis()
        connectionsClient.requestConnection(
            getAdvertisedName(),
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.i(TAG, "[requestConnection] Successfully requested connection to $endpointId")
        }.addOnFailureListener { e ->
            connectingEndpoints.remove(endpointId)
            Log.e(TAG, "[requestConnection] Failed requesting connection to $endpointId", e)
        }
    }

    fun startMesh() {
        startAdvertising()
        startDiscovery()
    }

    fun stopMesh() {
        stopAdvertising()
        stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        connectingEndpoints.clear()
        _connectedPeerCount.value = 0
    }

    fun restartMesh() {
        stopMesh()
        scope.launch {
            delay(500)
            startMesh()
        }
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startAdvertising(
            getAdvertisedName(),
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            _isAdvertising.value = true
            Log.i(TAG, "[startAdvertising] Started advertising service: $SERVICE_ID as '${getAdvertisedName()}'")
        }.addOnFailureListener { e ->
            _isAdvertising.value = false
            Log.e(TAG, "[startAdvertising] Advertising start failed", e)
        }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            _isDiscovering.value = true
            Log.i(TAG, "[startDiscovery] Started discovery for service: $SERVICE_ID")
        }.addOnFailureListener { e ->
            _isDiscovering.value = false
            Log.e(TAG, "[startDiscovery] Discovery start failed", e)
        }
    }

    private fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
    }

    private fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
    }

    private fun handleIncomingJson(json: String, fromEndpointId: String) {
        scope.launch {
            try {
                val packet = gson.fromJson(json, NearbyPacket::class.java) ?: return@launch

                // Duplicate suppression
                if (processedPacketIds.contains(packet.packetId)) {
                    Log.d(TAG, "Suppressed duplicate packet: ${packet.packetId}")
                    return@launch
                }
                processedPacketIds.add(packet.packetId)

                Log.i(TAG, "[handleIncomingJson] Received ${packet.kind} packet (${packet.packetId}) from ${packet.senderName} [${packet.senderId}] via endpoint $fromEndpointId")

                // Route according to kind
                when (packet.kind) {
                    PayloadKind.EMERGENCY -> {
                        onEmergencyReceived?.invoke(packet)
                        // Send automatic ACK back to sender
                        sendAck(packet.packetId, packet.senderId)
                        // Adaptive bounded relay
                        relayPacketIfEligible(packet, fromEndpointId)
                    }
                    PayloadKind.STATUS -> {
                        onStatusReceived?.invoke(packet)
                        relayPacketIfEligible(packet, fromEndpointId)
                    }
                    PayloadKind.ACK -> {
                        onAckReceived?.invoke(packet)
                    }
                    PayloadKind.TEAM_LOCATION -> {
                        onTeamLocationReceived?.invoke(packet)
                    }
                    PayloadKind.COMMUNITY_ALERT -> {
                        onCommunityAlertReceived?.invoke(packet)
                        relayPacketIfEligible(packet, fromEndpointId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming payload", e)
            }
        }
    }

    private fun relayPacketIfEligible(packet: NearbyPacket, incomingEndpointId: String) {
        val nextHop = packet.hopCount + 1
        if (nextHop > RoutingPolicy.MAX_HOPS) {
            Log.d(TAG, "Packet ${packet.packetId} reached max hops (${packet.hopCount}). Dropping relay.")
            return
        }

        val urgency = packet.urgency ?: "MEDIUM"
        val relayScore = RoutingPolicy.computeRelayScore(
            urgency = urgency,
            hopCount = packet.hopCount,
            createdAtMillis = packet.timestamp
        )

        if (RoutingPolicy.shouldRelay(packet.hopCount, relayScore)) {
            val forwardedPacket = packet.copy(hopCount = nextHop)
            val json = gson.toJson(forwardedPacket)
            val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))

            // Forward to all connected peers except the one it arrived from
            for (endpoint in connectedEndpoints.keys) {
                if (endpoint != incomingEndpointId) {
                    connectionsClient.sendPayload(endpoint, payload)
                }
            }
            Log.i(TAG, "[relayPacketIfEligible] Relayed packet ${packet.packetId} (hop=$nextHop, score=$relayScore)")
        } else {
            Log.d(TAG, "Packet ${packet.packetId} did not meet relay threshold (score=$relayScore, threshold=${RoutingPolicy.RELAY_THRESHOLD})")
        }
    }

    fun broadcastPacket(packet: NearbyPacket) {
        scope.launch {
            try {
                processedPacketIds.add(packet.packetId)
                val json = gson.toJson(packet)
                val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))

                val targets = connectedEndpoints.keys.toList()
                if (targets.isNotEmpty()) {
                    connectionsClient.sendPayload(targets, payload)
                        .addOnSuccessListener {
                            Log.i(TAG, "[broadcastPacket] Broadcasted ${packet.kind} (${packet.packetId}) to ${targets.size} endpoints: $targets")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "[broadcastPacket] Broadcast failed", e)
                            pendingOutbox.add(packet)
                        }
                } else {
                    // No peers currently connected: queue in outbox for immediate transmission on next connection!
                    Log.w(TAG, "[broadcastPacket] No connected endpoints. Packet ${packet.packetId} (${packet.kind}) saved in pending outbox.")
                    pendingOutbox.add(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed serializing packet", e)
            }
        }
    }

    private fun flushPendingOutbox(targetEndpointId: String) {
        scope.launch {
            if (pendingOutbox.isEmpty()) return@launch
            Log.i(TAG, "[flushPendingOutbox] Flushing ${pendingOutbox.size} queued packets to $targetEndpointId")
            while (pendingOutbox.isNotEmpty()) {
                val packet = pendingOutbox.poll() ?: break
                try {
                    val json = gson.toJson(packet)
                    val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
                    connectionsClient.sendPayload(targetEndpointId, payload)
                        .addOnSuccessListener {
                            Log.i(TAG, "[flushPendingOutbox] Successfully transmitted queued packet ${packet.packetId} to $targetEndpointId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "[flushPendingOutbox] Failed sending queued packet ${packet.packetId}", e)
                            pendingOutbox.add(packet)
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error flushing queued packet", e)
                }
            }
        }
    }

    fun sendAck(originalPacketId: String, destinationSenderId: String) {
        val ackPacket = NearbyPacket(
            packetId = "ack_${System.currentTimeMillis()}_${(1000..9999).random()}",
            kind = PayloadKind.ACK,
            senderId = deviceNodeId,
            senderName = deviceDisplayName,
            emergencyId = originalPacketId
        )
        Log.i(TAG, "[sendAck] Sending ACK for emergency $originalPacketId to $destinationSenderId")
        broadcastPacket(ackPacket)
    }
}
