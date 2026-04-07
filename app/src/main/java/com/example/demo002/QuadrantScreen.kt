package com.example.demo002

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuadrantScreen(
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: TaskViewModel = viewModel()
    val quadrantData by viewModel.quadrantData.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 使用与主界面相同的背景
        MyFirstScreen()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding() // 修复底部被系统导航条遮挡的问题
                .padding(horizontal = 20.dp) // 增大左右边距
        ) {
            // 标题区 - 左对齐，参考搜索界面和设置界面
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "四象限规划",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "高效安排您的日程",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }

            // 2x2 田字格布局
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(bottom = 24.dp) // 底部留白，让卡片不贴底
            ) {
                // 第一行：Q1 和 Q2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Q1: 重要且紧急 (左上)
                    QuadrantCard(
                        title = "重要且紧急",
                        taskCount = quadrantData.q1.size,
                        tasks = quadrantData.q1,
                        backgroundColor = Color(0xFFFF6B6B).copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp)) // 增大列间距
                    
                    // Q2: 重要不紧急 (右上)
                    QuadrantCard(
                        title = "重要不紧急",
                        taskCount = quadrantData.q2.size,
                        tasks = quadrantData.q2,
                        backgroundColor = Color(0xFFFFB347).copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp)) // 增大气行间距
                
                // 第二行：Q3 和 Q4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Q3: 紧急不重要 (左下)
                    QuadrantCard(
                        title = "紧急不重要",
                        taskCount = quadrantData.q3.size,
                        tasks = quadrantData.q3,
                        backgroundColor = Color(0xFF7DD3FC).copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp)) // 增大列间距
                    
                    // Q4: 不紧急不重要 (右下)
                    QuadrantCard(
                        title = "不紧急不重要",
                        taskCount = quadrantData.q4.size,
                        tasks = quadrantData.q4,
                        backgroundColor = Color(0xFF94A3B8).copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuadrantCard(
    title: String,
    taskCount: Int,
    tasks: List<Task>,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp), // 更圆润的边角
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) // 添加微弱的白边，打造毛玻璃质感
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // 增加内部边距
        ) {
            // 标题和任务数量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 任务数量做成小胶囊样式
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$taskCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 任务列表
            if (tasks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp), // 增大任务卡片之间的间距
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(tasks) { task ->
                        TaskItem(task = task)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无任务",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.25f)) // 稍微提高一点透明度
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)) // 增加细边框
            .padding(horizontal = 12.dp, vertical = 12.dp) // 增加内部呼吸感
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 任务标题
            Text(
                text = task.title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            
            // 截止日期（如果有）
            task.dueDate?.let { dueDate ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "截止: ${dueDate.format(DateTimeFormatter.ofPattern("MM/dd"))}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun QuadrantScreenPreview() {
    QuadrantScreen()
}
