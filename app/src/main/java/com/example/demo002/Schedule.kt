package com.example.demo002

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ══════════════════════════════════════════════
//  数据模型
// ══════════════════════════════════════════════

data class SubTask(
    val id     : Int,
    val title  : String,
    val isDone : Boolean    = false,
    val dueDate: LocalDate? = null
)

enum class Priority(val label: String, val color: Color, val order: Int) {
    HIGH  ("重要", Color(0xFFFF6B6B), 0),
    MEDIUM("普通", Color(0xFFFFB347), 1),
    LOW   ("次要", Color(0xFF7DD3FC), 2)
}

enum class TaskRepeatMode(val label: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月"),
    YEARLY("每年")
}

data class TaskTag(val label: String, val color: Color)

val PRESET_TAGS = listOf(
    TaskTag("学习", Color(0xFF818CF8)),
    TaskTag("编程", Color(0xFF34D399)),
    TaskTag("专业", Color(0xFFFB7185)),
    TaskTag("社团", Color(0xFFF59E0B)),
    TaskTag("家庭", Color(0xFF60A5FA)),
    TaskTag("朋友", Color(0xFFA78BFA)),
    TaskTag("生活", Color(0xFF2DD4BF)),
    TaskTag("锻炼", Color(0xFFEC4899)),
    TaskTag("习惯", Color(0xFF6EE7B7)),
    TaskTag("其他", Color(0xFF94A3B8))
)

data class Task(
    val id       : Int,
    val title    : String,
    val note     : String        = "",
    val dueDate  : LocalDate?    = null,
    val startTime: java.time.LocalTime? = null,
    val endTime  : java.time.LocalTime? = null,
    val priority : Priority      = Priority.MEDIUM,
    val isDone   : Boolean       = false,
    val tags     : List<TaskTag> = emptyList(),
    // val subTasks : List<SubTask> = emptyList(), // 暂时屏蔽子任务功能
    val location : String        = "",
    val reminderOffset: Int?     = null  // 提醒提前分钟数，null表示不提醒，0表示准时
)

// ══════════════════════════════════════════════
//  主界面
// ══════════════════════════════════════════════

