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

        // 外层容器：使用背景色对比度区分
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(
                    ColorProvider(
                        day = Color(0xFFF8FAFC),  // 浅色背景
                        night = Color(0xFF1C1C1E)  // 深色背景
                    )
                )
                .cornerRadius(18.dp)
                .clickable(actionStartActivity(componentName))
                .padding(horizontal = 12.dp, vertical = 10.dp)  // 减少内边距
        ) {
            if (task == null) {
                // 无紧急任务状态
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier.size(44.dp)
                            .background(
                                ColorProvider(
                                    day = Color(0xFF10B981),
                                    night = Color(0xCC065F46)
                                )
                            )
                            .cornerRadius(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✅", style = TextStyle(fontSize = 22.sp))
                    }
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Text(
                        "今日任务已清空",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFF065F46), night = Color(0xFF34D399)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        "继续保持！",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF)),
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                val timeText = task.startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "全天"
                val noteText = task.note

                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：紧急指示器
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = GlanceModifier.size(36.dp)
                                .background(
                                    ColorProvider(
                                        day = Color(0xFFEF4444),
                                        night = Color(0xFFDC2626)
                                    )
                                )
                                .cornerRadius(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", style = TextStyle(fontSize = 18.sp))
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            "紧急",
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFFEF4444), night = Color(0xFFF87171)),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(12.dp))

                    // 中间：任务信息
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = task.title,
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFF1E293B), night = Color.White),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (noteText?.isNotEmpty() == true) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = noteText,
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8)),
                                    fontSize = 11.sp
                                ),
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    // 右侧：时间信息
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(
                                    ColorProvider(
                                        day = Color(0xFFDBEAFE),
                                        night = Color(0xCC1E293B)
                                    )
                                )
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timeText,
                                style = TextStyle(
                                    color = ColorProvider(day = Color(0xFF1E40AF), night = Color(0xFF93C5FD)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            "点击处理",
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFF9CA3AF)),
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
