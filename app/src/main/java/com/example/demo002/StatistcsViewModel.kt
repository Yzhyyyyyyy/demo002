package com.example.demo002

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import java.time.LocalDate

// ══════════════════════════════════════════════
//  统计数据模型
// ══════════════════════════════════════════════

data class DailyStats(
    val date : LocalDate,
    val total: Int,
    val done : Int,
    val rate : Float = if (total > 0) done.toFloat() / total else 0f
)

data class TagStats(
    val tag  : TaskTag,
    val count: Int
)

data class StatisticsUiState(
    val todayTotal    : Int              = 0,
    val todayDone     : Int              = 0,
    val todayRate     : Float            = 0f,
    val weekTotal     : Int              = 0,
    val weekDone      : Int              = 0,
    val weekRate      : Float            = 0f,
    val monthTotal    : Int              = 0,
    val monthDone     : Int              = 0,
    val monthRate     : Float            = 0f,
    val streakDays    : Int              = 0,
    val last14Days    : List<DailyStats> = emptyList(),
    val priorityStats : Map<Priority, Int> = emptyMap(),
    val tagStats      : List<TagStats>   = emptyList(),
    val totalCreated  : Int              = 0,
    val totalCompleted: Int              = 0
)

// ══════════════════════════════════════════════
//  ViewModel —— 与 TaskViewModel 完全相同的注入方式
// ══════════════════════════════════════════════

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {

    // 直接复用同一个 dao，不需要 Repository
    private val dao = AppDatabase.getInstance(app).taskDao()

    val uiState: StateFlow<StatisticsUiState> =
        dao.getAllTasksWithSubTasks()
            .map { list -> list.map { it.toTask() } }   // 复用已有的 toTask()
            .map { tasks -> computeStats(tasks) }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = StatisticsUiState()
            )

    // ══════════════════════════════════════════════
    //  统计计算
    // ══════════════════════════════════════════════

    private fun computeStats(tasks: List<Task>): StatisticsUiState {
        val today      = LocalDate.now()
        val weekStart  = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val monthStart = today.withDayOfMonth(1)

        // ── 今日 ──
        val todayTasks = tasks.filter { it.dueDate == today }
        val todayTotal = todayTasks.size
        val todayDone  = todayTasks.count { it.isDone }

        // ── 本周 ──
        val weekTasks = tasks.filter {
            it.dueDate != null
                    && !it.dueDate.isBefore(weekStart)
                    && !it.dueDate.isAfter(today)
        }
        val weekTotal = weekTasks.size
        val weekDone  = weekTasks.count { it.isDone }

        // ── 本月 ──
        val monthTasks = tasks.filter {
            it.dueDate != null
                    && !it.dueDate.isBefore(monthStart)
                    && !it.dueDate.isAfter(today)
        }
        val monthTotal = monthTasks.size
        val monthDone  = monthTasks.count { it.isDone }

        // ── 近14天每日数据 ──
        val last14Days = (13 downTo 0).map { offset ->
            val date     = today.minusDays(offset.toLong())
            val dayTasks = tasks.filter { it.dueDate == date }
            DailyStats(
                date  = date,
                total = dayTasks.size,
                done  = dayTasks.count { it.isDone }
            )
        }

        // ── 连续完成天数（streak）──
        // 规则：从昨天往前，有任务且全部完成则 +1，无任务则跳过，有未完成则停止
        var streak    = 0
        var checkDate = today.minusDays(1)
        repeat(90) {                              // 最多往前查90天
            val dayTasks = tasks.filter { it.dueDate == checkDate }
            when {
                dayTasks.isEmpty()         -> { /* 跳过无任务的天 */ }
                dayTasks.all { it.isDone } -> streak++
                else                       -> return@repeat   // 遇到未完成，停止
            }
            checkDate = checkDate.minusDays(1)
        }

        // ── 优先级分布（只统计有 dueDate 的任务）──
        val priorityStats = tasks
            .filter { it.dueDate != null }
            .groupBy { it.priority }
            .mapValues { it.value.size }

        // ── 标签使用排行（Top 6）──
        val tagStats = tasks
            .filter { it.dueDate != null }
            .flatMap { it.tags }
            .groupBy { it.label }
            .map { (_, tagList) ->
                TagStats(tag = tagList.first(), count = tagList.size)
            }
            .sortedByDescending { it.count }
            .take(6)

        return StatisticsUiState(
            todayTotal     = todayTotal,
            todayDone      = todayDone,
            todayRate      = if (todayTotal > 0) todayDone.toFloat() / todayTotal else 0f,
            weekTotal      = weekTotal,
            weekDone       = weekDone,
            weekRate       = if (weekTotal > 0) weekDone.toFloat() / weekTotal else 0f,
            monthTotal     = monthTotal,
            monthDone      = monthDone,
            monthRate      = if (monthTotal > 0) monthDone.toFloat() / monthTotal else 0f,
            streakDays     = streak,
            last14Days     = last14Days,
            priorityStats  = priorityStats,
            tagStats       = tagStats,
            totalCreated   = tasks.filter { it.dueDate != null }.size,
            totalCompleted = tasks.count { it.isDone }
        )
    }
}