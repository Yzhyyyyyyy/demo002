package com.example.demo002

import android.app.Application
import android.content.Context
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

    /**
     * 更新所有小组件
     */
    private fun updateWidgets() {
        viewModelScope.launch {
            try {
                TodayScheduleWidget().updateAll(getApplication())
                UrgentTaskWidget().updateAll(getApplication())
            } catch (e: Exception) {
                // 小组件可能尚未初始化，忽略错误
                e.printStackTrace()
            }
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
            reminderOffset = reminderOffset
        )
        dao.upsertTaskWithSubTasks(task)
        // 设置提醒
        TaskReminderManager.scheduleReminder(getApplication(), task)
        // 更新小组件
        updateWidgets()
    }

    /**
     * 长期任务：在 [startDate, endDate] 范围内，
     * 根据重复模式生成任务，批量插入。
     */
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
                        id        = 0,
                        title     = title,
                        note      = note,
                        dueDate   = cur,
                        startTime = startTime,
                        endTime   = endTime,
                        priority  = priority,
                        tags      = tags,
                        location  = location,
                        reminderOffset = reminderOffset
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
                            id        = 0,
                            title     = title,
                            note      = note,
                            dueDate   = cur,
                            startTime = startTime,
                            endTime   = endTime,
                            priority  = priority,
                            tags      = tags,
                            location  = location,
                            reminderOffset = reminderOffset
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
                        id        = 0,
                        title     = title,
                        note      = note,
                        dueDate   = cur,
                        startTime = startTime,
                        endTime   = endTime,
                        priority  = priority,
                        tags      = tags,
                        location  = location,
                        reminderOffset = reminderOffset
                    )
                    dao.upsertTaskWithSubTasks(task)
                    TaskReminderManager.scheduleReminder(getApplication(), task)
                    cur = cur.plusMonths(1)
                }
            }
            TaskRepeatMode.YEARLY -> {
                while (!cur.isAfter(endDate)) {
                    val task = Task(
                        id        = 0,
                        title     = title,
                        note      = note,
                        dueDate   = cur,
                        startTime = startTime,
                        endTime   = endTime,
                        priority  = priority,
                        tags      = tags,
                        location  = location,
                        reminderOffset = reminderOffset
                    )
                    dao.upsertTaskWithSubTasks(task)
                    TaskReminderManager.scheduleReminder(getApplication(), task)
                    cur = cur.plusYears(1)
                }
            }
        }
        // 更新小组件
        updateWidgets()
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(task)
        // 更新提醒
        TaskReminderManager.scheduleReminder(getApplication(), task)
        // 更新小组件
        updateWidgets()
    }

    fun deleteTask(taskId: Int) = viewModelScope.launch {
        // 先取消提醒
        TaskReminderManager.cancelReminder(getApplication(), taskId)
        // 然后删除任务
        dao.deleteTaskById(taskId)
        // 更新小组件
        updateWidgets()
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
        // 更新小组件
        updateWidgets()
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

    // ══════════════════════════════════════════════
    //  四象限分类（艾森豪威尔矩阵）
    // ══════════════════════════════════════════════

    data class QuadrantData(
        val q1: List<Task>,
        val q2: List<Task>,
        val q3: List<Task>,
        val q4: List<Task>
    )

    val quadrantData: StateFlow<QuadrantData> = tasks
        .map { taskList ->
            val now = LocalDate.now()
            val tomorrow = now.plusDays(1)

            val q1 = mutableListOf<Task>()
            val q2 = mutableListOf<Task>()
            val q3 = mutableListOf<Task>()
            val q4 = mutableListOf<Task>()

            for (task in taskList) {
                // 只处理未完成的任务
                if (task.isDone) continue

                val isImportant = task.priority == Priority.HIGH
                val isUrgent = task.dueDate != null && !task.dueDate.isAfter(tomorrow)

                when {
                    isImportant && isUrgent -> q1.add(task)
                    isImportant && !isUrgent -> q2.add(task)
                    !isImportant && isUrgent -> q3.add(task)
                    !isImportant && !isUrgent -> q4.add(task)
                }
            }

            QuadrantData(q1, q2, q3, q4)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = QuadrantData(emptyList(), emptyList(), emptyList(), emptyList())
        )
}