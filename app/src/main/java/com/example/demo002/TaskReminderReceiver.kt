package com.example.demo002

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "task_reminder_channel"
        private const val CHANNEL_NAME = "任务提醒"
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TASK_TITLE = "task_title"
        private const val EXTRA_TASK_NOTE = "task_note"
        private const val EXTRA_TASK_TIME = "task_time"

        // 创建通知渠道
        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "任务开始或截止时间提醒"
                    enableVibration(true)
                    enableLights(true)
                    lightColor = android.graphics.Color.GREEN
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        // 创建用于设置闹钟的 Intent
        fun createIntent(context: Context, taskId: Int, title: String, note: String, time: String): Intent {
            return Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, title)
                putExtra(EXTRA_TASK_NOTE, note)
                putExtra(EXTRA_TASK_TIME, time)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "任务提醒"
        val note = intent.getStringExtra(EXTRA_TASK_NOTE) ?: ""
        val time = intent.getStringExtra(EXTRA_TASK_TIME) ?: ""

        // 构建通知内容
        val notificationBody = if (note.isNotEmpty()) {
            "$note\n时间: $time"
        } else {
            "时间: $time"
        }

        // 确保通知渠道存在
        ensureChannel(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $title")
            .setContentText(notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // 使用 taskId 作为通知 ID，确保每个任务的通知是独立的
        val notificationId = 2000 + (taskId % 1000) // 确保在有效范围内
        manager.notify(notificationId, notification)
    }
}