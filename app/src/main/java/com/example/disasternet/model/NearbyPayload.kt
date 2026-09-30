package com.example.disasternet.model

data class NearbyPacket(
    val packetId: String,
    val kind: String, // EMERGENCY, STATUS, ACK, TEAM_LOCATION, COMMUNITY_ALERT
    val senderId: String,
    val senderName: String,
    val emergencyId: String? = null,
    val category: String? = null,
    val urgency: String? = null,
    val message: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
    val manualAddress: String? = null,
    val victimCount: Int = 1,
    val status: String? = null,
    val hopCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val responderLatitude: Double? = null,
    val responderLongitude: Double? = null,
    val responderName: String? = null,
    val alertTitle: String? = null,
    val alertBody: String? = null,
    val alertSeverity: String? = null
)
