package com.example.demo002

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).taskDao()

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
        title   : String,
        note    : String,
        dueDate : LocalDate?,
        priority: Priority,
        tags    : List<TaskTag>
    ) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(
            Task(
                id       = 0,
                title    = title,
                note     = note,
                dueDate  = dueDate,
                priority = priority,
                tags     = tags
            )
        )
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
        tags     : List<TaskTag>
    ) = viewModelScope.launch {
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            if (cursor.dayOfWeek.value in weekDays) {
                dao.upsertTaskWithSubTasks(
                    Task(
                        id       = 0,
                        title    = title,
                        note     = note,
                        dueDate  = cursor,
                        priority = priority,
                        tags     = tags
                    )
                )
            }
            cursor = cursor.plusDays(1)
        }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(task)
    }

    fun deleteTask(taskId: Int) = viewModelScope.launch {
        dao.deleteTaskById(taskId)
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        dao.upsertTaskWithSubTasks(task.copy(isDone = !task.isDone))
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