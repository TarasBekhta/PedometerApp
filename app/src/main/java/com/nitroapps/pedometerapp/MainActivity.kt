package com.nitroapps.pedometerapp

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity(), SensorEventListener, StepListener {
    private var numSteps: Int = 0
    private var startIntent: Intent? = null

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        numSteps = this.getSharedPreferences(packageName, Context.MODE_PRIVATE).getInt(Constants.STEPS_COUNT, 0)
        stepsCountTextview.text = "Number of steps: $numSteps"

        btnStart.setOnClickListener {
            startIntent = Intent(this@MainActivity, StepsForegroundService::class.java)
            startIntent!!.action = Constants.STARTFOREGROUND_ACTION
            startService(startIntent)
        }

        btnStop.setOnClickListener {
            startIntent = Intent(this@MainActivity, StepsForegroundService::class.java)
            startIntent!!.action = Constants.STOPFOREGROUND_ACTION
            startService(startIntent)
        }
    }

    override fun onDestroy() {
        if(startIntent != null) {
            stopService(startIntent)
        }
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
//        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
//            simpleStepDetector!!.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2])
//        }
    }

    override fun onResume() {
        super.onResume()

        numSteps = this.getSharedPreferences(packageName, Context.MODE_PRIVATE).getInt(Constants.STEPS_COUNT, 0)
        stepsCountTextview.text = "Number of steps: $numSteps"
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStep(stepEvent: StepEvent) {
        numSteps = stepEvent.steps
        stepsCountTextview.text = "Number of steps: $numSteps"
    }

    override fun step(timeNs: Long) {
        numSteps++
        stepsCountTextview.text = "Number of steps: $numSteps"
    }

}