@Composable
fun Schedule(
    onNavigateToSearch  : () -> Unit    = {},
    onNavigateToSettings: () -> Unit    = {},
    onNavigateToDetail  : (Int) -> Unit = {},
    onNavigateToQuadrant: () -> Unit    = {}, // 新增：导航到四象限视图
    initialShowAddDialog: Boolean       = false
) {
    val viewModel          : TaskViewModel = viewModel()
    val tasks              by viewModel.tasks.collectAsState()
    val recurringSchedules by viewModel.recurringSchedules.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask   by remember { mutableStateOf<Task?>(null) }
    var selectedDate  by remember { mutableStateOf(LocalDate.now()) }
    var weekStart     by remember { mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) }

    // 如果initialShowAddDialog为true，则显示添加对话框
    LaunchedEffect(initialShowAddDialog) {
        if (initialShowAddDialog) {
            showAddDialog = true
        }
    }
    var isMonthView   by remember { mutableStateOf(false) } // 月视图切换状态

    // ── 引导层状态 ──
    val context = LocalContext.current
    val onboardingPrefs = remember {
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    }
    var showOnboarding by remember {
        mutableStateOf(!onboardingPrefs.getBoolean("onboarding_shown", false))
    }

    val todayDayOfWeek = selectedDate.dayOfWeek.value

    val recurringAsTasks = recurringSchedules
        .filter { it.isEnabled && todayDayOfWeek in it.weekDays }
        .map { s ->
            Task(
                id        = -s.id,
                title     = s.title,
                note      = s.note,
                dueDate   = selectedDate,
                startTime = null,
                endTime   = null,
                priority  = s.priority,
                isDone    = false,
                tags      = s.tags
                // subTasks  = s.subTasks // 暂时屏蔽子任务功能
            )
        }

    val filteredTasks = (tasks.filter { it.dueDate == selectedDate } + recurringAsTasks)
        .sortedBy { it.priority.order }
    val pendingTasks  = filteredTasks.filter { !it.isDone }
    val doneTasks     = filteredTasks.filter { it.isDone }

    Box(modifier = Modifier.fillMaxSize()) {
        MyFirstScreen()
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                pendingCount = pendingTasks.size,
                onSearch     = onNavigateToSearch,
                onSettings   = onNavigateToSettings,
                onToggleMonthView = { isMonthView = !isMonthView }, // 切换月视图状态
                onNavigateToQuadrant = onNavigateToQuadrant // 新增：导航到四象限视图
            )
            if (isMonthView) {
                // 月视图
                MonthView(
                    selectedDate = selectedDate,
                    tasks = tasks,
                    onDateSelected = { date ->
                        selectedDate = date
                        // 关键修复：同步更新周视图的起始日期为选中日期所在周的周一
                        weekStart = date.with(java.time.DayOfWeek.MONDAY)
                        isMonthView = false // 点击日期后切回周视图
                    },
                    onMonthChange = { delta ->
                        // 月份切换逻辑
                        val newMonth = YearMonth.from(selectedDate).plusMonths(delta.toLong())
                        selectedDate = newMonth.atDay(minOf(selectedDate.dayOfMonth, newMonth.lengthOfMonth()))
                    }
                )
            } else {
                // 周视图
                WeekDateSelector(
                    weekStart      = weekStart,
                    selectedDate   = selectedDate,
                    onDateSelected = { selectedDate = it },
                    onWeekChange   = { weekStart = weekStart.plusWeeks(it.toLong()) },
                    onJumpToDate   = { date ->
                        selectedDate = date
                        weekStart    = date.with(DayOfWeek.MONDAY)
                    }
                )
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding      = PaddingValues(bottom = 100.dp, top = 8.dp)
                ) {
                    if (pendingTasks.isNotEmpty()) {
                        item { SectionHeader("待办", pendingTasks.size, Color(0xFF1C1C1E)) }
                        items(pendingTasks, key = { it.id }) { task ->
                            val isRecurring = task.id < 0
                            SwipeableTaskCard(
                                task        = task,
                                isRecurring = isRecurring,
                                onComplete  = {
                                    if (!isRecurring) viewModel.updateTask(task.copy(isDone = true))
                                },
                                onDelete    = {
                                    if (!isRecurring) viewModel.deleteTask(task.id)
                                },
                                onEdit      = {
                                    if (!isRecurring) editingTask = task
                                },
                                onTap       = {
                                    if (!isRecurring) onNavigateToDetail(task.id)
                                }
                            )
                        }
                    }
                    if (doneTasks.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            SectionHeader("已完成", doneTasks.size, Color(0xFF94A3B8))
                        }
                        items(doneTasks, key = { it.id }) { task ->
                            SwipeableTaskCard(
                                task        = task,
                                isRecurring = false,
                                onComplete  = { viewModel.updateTask(task.copy(isDone = false)) },
                                onDelete    = { viewModel.deleteTask(task.id) },
                                onEdit      = { editingTask = task },
                                onTap       = { onNavigateToDetail(task.id) }
                            )
                        }
                    }
                    if (filteredTasks.isEmpty()) { item { EmptyState() } }
                }
            }
        }

        FloatingActionButton(
            onClick        = { showAddDialog = true },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF1C1C1E),
            contentColor   = Color.White,
            shape          = CircleShape
        ) { Icon(Icons.Rounded.Add, "添加任务") }

        // ── 引导覆盖层（放在最顶层）──
        if (showOnboarding) {
            OnboardingOverlay(
                onFinished = {
                    onboardingPrefs.edit().putBoolean("onboarding_shown", true).apply()
                    showOnboarding = false
                }
            )
        }
    }

    if (showAddDialog || editingTask != null) {
        TaskDialog(
            task        = editingTask,
            initialDate = selectedDate,
            onDismiss   = { showAddDialog = false; editingTask = null },
            onConfirm   = { title, note, date, startTime, endTime, priority, tags, repeatMode, weekDays, endDate, location, reminderOffset ->
                if (editingTask != null) {
                    viewModel.updateTask(
                        editingTask!!.copy(
                            title     = title,
                            note      = note,
                            dueDate   = date,
                            startTime = startTime,
                            endTime   = endTime,
                            priority  = priority,
                            tags      = tags,
                            location  = location,
                            reminderOffset = reminderOffset
                        )
                    )
                } else if (endDate != null && date != null) {
                    viewModel.addRecurringTasks(
                        title     = title,
                        note      = note,
                        startDate = date,
                        endDate   = endDate,
                        repeatMode = repeatMode,
                        weekDays  = weekDays,
                        priority  = priority,
                        tags      = tags,
                        startTime = startTime,
                        endTime   = endTime,
                        location  = location,
                        reminderOffset = reminderOffset
                    )
                } else {
                    viewModel.addTask(title, note, date, startTime, endTime, priority, tags, location, reminderOffset)
                }
                showAddDialog = false
                editingTask   = null
            }
        )
    }
}

// ══════════════════════════════════════════════
//  顶部栏
// ══════════════════════════════════════════════

