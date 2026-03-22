package com.example.demo002

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter

// ══════════════════════════════════════════════
//  统计主界面
// ══════════════════════════════════════════════

@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val vm: StatisticsViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        MyFirstScreen()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── 顶部栏：标题靠左，无返回按钮 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.Start          // ✅ 靠左
            ) {
                Text(
                    "数据统计",
                    fontSize      = 28.sp,
                    fontWeight    = FontWeight.Black,
                    color         = Color(0xFF1C1C1E),
                    letterSpacing = 0.5.sp
                )
                Text(
                    "回顾你的任务历程",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color(0xFF94A3B8)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                StreakAndOverviewRow(state)
                CompletionRateSection(state)
                TrendChartSection(state.last14Days)
                PriorityDistributionSection(state.priorityStats)
                if (state.tagStats.isNotEmpty()) {
                    TagRankingSection(state.tagStats)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════
//  连续打卡 + 总览  ✅ 右侧两格填满高度
// ══════════════════════════════════════════════

@Composable
fun StreakAndOverviewRow(state: StatisticsUiState) {
    // IntrinsicSize.Min 让 Row 高度由最高子项决定，右侧两格再 fillMaxHeight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),               // ✅ 关键：统一行高
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 左：火焰卡片
        StatCard(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),                     // ✅ 撑满行高
            bgColor  = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center  // 垂直居中
            ) {
                Text("🔥", fontSize = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${state.streakDays}",
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color.White
                )
                Text(
                    "连续完成天数",
                    fontSize  = 11.sp,
                    color     = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 右：累计创建 + 累计完成，各占一半高度
        Column(
            modifier            = Modifier
                .weight(1f)
                .fillMaxHeight(),                     // ✅ 撑满行高
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 累计创建
            StatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),                      // ✅ 各占一半
                bgColor  = Color(0xFF7DD3FC).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "${state.totalCreated}",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Black,
                            color      = Color(0xFF1C1C1E)
                        )
                        Text("累计创建", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
            // 累计完成
            StatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),                      // ✅ 各占一半
                bgColor  = Color(0xFF34D399).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✅", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "${state.totalCompleted}",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Black,
                            color      = Color(0xFF1C1C1E)
                        )
                        Text("累计完成", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  完成率三格
// ══════════════════════════════════════════════

@Composable
fun CompletionRateSection(state: StatisticsUiState) {
    StatsSection(title = "完成率") {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RateCircle(label = "今日", rate = state.todayRate,
                done = state.todayDone, total = state.todayTotal,
                color = Color(0xFF7DD3FC))
            RateCircle(label = "本周", rate = state.weekRate,
                done = state.weekDone, total = state.weekTotal,
                color = Color(0xFF818CF8))
            RateCircle(label = "本月", rate = state.monthRate,
                done = state.monthDone, total = state.monthTotal,
                color = Color(0xFF34D399))
        }
    }
}

@Composable
fun RateCircle(
    label: String,
    rate : Float,
    done : Int,
    total: Int,
    color: Color,
    size : Dp = 80.dp
) {
    val animatedRate by animateFloatAsState(
        targetValue   = rate,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label         = "rate_$label"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier         = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke  = 8.dp.toPx()
                val inset   = stroke / 2
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color      = Color(0xFFE2E8F0),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color      = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedRate,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(animatedRate * 100).toInt()}%",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF1C1C1E)
                )
                Text(
                    "$done/$total",
                    fontSize = 10.sp,
                    color    = Color(0xFF94A3B8)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF64748B)
        )
    }
}

// ══════════════════════════════════════════════
//  近14天趋势折线图
// ══════════════════════════════════════════════

