package com.example.demo002

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demo002.ui.theme.Demo002Theme

object Routes {
    const val SPLASH      = "splash"
    const val HOME        = "home"
    const val SCHEDULE    = "schedule"
    const val SEARCHING   = "searching"
    const val SETTING     = "setting"
    const val TASK_DETAIL = "task_detail/{taskId}"  // ← 新增
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Setting()
        }

        // Task 详情页 ← 新增
        composable(Routes.TASK_DETAIL) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: return@composable
            TaskDetailScreen(
                taskId      = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}