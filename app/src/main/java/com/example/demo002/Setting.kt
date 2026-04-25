package com.example.demo002

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.demo002.ui.theme.Demo002Theme

data class SubTaskInput(
    val title : String  = "",
    val isDone: Boolean = false
)

data class RecurringSchedule(
    val id       : Int,
    val title    : String,
    val note     : String        = "",
    val priority : Priority      = Priority.MEDIUM,
    val tags     : List<TaskTag> = emptyList(),
    // val subTasks : List<SubTask> = emptyList(), // 暂时屏蔽子任务功能
    val weekDays : Set<Int>      = emptySet(),
    val isEnabled: Boolean       = true
)

@Composable
fun Setting(
    onContactAuthor    : () -> Unit = {},
    onNavigateBack     : () -> Unit = {},
    onNavigateToStats  : () -> Unit = {}          // ← 新增参数
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current

    val prefs = remember {
        context.getSharedPreferences("notify_prefs", android.content.Context.MODE_PRIVATE)
    }

    var notifyEnabled by remember { mutableStateOf(prefs.getBoolean("notify_enabled", false)) }
    var notifyHour    by remember { mutableStateOf(prefs.getInt("notify_hour", 8)) }
    var notifyMinute  by remember { mutableStateOf(prefs.getInt("notify_minute", 0)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            prefs.edit().putBoolean("notify_enabled", true).apply()
            notifyEnabled = true
            AlarmScheduler.schedule(context, notifyHour, notifyMinute)
        } else {
            prefs.edit().putBoolean("notify_enabled", false).apply()
            notifyEnabled = false
        }
    }

    fun enableNotifyWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                prefs.edit().putBoolean("notify_enabled", true).apply()
                notifyEnabled = true
                AlarmScheduler.schedule(context, notifyHour, notifyMinute)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            prefs.edit().putBoolean("notify_enabled", true).apply()
            notifyEnabled = true
            AlarmScheduler.schedule(context, notifyHour, notifyMinute)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyFirstScreen()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text          = "设置",
                        fontSize      = 28.sp,
                        fontWeight    = FontWeight.Black,
                        color         = Color(0xFF1C1C1E),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text       = "个性化你的日程体验",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color(0xFF94A3B8)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ════════════════════════════
                //  数据统计（新增）
                // ════════════════════════════
                SettingSection(title = "数据") {
                    SettingNavRow(
                        icon    = Icons.Rounded.DateRange,
                        iconBg  = Color(0xFF818CF8),
                        title   = "数据统计",
                        subtitle = "完成率、趋势、标签排行",
                        onClick  = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToStats()
                        }
                    )
                }

                // ════════════════════════════
                //  通知设置
                // ════════════════════════════
                SettingSection(title = "通知") {
                    SettingToggleRow(
                        icon            = Icons.Rounded.Notifications,
                        iconBg          = Color(0xFF7DD3FC),
                        title           = "每日推送",
                        subtitle        = "每天定时发送日程完成情况",
                        checked         = notifyEnabled,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (checked) {
                                enableNotifyWithPermission()
                            } else {
                                prefs.edit().putBoolean("notify_enabled", false).apply()
                                notifyEnabled = false
                                AlarmScheduler.cancel(context)
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = notifyEnabled,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color    = Color(0xFFF1F5F9)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text          = "推送时间",
                                fontSize      = 12.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color(0xFF94A3B8),
                                letterSpacing = 1.sp,
                                modifier      = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                TimePickerColumn(
                                    value         = notifyHour,
                                    range         = 0..23,
                                    onValueChange = { newVal ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        notifyHour = newVal
                                        prefs.edit().putInt("notify_hour", newVal).apply()
                                        if (notifyEnabled) {
                                            AlarmScheduler.schedule(context, newVal, notifyMinute)
                                            DailyNotificationReceiver.sendTimeChangedNotification(
                                                context, newVal, notifyMinute
                                            )
                                        }
                                    },
                                    label = "时"
                                )
                                Text(
                                    text       = ":",
                                    fontSize   = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color      = Color(0xFF1C1C1E),
                                    modifier   = Modifier.padding(horizontal = 12.dp)
                                )
                                TimePickerColumn(
                                    value         = notifyMinute,
                                    range         = 0..59,
                                    onValueChange = { newVal ->
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        notifyMinute = newVal
                                        prefs.edit().putInt("notify_minute", newVal).apply()
                                        if (notifyEnabled) {
                                            AlarmScheduler.schedule(context, notifyHour, newVal)
                                            DailyNotificationReceiver.sendTimeChangedNotification(
                                                context, notifyHour, newVal
                                            )
                                        }
                                    },
                                    label = "分"
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }

                // ════════════════════════════
                //  关于 / 联系作者
                // ════════════════════════════
                SettingSection(title = "关于") {
                    ContactAuthorRow(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onContactAuthor()
                        }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════
//  新增：导航行（带箭头，点击跳转）
// ══════════════════════════════════════════════

@Composable
fun SettingNavRow(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    iconBg  : Color,
    title   : String,
    subtitle: String,
    onClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconBg,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text       = title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1C1C1E)
                )
                Text(
                    text     = subtitle,
                    fontSize = 11.sp,
                    color    = Color(0xFF94A3B8)
                )
            }
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint               = Color(0xFFCBD5E1),
            modifier           = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ContactAuthorRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFB923C).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint               = Color(0xFFFB923C),
                    modifier           = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text       = "联系作者",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFF1C1C1E)
                )
                Text(
                    text     = "反馈建议 · 功能请求 · 打个招呼",
                    fontSize = 11.sp,
                    color    = Color(0xFF94A3B8)
                )
            }
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint               = Color(0xFFCBD5E1),
            modifier           = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingSection(
    title          : String,
    trailingContent: @Composable (() -> Unit)? = null,
    content        : @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = title,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF94A3B8),
                letterSpacing = 2.sp
            )
            trailingContent?.invoke()
        }
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
                .background(Color.White.copy(alpha = 0.92f))
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun SettingToggleRow(
    icon           : ImageVector,
    iconBg         : Color,
    title          : String,
    subtitle       : String,
    checked        : Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled        : Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconBg,
                    modifier           = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text       = title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (enabled) Color(0xFF1C1C1E) else Color(0xFFB0BEC5)
                )
                Text(
                    text     = subtitle,
                    fontSize = 11.sp,
                    color    = Color(0xFF94A3B8)
                )
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            enabled         = enabled,
            colors          = SwitchDefaults.colors(
                checkedThumbColor           = Color.White,
                checkedTrackColor           = Color(0xFF1C1C1E),
                uncheckedThumbColor         = Color.White,
                uncheckedTrackColor         = Color(0xFFE2E8F0),
                uncheckedBorderColor        = Color(0xFFE2E8F0),
                disabledCheckedTrackColor   = Color(0xFF94A3B8),
                disabledUncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}

@Composable
fun TimePickerColumn(
    value        : Int,
    range        : IntRange,
    onValueChange: (Int) -> Unit,
    label        : String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick  = { onValueChange(if (value >= range.last) range.first else value + 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector        = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint               = Color(0xFF475569),
                modifier           = Modifier.size(22.dp)
            )
        }
        Text(
            text       = "%02d".format(value),
            fontSize   = 36.sp,
            fontWeight = FontWeight.Black,
            color      = Color(0xFF1C1C1E)
        )
        Text(
            text     = label,
            fontSize = 11.sp,
            color    = Color(0xFF94A3B8)
        )
        IconButton(
            onClick  = { onValueChange(if (value <= range.first) range.last else value - 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector        = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint               = Color(0xFF475569),
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingPreview() {
    Demo002Theme {
        Setting()
    }
}