@Composable
fun TopBar(
    pendingCount: Int,
    onSearch   : () -> Unit = {},
    onSettings : () -> Unit = {},
    onToggleMonthView: () -> Unit = {}, // 月视图切换回调
    onNavigateToQuadrant: () -> Unit = {} // 新增：导航到四象限视图
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Schedule",
                fontSize      = 30.sp,
                fontWeight    = FontWeight.Black,
                color         = Color(0xFF1C1C1E),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (pendingCount > 0) "还有 $pendingCount 件事待完成" else "今天全部完成啦 🎉",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = if (pendingCount > 0) Color(0xFF94A3B8) else Color(0xFF7DD3FC)
            )
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 按照要求调整顺序：月视图 -> 四象限 -> 搜索 -> 设置
            val actions: List<Pair<androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>> = listOf(
                Icons.Rounded.DateRange to onToggleMonthView,
                Icons.Rounded.Star      to onNavigateToQuadrant,
                Icons.Rounded.Search    to onSearch,
                Icons.Rounded.Settings  to onSettings
            )

            actions.forEach { (icon, action) ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            3.dp, RoundedCornerShape(13.dp),
                            ambientColor = Color(0xFF94A3B8).copy(alpha = 0.12f),
                            spotColor    = Color(0xFF94A3B8).copy(alpha = 0.08f)
                        )
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            action()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint     = Color(0xFF475569),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  周视图日期选择器
// ══════════════════════════════════════════════

@Composable
fun WeekDateSelector(
    weekStart     : LocalDate,
    selectedDate  : LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onWeekChange  : (Int) -> Unit,
    onJumpToDate  : (LocalDate) -> Unit
) {
    val viewModel: TaskViewModel = viewModel()
    val tasks by viewModel.tasks.collectAsState()

    val today      = LocalDate.now()
    val cnDays     = listOf("一", "二", "三", "四", "五", "六", "日")
    val weekDates  = (0..6).map { weekStart.plusDays(it.toLong()) }
    val monthLabel = weekStart.plusDays(3)
        .format(DateTimeFormatter.ofPattern("yyyy年M月"))

    // 计算每个日期是否有任务
    val dateHasTasks = remember(weekDates, tasks) {
        weekDates.associateWith { date ->
            tasks.any { it.dueDate == date }
        }
    }

    var showCalendar by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // 判断是否需要显示"今天"按钮
    val showTodayButton = selectedDate != today

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 顶部控制栏 - 重新设计
        Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：周导航
    Row(
        verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
    ) {
            IconButton(
                    onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWeekChange(-1)
                },
                    modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null,
                        tint     = Color(0xFF1C1C1E),
                        modifier = Modifier.size(22.dp)
                )
            }

                Spacer(Modifier.width(8.dp))

                // 月份标签，点击可跳转到日历
                Box(
                            modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCalendar = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            monthLabel,
                            fontSize      = 15.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = Color(0xFF1C1C1E),
                            letterSpacing = 0.5.sp
                            )
                        Icon(
                            Icons.Rounded.DateRange, null,
                            tint     = Color(0xFF7DD3FC),
                            modifier = Modifier.size(16.dp)
                        )
                }
            }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWeekChange(1)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                        tint     = Color(0xFF1C1C1E),
                        modifier = Modifier.size(22.dp)
                    )
        }
    }

            // 右侧：今天按钮（仅在非今天时显示）
            AnimatedVisibility(
                visible = showTodayButton,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onJumpToDate(today)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color(0xFF7DD3FC).copy(alpha = 0.12f),
                            contentColor = Color(0xFF1C1C1E)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Star, null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "今天",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
}
                    }
                }
            }
        }

        // 周日期网格 - 优化设计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(
                    width = 1.dp,
                    color = Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDates.forEachIndexed { idx, date ->
                val isSelected = date == selectedDate
                val isToday    = date == today
                val hasTasks   = dateHasTasks[date] == true

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> Color(0xFF1C1C1E)
                                isToday    -> Color(0xFF7DD3FC).copy(alpha = 0.15f)
                                else       -> Color.Transparent
                            }
                        )
                        .clickable {
                            if (date != selectedDate) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onDateSelected(date)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    // 星期几
                    Text(
                        cnDays[idx],
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = if (isSelected) Color.White.copy(alpha = 0.9f)
                                       else Color(0xFF64748B),
                        letterSpacing = 0.3.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    // 日期数字
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = when {
                                isSelected -> Color.White
                                isToday    -> Color(0xFF3B82F6)
                                else       -> Color(0xFF1C1C1E)
                            }
                        )

                        // 任务标记点
                        if (hasTasks && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isToday) Color(0xFF3B82F6)
                                        else Color(0xFFFB7185)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // 底部指示器
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                when {
                                    isSelected -> Color.White
                                    isToday    -> Color(0xFF3B82F6)
                                    hasTasks   -> Color(0xFFFB7185).copy(alpha = 0.7f)
                                    else       -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }

    }

    if (showCalendar) {
        JumpToDateDialog(
            initialDate = selectedDate,
            onDismiss   = { showCalendar = false },
            onConfirm   = { date ->
                onJumpToDate(date)
                showCalendar = false
            }
        )
    }
}

// ══════════════════════════════════════════════
//  跳转日期弹窗
// ══════════════════════════════════════════════

