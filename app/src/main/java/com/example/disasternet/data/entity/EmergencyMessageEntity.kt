package com.example.disasternet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.EmergencyStatus
import com.example.disasternet.model.PayloadKind

@Entity(tableName = "emergency_messages")
data class EmergencyMessageEntity(
    @PrimaryKey
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
) {
    fun toDomain(): EmergencyMessage = EmergencyMessage(
        id = id,
        senderId = senderId,
        senderName = senderName,
        message = message,
        category = category,
        urgency = urgency,
        latitude = latitude,
        longitude = longitude,
        locationLabel = locationLabel,
        manualAddress = manualAddress,
        victimCount = victimCount,
        createdAt = createdAt,
        status = status,
        hopCount = hopCount,
        kind = kind,
        aiConfidence = aiConfidence,
        priorityScore = priorityScore
    )

    companion object {
        fun fromDomain(model: EmergencyMessage): EmergencyMessageEntity = EmergencyMessageEntity(
            id = model.id,
            senderId = model.senderId,
            senderName = model.senderName,
            message = model.message,
            category = model.category,
            urgency = model.urgency,
            latitude = model.latitude,
            longitude = model.longitude,
            locationLabel = model.locationLabel,
            manualAddress = model.manualAddress,
            victimCount = model.victimCount,
            createdAt = model.createdAt,
            status = model.status,
            hopCount = model.hopCount,
            kind = model.kind,
            aiConfidence = model.aiConfidence,
            priorityScore = model.priorityScore
        )
    }
}
