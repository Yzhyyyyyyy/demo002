package com.example.demo002

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 在应用启动时初始化 LeanCloud SDK
        LeanCloudManager.initialize(this)
    }
}
