package com.example.demo002

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

object TaskReminderManager {

    /**
     * 为任务设置提醒
     * @param context 上下文
     * @param task 任务对象
     */
    fun scheduleReminder(context: Context, task: Task) {
        // 检查是否需要设置提醒
        if (task.reminderOffset == null) {
            // 没有设置提醒，取消可能存在的旧提醒
            cancelReminder(context, task.id)
            return
        }

        // 检查是否有截止日期和开始时间
        if (task.dueDate == null || task.startTime == null) {
            // 没有具体时间，无法设置提醒
            return
        }

        // 计算提醒时间
        val reminderTime = calculateReminderTime(task)
        if (reminderTime == null) {
            return
        }

        // 创建 Intent
        val intent = TaskReminderReceiver.createIntent(
            context,
            task.id,
            task.title,
            task.note,
            formatTaskTime(task)
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id, // 使用 task.id 作为 requestCode，确保每个任务唯一
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 设置精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要检查是否有精确闹钟权限
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                // 没有权限，使用不精确的闹钟
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } else {
            // Android 11 及以下
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    /**
     * 取消任务的提醒
     * @param context 上下文
     * @param taskId 任务ID
     */
    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * 重新安排所有任务的提醒（例如在设备重启后）
     * @param context 上下文
     * @param tasks 任务列表
     */
    fun rescheduleAllReminders(context: Context, tasks: List<Task>) {
        for (task in tasks) {
            if (task.reminderOffset != null && !task.isDone) {
                scheduleReminder(context, task)
            }
        }
    }

    /**
     * 计算提醒的具体时间戳
     * @param task 任务对象
     * @return 提醒时间的时间戳（毫秒），如果无法计算则返回 null
     */
    private fun calculateReminderTime(task: Task): Long? {
        if (task.dueDate == null || task.startTime == null || task.reminderOffset == null) {
            return null
        }

        // 将 LocalDate 和 LocalTime 组合成 LocalDateTime
        val taskDateTime = LocalDateTime.of(task.dueDate, task.startTime)
        
        // 减去提醒提前的分钟数
        val reminderDateTime = taskDateTime.minusMinutes(task.reminderOffset!!.toLong())
        
        // 转换为时间戳
        return reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 格式化任务时间用于显示
     * @param task 任务对象
     * @return 格式化的时间字符串
     */
    private fun formatTaskTime(task: Task): String {
        return if (task.startTime != null && task.endTime != null) {
            "${task.startTime} - ${task.endTime}"
        } else if (task.startTime != null) {
            task.startTime.toString()
        } else {
            "全天"
        }
    }

    /**
     * 检查提醒是否已经过期（提醒时间早于当前时间）
     * @param task 任务对象
     * @return 如果提醒已过期返回 true
     */
    fun isReminderExpired(task: Task): Boolean {
        val reminderTime = calculateReminderTime(task) ?: return true
        return reminderTime < System.currentTimeMillis()
    }
}