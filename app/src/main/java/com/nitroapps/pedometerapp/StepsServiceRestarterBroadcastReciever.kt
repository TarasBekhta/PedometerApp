package com.nitroapps.pedometerapp

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log


class StepsServiceRestarterBroadcastReciever : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(StepsServiceRestarterBroadcastReciever::class.java.simpleName, "Service Stops! Oooooooooooooppppssssss!!!!")
        //context.startService(Intent(context, StepsForegroundService::class.java))
        val startIntent = Intent(context, StepsForegroundService::class.java)
        startIntent!!.action = Constants.STARTFOREGROUND_ACTION
        context.startService(startIntent)
    }
}