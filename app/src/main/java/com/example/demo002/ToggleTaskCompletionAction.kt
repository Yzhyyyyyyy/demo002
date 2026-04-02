package com.example.demo002

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.first

class ToggleTaskCompletionAction : ActionCallback {

    companion object {
        // 修复：改回 Int，因为你的 task.id 是 Int 类型
        val taskIdKey = ActionParameters.Key<Int>("task_id")
        private const val TAG = "ToggleTaskCompletion"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        Log.d(TAG, "开始处理任务ID: $taskId")

        try {
            val dao = AppDatabase.getInstance(context).taskDao()
            Log.d(TAG, "数据库实例获取成功")
            
            val tasks = dao.getAllTasksWithSubTasks().first()
            Log.d(TAG, "获取到 ${tasks.size} 个任务")
            
            val taskEntity = tasks.find { it.task.id == taskId }?.task
            if (taskEntity == null) {
                Log.e(TAG, "未找到ID为 $taskId 的任务")
                return
            }
            
            Log.d(TAG, "找到任务: ${taskEntity.title}, 当前状态: ${taskEntity.isDone}")
            val updatedTask = taskEntity.copy(isDone = !taskEntity.isDone)
            Log.d(TAG, "更新后状态: ${updatedTask.isDone}")
            
            dao.upsertTask(updatedTask)
            Log.d(TAG, "数据库更新成功")

            // 双重刷新机制：Glance updateAll + 直接 AppWidgetManager 更新
            Log.d(TAG, "开始刷新小组件...")
            
            // 1. 使用 Glance 的 updateAll（可能异步）
            TodayScheduleWidget().updateAll(context)
            UrgentTaskWidget().updateAll(context)
            
            // 2. 直接通过 AppWidgetManager 强制刷新所有实例
            forceRefreshAppWidgets(context)
            
            Log.d(TAG, "小组件刷新完成")

        } catch (e: Exception) {
            Log.e(TAG, "处理任务时发生错误", e)
            e.printStackTrace()
        }
    }
    
    private fun forceRefreshAppWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // 刷新今日日程小组件
            val todayScheduleComponent = ComponentName(context, TodayScheduleWidgetReceiver::class.java)
            val todayScheduleIds = appWidgetManager.getAppWidgetIds(todayScheduleComponent)
            if (todayScheduleIds.isNotEmpty()) {
                Log.d(TAG, "强制刷新今日日程小组件，实例ID: ${todayScheduleIds.joinToString()}")
                appWidgetManager.notifyAppWidgetViewDataChanged(todayScheduleIds, android.R.id.list)
            }
            
            // 刷新紧急任务小组件
            val urgentTaskComponent = ComponentName(context, UrgentTaskWidgetReceiver::class.java)
            val urgentTaskIds = appWidgetManager.getAppWidgetIds(urgentTaskComponent)
            if (urgentTaskIds.isNotEmpty()) {
                Log.d(TAG, "强制刷新紧急任务小组件，实例ID: ${urgentTaskIds.joinToString()}")
                appWidgetManager.notifyAppWidgetViewDataChanged(urgentTaskIds, android.R.id.list)
            }

        } catch (e: Exception) {
            Log.e(TAG, "强制刷新小组件时出错", e)
        }
    }
}