@Composable
fun TrendChartSection(days: List<DailyStats>) {
    StatsSection(title = "近14天趋势") {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier              = Modifier.padding(bottom = 12.dp)
            ) {
                LegendDot(color = Color(0xFF818CF8), label = "创建")
                LegendDot(color = Color(0xFF34D399), label = "完成")
            }

            val maxVal = (days.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
            val animProgress by animateFloatAsState(
                targetValue   = 1f,
                animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
                label         = "chart_anim"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (days.isEmpty()) return@Canvas
                val w        = size.width
                val h        = size.height
                val padLeft  = 8.dp.toPx()
                val padRight = 8.dp.toPx()
                val padTop   = 8.dp.toPx()
                val padBot   = 24.dp.toPx()
                val chartW   = w - padLeft - padRight
                val chartH   = h - padTop - padBot
                val step     = chartW / (days.size - 1).coerceAtLeast(1)

                fun xOf(i: Int) = padLeft + i * step
                fun yOf(v: Int) = padTop + chartH * (1f - v.toFloat() / maxVal)

                for (i in 0..3) {
                    val y = padTop + chartH * i / 3f
                    drawLine(
                        color       = Color(0xFFE2E8F0),
                        start       = Offset(padLeft, y),
                        end         = Offset(w - padRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun drawSeries(values: List<Int>, seriesColor: Color) {
                    val path        = Path()
                    val totalPoints = ((days.size - 1) * animProgress)
                        .toInt().coerceIn(0, days.size - 1)
                    days.take(totalPoints + 1).forEachIndexed { i, _ ->
                        val x = xOf(i); val y = yOf(values[i])
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = seriesColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                    days.take(totalPoints + 1).forEachIndexed { i, _ ->
                        if (values[i] > 0) {
                            drawCircle(seriesColor, radius = 4.dp.toPx(),
                                center = Offset(xOf(i), yOf(values[i])))
                            drawCircle(Color.White, radius = 2.dp.toPx(),
                                center = Offset(xOf(i), yOf(values[i])))
                        }
                    }
                }

                drawSeries(days.map { it.total }, Color(0xFF818CF8))
                drawSeries(days.map { it.done },  Color(0xFF34D399))
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val fmt = DateTimeFormatter.ofPattern("M/d")
                listOf(0, 6, 13).forEach { idx ->
                    Text(
                        days.getOrNull(idx)?.date?.format(fmt) ?: "",
                        fontSize = 10.sp,
                        color    = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
    }
}

// ══════════════════════════════════════════════
//  优先级分布
// ══════════════════════════════════════════════

@Composable
fun PriorityDistributionSection(priorityStats: Map<Priority, Int>) {
    val total = priorityStats.values.sum().coerceAtLeast(1)
    StatsSection(title = "优先级分布") {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Priority.entries.forEach { p ->
                val count = priorityStats[p] ?: 0
                val ratio = count.toFloat() / total
                val animRatio by animateFloatAsState(
                    targetValue   = ratio,
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label         = "priority_${p.name}"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(p.color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        p.label,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFF475569),
                        modifier   = Modifier.width(32.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animRatio)
                                .clip(RoundedCornerShape(4.dp))
                                .background(p.color)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$count",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF475569),
                        modifier   = Modifier.width(24.dp),
                        textAlign  = TextAlign.End
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  标签排行
// ══════════════════════════════════════════════

@Composable
fun TagRankingSection(tagStats: List<TagStats>) {
    val maxCount = tagStats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    StatsSection(title = "标签使用排行") {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tagStats.forEachIndexed { index, ts ->
                val animRatio by animateFloatAsState(
                    targetValue   = ts.count.toFloat() / maxCount,
                    animationSpec = tween(800, delayMillis = index * 60, easing = EaseOutCubic),
                    label         = "tag_${ts.tag.label}"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${index + 1}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (index < 3) Color(0xFFFFB347) else Color(0xFFCBD5E1),
                        modifier   = Modifier.width(16.dp),
                        textAlign  = TextAlign.Center
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ts.tag.color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            ts.tag.label,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = ts.tag.color
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animRatio)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ts.tag.color)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${ts.count}次",
                        fontSize   = 11.sp,
                        color      = Color(0xFF94A3B8),
                        modifier   = Modifier.width(32.dp),
                        textAlign  = TextAlign.End
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
//  通用卡片容器
// ══════════════════════════════════════════════

@Composable
fun StatsSection(
    title  : String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.Bold,
            color         = Color(0xFF94A3B8),
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation    = 4.dp,
                    shape        = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFF94A3B8).copy(alpha = 0.08f),
                    spotColor    = Color(0xFF94A3B8).copy(alpha = 0.06f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.92f)),
            content = content
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    bgColor : Color    = Color.White,
    content : @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation    = 4.dp,
                shape        = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF94A3B8).copy(alpha = 0.08f),
                spotColor    = Color(0xFF94A3B8).copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        content             = content
    )
}