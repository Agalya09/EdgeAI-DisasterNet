package com.example.disasternet.model

object EmergencyStatus {
    const val SOS_SENT = "SOS SENT"
    const val REQUEST_RECEIVED = "REQUEST RECEIVED"
    const val RESCUE_TEAM_ASSIGNED = "RESCUE TEAM ASSIGNED"
    const val HELP_ON_THE_WAY = "HELP ON THE WAY"
    const val RESCUE_TEAM_ARRIVED = "RESCUE TEAM ARRIVED"
    const val RESOLVED = "RESOLVED"
    const val UNABLE_TO_REACH = "UNABLE TO REACH"
}

object EmergencyCategory {
    const val MEDICAL = "Medical"
    const val TRAPPED = "Trapped"
    const val FIRE = "Fire"
    const val SAFETY = "Safety"
    const val OTHER = "Other"
    const val GENERAL = "General"
}

object UrgencyLevel {
    const val CRITICAL = "CRITICAL"
    const val HIGH = "HIGH"
    const val MEDIUM = "MEDIUM"
    const val LOW = "LOW"
}

object PayloadKind {
    const val EMERGENCY = "EMERGENCY"
    const val STATUS = "STATUS"
    const val ACK = "ACK"
    const val TEAM_LOCATION = "TEAM_LOCATION"
    const val COMMUNITY_ALERT = "COMMUNITY_ALERT"
}

data class EmergencyMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val category: String,
    val urgency: String,
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val manualAddress: String = "",
    val victimCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = EmergencyStatus.SOS_SENT,
    val hopCount: Int = 0,
    val kind: String = PayloadKind.EMERGENCY,
    val aiConfidence: Int = 0,
    val priorityScore: Double = 0.0
)
