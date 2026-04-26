package com.example.demo002

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [TaskEntity::class, SubTaskEntity::class],
    version   = 5,
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
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()  // 开发期间版本不兼容时自动重建
                    .build()
                    .also { INSTANCE = it }
            }
        }
        
        // 从版本 3 到 4 的迁移：添加 reminderOffset 列
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN reminderOffset INTEGER")
            }
        }
    }
}