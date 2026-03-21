package com.example.demo002

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [TaskEntity::class, SubTaskEntity::class],
    version   = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule_db"
                )
                    .fallbackToDestructiveMigration()  // 开发期间版本不兼容时自动重建
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}