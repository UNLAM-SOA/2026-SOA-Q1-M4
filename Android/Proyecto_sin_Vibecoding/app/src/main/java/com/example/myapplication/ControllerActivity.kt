package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ControllerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.controller)

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

        val btnSensors = findViewById<Button>(R.id.btnSensors)

        btnSensors.setOnClickListener {
            startActivity(
                Intent(this, SensorActivity::class.java)
            )
        }
    }
}