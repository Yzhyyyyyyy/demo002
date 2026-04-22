package com.example.demo002

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).taskDao()

    // 同步仓库
    private val syncRepository = SyncRepository(app, dao)

    // 同步状态
    private val _syncStatus = MutableStateFlow(SyncStatus(
        isSyncing = false, pendingCount = 0, conflictCount = 0,
        lastSyncTime = 0L, isLoggedIn = LeanCloudManager.isLoggedIn()
    ))
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

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

    private fun updateWidgets() {
        viewModelScope.launch {
            try {
                TodayScheduleWidget().updateAll(getApplication())
                UrgentTaskWidget().updateAll(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun refreshSyncStatus() {
        viewModelScope.launch {
            _syncStatus.value = syncRepository.getSyncStatus()
        }
    }

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
            reminderOffset = reminderOffset,
            syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertTaskWithSubTasks(task)
        TaskReminderManager.scheduleReminder(getApplication(), task)
        updateWidgets()
        refreshSyncStatus()

        // 通过 SyncRepository 同步到云端
        if (LeanCloudManager.isLoggedIn()) {
            syncRepository.triggerSync()
            refreshSyncStatus()
        }
    }

    fun addRecurringTasks(
        title    : String,
        note     : String,
        startDate: LocalDate,
        endDate  : LocalDate,
        repeatMode: TaskRepeatMode,
        weekDays : Set<Int>,
        priority : Priority,
        tags     : List<TaskTag>,
        startTime: java.time.LocalTime?,
        endTime  : java.time.LocalTime?,
        location : String = "",
        reminderOffset: Int? = null
    ) = viewModelScope.launch {
        var cur = startDate

        when (repeatMode) {
            TaskRepeatMode.DAILY -> {
                while (!cur.isAfter(endDate)) {
                    val task = Task(
                        id = 0, title = title, note = note, dueDate = cur,
                        startTime = startTime, endTime = endTime, priority = priority,
                        tags = tags, location = location, reminderOffset = reminderOffset,
                        syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.upsertTaskWithSubTasks(task)
                    TaskReminderManager.scheduleReminder(getApplication(), task)
                    cur = cur.plusDays(1)
                }
            }
            TaskRepeatMode.WEEKLY -> {
                while (!cur.isAfter(endDate)) {
                    if (cur.dayOfWeek.value in weekDays) {
                        val task = Task(
                            id = 0, title = title, note = note, dueDate = cur,
                            startTime = startTime, endTime = endTime, priority = priority,
                            tags = tags, location = location, reminderOffset = reminderOffset,
                            syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.upsertTaskWithSubTasks(task)
                        TaskReminderManager.scheduleReminder(getApplication(), task)
                    }
                    cur = cur.plusDays(1)
                }
            }
            TaskRepeatMode.MONTHLY -> {
                while (!cur.isAfter(endDate)) {
                    val task = Task(
                        id = 0, title = title, note = note, dueDate = cur,
                        startTime = startTime, endTime = endTime, priority = priority,
                        tags = tags, location = location, reminderOffset = reminderOffset,
                        syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.upsertTaskWithSubTasks(task)
                    TaskReminderManager.scheduleReminder(getApplication(), task)
                    cur = cur.plusMonths(1)
                }
            }
            TaskRepeatMode.YEARLY -> {
                while (!cur.isAfter(endDate)) {
                    val task = Task(
                        id = 0, title = title, note = note, dueDate = cur,
                        startTime = startTime, endTime = endTime, priority = priority,
                        tags = tags, location = location, reminderOffset = reminderOffset,
                        syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.upsertTaskWithSubTasks(task)
                    TaskReminderManager.scheduleReminder(getApplication(), task)
                    cur = cur.plusYears(1)
                }
            }
        }
        updateWidgets()
        refreshSyncStatus()

        if (LeanCloudManager.isLoggedIn()) {
            syncRepository.triggerSync()
            refreshSyncStatus()
        }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        val taskWithSync = task.copy(
            syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertTaskWithSubTasks(taskWithSync)
        TaskReminderManager.scheduleReminder(getApplication(), task)
        updateWidgets()
        refreshSyncStatus()

        if (LeanCloudManager.isLoggedIn()) {
            syncRepository.triggerSync()
            refreshSyncStatus()
        }
    }

    fun deleteTask(taskId: Int) = viewModelScope.launch {
        TaskReminderManager.cancelReminder(getApplication(), taskId)
        // 软删除：标记 deleted=true + syncStatus=pending
        syncRepository.deleteTask(taskId)
        updateWidgets()
        refreshSyncStatus()
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        val updatedTask = task.copy(
            isDone = !task.isDone,
            syncStatus = if (LeanCloudManager.isLoggedIn()) "pending" else "synced",
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertTaskWithSubTasks(updatedTask)
        if (updatedTask.isDone) {
            TaskReminderManager.cancelReminder(getApplication(), task.id)
        } else if (updatedTask.reminderOffset != null) {
            TaskReminderManager.scheduleReminder(getApplication(), updatedTask)
        }
        updateWidgets()
        refreshSyncStatus()

        if (LeanCloudManager.isLoggedIn()) {
            syncRepository.triggerSync()
            refreshSyncStatus()
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
    //  规律日程
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

    // ══════════════════════════════════════════════
    //  同步
    // ══════════════════════════════════════════════

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.triggerSync()
            refreshSyncStatus()
        }
    }

    /**
     * 登录成功后触发首次同步
     */
    fun onLoginSuccess() {
        syncRepository.startAutoSync()
        refreshSyncStatus()
    }
}
