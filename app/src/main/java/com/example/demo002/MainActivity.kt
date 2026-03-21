package com.example.demo002

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demo002.ui.theme.Demo002Theme

object Routes {
    const val SPLASH         = "splash"
    const val HOME           = "home"
    const val SCHEDULE       = "schedule"
    const val SEARCHING      = "searching"
    const val SETTING        = "setting"
    const val HELP_DEVELOPER = "help_developer"
    const val TASK_DETAIL    = "task_detail/{taskId}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeState.init(this)
        // ── 1. 创建通知渠道（和 DailyNotificationReceiver 里的 CHANNEL_ID 保持一致）──
        val channel = NotificationChannel(
            "daily_schedule_channel",
            "每日日程提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每天定时推送当日任务完成情况"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        // ── 2. 恢复闹钟（App 更新 / 冷启动后重新注册，防止丢失）──
        val prefs   = getSharedPreferences("notify_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("notify_enabled", true)
        if (enabled) {
            AlarmScheduler.schedule(
                context = this,
                hour    = prefs.getInt("notify_hour", 8),
                minute  = prefs.getInt("notify_minute", 0)
            )
        }

        setContent {
            Demo002Theme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Routes.SPLASH
    ) {

        // 开场动画页
        composable(Routes.SPLASH) {
            StartAnimationScreen(
                onSplashFinished = {
                    navController.navigate(Routes.SCHEDULE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // 主页
        composable(Routes.HOME) {
            MyFirstScreen(
                onStartClick = {
                    navController.navigate(Routes.SCHEDULE)
                }
            )
        }

        // Schedule 页
        composable(Routes.SCHEDULE) {
            Schedule(
                onNavigateToSearch   = { navController.navigate(Routes.SEARCHING) },
                onNavigateToSettings = { navController.navigate(Routes.SETTING) },
                onNavigateToDetail   = { taskId ->
                    navController.navigate("task_detail/$taskId")
                }
            )
        }

        // Searching 页
        composable(Routes.SEARCHING) {
            Searching()
        }

        // Setting 页
        composable(Routes.SETTING) {
            Setting(
                onContactAuthor = {
                    navController.navigate(Routes.HELP_DEVELOPER)
                }
            )
        }

        // HelpDeveloper 页
        composable(Routes.HELP_DEVELOPER) {
            HelpDeveloper(
                onBack = { navController.popBackStack() }
            )
        }

        // Task 详情页
        composable(Routes.TASK_DETAIL) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
                ?: return@composable
            TaskDetailScreen(
                taskId         = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}