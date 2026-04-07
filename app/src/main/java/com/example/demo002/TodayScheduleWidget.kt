package com.example.demo002

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.appwidget.lazy.*
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TodayScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current
        val tasks = remember { mutableStateOf<List<Task>>(emptyList()) }
        LaunchedEffect(Unit) { tasks.value = loadTodayTasks(context) }

        val componentName = ComponentName(context, MainActivity::class.java)

        // 外层容器：半透明背景 + 圆角
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(
                    ColorProvider(
                        day = Color(0xFFF8FAFC),  // 浅色背景
                        night = Color(0xFF1C1C1E)  // 深色背景
                    )
                )
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(componentName))
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .padding(4.dp)  // 极小的内边距，避免与系统边距冲突
            ) {
                // 头部 - 使用半透明背景
                Box(
                    modifier = GlanceModifier.fillMaxWidth()
                        .background(
                            ColorProvider(
                                day = Color(0xF0F8FAFC),  // 浅色半透明
                                night = Color(0xCC2C2C2E)
                            )
                        )
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 图标容器
                        Box(
                            modifier = GlanceModifier.size(36.dp)
                                .background(
                                    ColorProvider(
                                        day = Color(0xFF6366F1),
                                        night = Color(0xFF818CF8)
                                    )
                                )
                                .cornerRadius(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📅", style = TextStyle(fontSize = 18.sp))
                        }
                        Spacer(modifier = GlanceModifier.width(12.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                "今日日程",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(day = Color(0xFF1E293B), night = Color.White),
                                    fontSize = 18.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                "${tasks.value.size}项任务",
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8)),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        // 状态指示器
                        Box(
                            modifier = GlanceModifier
                                .background(
                                    ColorProvider(
                                        day = Color(0x337DD3FC),
                                        night = Color(0x33636CF7)
                                    )
                                )
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "进行中",
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF0EA5E9), night = Color(0xFF60A5FA)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(12.dp))
                
                // 列表区域
                if (tasks.value.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize()
                            .background(
                                ColorProvider(
                                    day = Color.White.copy(alpha = 0.6f),
                                    night = Color(0xCC2C2C2E)
                                )
                            )
                            .cornerRadius(16.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = GlanceModifier.size(32.dp)
                                    .background(
                                        ColorProvider(
                                            day = Color(0xFFF0F9FF),
                                            night = Color(0xCC1E293B)
                                        )
                                    )
                                    .cornerRadius(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎉", style = TextStyle(fontSize = 18.sp))
                            }
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                "今日无待办",
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF1E293B), night = Color.White),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                "享受轻松的一天",
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8)),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        itemsIndexed(tasks.value) { index, task ->
                            TaskItem(task, componentName)
                            if (index < tasks.value.size - 1) {
                                Spacer(modifier = GlanceModifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskItem(task: Task, componentName: ComponentName) {
        val timeText = task.startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天"
        val noteText = task.note

        // 任务卡片 - 使用背景色对比度区分
        Box(
            modifier = GlanceModifier.fillMaxWidth()
                .background(
                    ColorProvider(
                        day = Color.White,
                        night = Color(0xFF2C2C2E)
                    )
                )
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable(actionRunCallback<ToggleTaskCompletionAction>(
                    actionParametersOf(ToggleTaskCompletionAction.taskIdKey to task.id)
                ))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：优先级指示器（微妙的竖线）
                Box(
                    modifier = GlanceModifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(
                            when (task.priority) {
                                Priority.HIGH -> ColorProvider(day = Color(0xFFEF4444), night = Color(0xFFF87171))
                                Priority.MEDIUM -> ColorProvider(day = Color(0xFFF59E0B), night = Color(0xFFFBBF24))
                                Priority.LOW -> ColorProvider(day = Color(0xFF10B981), night = Color(0xFF34D399))
                            }
                        )
                        .cornerRadius(2.dp)
                ) {}
                Spacer(modifier = GlanceModifier.width(8.dp))
                
                // 中间：任务信息
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = task.title,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (task.isDone)
                                ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF6B7280))
                            else
                                ColorProvider(day = Color(0xFF1E293B), night = Color.White),
                            fontSize = 13.sp,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1
                    )

                    if (noteText.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = noteText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // 右侧：时间和状态
                Column(horizontalAlignment = Alignment.End) {
                    // 时间标签 - 带背景
                    Box(
                        modifier = GlanceModifier
                            .background(
                                if (task.isDone)
                                    ColorProvider(day = Color(0xFFF1F5F9), night = Color(0xCC374151))
                                else
                                    ColorProvider(day = Color(0xFFDBEAFE), night = Color(0xCC1E293B))
                            )
                            .cornerRadius(8.dp)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            timeText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = if (task.isDone)
                                    ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF6B7280))
                                else
                                    ColorProvider(day = Color(0xFF1E40AF), night = Color(0xFF93C5FD)),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    // 状态指示器
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        if (task.isDone) "已完成" else "进行中",
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = if (task.isDone)
                                ColorProvider(day = Color(0xFF94A3B8), night = Color(0xFF6B7280))
                            else
                                ColorProvider(day = Color(0xFF10B981), night = Color(0xFF34D399)),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }

    companion object {
        suspend fun loadTodayTasks(context: Context): List<Task> = withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getInstance(context).taskDao()
                val today = LocalDate.now()
                dao.getAllTasksWithSubTasks().first().map { it.toTask() }
                    .filter { it.dueDate?.isEqual(today) == true }
                    .sortedWith(compareBy(
                        { it.isDone },  // 未完成(false)在前，已完成(true)在后
                        { it.startTime ?: LocalTime.MIN }  // 然后按开始时间排序
                    ))
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
