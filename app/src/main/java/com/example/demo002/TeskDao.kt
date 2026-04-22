package com.example.demo002

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ── UI 使用：查询未删除的任务（含子任务）──
    @Transaction
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY id ASC")
    fun getAllTasksWithSubTasks(): Flow<List<TaskWithSubTasks>>

    // ── UI 使用：查询未删除的扁平任务 ──
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // ── 同步使用：查询所有任务（含已删除，SyncRepository 用）──
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasksIncludingDeleted(): Flow<List<TaskEntity>>

    // ── 搜索：按标题、备注或地点模糊匹配（仅未删除）──
    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE (title LIKE '%' || :query || '%'
           OR note  LIKE '%' || :query || '%'
           OR location LIKE '%' || :query || '%')
           AND deleted = 0
        ORDER BY id ASC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithSubTasks>>

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
        upsertTask(task.toEntity())
    }

    // ── 硬删除 Task（通过 id）──
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    // ── 同步相关查询 ──

    // 根据 serverId 查询任务（挂起函数）
    @Query("SELECT * FROM tasks WHERE serverId = :serverId LIMIT 1")
    suspend fun getTaskByServerId(serverId: String): TaskEntity?

    // 根据 id 查询单个任务（挂起函数，供 SyncRepository 使用）
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskByRawId(taskId: Int): TaskEntity?

    // 根据 id 查询单个任务（Flow 版本，供 UI 观察）
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskByIdFlow(taskId: Int): Flow<TaskEntity?>

    // 查询单个任务及其子任务
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun getTaskWithSubTasks(taskId: Int): Flow<TaskWithSubTasks?>

    // 更新任务的 serverId
    @Query("UPDATE tasks SET serverId = :serverId WHERE id = :taskId")
    suspend fun updateTaskServerId(taskId: Int, serverId: String)

    // 查询需要同步的任务（pending 或 conflict 状态，含已删除的也需要同步删除操作）
    @Query("SELECT * FROM tasks WHERE syncStatus IN ('pending', 'conflict')")
    fun getPendingSyncTasks(): Flow<List<TaskEntity>>

    // 查询未删除的任务
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY id ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>
}
