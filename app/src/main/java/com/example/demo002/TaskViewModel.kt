package com.example.demo002

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).taskDao()

    // 保存日程页选中的日期，导航返回后不丢失 
    var selectedDate by mutableStateOf(LocalDate.now()) 
        private set 
    var weekStart by mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) 
        private set 
    
    fun selectDate(date: LocalDate) { 
        selectedDate = date 
        weekStart = date.with(DayOfWeek.MONDAY) 
    } 
    
    fun changeWeek(delta: Int) { 
        weekStart = weekStart.plusWeeks(delta.toLong()) 
    } 
    
    fun jumpToDate(date: LocalDate) { 
        selectedDate = date 
        weekStart = date.with(DayOfWeek.MONDAY) 
    } 
    
    fun jumpToToday() { 
        val today = LocalDate.now() 
        selectedDate = today 
        weekStart = today.with(DayOfWeek.MONDAY) 
    }

    // ══════════════════════════════════════════════
    //  普通任务
    // ══════════════════════════════════════════════

    val tasks: StateFlow<List<Task>> = dao.getAllTasksWithSubTasks()
        .map { list: List<TaskWithSubTasks> -> list.map { it.toTask() } }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(
        title    : String,
        note     : String,
        dueDate  : LocalDate?,
        startTime: java.time.LocalTime?,
        endTime  : java.time.LocalTime?,
        priority : Priority,
        tags     : List<TaskTag>,
        location : String = "",
        reminderOffset: Int? = null
    ) = viewModelScope.launch {
        val task = Task(
            id        = 0,
            title     = title,
            note      = note,
            dueDate   = dueDate,
            startTime = startTime,
            endTime   = endTime,
            priority  = priority,
            tags      = tags,
            location  = location,
            reminderOffset = reminderOffset
        )
        dao.upsertTaskWithSubTasks(task)
        // 设置提醒
        TaskReminderManager.scheduleReminder(getApplication(), task)
    }

    /**
     * 长期任务：在 [startDate, endDate] 范围内，
     * 找出所有符合 weekDays（1=周一…7=周日）的日期，批量插入。
     */
    fun addRecurringTasks(
        title    : String,
        note     : String,
        startDate: LocalDate,
        endDate  : LocalDate,
        weekDays : Set<Int>,
        priority : Priority,
        tags     : List<TaskTag>,
        startTime: java.time.LocalTime?,
        endTime  : java.time.LocalTime?,
        location : String = "",
        reminderOffset: Int? = null
    ) = viewModelScope.launch {
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            if (cursor.dayOfWeek.value in weekDays) {
                val task = Task(
                    id        = 0,
                    title     = title,
                    note      = note,
                    dueDate   = cursor,
                    startTime = startTime,
                    endTime   = endTime,
                    priority  = priority,
                    tags      = tags,
                    location  = location,
                    reminderOffset = reminderOffset
                )
                dao.upsertTaskWithSubTasks(task)
                // 为每个重复任务设置提醒
                TaskReminderManager.scheduleReminder(getApplication(), task)
            }
            cursor = cursor.plusDays(1)
        }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(task)
        // 更新提醒
        TaskReminderManager.scheduleReminder(getApplication(), task)
    }

    fun deleteTask(taskId: Int) = viewModelScope.launch {
        // 先取消提醒
        TaskReminderManager.cancelReminder(getApplication(), taskId)
        // 然后删除任务
        dao.deleteTaskById(taskId)
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(isDone = !task.isDone)
        dao.upsertTaskWithSubTasks(updatedTask)
        // 如果任务标记为完成，取消提醒；如果标记为未完成，重新设置提醒
        if (updatedTask.isDone) {
            TaskReminderManager.cancelReminder(getApplication(), task.id)
        } else if (updatedTask.reminderOffset != null) {
            TaskReminderManager.scheduleReminder(getApplication(), updatedTask)
        }
    }

    // ══════════════════════════════════════════════
    //  搜索
    // ══════════════════════════════════════════════

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Task>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query: String ->
            if (query.isBlank()) {
                flowOf<List<Task>>(emptyList())
            } else {
                dao.searchTasks(query).map<List<TaskWithSubTasks>, List<Task>> { list ->
                    list.map { it.toTask() }
                }
            }
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // ══════════════════════════════════════════════
    //  规律日程（内存存储，可后续接 Room）
    // ══════════════════════════════════════════════

    private val _recurringSchedules = MutableStateFlow<List<RecurringSchedule>>(emptyList())
    val recurringSchedules: StateFlow<List<RecurringSchedule>> = _recurringSchedules.asStateFlow()

    private var nextRecurringId: Int = 1

    fun addRecurringSchedule(schedule: RecurringSchedule) {
        val withId = schedule.copy(id = nextRecurringId++)
        _recurringSchedules.update { current -> current + withId }
    }

    fun updateRecurringSchedule(schedule: RecurringSchedule) {
        _recurringSchedules.update { current ->
            current.map { if (it.id == schedule.id) schedule else it }
        }
    }

    fun deleteRecurringSchedule(id: Int) {
        _recurringSchedules.update { current -> current.filter { it.id != id } }
    }
}