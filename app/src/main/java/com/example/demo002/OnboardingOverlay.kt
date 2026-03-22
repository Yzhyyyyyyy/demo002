package com.example.demo002

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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

// ══════════════════════════════════════════════
//  引导步骤数据
// ══════════════════════════════════════════════

private data class OnboardingStep(
    val emoji    : String,
    val title    : String,
    val desc     : String,
    val highlight: HighlightArea? = null
)

data class HighlightArea(
    val topFraction   : Float,
    val bottomFraction: Float,
    val horizontalPad : Float = 16f
)

private val ONBOARDING_STEPS = listOf(
    OnboardingStep(
        emoji     = "👆",
        title     = "切换日期",
        desc      = "点击上方日期格子选择当天\n左右箭头可以切换上/下一周\n点击月份标签可跳转到任意日期",
        highlight = HighlightArea(0.13f, 0.27f)
    ),
    OnboardingStep(
        emoji     = "👉",
        title     = "右滑完成任务",
        desc      = "将任务卡片向右滑动\n滑过阈值后松手即可标记完成 ✅\n已完成的任务右滑可撤销",
        highlight = HighlightArea(0.28f, 0.52f)
    ),
    OnboardingStep(
        emoji     = "👈",
        title     = "左滑删除任务",
        desc      = "将任务卡片向左滑动\n滑过阈值后松手即可删除任务 🗑️\n删除后无法恢复，请谨慎操作",
        highlight = HighlightArea(0.28f, 0.52f)
    ),
    OnboardingStep(
        emoji     = "🔍",
        title     = "搜索与设置",
        desc      = "右上角搜索按钮可以快速找到任务\n设置按钮可以开启每日通知提醒",
        highlight = HighlightArea(0.04f, 0.13f, horizontalPad = 220f)
    ),
    OnboardingStep(
        emoji     = "➕",
        title     = "添加新任务",
        desc      = "点击右下角 + 按钮添加任务\n支持设置优先级、标签、截止日期\n还可以创建按星期重复的长期任务",
        highlight = HighlightArea(0.88f, 0.98f, horizontalPad = 280f)
    )
)

// ══════════════════════════════════════════════
//  引导覆盖层入口
// ══════════════════════════════════════════════

@Composable
fun OnboardingOverlay(onFinished: () -> Unit) {
    // ✅ 修复4：mutableStateOf(Int) → mutableIntStateOf
    var currentStep by remember { mutableIntStateOf(0) }
    val step       = ONBOARDING_STEPS[currentStep]
    val totalSteps = ONBOARDING_STEPS.size
    val haptic     = LocalHapticFeedback.current

    val overlayAlpha by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(400),
        label         = "overlay_alpha"
    )

    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(400)),
        exit    = fadeOut(tween(300))
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight
            // ✅ 修复2&3：删除从未使用的 screenWidth，同时消除问题1

            // ── 半透明遮罩 ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000).copy(alpha = 0.72f * overlayAlpha))
            )

            // ── 高亮框 ──
            step.highlight?.let { hl ->
                val topDp    = screenHeight * hl.topFraction
                val bottomDp = screenHeight * hl.bottomFraction
                val hPad     = hl.horizontalPad.dp

                // 边框
                Box(
                    modifier = Modifier
                        .padding(start = hPad, end = hPad, top = topDp)
                        .height(bottomDp - topDp)
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(18.dp)
                        )
                )

                // 内部透出效果
                Box(
                    modifier = Modifier
                        .padding(start = hPad, end = hPad, top = topDp)
                        .height(bottomDp - topDp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }

            // ── 说明卡片（固定在屏幕中下方）──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 48.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 步骤指示点
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(totalSteps) { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == currentStep) 20.dp else 6.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (idx == currentStep) Color(0xFF1C1C1E)
                                    else Color(0xFFE2E8F0)
                                )
                        )
                    }
                }

                // Emoji
                Text(text = step.emoji, fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))

                // 标题
                Text(
                    text       = step.title,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color(0xFF1C1C1E)
                )
                Spacer(Modifier.height(10.dp))

                // 描述
                Text(
                    text       = step.desc,
                    fontSize   = 14.sp,
                    color      = Color(0xFF64748B),
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(24.dp))

                // 按钮行
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // 上一步（第一步时隐藏）
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentStep--
                            },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("上一步", color = Color(0xFF64748B))
                        }
                    }

                    // 下一步 / 知道了
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                onFinished()
                            }
                        },
                        // ✅ 修复：去掉无意义的 if/else，直接 weight(1f)
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C1C1E)
                        )
                    ) {
                        Text(
                            if (currentStep < totalSteps - 1) "下一步" else "知道了 🎉",
                            color = Color.White
                        )
                        if (currentStep < totalSteps - 1) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 跳过按钮
                if (currentStep < totalSteps - 1) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFinished()
                    }) {
                        Text(
                            "跳过引导",
                            fontSize = 13.sp,
                            color    = Color(0xFFB0BEC5)
                        )
                    }
                }
            }
        }
    }
}