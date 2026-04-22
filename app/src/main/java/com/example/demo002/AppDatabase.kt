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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
        
        // 从版本 4 到 5 的迁移：添加同步字段
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为tasks表添加同步字段
                database.execSQL("ALTER TABLE tasks ADD COLUMN serverId TEXT")
                database.execSQL("ALTER TABLE tasks ADD COLUMN syncStatus TEXT DEFAULT 'synced'")
                database.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN deleted INTEGER DEFAULT 0") // SQLite用0/1表示boolean
                
                // 为subtasks表添加同步字段
                database.execSQL("ALTER TABLE subtasks ADD COLUMN serverId TEXT")
                database.execSQL("ALTER TABLE subtasks ADD COLUMN syncStatus TEXT DEFAULT 'synced'")
                database.execSQL("ALTER TABLE subtasks ADD COLUMN updatedAt INTEGER DEFAULT 0")
                database.execSQL("ALTER TABLE subtasks ADD COLUMN deleted INTEGER DEFAULT 0")
            }
        }
    }
}