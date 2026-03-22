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
                onNavigateToDetail   = { taskId -> navController.navigate("task_detail/$taskId") }
            )
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
    }
}