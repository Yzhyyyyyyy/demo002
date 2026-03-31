package com.example.demo002

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 月视图组件
 * @param selectedDate 当前选中的日期
 * @param tasks 所有任务列表
 * @param onDateSelected 当用户点击某个日期时的回调
 * @param onMonthChange 月份切换回调 (delta: -1 表示上个月, 1 表示下个月)
 */
@Composable
fun MonthView(
    selectedDate: LocalDate,
    tasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (Int) -> Unit = {}
) {
    val currentMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=周一, 7=周日
    val daysInMonth = currentMonth.lengthOfMonth()
    
    // 计算需要显示的日期列表（包括前一个月的空白占位符）
    val days = mutableListOf<DayItem>()
    
    // 添加前一个月的空白占位符
    val blankDays = if (firstDayOfWeek == 7) 6 else firstDayOfWeek - 1 // 转换为0-6（周一为0）
    repeat(blankDays) {
        days.add(DayItem.Empty)
    }
    
    // 添加当前月的日期
    for (day in 1..daysInMonth) {
        val date = currentMonth.atDay(day)
        val dayTasks = tasks.filter { it.dueDate == date }
        days.add(DayItem.Date(date, dayTasks))
    }
    
    // 添加后一个月的空白占位符，使总天数能被7整除
    val totalCells = 7 * 6 // 6行
    val remainingCells = totalCells - days.size
    repeat(remainingCells) {
        days.add(DayItem.Empty)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 月份导航栏
        MonthHeader(
            currentMonth = currentMonth,
            onPreviousMonth = { onMonthChange(-1) },
            onNextMonth = { onMonthChange(1) }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 星期标题
        WeekDaysHeader()
        
        Spacer(Modifier.height(8.dp))
        
        // 日历网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days) { dayItem ->
                DayCell(
                    dayItem = dayItem,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

/**
 * 月份导航栏
 */
@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "上个月",
                tint = Color(0xFF475569)
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.DateRange,
                contentDescription = null,
                tint = Color(0xFF1C1C1E),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
        }
        
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "下个月",
                tint = Color(0xFF475569)
            )
        }
    }
}

/**
 * 星期标题
 */
@Composable
private fun WeekDaysHeader() {
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekDays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 日期单元格
 */
@Composable
private fun DayCell(
    dayItem: DayItem,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    when (dayItem) {
        is DayItem.Date -> {
            val date = dayItem.date
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()
            val hasTasks = dayItem.tasks.isNotEmpty()
            
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .clickable { onDateSelected(date) }
                    .background(
                        if (isSelected) Color(0xFF1C1C1E)
                        else if (isToday) Color(0xFF7DD3FC).copy(alpha = 0.2f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> Color.White
                            isToday -> Color(0xFF1C1C1E)
                            else -> Color(0xFF475569)
                        }
                    )
                    
                    // 任务指示器
                    if (hasTasks) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else Color(0xFFFF6B6B)
                                )
                        )
                    }
                }
            }
        }
        DayItem.Empty -> {
            Box(
                modifier = Modifier.aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // 空白单元格，不显示任何内容
            }
        }
    }
}

/**
 * 日期项
 */
sealed class DayItem {
    data class Date(val date: LocalDate, val tasks: List<Task>) : DayItem()
    object Empty : DayItem()
}