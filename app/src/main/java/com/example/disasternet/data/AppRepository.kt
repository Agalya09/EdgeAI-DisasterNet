package com.example.disasternet.data

import android.content.Context
import android.util.Log
import com.example.disasternet.ai.EdgeAiEngine
import com.example.disasternet.ai.TriageEngine
import com.example.disasternet.data.entity.AlertEntity
import com.example.disasternet.data.entity.EmergencyMessageEntity
import com.example.disasternet.data.entity.MeshNodeEntity
import com.example.disasternet.location.LocationHelper
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.EmergencyStatus
import com.example.disasternet.model.NearbyPacket
import com.example.disasternet.model.PayloadKind
import com.example.disasternet.model.Role
import com.example.disasternet.network.NearbyMeshManager
import com.example.disasternet.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveTeamLocation(
    val rescuerName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class AppRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val meshManager: NearbyMeshManager,
    private val locationHelper: LocationHelper,
    private val notificationHelper: NotificationHelper,
    private val currentUserId: String,
    private val currentUserName: String
) {
    private val TAG = "AppRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Rescuer team location received over mesh for victim display
    private val _activeTeamLocation = MutableStateFlow<ActiveTeamLocation?>(null)
    val activeTeamLocation: StateFlow<ActiveTeamLocation?> = _activeTeamLocation.asStateFlow()

    init {
        setupMeshListeners()
    }

    // ==========================================
    // USER Queries (STRICT PRIVACY ENFORCEMENT)
    // ==========================================
    fun getUserRequestsFlow(): Flow<List<EmergencyMessage>> {
        return database.emergencyDao().getForSenderFlow(currentUserId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getLatestUserRequestFlow(): Flow<EmergencyMessage?> {
        return database.emergencyDao().getLatestForSenderFlow(currentUserId).map { it?.toDomain() }
    }

    fun getAlertsFlow(): Flow<List<AlertEntity>> {
        return database.alertDao().getAllFlow()
    }

    // ==========================================
    // RESCUER Queries
    // ==========================================
    fun getRescuerQueueFlow(): Flow<List<EmergencyMessage>> {
        return database.emergencyDao().getAllPendingFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMeshNodesFlow(): Flow<List<MeshNodeEntity>> {
        return database.meshNodeDao().getAllFlow()
    }

    // ==========================================
    // Operations
    // ==========================================

    fun sendSos(
        category: String,
        description: String,
        latitude: Double,
        longitude: Double,
        locationLabel: String,
        manualAddress: String = "",
        victimCount: Int = 1
    ): String {
        val emergencyId = "sos_${UUID.randomUUID().toString().take(8)}"

        // 1. Local Edge AI classification (fully offline, no cloud)
        val classification = EdgeAiEngine.classify(description, category)

        // 2. Compute Triage priority score
        val priorityScore = TriageEngine.computePriorityScore(
            category = classification.category,
            urgency = classification.urgency,
            confidence = classification.confidence,
            victimCount = victimCount
        )

        // 3. Save to Room database
        val entity = EmergencyMessageEntity(
            id = emergencyId,
            senderId = currentUserId,
            senderName = currentUserName,
            message = description.ifBlank { "Emergency assistance requested" },
            category = classification.category,
            urgency = classification.urgency,
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel,
            manualAddress = manualAddress,
            victimCount = victimCount,
            createdAt = System.currentTimeMillis(),
            status = EmergencyStatus.SOS_SENT,
            hopCount = 0,
            kind = PayloadKind.EMERGENCY,
            aiConfidence = classification.confidence,
            priorityScore = priorityScore
        )

        scope.launch {
            database.emergencyDao().insertOrUpdate(entity)
            Log.i(TAG, "Saved emergency $emergencyId to Room with status ${entity.status}")
        }

        // 4. Construct Nearby packet and broadcast via Nearby Connections
        val packet = NearbyPacket(
            packetId = emergencyId,
            kind = PayloadKind.EMERGENCY,
            senderId = currentUserId,
            senderName = currentUserName,
            emergencyId = emergencyId,
            category = classification.category,
            urgency = classification.urgency,
            message = entity.message,
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel,
            manualAddress = manualAddress,
            victimCount = victimCount,
            status = EmergencyStatus.SOS_SENT,
            hopCount = 0,
            timestamp = entity.createdAt
        )
        meshManager.broadcastPacket(packet)

        // 5. Trigger notification
        notificationHelper.showSosSentNotification(classification.category)

        return emergencyId
    }

    fun updateEmergencyStatus(emergencyId: String, newStatus: String, currentResponderLocation: android.location.Location? = null) {
        scope.launch {
            val existing = database.emergencyDao().getById(emergencyId)
            database.emergencyDao().updateStatus(emergencyId, newStatus)
            Log.i(TAG, "Updated status of $emergencyId to $newStatus")

            // Broadcast status packet to mesh
            val statusPacket = NearbyPacket(
                packetId = "status_${UUID.randomUUID().toString().take(8)}",
                kind = PayloadKind.STATUS,
                senderId = currentUserId,
                senderName = currentUserName,
                emergencyId = emergencyId,
                status = newStatus,
                timestamp = System.currentTimeMillis()
            )
            meshManager.broadcastPacket(statusPacket)

            // Community Alert broadcasting on emergency acceptance
            if (newStatus == EmergencyStatus.RESCUE_TEAM_ASSIGNED && existing != null) {
                val communityAlertPacket = NearbyPacket(
                    packetId = "comm_${UUID.randomUUID().toString().take(8)}",
                    kind = PayloadKind.COMMUNITY_ALERT,
                    senderId = currentUserId,
                    senderName = "Rescue Coordinator",
                    latitude = existing.latitude,
                    longitude = existing.longitude,
                    alertTitle = "Emergency Nearby",
                    alertBody = "Rescue operations initiated for an incident reported nearby.",
                    alertSeverity = "HIGH",
                    timestamp = System.currentTimeMillis()
                )
                meshManager.broadcastPacket(communityAlertPacket)
            }
        }
    }

    fun broadcastResponderLocation(lat: Double, lon: Double, responderName: String) {
        val locationPacket = NearbyPacket(
            packetId = "loc_${UUID.randomUUID().toString().take(8)}",
            kind = PayloadKind.TEAM_LOCATION,
            senderId = currentUserId,
            senderName = responderName,
            responderLatitude = lat,
            responderLongitude = lon,
            responderName = responderName,
            timestamp = System.currentTimeMillis()
        )
        meshManager.broadcastPacket(locationPacket)
    }

    private fun setupMeshListeners() {
        meshManager.onEmergencyReceived = { packet ->
            scope.launch {
                val emergencyId = packet.emergencyId ?: packet.packetId
                val classification = EdgeAiEngine.classify(packet.message ?: "", packet.category)
                val currentLoc = locationHelper.currentLocation.value
                val distMeters = if (currentLoc != null && packet.latitude != null && packet.longitude != null) {
                    LocationHelper.calculateDistance(currentLoc.latitude, currentLoc.longitude, packet.latitude, packet.longitude)
                } else null

                val priorityScore = TriageEngine.computePriorityScore(
                    category = packet.category ?: classification.category,
                    urgency = packet.urgency ?: classification.urgency,
                    confidence = classification.confidence,
                    victimCount = packet.victimCount,
                    distanceMeters = distMeters
                )

                val entity = EmergencyMessageEntity(
                    id = emergencyId,
                    senderId = packet.senderId,
                    senderName = packet.senderName,
                    message = packet.message ?: "Emergency alert",
                    category = packet.category ?: classification.category,
                    urgency = packet.urgency ?: classification.urgency,
                    latitude = packet.latitude ?: 0.0,
                    longitude = packet.longitude ?: 0.0,
                    locationLabel = packet.locationLabel ?: "Nearby Area",
                    manualAddress = packet.manualAddress ?: "",
                    victimCount = packet.victimCount,
                    createdAt = packet.timestamp,
                    status = packet.status ?: EmergencyStatus.SOS_SENT,
                    hopCount = packet.hopCount,
                    kind = PayloadKind.EMERGENCY,
                    aiConfidence = classification.confidence,
                    priorityScore = priorityScore
                )

                database.emergencyDao().insertOrUpdate(entity)
                Log.i(TAG, "[onEmergencyReceived] Stored emergency from ${packet.senderName} with address '${packet.manualAddress}' in Room.")
            }
        }

        meshManager.onStatusReceived = { packet ->
            scope.launch {
                val targetEmergencyId = packet.emergencyId ?: return@launch
                val newStatus = packet.status ?: return@launch

                val existing = database.emergencyDao().getById(targetEmergencyId)
                if (existing != null) {
                    database.emergencyDao().updateStatus(targetEmergencyId, newStatus)
                    Log.i(TAG, "[onStatusReceived] Updated emergency $targetEmergencyId status to $newStatus")

                    // If this update is for the current user's request, notify them!
                    if (existing.senderId == currentUserId) {
                        when (newStatus) {
                            EmergencyStatus.REQUEST_RECEIVED -> notificationHelper.showRequestReceivedNotification()
                            EmergencyStatus.RESCUE_TEAM_ASSIGNED -> notificationHelper.showRescueTeamAssignedNotification(packet.senderName)
                            EmergencyStatus.HELP_ON_THE_WAY -> notificationHelper.showHelpOnTheWayNotification()
                            EmergencyStatus.RESCUE_TEAM_ARRIVED -> notificationHelper.showRescueTeamArrivedNotification()
                            EmergencyStatus.RESOLVED -> notificationHelper.showResolvedNotification()
                        }
                    }
                }
            }
        }

        meshManager.onAckReceived = { packet ->
            scope.launch {
                val ackTarget = packet.emergencyId ?: return@launch
                val existing = database.emergencyDao().getById(ackTarget)
                if (existing != null && existing.senderId == currentUserId && existing.status == EmergencyStatus.SOS_SENT) {
                    database.emergencyDao().updateStatus(ackTarget, EmergencyStatus.REQUEST_RECEIVED)
                    Log.i(TAG, "[onAckReceived] Updated $ackTarget to REQUEST_RECEIVED")
                    notificationHelper.showRequestReceivedNotification()
                }
            }
        }

        meshManager.onTeamLocationReceived = { packet ->
            val lat = packet.responderLatitude
            val lon = packet.responderLongitude
            if (lat != null && lon != null) {
                _activeTeamLocation.value = ActiveTeamLocation(
                    rescuerName = packet.responderName ?: packet.senderName,
                    latitude = lat,
                    longitude = lon,
                    timestamp = packet.timestamp
                )
            }
        }

        meshManager.onCommunityAlertReceived = { packet ->
            scope.launch {
                val alertLat = packet.latitude
                val alertLon = packet.longitude
                val currentLoc = locationHelper.currentLocation.value

                var isNearby = true
                var distMeters = 0.0
                if (currentLoc != null && alertLat != null && alertLon != null) {
                    distMeters = LocationHelper.calculateDistance(currentLoc.latitude, currentLoc.longitude, alertLat, alertLon)
                    isNearby = LocationHelper.isWithinCommunityAlertRadius(distMeters)
                }

                if (isNearby) {
                    val alertId = packet.packetId
                    val distStr = if (distMeters > 0) {
                        if (distMeters >= 1000) String.format(java.util.Locale.US, " (approx %.1f km away)", distMeters / 1000.0)
                        else " (approx ${distMeters.toInt()}m away)"
                    } else ""

                    val alert = AlertEntity(
                        id = alertId,
                        title = packet.alertTitle ?: "Emergency Nearby",
                        body = (packet.alertBody ?: "Emergency incident reported in your vicinity.") + distStr,
                        severity = packet.alertSeverity ?: "HIGH",
                        createdAt = packet.timestamp
                    )
                    database.alertDao().insert(alert)
                    notificationHelper.showEmergencyNearbyNotification(distMeters)
                }
            }
        }

        meshManager.onPeerConnected = { id, name ->
            scope.launch {
                database.meshNodeDao().upsert(
                    MeshNodeEntity(
                        id = id,
                        name = name,
                        connectionType = "Nearby P2P",
                        lastSeen = System.currentTimeMillis(),
                        connected = true
                    )
                )

                // Sync pending emergencies to newly connected peer:
                val latest = database.emergencyDao().getLatestForSender(currentUserId)
                if (latest != null && latest.status == EmergencyStatus.SOS_SENT) {
                    Log.i(TAG, "[onPeerConnected] Syncing pending SOS ${latest.id} to new peer $id")
                    val packet = NearbyPacket(
                        packetId = latest.id,
                        kind = PayloadKind.EMERGENCY,
                        senderId = latest.senderId,
                        senderName = latest.senderName,
                        emergencyId = latest.id,
                        category = latest.category,
                        urgency = latest.urgency,
                        message = latest.message,
                        latitude = latest.latitude,
                        longitude = latest.longitude,
                        locationLabel = latest.locationLabel,
                        manualAddress = latest.manualAddress,
                        victimCount = latest.victimCount,
                        status = latest.status,
                        hopCount = latest.hopCount,
                        timestamp = latest.createdAt
                    )
                    meshManager.broadcastPacket(packet)
                }
            }
        }

        meshManager.onPeerDisconnected = { id ->
            scope.launch {
                database.meshNodeDao().updateConnection(id, false, System.currentTimeMillis())
            }
        }
    }
}
