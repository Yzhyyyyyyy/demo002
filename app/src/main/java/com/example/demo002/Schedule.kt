package com.example.demo002

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    HIGH  ("紧急", Color(0xFFFF6B6B), 0),
    MEDIUM("中等", Color(0xFFFFB347), 1),
    LOW   ("轻松", Color(0xFF7DD3FC), 2)
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
    val id      : Int,
    val title   : String,
    val note    : String        = "",
    val dueDate : LocalDate?    = null,
    val priority: Priority      = Priority.MEDIUM,
    val isDone  : Boolean       = false,
    val tags    : List<TaskTag> = emptyList(),
    val subTasks: List<SubTask> = emptyList()
)

// ══════════════════════════════════════════════
//  主界面
// ══════════════════════════════════════════════

@Composable
fun Schedule(
    onNavigateToSearch  : () -> Unit    = {},
    onNavigateToSettings: () -> Unit    = {},
    onNavigateToDetail  : (Int) -> Unit = {}
) {
    val viewModel          : TaskViewModel = viewModel()
    val tasks              by viewModel.tasks.collectAsState()
    val recurringSchedules by viewModel.recurringSchedules.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask   by remember { mutableStateOf<Task?>(null) }
    var selectedDate  by remember { mutableStateOf(LocalDate.now()) }
    var weekStart     by remember { mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) }

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
                id       = -s.id,
                title    = s.title,
                note     = s.note,
                dueDate  = selectedDate,
                priority = s.priority,
                isDone   = false,
                tags     = s.tags,
                subTasks = s.subTasks
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
                onSettings   = onNavigateToSettings
            )
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
            onConfirm   = { title, note, date, priority, tags, weekDays, endDate ->
                if (editingTask != null) {
                    viewModel.updateTask(
                        editingTask!!.copy(
                            title    = title,
                            note     = note,
                            dueDate  = date,
                            priority = priority,
                            tags     = tags
                        )
                    )
                } else if (weekDays.isNotEmpty() && endDate != null && date != null) {
                    viewModel.addRecurringTasks(
                        title     = title,
                        note      = note,
                        startDate = date,
                        endDate   = endDate,
                        weekDays  = weekDays,
                        priority  = priority,
                        tags      = tags
                    )
                } else {
                    viewModel.addTask(title, note, date, priority, tags)
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
    onSettings : () -> Unit = {}
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
            listOf(
                Icons.Rounded.Search   to onSearch,
                Icons.Rounded.Settings to onSettings
            ).forEach { (icon, action) ->
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
    val today      = LocalDate.now()
    val cnDays     = listOf("一", "二", "三", "四", "五", "六", "日")
    val weekDates  = (0..6).map { weekStart.plusDays(it.toLong()) }
    val monthLabel = weekStart.plusDays(3)
        .format(DateTimeFormatter.ofPattern("yyyy年M月"))

    var showCalendar by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onWeekChange(-1)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null,
                    tint     = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showCalendar = true
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    monthLabel,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown, null,
                    tint     = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick  = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onWeekChange(1)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                    tint     = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekDates.forEachIndexed { idx, date ->
                val isSelected = date == selectedDate
                val isToday    = date == today
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> Color(0xFF1C1C1E)
                                isToday    -> Color(0xFF7DD3FC).copy(alpha = 0.18f)
                                else       -> Color.White.copy(alpha = 0.55f)
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
                    Text(
                        cnDays[idx],
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        date.dayOfMonth.toString(),
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Black,
                        color      = when {
                            isSelected -> Color.White
                            isToday    -> Color(0xFF3B82F6)
                            else       -> Color(0xFF1C1C1E)
                        }
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isToday && isSelected -> Color(0xFF7DD3FC)
                                    isToday               -> Color(0xFF3B82F6)
                                    else                  -> Color.Transparent
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
                                    onDelete()
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
    val doneCount  = task.subTasks.count { it.isDone }
    val totalCount = task.subTasks.size
    val progress   = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

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
                onTap()
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
                if (totalCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$doneCount/$totalCount",
                        fontSize   = 10.sp,
                        color      = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (totalCount > 0) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (progress >= 1f) Color(0xFF34D399) else task.priority.color
                        )
                )
            }
        }

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
//  添加 / 编辑 Dialog
// ══════════════════════════════════════════════

@Composable
fun TaskDialog(
    task       : Task?,
    initialDate: LocalDate,
    onDismiss  : () -> Unit,
    onConfirm  : (
        title   : String,
        note    : String,
        date    : LocalDate?,
        priority: Priority,
        tags    : List<TaskTag>,
        weekDays: Set<Int>,
        endDate : LocalDate?
    ) -> Unit
) {
    var title         by remember { mutableStateOf(task?.title ?: "") }
    var note          by remember { mutableStateOf(task?.note ?: "") }
    var priority      by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var dueDate       by remember { mutableStateOf(task?.dueDate ?: initialDate) }
    var selectedTags  by remember { mutableStateOf(task?.tags?.toSet() ?: emptySet<TaskTag>()) }
    var calendarMonth by remember { mutableStateOf(YearMonth.from(dueDate)) }
    var isRecurring   by remember { mutableStateOf(false) }
    var weekDays      by remember { mutableStateOf(emptySet<Int>()) }
    var endDate       by remember { mutableStateOf(dueDate.plusMonths(1)) }
    var endMonth      by remember { mutableStateOf(YearMonth.from(endDate)) }

    val cnWeekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val canConfirm = title.isNotBlank() &&
            (!isRecurring || (weekDays.isNotEmpty() && !endDate.isBefore(dueDate)))

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
            Spacer(Modifier.height(16.dp))

            DialogSectionLabel("紧急程度")
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
                    if (weekDays.isNotEmpty()) {
                        val dayCount = weekDays.sumOf { day ->
                            var count = 0
                            var cur   = dueDate
                            while (!cur.isAfter(endDate)) {
                                if (cur.dayOfWeek.value == day) count++
                                cur = cur.plusDays(1)
                            }
                            count
                        }
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

            AnimatedVisibility(
                visible = !isRecurring,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
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
                    Spacer(Modifier.height(20.dp))
                }
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
                                priority,
                                selectedTags.toList(),
                                if (isRecurring) weekDays else emptySet(),
                                if (isRecurring) endDate else null
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
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null,
                    tint     = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                fontSize   = 13.sp,
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
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                    tint     = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            cnDays.forEach { d ->
                Text(
                    d,
                    modifier   = Modifier.weight(1f),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF94A3B8),
                    textAlign  = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val rows = (startOffset + daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date    = currentMonth.atDay(dayNum)
                        val isSel   = date == selectedDate
                        val isToday = date == today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSel   -> Color(0xFF1C1C1E)
                                        isToday -> Color(0xFF7DD3FC).copy(alpha = 0.25f)
                                        else    -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (date != selectedDate) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onDateSelected(date)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$dayNum",
                                fontSize   = 12.sp,
                                fontWeight = if (isSel || isToday) FontWeight.Bold else FontWeight.Normal,
                                color      = when {
                                    isSel   -> Color.White
                                    isToday -> Color(0xFF3B82F6)
                                    else    -> Color(0xFF374151)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  Preview
// ══════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
fun SchedulePreview() {
    Schedule()
}