package com.example.demo002

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ── 查询：返回 Flow，数据变化时自动通知 UI ──
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasksWithSubTasks(): Flow<List<TaskWithSubTasks>>

    // ── 插入 / 更新 Task ──
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity): Long

    // ── 删除 Task（子任务因 CASCADE 自动删除）──
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // ── 插入 / 更新子任务列表（先删旧的再插新的）──
    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubTasksByTaskId(taskId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>)

    // ── 组合：保存整个 Task（含子任务）──
    @Transaction
    suspend fun upsertTaskWithSubTasks(task: Task) {
        val taskId = upsertTask(task.toEntity()).let {
            // autoGenerate 时返回新 id，否则用原 id
            if (task.id == 0) it.toInt() else task.id
        }
        deleteSubTasksByTaskId(taskId)
        insertSubTasks(task.subTasks.mapIndexed { index, sub ->
            sub.toEntity(taskId, index)
        })
    }

    // ── 删除 Task（通过 id）──
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)
}