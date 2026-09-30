package com.example.disasternet

import com.example.disasternet.model.NearbyPacket
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class NearbyPayloadSerializationTest {

    private val gson = Gson()

    @Test
    fun testNearbyPacketSerializationWithManualAddress() {
        val packet = NearbyPacket(
            packetId = "pkt_test_101",
            kind = "EMERGENCY",
            senderId = "citizen_node_42",
            senderName = "Citizen Alice",
            emergencyId = "sos_4242",
            category = "MEDICAL",
            urgency = "HIGH",
            message = "Need oxygen tank immediately",
            latitude = 12.9716,
            longitude = 77.5946,
            locationLabel = "Sector 4",
            manualAddress = "Apt 4B, Blue Horizon Tower, near City Hospital",
            victimCount = 2,
            status = "SOS_SENT",
            hopCount = 1
        )

        val json = gson.toJson(packet)
        assertTrue(json.contains("Apt 4B, Blue Horizon Tower"))
        assertTrue(json.contains("Citizen Alice"))

        val deserialized = gson.fromJson(json, NearbyPacket::class.java)
        assertEquals(packet.packetId, deserialized.packetId)
        assertEquals(packet.senderName, deserialized.senderName)
        assertEquals(packet.manualAddress, deserialized.manualAddress)
        assertEquals(packet.victimCount, deserialized.victimCount)
        assertEquals(packet.latitude, deserialized.latitude)
        assertEquals(packet.longitude, deserialized.longitude)
    }

    @Test
    fun testNearbyPacketOptionalAddressNullHandling() {
        val packet = NearbyPacket(
            packetId = "pkt_test_102",
            kind = "EMERGENCY",
            senderId = "citizen_node_99",
            senderName = "Citizen Bob",
            emergencyId = "sos_9999",
            manualAddress = null
        )

        val json = gson.toJson(packet)
        val deserialized = gson.fromJson(json, NearbyPacket::class.java)

        assertNull(deserialized.manualAddress)
        assertEquals("Citizen Bob", deserialized.senderName)
    }
}
