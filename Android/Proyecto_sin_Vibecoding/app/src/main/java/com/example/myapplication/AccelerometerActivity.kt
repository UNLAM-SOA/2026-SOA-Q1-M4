package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import kotlin.math.abs

class AccelerometerActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var tvMovimiento: TextView
    private lateinit var leftDistanceText: TextView
    private lateinit var rightDistanceText: TextView
    private lateinit var ivLedStatus: ImageView

    private val mqtt by lazy {
        (application as CustomApplication).mqttManager
    }

    private var lastDirection = ""

    companion object {
        // Ajustamos el umbral de sensibilidad
        private const val THRESHOLD = 3.5f
        private const val LIGHT_THRESHOLD = 3000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acelerometro)

        tvMovimiento = findViewById(R.id.tvMovimiento)
        leftDistanceText = findViewById(R.id.leftDistanceText)
        rightDistanceText = findViewById(R.id.rightDistanceText)
        ivLedStatus = findViewById(R.id.ivLedStatus)

        val topBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    ConfigModalFragment().show(supportFragmentManager, "settings")
                    true
                }
                else -> false
            }
        }

        findViewById<Button>(R.id.btnControllerActivity).setOnClickListener {
            startActivity(Intent(this, ControllerActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnSensorActivity).setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnAccelerometerActivity).isEnabled = false

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mqtt.stateFlow.collect { state ->
                    val distIzq = state.distanceLeft?.toInt() ?: 0
                    val distDer = state.distanceRight?.toInt() ?: 0
                    leftDistanceText.text = "Distancia Izq: $distIzq cm"
                    rightDistanceText.text = "Distancia Der: $distDer cm"
                    

                    val lightValue = state.light ?: 1000
                    if (lightValue > LIGHT_THRESHOLD) {
                        ivLedStatus.setColorFilter(Color.YELLOW) // Encendido (Oscuro)
                    } else {
                        ivLedStatus.setColorFilter(Color.GRAY) // Apagado (Luz)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        mqtt.sendCommand("stop")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            
            val steer = event.values[1]
            val drive = event.values[0]

            val absSteer = abs(steer)
            val absDrive = abs(drive)


            val direction = when {
                absSteer < THRESHOLD && absDrive < THRESHOLD -> "stop"
                absSteer > absDrive -> {

                    if (steer > THRESHOLD) "right" else if (steer < -THRESHOLD) "left" else "stop"
                }
                else -> {

                    if (drive > THRESHOLD) "forward" else if (drive < -THRESHOLD) "backward" else "stop"
                }
            }


            if (direction != lastDirection) {
                lastDirection = direction
                mqtt.sendCommand(direction)

                updateMovementUI(direction)
            }
        }
    }

    private fun updateMovementUI(direction: String) {
        tvMovimiento.text = "ESTADO: ${direction.uppercase()}"
        when (direction) {
            "stop" -> tvMovimiento.setTextColor(Color.RED)
            else -> tvMovimiento.setTextColor(Color.parseColor("#388E3C"))
        }
    }
}
