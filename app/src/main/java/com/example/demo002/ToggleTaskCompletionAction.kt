package com.example.demo002

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.first

class ToggleTaskCompletionAction : ActionCallback {

    companion object {
        val taskIdKey = ActionParameters.Key<Int>("task_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return

        try {
            val dao = AppDatabase.getInstance(context).taskDao()
            // 查找任务
            val tasks = dao.getAllTasksWithSubTasks().first()
            val taskEntity = tasks.find { it.task.id == taskId }?.task ?: return

            // 切换状态
            val updatedTask = taskEntity.copy(isDone = !taskEntity.isDone)

            // 因为 dao.upsertTaskWithSubTasks 需要 Task 对象，我们需要转换一下
            // 为了简单起见，我们直接更新 Entity
            dao.upsertTask(updatedTask)

            // 刷新所有保留的小组件
            TodayScheduleWidget().updateAll(context)
            UrgentTaskWidget().updateAll(context)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
