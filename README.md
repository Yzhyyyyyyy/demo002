# 📅 极简日程 (Schedule App)

基于 Jetpack Compose 与现代 Android 架构打造的轻量级、高颜值的日程与任务管理应用。不仅帮助你记录待办事项，更通过“四象限工作法”和直观的数据统计，助你高效规划每一天。

## ✨ 核心功能 (Features)

*   **🎨 全面拥抱 Compose**：100% 使用 Jetpack Compose 构建 UI，包含流畅的页面切换动画与丝滑的交互体验。
*   **📊 四象限工作法**：内置艾森豪威尔矩阵（四象限视图），帮助你按“重要”与“紧急”程度对任务进行分类，告别瞎忙。
*   **📱 桌面小组件 (App Widgets)**：基于最新的 **Jetpack Glance** 框架开发。
    *   **今日日程小组件**：在桌面直观查看今天的所有任务。
    *   **紧急任务小组件**：聚焦最重要的事情。
    *   *支持在桌面直接点击 Checkbox 完成任务，无需打开 App！*
*   **⏰ 智能提醒系统**：
    *   **每日早晨播报**：通过 `AlarmManager` 配合 `Notification`，每天定时推送今日日程概览。
    *   **任务准时提醒**：支持为单个任务设置提前提醒（如提前 10 分钟），再也不会错过重要会议。
*   **📈 详细的数据统计**：自动追踪你的任务完成率（日/周/月），并通过自定义 `Canvas` 绘制精美的图表，让你的进步肉眼可见。
*   **🔍 全局搜索与标签**：支持按标题、备注、地点进行模糊搜索，并支持为任务添加自定义标签。
*   **💾 本地优先**：使用 `Room` 数据库进行本地持久化存储，保护隐私，离线可用。

---

## 🧭 核心代码导航 (Code Navigation)

想要快速了解本项目的代码实现？点击下方链接直接跳转到对应的核心界面源码：

### 🖼️ UI 界面 (Compose)
*   [🏠 **MainActivity.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/MainActivity.kt) - 应用主入口与 Navigation 路由配置。
*   [✅ **Schedule.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/Schedule.kt) - 核心日程列表页，包含任务的增删改查交互。
*   [🗂️ **QuadrantScreen.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/QuadrantScreen.kt) - 四象限工作法视图实现。
*   [📅 **MonthView.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/MonthView.kt) - 自定义日历视图组件。
*   [📊 **StatisticsScreen.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/StatisticsScreen.kt) - 数据统计页，包含 Canvas 绘制的精美图表。

### 🧩 桌面小组件 (Jetpack Glance)
*   [📱 **TodayScheduleWidget.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/TodayScheduleWidget.kt) - 今日日程小组件 UI 及其交互逻辑。
*   [⚡ **ToggleTaskCompletionAction.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/ToggleTaskCompletionAction.kt) - 小组件点击完成任务的后台处理逻辑。

### 🗄️ 数据与架构 (Architecture)
*   [🧠 **TaskViewModel.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/TaskViewModel.kt) - 核心 ViewModel，负责处理 UI 状态与业务逻辑。
*   [💾 **AppDatabase.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/AppDatabase.kt) - Room 数据库配置与版本迁移。
*   [🔔 **TaskReminderManager.kt**](https://github.com/Yzhyyyyyyy/demo002/blob/main/app/src/main/java/com/example/demo002/TaskReminderManager.kt) - 任务闹钟与系统通知管理。

---

## 🛠️ 技术栈 (Tech Stack)

*   **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **架构**: MVVM (Model-View-ViewModel) + 单向数据流 (UDF)
*   **本地数据库**: [Room](https://developer.android.com/training/data-storage/room) (配合 Kotlin Coroutines & Flow 实现响应式数据流)
*   **页面路由**: Navigation Compose
*   **桌面小组件**: [Jetpack Glance](https://developer.android.com/jetpack/compose/glance)
*   **后台任务与通知**: `AlarmManager`, `BroadcastReceiver`, `NotificationManager`
*   **异步编程**: Kotlin Coroutines (协程)

## 🚀 如何运行与使用 (How to Use)

### 环境要求
*   Android Studio Iguana | 2023.2.1 或更高版本
*   JDK 11+
*   Android SDK 26 (Android 8.0) 及以上

### 编译运行步骤
1.  **克隆项目**到本地：
    ```bash
    git clone https://github.com/Yzhyyyyyyy/demo002.git
    ```
2.  使用 Android Studio 打开项目文件夹。
3.  等待 Gradle 同步完成（可能需要下载相关的依赖包，如 Room、Glance 等）。
4.  点击顶部的 **Run 'app'** 按钮，在模拟器或真机上运行。

## 🤝 贡献与反馈
如果你在学习 Compose 或 Glance 的过程中有任何想法，或者发现了 Bug，欢迎提交 [Issue](https://github.com/Yzhyyyyyyy/demo002/issues) 或 Pull Request！
