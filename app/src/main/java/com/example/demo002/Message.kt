package com.example.demo002

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate

class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID      = "daily_schedule_channel"
        private const val CHANNEL_NAME    = "每日日程提醒"
        private const val NOTIFICATION_ID = 1001
        // 时间修改通知用不同 id，避免覆盖每日通知
        private const val TIME_CHANGED_NOTIFICATION_ID = 1002

        // ── 供 Setting.kt 调用：立即发送"时间已修改"通知 ──
        fun sendTimeChangedNotification(context: Context, hour: Int, minute: Int) {
            val timeStr = "%02d:%02d".format(hour, minute)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            ensureChannel(context, manager)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("⏰ 通知时间已更新")
                .setContentText("将在每天 $timeStr 推送日程提醒")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            manager.notify(TIME_CHANGED_NOTIFICATION_ID, notification)
        }

        // ── 创建通知渠道（内部复用）──
        private fun ensureChannel(context: Context, manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "每天定时推送当日任务完成情况"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val (title, body) = buildNotificationContent(context)
        sendNotification(context, title, body)
    }

    // ══════════════════════════════════════════════
    //  读取今日任务完成情况（原始 SQL 查询，同步）
    // ══════════════════════════════════════════════

    private fun buildNotificationContent(context: Context): Pair<String, String> {
        val today = LocalDate.now().toString()

        val db     = AppDatabase.getInstance(context)
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT isDone FROM tasks WHERE dueDate = ?",
            arrayOf(today)
        )

        var total = 0
        var done  = 0

        cursor.use {
            while (it.moveToNext()) {
                total++
                if (it.getInt(0) == 1) done++
            }
        }

        val remaining = total - done

        return when {
            total == 0     -> Pair(
                "📅 今日日程",
                "今天没有安排任务，好好休息一下吧 😊"
            )
            remaining == 0 -> Pair(
                "🎉 今日任务全部完成！",
                "太棒了！今天共 $total 个任务，全部完成 ✅"
            )
            done == 0      -> Pair(
                "📋 今日日程提醒",
                "今天共 $total 个任务，还没开始，加油！💪"
            )
            else           -> Pair(
                "📋 今日日程提醒",
                "今天共 $total 个任务，已完成 $done 个 ✅，还剩 $remaining 个待完成 💪"
            )
        }
    }

    // ══════════════════════════════════════════════
    //  发送每日通知
    // ══════════════════════════════════════════════

    private fun sendNotification(context: Context, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        ensureChannel(context, manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}