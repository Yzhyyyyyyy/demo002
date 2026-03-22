package com.example.demo002

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun Searching(
    onNavigateToDetail: (Int) -> Unit = {}
) {
    val viewModel: TaskViewModel = viewModel()
    val query   by viewModel.searchQuery.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    // 【触感①】搜索结果从无到有时给一次轻触反馈
    val prevResultCount = remember { mutableIntStateOf(0) }
    LaunchedEffect(results.size) {
        if (results.isNotEmpty() && prevResultCount.intValue == 0 && query.isNotBlank()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        prevResultCount.intValue = results.size
    }

    Box(modifier = Modifier.fillMaxSize()) {

        MyFirstScreen()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(Modifier.height(20.dp))

            // ── 标题区 ──
            Text(
                text          = "搜索",
                fontSize      = 30.sp,
                fontWeight    = FontWeight.Black,
                color         = Color(0xFF1C1C1E),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (query.isBlank()) "查找你的任务"
                else if (results.isEmpty()) "未找到匹配结果"
                else "找到 ${results.size} 条结果",
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Medium,
                color         = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(20.dp))

            // ── 搜索框 ──
            OutlinedTextField(
                value         = query,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder   = {
                    Text(
                        "标题 / 备注",
                        fontSize = 14.sp,
                        color    = Color(0xFFCBD5E1)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint     = if (query.isNotBlank()) Color(0xFF1C1C1E)
                        else Color(0xFFB0BEC5),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = {
                                // 【触感②】清空搜索内容 - 中等强度（清除动作）
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onSearchQueryChange("")
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "清空",
                                tint     = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier   = Modifier.fillMaxWidth(),
                shape      = RoundedCornerShape(16.dp),
                singleLine = true,
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Color(0xFF1C1C1E),
                    unfocusedBorderColor    = Color(0xFFE2E8F0),
                    focusedContainerColor   = Color.White.copy(alpha = 0.92f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.70f),
                    cursorColor             = Color(0xFF1C1C1E),
                    focusedTextColor        = Color(0xFF1C1C1E),
                    unfocusedTextColor      = Color(0xFF1C1C1E)
                )
            )

            Spacer(Modifier.height(24.dp))

            // ── 内容区 ──
            when {

                query.isBlank() -> SearchEmptyHint()

                results.isEmpty() -> SearchNoResultHint(query)

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding      = PaddingValues(bottom = 40.dp)
                    ) {
                        items(results, key = { it.id }) { task ->
                            SwipeableTaskCard(
                                task       = task,
                                onComplete = { viewModel.toggleDone(task) },
                                onDelete   = { viewModel.deleteTask(task.id) },
                                onEdit     = { viewModel.updateTask(task) },
                                onTap      = {
                                    // 【触感③】点击搜索结果跳转详情 - 轻触
                                    // 注：SwipeableTaskCard 内部的 onTap 已有触感
                                    // 此处由 TaskCard 内部的 clickable 触感覆盖，无需重复
                                    onNavigateToDetail(task.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 未输入状态 ──
@Composable
private fun SearchEmptyHint() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFE2E8F0))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text          = "输入关键词",
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF1C1C1E),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "支持按标题或备注模糊搜索",
                fontSize = 13.sp,
                color    = Color(0xFFB0BEC5)
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}

// ── 无结果状态 ──
@Composable
private fun SearchNoResultHint(query: String) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFE2E8F0))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text          = "「$query」",
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF1C1C1E),
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "没有找到相关任务",
                fontSize = 13.sp,
                color    = Color(0xFFB0BEC5)
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}