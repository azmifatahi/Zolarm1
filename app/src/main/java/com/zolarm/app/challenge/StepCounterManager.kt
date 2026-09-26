package com.zolarm.app.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepCounterManager(
    context: Context,
    private val target: Int = 10,
    private val onProgress: (current: Int, target: Int) -> Unit,
    private val onCompleted: () -> Unit,
    private val onSensorMissing: () -> Unit = {}
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var baseline = -1f
    private var steps = 0
    private var finished = false

    fun start() {
        val sensor = detector ?: counter
        if (sensor == null) { onSensorMissing(); return }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        onProgress(0, target)
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (finished) return
        steps = when (e.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> steps + e.values[0].toInt().coerceAtLeast(1)
            Sensor.TYPE_STEP_COUNTER -> {
                if (baseline < 0f) baseline = e.values[0]
                (e.values[0] - baseline).toInt()
            }
            else -> steps
        }.coerceIn(0, target)
        onProgress(steps, target)
        if (steps >= target) {
            finished = true
            stop()
            onCompleted()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
