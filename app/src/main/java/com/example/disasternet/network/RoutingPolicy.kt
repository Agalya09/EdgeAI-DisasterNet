package com.example.disasternet.network

import com.example.disasternet.model.UrgencyLevel
import kotlin.math.max

object RoutingPolicy {

    const val URGENCY_WEIGHT = 0.50
    const val HOP_EFFICIENCY_WEIGHT = 0.30
    const val FRESHNESS_WEIGHT = 0.20
    const val RELAY_THRESHOLD = 0.45
    const val MAX_HOPS = 4
    const val FRESHNESS_WINDOW_SECONDS = 30.0

    /**
     * Adaptive relay scoring formula:
     * Score = 0.50 * Urgency + 0.30 * HopEfficiency + 0.20 * Freshness
     */
    fun computeRelayScore(urgency: String, hopCount: Int, createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Double {
        val urgencyFactor = when (urgency) {
            UrgencyLevel.CRITICAL -> 1.0
            UrgencyLevel.HIGH -> 0.75
            UrgencyLevel.MEDIUM -> 0.50
            else -> 0.25
        }

        val hopEfficiency = (1.0 - (hopCount.toDouble() / MAX_HOPS.toDouble())).coerceIn(0.0, 1.0)

        val ageSeconds = max(0.0, (nowMillis - createdAtMillis) / 1000.0)
        val freshness = max(0.0, 1.0 - (ageSeconds / FRESHNESS_WINDOW_SECONDS))

        return (URGENCY_WEIGHT * urgencyFactor) +
                (HOP_EFFICIENCY_WEIGHT * hopEfficiency) +
                (FRESHNESS_WEIGHT * freshness)
    }

    /**
     * Relay condition: hopCount < 4 AND score >= 0.45
     */
    fun shouldRelay(hopCount: Int, score: Double): Boolean {
        return hopCount < MAX_HOPS && score >= RELAY_THRESHOLD
    }
}
