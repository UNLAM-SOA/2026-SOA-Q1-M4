package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class SensorActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private lateinit var directionText: TextView

    companion object {
        private const val THRESHOLD = 15f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.sensor)

        val topBar = findViewById<MaterialToolbar>(R.id.topAppBar)

        topBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    ConfigModalFragment()
                        .show(supportFragmentManager, "settings")
                    true
                }

                else -> false
            }
        }

        val btnController = findViewById<Button>(R.id.btnController)

        directionText = findViewById(R.id.txtSensorOutput)

        btnController.setOnClickListener {
            startActivity(
                Intent(this, ControllerActivity::class.java)
            )
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun onResume() {
        super.onResume()

        rotationSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                event.values
            )

            SensorManager.getOrientation(
                rotationMatrix,
                orientation
            )

            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

            val absPitch = kotlin.math.abs(pitch)
            val absRoll = kotlin.math.abs(roll)

            val direction = when {

                absPitch < THRESHOLD &&
                        absRoll < THRESHOLD -> {
                    "STOP"
                }

                absPitch >= absRoll -> {

                    when {
                        pitch < -THRESHOLD -> "BACKWARD"
                        pitch > THRESHOLD -> "FORWARD"
                        else -> "STOP"
                    }
                }

                else -> {

                    when {
                        roll > THRESHOLD -> "RIGHT"
                        roll < -THRESHOLD -> "LEFT"
                        else -> "STOP"
                    }
                }
            }

            directionText.text = direction
        }
    }
}