@Composable
fun JumpToDateDialog(
    initialDate: LocalDate,
    onDismiss  : () -> Unit,
    onConfirm  : (LocalDate) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    Dialog(onDismissRequest = onDismiss) {
        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Text(
                "跳转到日期",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                color      = Color(0xFF1C1C1E)
            )
            Spacer(Modifier.height(16.dp))
            MiniCalendar(
                currentMonth   = currentMonth,
                selectedDate   = selectedDate,
                onDateSelected = { selectedDate = it },
                onMonthChange  = { currentMonth = currentMonth.plusMonths(it.toLong()) }
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) { Text("取消", color = Color(0xFF94A3B8)) }
                Button(
                    onClick  = { onConfirm(selectedDate) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
                ) { Text("跳转", color = Color.White) }
}
        }
    }
}

// ══════════════════════════════════════════════
//  分区标题
// ══════════════════════════════════════════════

@Composable
fun SectionHeader(title: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text(
            title,
            fontSize      = 13.sp,
            fontWeight    = FontWeight.Bold,
            color         = color,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                "$count",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

// ══════════════════════════════════════════════
//  可双向滑动的任务卡片
// ══════════════════════════════════════════════

@Composable
fun SwipeableTaskCard(
    task        : Task,
    onComplete  : () -> Unit,
    onDelete    : () -> Unit,
    onEdit      : () -> Unit,
    onTap       : () -> Unit = {},
    isRecurring : Boolean    = false
) {
    var offsetX by remember(task.id) { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue   = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "swipe"
    )
    val completeThreshold = 180f
    val deleteThreshold   = -180f
    val completeAlpha     = (offsetX / completeThreshold).coerceIn(0f, 1f)
    val deleteAlpha       = ((-offsetX) / (-deleteThreshold)).coerceIn(0f, 1f)

    val haptic = LocalHapticFeedback.current
    var hasTriggeredCompleteHaptic by remember(task.id) { mutableStateOf(false) }
    var hasTriggeredDeleteHaptic   by remember(task.id) { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember(task.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF34D399).copy(alpha = completeAlpha)),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector        = if (task.isDone) Icons.Rounded.Refresh else Icons.Rounded.Check,
                contentDescription = "完成",
                tint               = Color.White.copy(alpha = completeAlpha),
                modifier           = Modifier.padding(start = 20.dp)
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFF6B6B).copy(alpha = deleteAlpha)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector        = Icons.Rounded.Delete,
                contentDescription = "删除",
                tint               = Color.White.copy(alpha = deleteAlpha),
                modifier           = Modifier.padding(end = 20.dp)
            )
        }
        if (isRecurring) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7DD3FC).copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh, "规律日程",
                    tint     = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        TaskCard(
            task     = task,
            onEdit   = onEdit,
            onTap    = onTap,
            modifier = Modifier
                .graphicsLayer { translationX = animatedOffset }
                .pointerInput(task.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredCompleteHaptic = false
                            hasTriggeredDeleteHaptic   = false
                        },
                        onDragEnd = {
                            when {
                                offsetX > completeThreshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onComplete()
                                    offsetX = 0f
                                }
                                offsetX < deleteThreshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDeleteConfirmDialog = true
                                }
                                else -> offsetX = 0f
                            }
                            hasTriggeredCompleteHaptic = false
                            hasTriggeredDeleteHaptic   = false
                        },
                        onHorizontalDrag = { _: PointerInputChange, delta: Float ->
                            offsetX = (offsetX + delta)
                                .coerceIn(deleteThreshold * 1.3f, completeThreshold * 1.3f)
                            if (offsetX >= completeThreshold && !hasTriggeredCompleteHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasTriggeredCompleteHaptic = true
                            } else if (offsetX < completeThreshold) {
                                hasTriggeredCompleteHaptic = false
                            }
                            if (offsetX <= deleteThreshold && !hasTriggeredDeleteHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasTriggeredDeleteHaptic = true
                            } else if (offsetX > deleteThreshold) {
                                hasTriggeredDeleteHaptic = false
                            }
                        }
                    )
                }
        )
        
        // 删除确认弹窗
        if (showDeleteConfirmDialog) {
            Dialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    offsetX = 0f // 让滑动的卡片回弹恢复原状
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "确认删除",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1C1C1E)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "确定要删除任务「${task.title}」吗？",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                offsetX = 0f // 让滑动的卡片回弹恢复原状
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text("取消", color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = false
                                offsetX = 0f // 让卡片回弹后再删除
                                onDelete()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                        ) {
                            Text("删除", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  任务卡片
// ══════════════════════════════════════════════

@Composable
fun TaskCard(
    task    : Task,
    onEdit  : () -> Unit,
    onTap   : () -> Unit = {},
    modifier: Modifier   = Modifier
) {
    val today      = LocalDate.now()
    val isOverdue  = task.dueDate != null && task.dueDate.isBefore(today) && !task.isDone
    // 暂时屏蔽子任务功能
    // val doneCount  = task.subTasks.count { it.isDone }
    // val totalCount = task.subTasks.size
    // val progress   = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                if (task.isDone) 0.dp else 4.dp,
                RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF7DD3FC).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (task.isDone) 0.5f else 0.88f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEdit()
            }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (task.isDone) Color(0xFFE2E8F0) else task.priority.color)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text           = task.title,
                    fontSize       = 15.sp,
                    fontWeight     = FontWeight.SemiBold,
                    color          = if (task.isDone) Color(0xFF94A3B8) else Color(0xFF1C1C1E),
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines       = 1,
                    overflow       = TextOverflow.Ellipsis
                )
                if (task.note.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.note,
                        fontSize = 12.sp,
                        color    = Color(0xFFB0BEC5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                
                if (task.dueDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Column {
                        // 日期行
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.DateRange, null,
                                tint     = if (isOverdue) Color(0xFFFF6B6B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = when (task.dueDate) {
                                    today              -> "今天"
                                    today.plusDays(1)  -> "明天"
                                    today.minusDays(1) -> "昨天"
                                    else -> task.dueDate.format(DateTimeFormatter.ofPattern("M月d日"))
                                },
                                fontSize   = 11.sp,
                                color      = if (isOverdue) Color(0xFFFF6B6B) else Color(0xFF94A3B8),
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isOverdue) {
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "已逾期",
                                    fontSize   = 10.sp,
                                    color      = Color(0xFFFF6B6B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // 时间行（如果有时间信息）
                        if (task.startTime != null || task.endTime != null) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Menu, null,
                                    tint     = Color(0xFF94A3B8),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = buildString {
                                        task.startTime?.let {
                                            append(it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                                        }
                                        if (task.startTime != null && task.endTime != null) {
                                            append(" - ")
                                        }
                                        task.endTime?.let {
                                            append(it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                                        }
                                    },
                                    fontSize   = 10.sp,
                                    color      = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                        
                        // 地点行（如果有地点信息）
                        if (task.location.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.LocationOn, null,
                                    tint     = Color(0xFF94A3B8),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    task.location,
                                    fontSize   = 10.sp,
                                    color      = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Normal,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                            }
                        
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(task.priority.color.copy(alpha = 0.13f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        task.priority.label,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = task.priority.color
                    )
                }
                // 暂时屏蔽子任务进度显示
                // if (totalCount > 0) {
                //     Spacer(Modifier.height(4.dp))
                //     Text(
                //         "$doneCount/$totalCount",
                //         fontSize   = 10.sp,
                //         color      = Color(0xFF94A3B8),
                //         fontWeight = FontWeight.Medium
                //     )
                // }
            }
        }

        // 暂时屏蔽子任务进度条
        // if (totalCount > 0) {
        //     Spacer(Modifier.height(10.dp))
        //     Box(
        //         modifier = Modifier
        //             .fillMaxWidth()
        //             .height(4.dp)
        //             .clip(RoundedCornerShape(2.dp))
        //             .background(Color(0xFFE2E8F0))
        //     ) {
        //         Box(
        //             modifier = Modifier
        //                 .fillMaxHeight()
        //                 .fillMaxWidth(progress)
        //                 .clip(RoundedCornerShape(2.dp))
        //                 .background(
        //                     if (progress >= 1f) Color(0xFF34D399) else task.priority.color
        //                 )
        //         )
        //     }
        // }

        if (task.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.padding(start = 16.dp)
            ) {
                task.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tag.color.copy(alpha = 0.13f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            tag.label,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = tag.color
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  空状态
// ══════════════════════════════════════════════

@Composable
fun EmptyState() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎯", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "这一天还没有任务",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF94A3B8)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "点击右下角 + 添加新任务",
            fontSize = 13.sp,
            color    = Color(0xFFB0BEC5)
        )
    }
}

// ══════════════════════════════════════════════
//  时间选择器组件
// ══════════════════════════════════════════════

@Composable
fun WheelTimePickerDialog(
    initialTime: java.time.LocalTime? = null,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalTime) -> Unit
) {
    val initialHour = initialTime?.hour ?: 12
    val initialMinute = initialTime?.minute ?: 0
    
    var hourState by remember { mutableIntStateOf(initialHour) }
    var minuteState by remember { mutableIntStateOf(initialMinute) }
    
    // 无限循环滚动初始化
    val hourItems = (0..23).map { it.toString().padStart(2, '0') }
    val minuteItems = (0..59).map { it.toString().padStart(2, '0') }
    
    // 设置中心点，让用户一开始就可以向上或向下无限滑动
    val center = Int.MAX_VALUE / 2
    val hourInitIndex = center - (center % hourItems.size) + initialHour
    val minuteInitIndex = center - (center % minuteItems.size) + initialMinute
    
    val hourListState = rememberLazyListState(
        initialFirstVisibleItemIndex = hourInitIndex
    )
    val minuteListState = rememberLazyListState(
        initialFirstVisibleItemIndex = minuteInitIndex
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "选择时间",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1C1C1E)
            )
            Spacer(Modifier.height(20.dp))
            
            // 时间选择器滚轮
            Row(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 小时滚轮
                WheelPicker(
                    items = hourItems,
                    listState = hourListState,
                    onIndexChanged = { hourState = it },
                    modifier = Modifier.weight(1f)
                )
                
                // 分隔符
                Text(
                    ":",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                // 分钟滚轮
                WheelPicker(
                    items = minuteItems,
                    listState = minuteListState,
                    onIndexChanged = { minuteState = it },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(30.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) { Text("取消", color = Color(0xFF94A3B8)) }
                Button(
                    onClick = {
                        val selectedTime = java.time.LocalTime.of(
                            hourState,
                            minuteState
                        )
                        onConfirm(selectedTime)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
                ) { Text("确定", color = Color.White) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    listState: LazyListState,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    // 核心修复：通过 firstVisibleItemIndex 和 offset 精准判断正中间的 Item
    // 因为 contentPadding 是 55dp，Item 高度是 40dp，所以吸附在顶部的 item 就是视觉中心
    val selectedIndex by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            // 如果滚动偏移超过了 item 高度的一半(20dp)，说明下一个 item 更靠近中心
            if (offset > 20) firstIndex + 1 else firstIndex
        }
    }
    
    val haptic = LocalHapticFeedback.current
    
    // 当中间的项改变时，触发震动并回调正确的真实索引
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != -1 && items.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onIndexChanged(selectedIndex % items.size)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        // 已经删除了蓝色高亮框，现在只有文字的变化作为唯一标识
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 55.dp),
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            userScrollEnabled = true
        ) {
            items(Int.MAX_VALUE) { index ->
                val realIndex = index % items.size
                val isSelected = selectedIndex == index
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[realIndex],
                        // 加粗和放大效果，只有正中间的才会变大！
                        fontSize = if (isSelected) 22.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // 渐变遮罩（让上下边缘的数字看起来有淡出效果）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC).copy(alpha = 0.95f),
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xFFF8FAFC).copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    selectedTime: java.time.LocalTime?,
    onTimeSelected: (java.time.LocalTime?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("请选择时间", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    if (selectedTime != null) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTimeSelected(null)
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = "清除",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "选择时间",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedTrailingIconColor = Color(0xFF7DD3FC),
                    unfocusedTrailingIconColor = Color(0xFF94A3B8)
                )
            )
            
            // 添加点击监听器到整个TextField区域
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showTimePicker = true
                    }
            )
        }
    }
    
    if (showTimePicker) {
        WheelTimePickerDialog(
            initialTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                onTimeSelected(time)
                showTimePicker = false
            }
        )
    }
}

