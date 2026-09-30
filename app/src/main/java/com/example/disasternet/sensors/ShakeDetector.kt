package com.example.disasternet.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShakeTriggered: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastShakeTimestamp: Long = 0
    private var lastForceTimestamp: Long = 0

    private val SHAKE_THRESHOLD_GRAVITY = 2.7f
    private val SHAKE_WINDOW_MILLIS = 1800L
    private val SHAKE_DEBOUNCE_MILLIS = 220L
    private val REQUIRED_SHAKE_COUNT = 3

    fun startListening() {
        if (accelerometer != null && sensorManager != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        shakeCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH

        val gForce = sqrt(x * x + y * y + z * z)

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            if (now - lastForceTimestamp < SHAKE_DEBOUNCE_MILLIS) {
                return
            }
            lastForceTimestamp = now

            if (now - lastShakeTimestamp > SHAKE_WINDOW_MILLIS) {
                shakeCount = 0
            }

            shakeCount++
            lastShakeTimestamp = now

            if (shakeCount >= REQUIRED_SHAKE_COUNT) {
                shakeCount = 0
                onShakeTriggered()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
