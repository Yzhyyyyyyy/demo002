package com.example.demo002

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs   = context.getSharedPreferences("notify_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("notify_enabled", true)
            if (enabled) {
                val hour   = prefs.getInt("notify_hour", 8)
                val minute = prefs.getInt("notify_minute", 0)
                AlarmScheduler.schedule(context, hour, minute)
            }
        }
    }
}