// ══════════════════════════════════════════════
//  添加 / 编辑 Dialog
// ══════════════════════════════════════════════

@Composable
fun TaskDialog(
    task       : Task?,
    initialDate: LocalDate,
    onDismiss  : () -> Unit,
    onConfirm  : (
        title    : String,
        note     : String,
        date     : LocalDate?,
        startTime: java.time.LocalTime?,
        endTime  : java.time.LocalTime?,
        priority : Priority,
        tags     : List<TaskTag>,
        repeatMode: TaskRepeatMode,
        weekDays : Set<Int>,
        endDate  : LocalDate?,
        location : String,
        reminderOffset: Int?
    ) -> Unit
) {
    var title         by remember { mutableStateOf(task?.title ?: "") }
    var note          by remember { mutableStateOf(task?.note ?: "") }
    var priority      by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var dueDate       by remember { mutableStateOf(task?.dueDate ?: initialDate) }
    var startTime     by remember { mutableStateOf(task?.startTime) }
    var endTime       by remember { mutableStateOf(task?.endTime) }
    var selectedTags  by remember { mutableStateOf(task?.tags?.toSet() ?: emptySet<TaskTag>()) }
    var location      by remember { mutableStateOf(task?.location ?: "") }
    var reminderOffset by remember { mutableStateOf(task?.reminderOffset) }
    var calendarMonth by remember { mutableStateOf(YearMonth.from(dueDate)) }
    var isRecurring   by remember { mutableStateOf(false) }
    var repeatMode    by remember { mutableStateOf(TaskRepeatMode.WEEKLY) }
    var weekDays      by remember { mutableStateOf(emptySet<Int>()) }
    var endDate       by remember { mutableStateOf(dueDate.plusMonths(1)) }
    var endMonth      by remember { mutableStateOf(YearMonth.from(endDate)) }

    val cnWeekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val canConfirm = title.isNotBlank() &&
            (!isRecurring || ((repeatMode != TaskRepeatMode.WEEKLY || weekDays.isNotEmpty()) && !endDate.isBefore(dueDate)))

    val haptic = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Text(
                if (task == null) "添加任务" else "编辑任务",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black,
                color      = Color(0xFF1C1C1E)
            )
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("任务标题") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    focusedLabelColor  = Color(0xFF7DD3FC)
                )
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("备注（可选）") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                maxLines      = 2,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    focusedLabelColor  = Color(0xFF7DD3FC)
                )
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value         = location,
                onValueChange = { location = it },
                label         = { Text("地点（可选）") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                leadingIcon   = {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = "地点",
                        tint = Color(0xFF94A3B8)
                    )
                },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    focusedLabelColor  = Color(0xFF7DD3FC)
                )
            )
            Spacer(Modifier.height(16.dp))

            DialogSectionLabel("重要程度")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val sel = priority == p
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) p.color.copy(0.15f) else Color(0xFFF8FAFC))
                            .border(
                                if (sel) 1.5.dp else 1.dp,
                                if (sel) p.color else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                priority = p
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.label,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (sel) p.color else Color(0xFF94A3B8)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            DialogSectionLabel("标签")
            Spacer(Modifier.height(8.dp))
            PRESET_TAGS.chunked(5).forEach { rowTags ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.padding(bottom = 6.dp)
                ) {
                    rowTags.forEach { tag ->
                        val sel = tag in selectedTags
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) tag.color.copy(0.18f) else Color(0xFFF1F5F9))
                                .border(
                                    if (sel) 1.5.dp else 1.dp,
                                    if (sel) tag.color else Color(0xFFE2E8F0),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTags =
                                        if (sel) selectedTags - tag else selectedTags + tag
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                tag.label,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (sel) tag.color else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (task == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isRecurring) Color(0xFF818CF8).copy(alpha = 0.10f)
                            else Color(0xFFF8FAFC)
                        )
                        .border(
                            width = if (isRecurring) 1.5.dp else 1.dp,
                            color = if (isRecurring) Color(0xFF818CF8) else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Refresh, null,
                                tint     = Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "长期任务",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF1C1C1E)
                            )
                            Text(
                                "按星期在日期范围内批量生成",
                                fontSize = 11.sp,
                                color    = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Switch(
                        checked         = isRecurring,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isRecurring = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor    = Color.White,
                            checkedTrackColor    = Color(0xFF818CF8),
                            uncheckedThumbColor  = Color.White,
                            uncheckedTrackColor  = Color(0xFFE2E8F0),
                            uncheckedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = isRecurring && task == null,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column {
                    DialogSectionLabel("重复频率")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskRepeatMode.entries.forEach { mode ->
                            val selected = repeatMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Color(0xFF818CF8) else Color(0xFFF1F5F9)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        repeatMode = mode
                                        // 如果切换到非每周模式，清空星期选择
                                        if (mode != TaskRepeatMode.WEEKLY) {
                                            weekDays = emptySet()
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mode.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    if (repeatMode == TaskRepeatMode.WEEKLY) {
                        DialogSectionLabel("重复星期")
                        Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cnWeekDays.forEachIndexed { idx, name ->
                            val dayNum = idx + 1
                            val sel    = dayNum in weekDays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (sel) Color(0xFF818CF8) else Color(0xFFF1F5F9)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        weekDays =
                                            if (sel) weekDays - dayNum else weekDays + dayNum
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (sel) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    }

                    DialogSectionLabel("开始日期")
                    Spacer(Modifier.height(8.dp))
                    MiniCalendar(
                        currentMonth   = calendarMonth,
                        selectedDate   = dueDate,
                        onDateSelected = {
                            dueDate = it
                            if (endDate.isBefore(it)) {
                                endDate  = it.plusMonths(1)
                                endMonth = YearMonth.from(endDate)
                            }
                            calendarMonth = YearMonth.from(it)
                        },
                        onMonthChange  = { calendarMonth = calendarMonth.plusMonths(it.toLong()) }
                    )
                    Spacer(Modifier.height(16.dp))

                    DialogSectionLabel("结束日期")
                    Spacer(Modifier.height(8.dp))
                    MiniCalendar(
                        currentMonth   = endMonth,
                        selectedDate   = endDate,
                        onDateSelected = {
                            if (!it.isBefore(dueDate)) {
                                endDate  = it
                                endMonth = YearMonth.from(it)
                            }
                        },
                        onMonthChange  = { endMonth = endMonth.plusMonths(it.toLong()) }
                    )

                    Spacer(Modifier.height(8.dp))
                    val dayCount = when (repeatMode) {
                        TaskRepeatMode.DAILY -> java.time.temporal.ChronoUnit.DAYS.between(dueDate, endDate).toInt() + 1
                        TaskRepeatMode.WEEKLY -> if (weekDays.isNotEmpty()) {
                            weekDays.sumOf { day ->
                                var count = 0
                                var cur = dueDate
                                while (!cur.isAfter(endDate)) {
                                    if (cur.dayOfWeek.value == day) count++
                                    cur = cur.plusDays(1)
                                }
                                count
                            }
                        } else 0
                        TaskRepeatMode.MONTHLY -> {
                            var count = 0
                            var cur = dueDate
                            while (!cur.isAfter(endDate)) { count++; cur = cur.plusMonths(1) }
                            count
                        }
                        TaskRepeatMode.YEARLY -> {
                            var count = 0
                            var cur = dueDate
                            while (!cur.isAfter(endDate)) { count++; cur = cur.plusYears(1) }
                            count
                        }
                    }
                    if (dayCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "将生成 $dayCount 条任务",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF818CF8)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 截止日期、时间和提醒设置
            Column {
                // 截止日期 - 仅对单次任务显示
                AnimatedVisibility(
                    visible = !isRecurring,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        DialogSectionLabel("截止日期")
                        Spacer(Modifier.height(8.dp))
                        MiniCalendar(
                            currentMonth   = calendarMonth,
                            selectedDate   = dueDate,
                            onDateSelected = { dueDate = it; calendarMonth = YearMonth.from(it) },
                            onMonthChange  = { calendarMonth = calendarMonth.plusMonths(it.toLong()) }
                        )
                    }
                }
                
                // 时间选择器 - 对所有任务类型显示
                DialogSectionLabel("时间")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 开始时间
                    TimePickerField(
                        label = "开始时间",
                        selectedTime = startTime,
                        onTimeSelected = { time -> startTime = time },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 结束时间
                    TimePickerField(
                        label = "结束时间",
                        selectedTime = endTime,
                        onTimeSelected = { time -> endTime = time },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 提醒设置 - 对所有任务类型显示
                DialogSectionLabel("提醒")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()), // 添加横向滚动
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reminderOptions = listOf(
                        Pair(null, "无"),
                        Pair(0, "准时"),
                        Pair(5, "提前5分钟"),
                        Pair(15, "提前15分钟"),
                        Pair(30, "提前30分钟")
                    )
                    
                    reminderOptions.forEach { (offset, label) ->
                        val isSelected = reminderOffset == offset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF7DD3FC).copy(alpha = 0.15f)
                                    else Color(0xFFF8FAFC)
                                )
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF7DD3FC) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    reminderOffset = offset
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp), // 稍微增加一点左右内边距让它更好看
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF64748B),
                                maxLines = 1, // 强制单行
                                softWrap = false // 禁止换行
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) { Text("取消", color = Color(0xFF94A3B8)) }
                Button(
                    onClick  = {
                        if (canConfirm) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm(
                                title.trim(),
                                note.trim(),
                                dueDate,
                                startTime,
                                endTime,
                                priority,
                                selectedTags.toList(),
                                if (isRecurring) repeatMode else TaskRepeatMode.WEEKLY,
                                if (isRecurring) weekDays else emptySet(),
                                if (isRecurring) endDate else null,
                                location.trim(),
                                reminderOffset
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFF1C1C1E),
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    enabled  = canConfirm
                ) { Text("确认", color = Color.White) }
            }
        }
    }
}

