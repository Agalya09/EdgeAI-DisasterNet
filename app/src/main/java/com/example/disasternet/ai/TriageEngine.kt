package com.example.disasternet.ai

import com.example.disasternet.model.EmergencyCategory
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.UrgencyLevel
import kotlin.math.max

object TriageEngine {

    /**
     * Computes triage priority score for emergency queuing (0.0 to 10.0 scale).
     */
    fun computePriorityScore(
        category: String,
        urgency: String,
        confidence: Int,
        victimCount: Int = 1,
        distanceMeters: Double? = null
    ): Double {
        val categoryWeight = when (category) {
            EmergencyCategory.TRAPPED -> 4.0
            EmergencyCategory.FIRE -> 3.8
            EmergencyCategory.MEDICAL -> 3.5
            EmergencyCategory.SAFETY -> 2.5
            else -> 1.8
        }

        val urgencyMultiplier = when (urgency) {
            UrgencyLevel.CRITICAL -> 1.6
            UrgencyLevel.HIGH -> 1.25
            UrgencyLevel.MEDIUM -> 1.0
            else -> 0.75
        }

        val victimBoost = (victimCount - 1).coerceAtLeast(0) * 0.4
        val confidenceFactor = (confidence / 100.0) * 0.8

        var score = (categoryWeight * urgencyMultiplier) + victimBoost + confidenceFactor

        // Distance attenuation: closer emergencies slightly prioritized if responder coordinates known
        if (distanceMeters != null && distanceMeters >= 0) {
            val distKm = distanceMeters / 1000.0
            val proximityBonus = max(0.0, 1.5 - (distKm * 0.3))
            score += proximityBonus
        }

        return (score * 10.0).toInt() / 10.0
    }

    fun sortEmergencyQueue(
        messages: List<EmergencyMessage>,
        responderLat: Double? = null,
        responderLng: Double? = null,
        distanceCalculator: ((Double, Double, Double, Double) -> Double)? = null
    ): List<EmergencyMessage> {
        return messages.sortedWith(
            compareByDescending<EmergencyMessage> { it.urgency == UrgencyLevel.CRITICAL }
                .thenByDescending { it.priorityScore }
                .thenByDescending { it.createdAt }
        )
    }
}
