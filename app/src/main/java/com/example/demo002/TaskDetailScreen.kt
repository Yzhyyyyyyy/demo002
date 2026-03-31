package com.example.demo002

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.List
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ══════════════════════════════════════════════
//  TaskDetailScreen
// ══════════════════════════════════════════════
@Composable
fun TaskDetailScreen(
    taskId        : Int,
    onNavigateBack: () -> Unit
) {
    val viewModel: TaskViewModel = viewModel()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val taskOrNull = tasks.find { it.id == taskId }
    val isLoading  = tasks.isEmpty()

    var showEditTask   by remember { mutableStateOf(false) }
    var showAddSubTask by remember { mutableStateOf(false) }
    var editingSubTask by remember { mutableStateOf<SubTask?>(null) }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(tasks) {
        if (tasks.isNotEmpty() && taskOrNull == null) {
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyFirstScreen()

        when {
            isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color       = Color(0xFF1C1C1E),
                        modifier    = Modifier.size(36.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            }

            taskOrNull != null -> {
                // ✅ 修复：用 derivedStateOf 保证每次重组都取最新 task
                val task by remember(taskId) {
                    derivedStateOf { tasks.find { it.id == taskId } ?: taskOrNull }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // ── 顶部栏 ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigateBack()
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack, "返回",
                                    tint     = Color(0xFF1C1C1E),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "任务详情",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Black,
                                color      = Color(0xFF1C1C1E)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(
                                    3.dp, RoundedCornerShape(12.dp),
                                    ambientColor = Color(0xFF94A3B8).copy(alpha = 0.12f),
                                    spotColor    = Color(0xFF94A3B8).copy(alpha = 0.08f)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showEditTask = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Edit, "编辑任务",
                                tint     = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── 内容 ──
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TaskInfoCard(task)
                        // 暂时屏蔽子任务功能
                        // SubTaskSection(
                        //     task       = task,
                        //     onToggle   = { sub ->
                        //         // ✅ 修复：每次回调都从最新 tasks 里取
                        //         val latest = tasks.find { it.id == taskId } ?: return@SubTaskSection
                        //         viewModel.updateTask(latest.copy(
                        //             subTasks = latest.subTasks.map {
                        //                 if (it.id == sub.id) it.copy(isDone = !it.isDone) else it
                        //             }
                        //         ))
                        //     },
                        //     onDelete   = { sub ->
                        //         // ✅ 修复：每次回调都从最新 tasks 里取
                        //         val latest = tasks.find { it.id == taskId } ?: return@SubTaskSection
                        //         viewModel.updateTask(latest.copy(
                        //             subTasks = latest.subTasks.filter { it.id != sub.id }
                        //         ))
                        //     },
                        //     onEdit     = { editingSubTask = it },
                        //     onAddClick = { showAddSubTask = true },
                        //     onReorder  = { newList ->
                        //         val latest = tasks.find { it.id == taskId } ?: return@SubTaskSection
                        //         viewModel.updateTask(latest.copy(subTasks = newList))
                        //     }
                        // )
                        // Spacer(Modifier.height(40.dp))
                    }
                }

                // ── 编辑任务弹窗 ──
                if (showEditTask) {
                    TaskDialog(
                        task        = task,
                        initialDate = task.dueDate ?: LocalDate.now(),
                        onDismiss   = { showEditTask = false },
                        onConfirm   = { title: String, note: String, date: LocalDate?,
                                        startTime: java.time.LocalTime?, endTime: java.time.LocalTime?,
                                        priority: Priority, tags: List<TaskTag>,
                                        _: Set<Int>, _: LocalDate?, location: String ->
                            val latest = tasks.find { it.id == taskId } ?: return@TaskDialog
                            viewModel.updateTask(
                                latest.copy(
                                    title     = title,
                                    note      = note,
                                    dueDate   = date,
                                    startTime = startTime,
                                    endTime   = endTime,
                                    priority  = priority,
                                    tags      = tags,
                                    location  = location
                                )
                            )
                            showEditTask = false
                        }
                    )
                }

                // ── 添加 / 编辑子任务弹窗 ──（暂时屏蔽子任务功能）
                // if (showAddSubTask || editingSubTask != null) {
                //     SubTaskDialog(
                //         subTask   = editingSubTask,
                //         onDismiss = { showAddSubTask = false; editingSubTask = null },
                //         onConfirm = { title: String, date: LocalDate? ->
                //             // ✅ 修复：每次回调都从最新 tasks 里取
                //             val latest = tasks.find { it.id == taskId } ?: return@SubTaskDialog
                //             if (editingSubTask != null) {
                //                 viewModel.updateTask(latest.copy(
                //                     subTasks = latest.subTasks.map {
                //                         if (it.id == editingSubTask!!.id)
                //                             it.copy(title = title, dueDate = date)
                //                         else it
                //                     }
                //                 ))
                //             } else {
                //                 viewModel.updateTask(
                //                     latest.copy(subTasks = latest.subTasks +
                //                             SubTask(0, title, dueDate = date))
                //                 )
                //             }
                //             showAddSubTask = false
                //             editingSubTask = null
                //         }
                //     )
                // }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  任务信息卡
// ══════════════════════════════════════════════
@Composable
fun TaskInfoCard(task: Task) {
    val today     = LocalDate.now()
    // 暂时屏蔽子任务功能
    // val doneCount = task.subTasks.count { it.isDone }
    // val total     = task.subTasks.size
    // val progress  = if (total > 0) doneCount.toFloat() / total else 0f
    val progress = 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                6.dp, RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF7DD3FC).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(5.dp).height(44.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(task.priority.color)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF1C1C1E)
                )
                if (task.note.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        task.note,
                        fontSize = 13.sp,
                        color    = Color(0xFF94A3B8)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(task.priority.color.copy(alpha = 0.13f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    task.priority.label,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = task.priority.color
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(Modifier.height(12.dp))
        if (task.dueDate != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.DateRange, null,
                    tint     = Color(0xFF94A3B8),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    when (task.dueDate) {
                        today             -> "今天截止"
                        today.plusDays(1) -> "明天截止"
                        else -> task.dueDate.format(DateTimeFormatter.ofPattern("M月d日 截止"))
                    },
                    fontSize   = 13.sp,
                    color      = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 显示时间信息
            if (task.startTime != null || task.endTime != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.List, null,
                        tint     = Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        buildString {
                            task.startTime?.let {
                                append(it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                            }
                            if (task.startTime != null && task.endTime != null) {
                                append(" - ")
                            }
                            task.endTime?.let {
                                append(it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                            }
                            if (task.startTime == null && task.endTime == null) {
                                append("未设置时间")
                            }
                        },
                        fontSize   = 13.sp,
                        color      = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 显示地点信息
            if (task.location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.LocationOn, null,
                        tint     = Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        task.location,
                        fontSize   = 13.sp,
                        color      = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (task.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                task.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tag.color.copy(alpha = 0.13f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            tag.label,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = tag.color
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        // 暂时屏蔽子任务进度显示
        // if (total > 0) {
        //     Row(
        //         modifier              = Modifier.fillMaxWidth(),
        //         horizontalArrangement = Arrangement.SpaceBetween,
        //         verticalAlignment     = Alignment.CenterVertically
        //     ) {
        //         Text(
        //             "子任务进度",
        //             fontSize   = 12.sp,
        //             color      = Color(0xFF94A3B8),
        //             fontWeight = FontWeight.Medium
        //         )
        //         Text(
        //             "$doneCount / $total",
        //             fontSize   = 12.sp,
        //             fontWeight = FontWeight.Bold,
        //             color      = if (progress >= 1f) Color(0xFF34D399) else Color(0xFF475569)
        //         )
        //     }
        //     Spacer(Modifier.height(6.dp))
        //     Box(
        //         modifier = Modifier
        //             .fillMaxWidth().height(6.dp)
        //             .clip(RoundedCornerShape(3.dp))
        //             .background(Color(0xFFE2E8F0))
        //     ) {
        //         val animProgress by animateFloatAsState(
        //             progress, tween(600, easing = EaseOutCubic), label = "progress"
        //         )
        //         Box(
        //             modifier = Modifier
        //                 .fillMaxHeight()
        //                 .fillMaxWidth(animProgress)
        //                 .clip(RoundedCornerShape(3.dp))
        //                 .background(
        //                     if (progress >= 1f) Color(0xFF34D399) else task.priority.color
        //                 )
        //         )
        //     }
        // }
    }
}

// ══════════════════════════════════════════════
//  拖拽状态（暂时屏蔽子任务功能）
// ══════════════════════════════════════════════
// class DragState {
//     var draggingId   by mutableStateOf<Int?>(null)
//     var dragOffsetY  by mutableFloatStateOf(0f)
//     var accDragY     by mutableFloatStateOf(0f)
//     var itemHeightPx by mutableFloatStateOf(72f)
//
//     fun start(id: Int, heightPx: Float) {
//         draggingId   = id
//         dragOffsetY  = 0f
//         accDragY     = 0f
//         itemHeightPx = heightPx
//     }
//     fun reset() {
//         draggingId  = null
//         dragOffsetY = 0f
//         accDragY    = 0f
//     }
// }

// ══════════════════════════════════════════════
//  子任务区块
// ══════════════════════════════════════════════
//  子任务区块（暂时屏蔽子任务功能）
// ══════════════════════════════════════════════
// @Composable
// fun SubTaskSection(
//     task      : Task,
//     onToggle  : (SubTask) -> Unit,
//     onDelete  : (SubTask) -> Unit,
//     onEdit    : (SubTask) -> Unit,
//     onAddClick: () -> Unit,
//     onReorder : (List<SubTask>) -> Unit
// ) {
//     val subTasks = remember(task.subTasks) {
//         mutableStateListOf<SubTask>().also { it.addAll(task.subTasks) }
//     }
//     LaunchedEffect(task.subTasks) {
//         if (subTasks.toList() != task.subTasks) {
//             subTasks.clear()
//             subTasks.addAll(task.subTasks)
//         }
//     }
//
//     val drag     = remember { DragState() }
//     val nudgeMap = remember { mutableStateMapOf<Int, Float>() }
//     val haptic   = LocalHapticFeedback.current
//
//     Column(
//         modifier = Modifier
//             .fillMaxWidth()
//             .shadow(
//                 4.dp, RoundedCornerShape(20.dp),
//                 ambientColor = Color(0xFF7DD3FC).copy(alpha = 0.10f)
//             )
//             .clip(RoundedCornerShape(20.dp))
//             .background(Color.White.copy(alpha = 0.92f))
//             .padding(16.dp)
//     ) {
//         Row(
//             modifier              = Modifier.fillMaxWidth(),
//             horizontalArrangement = Arrangement.SpaceBetween,
//             verticalAlignment     = Alignment.CenterVertically
//         ) {
//             Row(verticalAlignment = Alignment.CenterVertically) {
//                 Icon(
//                     Icons.AutoMirrored.Rounded.List, null,
//                     tint     = Color(0xFF475569),
//                     modifier = Modifier.size(18.dp)
//                 )
//                 Spacer(Modifier.width(6.dp))
//                 Text(
//                     "子任务",
//                     fontSize   = 15.sp,
//                     fontWeight = FontWeight.Black,
//                     color      = Color(0xFF1C1C1E)
//                 )
//                 if (subTasks.isNotEmpty()) {
//                     Spacer(Modifier.width(8.dp))
//                     Box(
//                         modifier = Modifier
//                             .clip(CircleShape)
//                             .background(Color(0xFFF1F5F9))
//                             .padding(horizontal = 8.dp, vertical = 2.dp)
//                     ) {
//                         Text(
//                             "${subTasks.size}",
//                             fontSize   = 11.sp,
//                             fontWeight = FontWeight.Bold,
//                             color      = Color(0xFF64748B)
//                         )
//                     }
//                 }
//             }
//             Box(
//                 modifier = Modifier
//                     .size(32.dp)
//                     .clip(CircleShape)
//                     .background(Color(0xFF1C1C1E))
//                     .clickable {
//                         haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//                         onAddClick()
//                     },
//                 contentAlignment = Alignment.Center
//             ) {
//                 Icon(
//                     Icons.Rounded.Add, null,
//                     tint     = Color.White,
//                     modifier = Modifier.size(16.dp)
//                 )
//             }
//         }
//
//         if (subTasks.isEmpty()) {
//             Spacer(Modifier.height(16.dp))
//             Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//                 Text(
//                     "还没有子任务，点击 + 添加",
//                     fontSize = 13.sp,
//                     color    = Color(0xFFB0BEC5)
//                 )
//             }
//             Spacer(Modifier.height(8.dp))
//         } else {
//             Spacer(Modifier.height(12.dp))
//             Row(
//                 verticalAlignment = Alignment.CenterVertically,
//                 modifier          = Modifier.padding(bottom = 8.dp, start = 2.dp)
//             ) {
//                 Icon(
//                     Icons.Rounded.Menu, null,
//                     tint     = Color(0xFFCBD5E1),
//                     modifier = Modifier.size(13.dp)
//                 )
//                 Spacer(Modifier.width(4.dp))
//                 Text(
//                     "长按可拖拽排序",
//                     fontSize = 11.sp,
//                     color    = Color(0xFFCBD5E1)
//                 )
//             }
//
//             subTasks.forEach { sub ->
//                 key(sub.id) {
//                     val isDragging  = drag.draggingId == sub.id
//                     val nudgeTarget = if (isDragging) 0f else (nudgeMap[sub.id] ?: 0f)
//                     val animNudge by animateFloatAsState(
//                         targetValue   = nudgeTarget,
//                         animationSpec = spring(
//                             dampingRatio = Spring.DampingRatioMediumBouncy,
//                             stiffness    = Spring.StiffnessMedium
//                         ),
//                         label = "nudge_${sub.id}"
//                     )
//
//                     Box(
//                         modifier = Modifier
//                             .zIndex(if (isDragging) 1f else 0f)
//                             .graphicsLayer {
//                                 translationY    = if (isDragging) drag.dragOffsetY else animNudge
//                                 scaleX          = if (isDragging) 1.04f else 1f
//                                 scaleY          = if (isDragging) 1.04f else 1f
//                                 shadowElevation = if (isDragging) 20f else 0f
//                                 alpha           = if (isDragging) 0.93f else 1f
//                             }
//                     ) {
//                         SwipeableSubTaskRow(
//                             subTask     = sub,
//                             isDragging  = isDragging,
//                             onToggle    = {
//                                 haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//                                 onToggle(sub)
//                             },
//                             onDelete    = {
//                                 haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                                 onDelete(sub)
//                             },
//                             onEdit      = { onEdit(sub) },
//                             onDragStart = { heightPx ->
//                                 drag.start(sub.id, heightPx)
//                                 nudgeMap.clear()
//                             },
//                             onDrag = { dy ->
//                                 drag.dragOffsetY += dy
//                                 drag.accDragY    += dy
//
//                                 val fromIdx = subTasks.indexOfFirst { it.id == drag.draggingId }
//                                 if (fromIdx < 0) return@SwipeableSubTaskRow
//
//                                 val steps = (drag.dragOffsetY / drag.itemHeightPx)
//                                     .toInt()
//                                     .coerceIn(-fromIdx, subTasks.lastIndex - fromIdx)
//                                 val toIdx = fromIdx + steps
//
//                                 subTasks.forEachIndexed { idx, s ->
//                                     if (s.id == drag.draggingId) return@forEachIndexed
//                                     val newNudge = when {
//                                         steps > 0 && idx in (fromIdx + 1)..toIdx ->
//                                             -drag.itemHeightPx
//                                         steps < 0 && idx in toIdx until fromIdx ->
//                                             drag.itemHeightPx
//                                         else -> 0f
//                                     }
//                                     if (nudgeMap[s.id] != newNudge && newNudge != 0f) {
//                                         haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//                                     }
//                                     nudgeMap[s.id] = newNudge
//                                 }
//                             },
//                             onDragEnd = {
//                                 val fromIdx = subTasks.indexOfFirst { it.id == drag.draggingId }
//                                 if (fromIdx >= 0) {
//                                     val steps = (drag.dragOffsetY / drag.itemHeightPx)
//                                         .toInt()
//                                         .coerceIn(-fromIdx, subTasks.lastIndex - fromIdx)
//                                     val toIdx = fromIdx + steps
//                                     if (toIdx != fromIdx) {
//                                         haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                                         val item = subTasks.removeAt(fromIdx)
//                                         subTasks.add(toIdx, item)
//                                     }
//                                 }
//                                 nudgeMap.clear()
//                                 onReorder(subTasks.toList())
//                                 drag.reset()
//                             }
//                         )
//                     }
//                     Spacer(Modifier.height(8.dp))
//                 }
//             }
//         }
//     }
// }

// ══════════════════════════════════════════════
//  可滑动 + 可拖拽的子任务行
// ══════════════════════════════════════════════
@Composable
fun SwipeableSubTaskRow(
    subTask    : SubTask,
    isDragging : Boolean,
    onToggle   : () -> Unit,
    onDelete   : () -> Unit,
    onEdit     : () -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag     : (Float) -> Unit,
    onDragEnd  : () -> Unit
) {
    var swipeOffsetX by remember(subTask.id) { mutableFloatStateOf(0f) }
    val animSwipeX   by animateFloatAsState(
        targetValue   = swipeOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "swipe_${subTask.id}"
    )
    val completeThreshold = 180f
    val deleteThreshold   = -180f
    val completeAlpha = (swipeOffsetX / completeThreshold).coerceIn(0f, 1f)
    val deleteAlpha   = ((-swipeOffsetX) / (-deleteThreshold)).coerceIn(0f, 1f)

    val haptic = LocalHapticFeedback.current
    var hasTriggeredCompleteHaptic by remember(subTask.id) { mutableStateOf(false) }
    var hasTriggeredDeleteHaptic   by remember(subTask.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // 左侧完成背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF34D399).copy(alpha = completeAlpha)),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                if (subTask.isDone) Icons.Rounded.Refresh else Icons.Rounded.Check,
                "完成",
                tint     = Color.White.copy(alpha = completeAlpha),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        // 右侧删除背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF6B6B).copy(alpha = deleteAlpha)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Rounded.Delete, "删除",
                tint     = Color.White.copy(alpha = deleteAlpha),
                modifier = Modifier.padding(end = 16.dp)
            )
        }
        SubTaskRowContent(
            subTask    = subTask,
            isDragging = isDragging,
            onToggle   = onToggle,
            onEdit     = onEdit,
            modifier   = Modifier
                .graphicsLayer { translationX = animSwipeX }
                .pointerInput(subTask.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { _ ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            swipeOffsetX = 0f
                            onDragStart(size.height.toFloat() + 8.dp.toPx())
                        },
                        onDrag       = { _, dragAmount -> onDrag(dragAmount.y) },
                        onDragEnd    = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
                .pointerInput(subTask.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredCompleteHaptic = false
                            hasTriggeredDeleteHaptic   = false
                        },
                        onDragEnd = {
                            when {
                                swipeOffsetX > completeThreshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggle()
                                    swipeOffsetX = 0f
                                }
                                swipeOffsetX < deleteThreshold -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDelete()
                                }
                                else -> swipeOffsetX = 0f
                            }
                            hasTriggeredCompleteHaptic = false
                            hasTriggeredDeleteHaptic   = false
                        },
                        onHorizontalDrag = { _, delta ->
                            swipeOffsetX = (swipeOffsetX + delta)
                                .coerceIn(deleteThreshold * 1.3f, completeThreshold * 1.3f)

                            if (swipeOffsetX >= completeThreshold && !hasTriggeredCompleteHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasTriggeredCompleteHaptic = true
                            } else if (swipeOffsetX < completeThreshold) {
                                hasTriggeredCompleteHaptic = false
                            }

                            if (swipeOffsetX <= deleteThreshold && !hasTriggeredDeleteHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasTriggeredDeleteHaptic = true
                            } else if (swipeOffsetX > deleteThreshold) {
                                hasTriggeredDeleteHaptic = false
                            }
                        }
                    )
                }
        )
    }
}

// ══════════════════════════════════════════════
//  子任务行内容
// ══════════════════════════════════════════════
@Composable
fun SubTaskRowContent(
    subTask   : SubTask,
    isDragging: Boolean,
    onToggle  : () -> Unit,
    onEdit    : () -> Unit,
    modifier  : Modifier = Modifier
) {
    val today  = LocalDate.now()
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isDragging     -> Color(0xFFE0F2FE)
                    subTask.isDone -> Color(0xFFF8FAFC)
                    else           -> Color(0xFFF1F5F9)
                }
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEdit()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Menu, "拖拽",
            tint     = if (isDragging) Color(0xFF7DD3FC) else Color(0xFFCBD5E1),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))

        // ── 勾选圆圈 ──
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (subTask.isDone) Color(0xFF34D399) else Color.White)
                .border(
                    1.5.dp,
                    if (subTask.isDone) Color(0xFF34D399) else Color(0xFFCBD5E1),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            if (subTask.isDone) {
                Icon(
                    Icons.Rounded.Check, null,
                    tint     = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                subTask.title,
                fontSize       = 14.sp,
                fontWeight     = FontWeight.Medium,
                color          = if (subTask.isDone) Color(0xFF94A3B8) else Color(0xFF1C1C1E),
                textDecoration = if (subTask.isDone) TextDecoration.LineThrough else null
            )
            if (subTask.dueDate != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    when (subTask.dueDate) {
                        today             -> "今天"
                        today.plusDays(1) -> "明天"
                        else -> subTask.dueDate.format(DateTimeFormatter.ofPattern("M月d日"))
                    },
                    fontSize = 11.sp,
                    color    = Color(0xFF94A3B8)
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "左滑删除",
            tint     = Color(0xFFE2E8F0),
            modifier = Modifier.size(14.dp)
        )
    }
}

// ══════════════════════════════════════════════
//  添加 / 编辑子任务弹窗
// ══════════════════════════════════════════════
@Composable
fun SubTaskDialog(
    subTask  : SubTask?,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate?) -> Unit
) {
    var title        by remember { mutableStateOf(subTask?.title ?: "") }
    // ✅ 修复：新增时不预设日期，编辑时用原有日期
    var dueDate      by remember { mutableStateOf<LocalDate?>(subTask?.dueDate) }
    var showCalendar by remember { mutableStateOf(false) }
    val today        = LocalDate.now()
    val haptic       = LocalHapticFeedback.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(22.dp)
        ) {
            Text(
                if (subTask == null) "添加子任务" else "编辑子任务",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                color      = Color(0xFF1C1C1E)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("子任务标题") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    focusedLabelColor  = Color(0xFF7DD3FC)
                )
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "截止日期",
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(today, today.plusDays(1), today.plusDays(2)).forEach { d ->
                    val label = when (d) {
                        today             -> "今天"
                        today.plusDays(1) -> "明天"
                        else              -> "后天"
                    }
                    val sel = dueDate == d
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Color(0xFF1C1C1E) else Color(0xFFF1F5F9))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // ✅ 再次点击同一个日期则取消选中（置为 null）
                                dueDate = if (dueDate == d) null else d
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (sel) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
                val isCustom = dueDate != null &&
                        dueDate !in listOf(today, today.plusDays(1), today.plusDays(2))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCustom) Color(0xFF1C1C1E) else Color(0xFFF1F5F9))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showCalendar = !showCalendar
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DateRange, null,
                        tint     = if (isCustom) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (showCalendar) {
                Spacer(Modifier.height(10.dp))
                var calMonth by remember {
                    mutableStateOf(YearMonth.from(dueDate ?: today))
                }
                MiniCalendar(
                    currentMonth   = calMonth,
                    selectedDate   = dueDate ?: today,
                    onDateSelected = { dueDate = it; showCalendar = false },
                    onMonthChange  = { calMonth = calMonth.plusMonths(it.toLong()) }
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text("取消", color = Color(0xFF94A3B8))
                }
                Button(
                    onClick  = {
                        if (title.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm(title.trim(), dueDate)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E)),
                    enabled  = title.isNotBlank()
                ) {
                    Text("确认", color = Color.White)
                }
            }
        }
    }
}