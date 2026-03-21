package com.example.demo002

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 全局主题状态单例
 * 任何 Composable 读取这里的值都会在变化时自动重组
 * 状态变化时自动持久化到 SharedPreferences
 */
object ThemeState {
    private var _followSystem by mutableStateOf(true)
    private var _darkMode     by mutableStateOf(false)

    // ── 对外暴露的 getter/setter ──
    var followSystem: Boolean
        get() = _followSystem
        set(value) {
            _followSystem = value
            saveToPrefs()
        }

    var darkMode: Boolean
        get() = _darkMode
        set(value) {
            _darkMode = value
            saveToPrefs()
        }

    // ── 持久化相关 ──
    private var prefs: android.content.SharedPreferences? = null

    /**
     * 初始化（在 MainActivity.onCreate 里调用）
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        // 从 SharedPreferences 读取保存的值
        _followSystem = prefs?.getBoolean("follow_system", true) ?: true
        _darkMode     = prefs?.getBoolean("dark_mode", false) ?: false
    }

    /**
     * 保存到 SharedPreferences
     */
    private fun saveToPrefs() {
        prefs?.edit()?.apply {
            putBoolean("follow_system", _followSystem)
            putBoolean("dark_mode", _darkMode)
            apply()
        }
    }
}