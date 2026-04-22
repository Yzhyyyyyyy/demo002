package com.example.demo002

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

// DataStore 用于存储同步状态
private val Context.dataStore by preferencesDataStore(name = "sync_preferences")

/**
 * 同步仓库
 * 负责协调本地 Room 数据库和远程 LeanCloud 的数据同步
 */
class SyncRepository(
    private val context: Context,
    private val localDao: TaskDao
) {

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val isSyncing = AtomicBoolean(false)

    companion object {
        private val LAST_SYNC_TIME_KEY = longPreferencesKey("last_sync_time")
    }

    /**
     * 获取最后同步时间
     */
    suspend fun getLastSyncTime(): Long {
        return context.dataStore.data
            .map { preferences -> preferences[LAST_SYNC_TIME_KEY] ?: 0L }
            .first()
    }

    /**
     * 更新最后同步时间
     */
    private suspend fun updateLastSyncTime() {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME_KEY] = System.currentTimeMillis()
        }
    }

    /**
     * 同步所有数据（双向同步）
     */
    suspend fun syncAllData(): Boolean {
        if (!isSyncing.compareAndSet(false, true)) {
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                if (!LeanCloudManager.isLoggedIn()) {
                    return@withContext false
                }

                // 1. 上传本地待同步的任务
                uploadPendingTasks()

                // 2. 下载远程任务到本地
                downloadRemoteTasks()

                // 3. 解决冲突（简单策略：远程优先）
                resolveConflicts()

                // 4. 更新同步时间
                updateLastSyncTime()

                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            isSyncing.set(false)
        }
    }

    /**
     * 上传本地待同步的任务
     */
    private suspend fun uploadPendingTasks() {
        val localTasks = localDao.getAllTasksIncludingDeleted().first()
            .filter { it.syncStatus == "pending" || it.syncStatus == "conflict" }

        for (taskEntity in localTasks) {
            try {
                val taskWithSubTasks = localDao.getTaskWithSubTasks(taskEntity.id).first()
                if (taskWithSubTasks == null) continue
                val task = taskWithSubTasks.toTask()

                // 上传到 LeanCloud
                val serverId = LeanCloudManager.saveTask(task)

                // 更新本地记录的 serverId 和同步状态
                val updatedEntity = taskEntity.copy(
                    serverId = serverId,
                    syncStatus = "synced",
                    updatedAt = System.currentTimeMillis()
                )
                localDao.upsertTask(updatedEntity)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 下载远程任务到本地
     */
    private suspend fun downloadRemoteTasks() {
        try {
            val remoteObjects = LeanCloudManager.fetchUserTasks()

            for (lcObject in remoteObjects) {
                val remoteTask = LeanCloudManager.convertToTask(lcObject)

                // 通过 serverId 检查本地是否已有
                val existingEntity = if (!remoteTask.serverId.isNullOrEmpty()) {
                    localDao.getTaskByServerId(remoteTask.serverId)
                } else {
                    null
                }

                if (existingEntity == null) {
                    // 新任务：插入到本地数据库，使用本地自动生成的 id
                    val taskEntity = remoteTask.toEntity().copy(id = 0)
                    val newId = localDao.upsertTask(taskEntity)

                    // 更新 serverId
                    if (remoteTask.serverId != null) {
                        localDao.updateTaskServerId(newId.toInt(), remoteTask.serverId)
                    }
                } else {
                    // 已存在：远程更新时间更晚时覆盖
                    if (remoteTask.updatedAt > existingEntity.updatedAt) {
                        val updatedEntity = remoteTask.toEntity().copy(id = existingEntity.id)
                        localDao.upsertTask(updatedEntity)
                    }
                }
            }

            // 检查本地有但远程已删除的任务
            markDeletedTasks(remoteObjects.map { it.objectId })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 标记远程已删除的任务
     */
    private suspend fun markDeletedTasks(remoteServerIds: List<String>) {
        val localTasks = localDao.getAllTasksIncludingDeleted().first()

        for (localTask in localTasks) {
            if (!localTask.serverId.isNullOrEmpty() &&
                !remoteServerIds.contains(localTask.serverId) &&
                !localTask.deleted) {

                val updatedEntity = localTask.copy(
                    deleted = true,
                    syncStatus = "synced",
                    updatedAt = System.currentTimeMillis()
                )
                localDao.upsertTask(updatedEntity)
            }
        }
    }

    /**
     * 解决冲突（简单策略：远程优先）
     */
    private suspend fun resolveConflicts() {
        val conflictTasks = localDao.getAllTasksIncludingDeleted().first()
            .filter { it.syncStatus == "conflict" }

        for (taskEntity in conflictTasks) {
            try {
                if (!taskEntity.serverId.isNullOrEmpty()) {
                    val updatedEntity = taskEntity.copy(
                        syncStatus = "synced",
                        updatedAt = System.currentTimeMillis()
                    )
                    localDao.upsertTask(updatedEntity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 手动触发同步
     */
    fun triggerSync() {
        syncScope.launch {
            syncAllData()
        }
    }

    /**
     * 启动时自动同步（登录后调用）
     */
    fun startAutoSync() {
        syncScope.launch {
            if (LeanCloudManager.isLoggedIn()) {
                syncAllData()
            }
        }
    }

    /**
     * 检查同步状态
     */
    suspend fun getSyncStatus(): SyncStatus {
        val pendingCount = localDao.getAllTasks().first()
            .count { it.syncStatus == "pending" }

        val conflictCount = localDao.getAllTasks().first()
            .count { it.syncStatus == "conflict" }

        val lastSyncTime = getLastSyncTime()

        return SyncStatus(
            isSyncing = isSyncing.get(),
            pendingCount = pendingCount,
            conflictCount = conflictCount,
            lastSyncTime = lastSyncTime,
            isLoggedIn = LeanCloudManager.isLoggedIn()
        )
    }

    /**
     * 获取需要同步的任务数量
     */
    suspend fun getPendingSyncCount(): Int {
        return localDao.getAllTasks().first()
            .count { it.syncStatus == "pending" || it.syncStatus == "conflict" }
    }

    /**
     * 添加任务（自动标记为待同步）
     */
    suspend fun addTask(task: Task): Long {
        val taskWithSync = task.copy(
            syncStatus = "pending",
            updatedAt = System.currentTimeMillis()
        )

        val taskId = localDao.upsertTask(taskWithSync.toEntity())

        syncScope.launch {
            syncAllData()
        }

        return taskId
    }

    /**
     * 更新任务（自动标记为待同步）
     */
    suspend fun updateTask(task: Task) {
        val taskWithSync = task.copy(
            syncStatus = "pending",
            updatedAt = System.currentTimeMillis()
        )

        localDao.upsertTask(taskWithSync.toEntity())

        syncScope.launch {
            syncAllData()
        }
    }

    /**
     * 删除任务（软删除，标记为待同步）
     */
    suspend fun deleteTask(taskId: Int) {
        val taskEntity = localDao.getTaskByRawId(taskId)
        if (taskEntity != null) {
            val updatedEntity = taskEntity.copy(
                deleted = true,
                syncStatus = "pending",
                updatedAt = System.currentTimeMillis()
            )
            localDao.upsertTask(updatedEntity)

            syncScope.launch {
                syncAllData()
            }
        }
    }
}

/**
 * 同步状态数据类
 */
data class SyncStatus(
    val isSyncing: Boolean,
    val pendingCount: Int,
    val conflictCount: Int,
    val lastSyncTime: Long,
    val isLoggedIn: Boolean
)
