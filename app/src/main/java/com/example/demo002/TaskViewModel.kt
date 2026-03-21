package com.example.demo002

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).taskDao()

    // ── 所有任务，Flow 自动更新 UI ──
    val tasks: StateFlow<List<Task>> = dao.getAllTasksWithSubTasks()
        .map { list -> list.map { it.toTask() } }
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5000),
            initialValue  = emptyList()
        )

    // ── 新增任务 ──
    fun addTask(
        title   : String,
        note    : String,
        dueDate : LocalDate?,
        priority: Priority,
        tags    : List<TaskTag>
    ) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(
            Task(
                id       = 0,   // autoGenerate
                title    = title,
                note     = note,
                dueDate  = dueDate,
                priority = priority,
                tags     = tags
            )
        )
    }

    // ── 更新任务（含子任务）──
    fun updateTask(task: Task) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(task)
    }

    // ── 删除任务 ──
    fun deleteTask(taskId: Int) = viewModelScope.launch {
        dao.deleteTaskById(taskId)
    }

    // ── 标记完成 / 取消完成 ──
    fun toggleDone(task: Task) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(task.copy(isDone = !task.isDone))
    }
}