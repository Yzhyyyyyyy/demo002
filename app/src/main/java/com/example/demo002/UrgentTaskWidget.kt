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
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

class UrgentTaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }

    @Composable
    private fun WidgetContent() {
        val context = LocalContext.current
        val urgentTask = remember { mutableStateOf<Task?>(null) }

        LaunchedEffect(Unit) {
            val tasks = TodayScheduleWidget.loadTodayTasks(context)
            urgentTask.value = tasks.filter { !it.isDone }
                .sortedWith(compareBy<Task> {
                    when(it.priority) {
                        Priority.HIGH -> 0
                        Priority.MEDIUM -> 1
                        Priority.LOW -> 2
                    }
                }.thenBy { it.startTime ?: java.time.LocalTime.MAX })
                .firstOrNull()
        }

        val task = urgentTask.value
        val componentName = ComponentName(context, MainActivity::class.java)

        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(day = Color(0xFFE3F2FD), night = Color(0xFF0F172A)))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(componentName))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (task == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier.size(40.dp)
                            .background(ColorProvider(day = Color(0xFFDBEAFE), night = Color(0xFF1E293B)))
                            .cornerRadius(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎉", style = TextStyle(fontSize = 20.sp))
                    }
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        "今日任务已清空",
                        style = TextStyle(color = ColorProvider(day = Color(0xFF1E40AF), night = Color(0xFF93C5FD)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        "继续保持！",
                        style = TextStyle(color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFFCBD5E1)), fontSize = 11.sp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            } else {
                val timeText = task.startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天"
                val noteText = task.note

                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 中间：任务信息（去掉了优先级指示器）
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", style = TextStyle(fontSize = 14.sp))
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = task.title,
                                style = TextStyle(color = ColorProvider(day = Color(0xFF111827), night = Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                        if (noteText?.isNotEmpty() == true) {
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Text(
                                text = noteText,
                                style = TextStyle(color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFFCBD5E1)), fontSize = 12.sp)
                                // 移除了 maxLines，允许长文本自然换行
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    // 右侧：时间信息
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = GlanceModifier.background(ColorProvider(day = Color(0xFFDBEAFE), night = Color(0xFF1E293B))).cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timeText,
                                style = TextStyle(color = ColorProvider(day = Color(0xFF1E40AF), night = Color(0xFF93C5FD)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            "点击查看",
                            style = TextStyle(color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF)), fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}
