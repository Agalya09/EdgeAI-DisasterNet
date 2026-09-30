package com.example.disasternet

import com.example.disasternet.model.UrgencyLevel
import com.example.disasternet.network.RoutingPolicy
import org.junit.Assert.*
import org.junit.Test

class RoutingPolicyTest {

    @Test
    fun testFreshCriticalPacketHasHighScore() {
        val now = System.currentTimeMillis()
        val score = RoutingPolicy.computeRelayScore(
            urgency = UrgencyLevel.CRITICAL,
            hopCount = 0,
            createdAtMillis = now,
            nowMillis = now
        )

        // Urgency=1.0*0.5 + HopEfficiency=1.0*0.3 + Freshness=1.0*0.2 = 1.0
        assertEquals(1.0, score, 0.001)
        assertTrue(RoutingPolicy.shouldRelay(hopCount = 0, score = score))
    }

    @Test
    fun testMaxHopPacketIsNotRelayed() {
        val now = System.currentTimeMillis()
        val score = RoutingPolicy.computeRelayScore(
            urgency = UrgencyLevel.CRITICAL,
            hopCount = RoutingPolicy.MAX_HOPS,
            createdAtMillis = now,
            nowMillis = now
        )

        // Even with a high score, hopCount >= MAX_HOPS must never be relayed
        assertFalse(RoutingPolicy.shouldRelay(hopCount = RoutingPolicy.MAX_HOPS, score = score))
    }

    @Test
    fun testExpiredLowUrgencyPacketFallsBelowThreshold() {
        val now = System.currentTimeMillis()
        val staleTime = now - 60_000 // 60 seconds old (Freshness window is 30s -> Freshness = 0.0)

        val score = RoutingPolicy.computeRelayScore(
            urgency = UrgencyLevel.LOW,
            hopCount = 3, // HopEfficiency = 1.0 - 3/4 = 0.25 -> 0.25 * 0.30 = 0.075
            createdAtMillis = staleTime,
            nowMillis = now
        )

        // Urgency=0.25*0.50=0.125 + 0.075 + 0.0 = 0.20 < 0.45 threshold
        assertTrue(score < RoutingPolicy.RELAY_THRESHOLD)
        assertFalse(RoutingPolicy.shouldRelay(hopCount = 3, score = score))
    }

    @Test
    fun testMediumUrgencyNearThreshold() {
        val now = System.currentTimeMillis()
        val score = RoutingPolicy.computeRelayScore(
            urgency = UrgencyLevel.HIGH,
            hopCount = 1,
            createdAtMillis = now,
            nowMillis = now
        )

        // High: 0.75*0.50=0.375 + Hop(1)=0.75*0.30=0.225 + Fresh=0.20 = 0.80 >= 0.45
        assertTrue(RoutingPolicy.shouldRelay(hopCount = 1, score = score))
    }
}
