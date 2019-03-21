package com.nitroapps.pedometerapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.util.Log
import org.greenrobot.eventbus.EventBus


class StepsForegroundService : Service(), SensorEventListener, StepListener {
    private val LOG_TAG = "PedometerForegroundService"
    private val WAKE_LOCK_TAG = "PedometerForegroundService::PedometerWakelockTag"

    private var simpleStepDetector: StepDetector? = null
    private var sensorManager: SensorManager? = null
    private var accel: Sensor? = null
    private var numSteps: Int = 0
    private var builder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var notification: Notification? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationIntent: Intent? = null
    private var sharedPrefs: SharedPreferences? = null
    private var skipRestarting: Boolean = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when {
            intent?.action.equals(Constants.STARTFOREGROUND_ACTION) -> {
                //Log.i(LOG_TAG, "Received Start Foreground Intent ")

                wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                        acquire()
                    }
                }

                sharedPrefs = this.getSharedPreferences(packageName, Context.MODE_PRIVATE)
                numSteps = sharedPrefs!!.getInt(Constants.STEPS_COUNT, 0)
                skipRestarting = false

                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                accel = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                simpleStepDetector = StepDetector()
                simpleStepDetector!!.registerListener(this)

                sensorManager!!.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST)

                notificationIntent = Intent(this, MainActivity::class.java)
                notificationIntent!!.action = Constants.MAIN_ACTION
                notificationIntent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

                val stopIntent = Intent(this, StepsForegroundService::class.java)
                stopIntent.action = Constants.STOP_ACTION
                val sstopIntent = PendingIntent.getService(this, 0, stopIntent, 0)

                //val icon = BitmapFactory.decodeResource(resources, R.drawable.ic_walk)

                val channelId =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createNotificationChannel("pedometer_service", "PedometerService")
                    } else {
                        // If earlier version channel ID is not used
                        // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                        ""
                    }


                builder = NotificationCompat.Builder(this, channelId)
                    .setContentTitle("Steps detector")
                    .setTicker("Steps detector")
                    .setContentText("Steps: $numSteps")
                    .setSmallIcon(R.drawable.ic_walk)
                    //.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setProgress(10000, numSteps, false)
                    .addAction(R.drawable.ic_stop, "Stop", sstopIntent)
                notification = builder?.build()
                startForeground(Constants.STEPS_FOREGROUND_SERVICE, notification)
            }
            intent?.action.equals(Constants.STOP_ACTION) -> {
                //Log.i(LOG_TAG, "Clicked stop")
                wakeLock?.release()
                sensorManager!!.unregisterListener(this)
            }
            intent?.action.equals(Constants.STOPFOREGROUND_ACTION) -> {
                //Log.i(LOG_TAG, "Received Stop Foreground Intent")
                sensorManager!!.unregisterListener(this)
                wakeLock?.release()
                skipRestarting = true
                stopForeground(true)
                stopSelf()
            }
        }
        return Service.START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        if(!skipRestarting) {
            val broadcastIntent = Intent(this, StepsServiceRestarterBroadcastReciever::class.java)
            sendBroadcast(broadcastIntent)
        }

        super.onDestroy()
    }

    override fun step(timeNs: Long) {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        numSteps++
        EventBus.getDefault().post(StepEvent(numSteps))
        sharedPrefs!!.edit().putInt(Constants.STEPS_COUNT, numSteps).commit()
        notification = builder
            ?.setContentText("Steps: $numSteps")
            ?.setProgress(10000, numSteps, false)
            ?.build()
        notificationManager?.notify(Constants.STEPS_FOREGROUND_SERVICE, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector!!.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}