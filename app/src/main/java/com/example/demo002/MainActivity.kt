package com.example.demo002

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    const val STATISTICS     = "statistics"          // ← 新增
    const val QUADRANT       = "quadrant"            // ← 新增：四象限视图
}

private const val ANIM_DURATION = 380

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channel = NotificationChannel(
            "daily_schedule_channel",
            "每日日程提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "每天定时推送当日任务完成情况"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val prefs   = getSharedPreferences("notify_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("notify_enabled", false)
        if (enabled) {
            AlarmScheduler.schedule(
                context = this,
                hour    = prefs.getInt("notify_hour", 8),
                minute  = prefs.getInt("notify_minute", 0)
            )
        }

        // 获取Intent参数：是否打开添加任务页面
        val openAddTask = intent.getBooleanExtra("open_add_task", false)

        setContent {
            Demo002Theme {
                AppNavigation(openAddTask = openAddTask)
            }
        }
    }
}

@Composable
fun AppNavigation(openAddTask: Boolean = false) {
    val navController = rememberNavController()
    
    // 用于跟踪是否已经处理过openAddTask，避免重复触发
    val hasHandledOpenAddTask = remember { mutableStateOf(false) }
    // 用于传递给Schedule的参数：是否显示添加对话框
    val shouldShowAddDialog = remember { mutableStateOf(false) }
    
    // 如果openAddTask为true，导航到添加任务页面
    LaunchedEffect(openAddTask) {
        if (openAddTask && !hasHandledOpenAddTask.value) {
            hasHandledOpenAddTask.value = true
            shouldShowAddDialog.value = true
            // 导航到SCHEDULE页面
            navController.navigate(Routes.SCHEDULE) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    NavHost(
        navController      = navController,
        startDestination   = Routes.SPLASH,
        enterTransition    = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
        exitTransition     = { fadeOut(animationSpec = tween(ANIM_DURATION)) },
        popEnterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
        popExitTransition  = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
    ) {

        composable(
            route           = Routes.SPLASH,
            enterTransition = { fadeIn(animationSpec = tween(600)) },
            exitTransition  = { fadeOut(animationSpec = tween(400)) }
        ) {
            StartAnimationScreen(
                onSplashFinished = {
                    navController.navigate(Routes.SCHEDULE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route           = Routes.HOME,
            enterTransition = { fadeIn(animationSpec = tween(ANIM_DURATION)) },
            exitTransition  = { fadeOut(animationSpec = tween(ANIM_DURATION)) }
        ) {
            MyFirstScreen(
                onStartClick = { navController.navigate(Routes.SCHEDULE) }
            )
        }

        composable(
            route              = Routes.SCHEDULE,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) {
            Schedule(
                onNavigateToSearch   = { navController.navigate(Routes.SEARCHING) },
                onNavigateToSettings = { navController.navigate(Routes.SETTING) },
                onNavigateToDetail   = { taskId -> navController.navigate("task_detail/$taskId") },
                onNavigateToQuadrant = { navController.navigate(Routes.QUADRANT) }, // 新增：导航到四象限视图
                initialShowAddDialog = shouldShowAddDialog.value
            )
            // 重置标志，避免重复显示
            LaunchedEffect(shouldShowAddDialog.value) {
                if (shouldShowAddDialog.value) {
                    shouldShowAddDialog.value = false
                }
            }
        }

        composable(
            route              = Routes.SEARCHING,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) {
            Searching()
        }

        composable(
            route              = Routes.SETTING,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) {
            Setting(
                onContactAuthor   = { navController.navigate(Routes.HELP_DEVELOPER) },
                onNavigateToStats = { navController.navigate(Routes.STATISTICS) }  // ← 新增
            )
        }

        composable(
            route              = Routes.HELP_DEVELOPER,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) {
            HelpDeveloper(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route              = Routes.TASK_DETAIL,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
                ?: return@composable
            TaskDetailScreen(
                taskId         = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── 统计页（新增）──
        composable(
            route              = Routes.STATISTICS,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── 四象限视图（新增）──
        composable(
            route              = Routes.QUADRANT,
            enterTransition    = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition     = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(ANIM_DURATION)) +
                        fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            popExitTransition  = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(ANIM_DURATION)) +
                        fadeOut(animationSpec = tween(ANIM_DURATION))
            }
        ) {
            QuadrantScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}