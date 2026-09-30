package com.example.disasternet

import com.example.disasternet.ai.EdgeAiEngine
import com.example.disasternet.model.EmergencyCategory
import com.example.disasternet.model.UrgencyLevel
import org.junit.Assert.*
import org.junit.Test

class EdgeAiEngineTest {

    @Test
    fun testMedicalClassification() {
        val input = "Victim has severe bleeding and broken bone from fall, losing consciousness"
        val result = EdgeAiEngine.classify(input)

        assertEquals(EmergencyCategory.MEDICAL, result.category)
        assertEquals(UrgencyLevel.CRITICAL, result.urgency)
        assertTrue("Expected confidence > 50, got ${result.confidence}", result.confidence > 50)
        assertEquals(EdgeAiEngine.MODEL_VERSION, result.modelVersion)
    }

    @Test
    fun testTrappedClassification() {
        val input = "Under rubble in collapsed building basement, concrete slab pinned leg"
        val result = EdgeAiEngine.classify(input)

        assertEquals(EmergencyCategory.TRAPPED, result.category)
        assertTrue(result.urgency == UrgencyLevel.CRITICAL || result.urgency == UrgencyLevel.HIGH)
        assertTrue("Expected confidence > 50, got ${result.confidence}", result.confidence > 50)
    }

    @Test
    fun testFireClassification() {
        val input = "House fire spreading rapidly with thick toxic smoke and flames"
        val result = EdgeAiEngine.classify(input)

        assertEquals(EmergencyCategory.FIRE, result.category)
        assertTrue("Expected confidence > 50, got ${result.confidence}", result.confidence > 50)
    }

    @Test
    fun testSafetyClassification() {
        val input = "Rising flood waters submerged ground floor, live electrical wire in water"
        val result = EdgeAiEngine.classify(input)

        assertEquals(EmergencyCategory.SAFETY, result.category)
        assertTrue("Expected confidence > 40, got ${result.confidence}", result.confidence > 40)
    }

    @Test
    fun testSoftmaxProbabilitiesNormalization() {
        val input = "Need food and clean drinking water after storm"
        val result = EdgeAiEngine.classify(input)

        assertNotNull(result.category)
        assertTrue("Confidence must be between 0 and 100", result.confidence in 0..100)
    }

    @Test
    fun testUserCategoryPriorHint() {
        val input = "General distress message"
        val result = EdgeAiEngine.classify(input, userSelectedCategory = EmergencyCategory.FIRE)

        // When user explicitly selects Fire, the engine incorporates the user prior
        assertEquals(EmergencyCategory.FIRE, result.category)
    }

    @Test
    fun testCriticalSignalsTriggerCriticalUrgency() {
        val input = "Person is not breathing and has no pulse"
        val result = EdgeAiEngine.classify(input)

        assertEquals(UrgencyLevel.CRITICAL, result.urgency)
    }
}
