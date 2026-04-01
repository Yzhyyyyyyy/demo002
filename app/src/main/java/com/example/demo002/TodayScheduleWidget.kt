package com.example.demo002

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(day = Color(0xFFF3F4F6), night = Color(0xFF121212)))
                .padding(12.dp)
                .clickable(actionStartActivity(componentName))
        ) {
            // 头部
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 16.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier.size(32.dp).background(ColorProvider(day = Color(0xFF4F46E5), night = Color(0xFF6366F1))).cornerRadius(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📅", style = TextStyle(fontSize = 16.sp))
                }
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(
                    "今日日程",
                    style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground, fontSize = 18.sp),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    "${tasks.value.size}项",
                    style = TextStyle(color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF)), fontSize = 14.sp)
                )
            }

            // 列表
            if (tasks.value.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E))).cornerRadius(16.dp).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", style = TextStyle(fontSize = 32.sp))
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text("今日无待办", style = TextStyle(color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF)), fontSize = 14.sp))
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text("享受轻松的一天", style = TextStyle(color = ColorProvider(day = Color(0xFF9CA3AF), night = Color(0xFF6B7280)), fontSize = 12.sp))
                    }
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks.value) { task ->
                        Column {
                            TaskItem(task, componentName)
                            Spacer(modifier = GlanceModifier.height(8.dp))
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

        Box(
            modifier = GlanceModifier.fillMaxWidth()
                .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
                .cornerRadius(16.dp)
                .padding(16.dp)
                .clickable(actionStartActivity(componentName))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 中间：任务信息（去掉了左侧的优先级竖线）
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = task.title,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (task.isDone) ColorProvider(day = Color(0xFF9CA3AF), night = Color(0xFF6B7280)) else GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1
                    )

                    if (noteText?.isNotEmpty() == true) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = noteText,
                            style = TextStyle(fontSize = 13.sp, color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF)))
                            // 移除了 maxLines，允许长文本自然换行
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // 右侧：时间与复选框
                Column(horizontalAlignment = Alignment.End) {
                    Text(timeText, style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.primary, fontWeight = FontWeight.Medium))
                    Spacer(modifier = GlanceModifier.height(8.dp))

                    val checkboxBgColor = if (task.isDone) Color(0xFF10B981) else Color(0xFFE5E7EB)
                    Box(
                        modifier = GlanceModifier.size(24.dp)
                            .background(ColorProvider(day = checkboxBgColor, night = checkboxBgColor))
                            .cornerRadius(12.dp)
                            .clickable(actionRunCallback<ToggleTaskCompletionAction>(
                                actionParametersOf(ToggleTaskCompletionAction.taskIdKey to task.id)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isDone) {
                            Text("✓", style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    }
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
                    .sortedBy { it.startTime ?: LocalTime.MIN }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