@Composable
fun DialogSectionLabel(text: String) {
    Text(
        text,
        fontSize      = 13.sp,
        fontWeight    = FontWeight.Bold,
        color         = Color(0xFF94A3B8),
        letterSpacing = 1.sp
    )
}

// ══════════════════════════════════════════════
//  内嵌小日历
// ══════════════════════════════════════════════

@Composable
fun MiniCalendar(
    currentMonth  : YearMonth,
    selectedDate  : LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange : (Int) -> Unit
) {
    val today       = LocalDate.now()
    val firstDay    = currentMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1
    val daysInMonth = currentMonth.lengthOfMonth()
    val cnDays      = listOf("一", "二", "三", "四", "五", "六", "日")

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(12.dp)
    ) {
        // 月份导航
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMonthChange(-1)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "上个月",
                    tint = Color(0xFF1C1C1E),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(
                currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月")),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF1C1C1E)
            )
            
            IconButton(
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMonthChange(1)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "下个月",
                    tint = Color(0xFF1C1C1E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // 星期标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            cnDays.forEach { day ->
                Text(
                    day,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF94A3B8),
                    modifier = Modifier.width(28.dp).wrapContentWidth()
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 日期网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 空白单元格（月初偏移）
            items(startOffset) {
                Box(modifier = Modifier.size(28.dp))
            }
            
            // 日期单元格
            items(daysInMonth) { day ->
                val date = currentMonth.atDay(day + 1)
                val isSelected = date == selectedDate
                val isToday = date == today
                
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> Color(0xFF7DD3FC)
                                isToday -> Color(0xFF7DD3FC).copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDateSelected(date)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (day + 1).toString(),
                        fontSize   = 12.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> Color.White
                            isToday -> Color(0xFF3B82F6)
                            else -> Color(0xFF1C1C1E)
                        }
                    )
                }
            }
        }
    }
}

