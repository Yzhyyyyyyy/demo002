package com.example.demo002

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 紧急任务小组件接收器
 */
class UrgentTaskWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = UrgentTaskWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 可以在这里执行额外的更新逻辑
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // 第一个小组件被添加到桌面时调用
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // 最后一个小组件从桌面移除时调用
    }
}