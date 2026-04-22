import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { saveTask, fetchUserTasks, deleteTask as lcDeleteTask } from '../utils/leancloud'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  const pendingTasks = computed(() =>
    tasks.value.filter(t => !t.isDone && !t.deleted)
  )

  const completedTasks = computed(() =>
    tasks.value.filter(t => t.isDone && !t.deleted)
  )

  const todayTasks = computed(() => {
    const today = new Date().toISOString().split('T')[0]
    return tasks.value.filter(t => t.dueDate && t.dueDate.startsWith(today) && !t.deleted)
  })

  async function loadTasks() {
    isLoading.value = true
    error.value = null
    try {
      const result = await fetchUserTasks()
      if (result.success) {
        tasks.value = result.tasks.map(t => ({
          id: t.serverId,
          serverId: t.serverId,
          title: t.title || '',
          note: t.note || '',
          dueDate: t.dueDate || null,
          startTime: t.startTime || null,
          endTime: t.endTime || null,
          priority: t.priority || 'MEDIUM',
          isDone: t.isDone || false,
          tags: t.tagLabels ? t.tagLabels.split(',').filter(Boolean) : [],
          location: t.location || '',
          reminderOffset: t.reminderOffset ?? null,
          syncStatus: t.syncStatus || 'synced',
          updatedAt: t.updatedAt || 0,
          deleted: t.deleted || false
        }))
      } else {
        error.value = result.error
      }
    } catch (err) {
      error.value = err.message
    } finally {
      isLoading.value = false
    }
  }

  async function saveTaskData(taskData) {
    isLoading.value = true
    error.value = null
    try {
      const data = {
        ...taskData,
        serverId: taskData.serverId || taskData.id || null,
        tags: taskData.tags || [],
        syncStatus: 'pending',
        updatedAt: Date.now(),
        deleted: false
      }

      if (!data.dueDate) {
        const today = new Date()
        data.dueDate = today.getFullYear() + '-' +
          String(today.getMonth() + 1).padStart(2, '0') + '-' +
          String(today.getDate()).padStart(2, '0')
      }

      const result = await saveTask(data)
      if (result.success) {
        await loadTasks()
        return { success: true, id: result.serverId }
      } else {
        error.value = result.error
        return { success: false, error: result.error }
      }
    } catch (err) {
      error.value = err.message
      return { success: false, error: err.message }
    } finally {
      isLoading.value = false
    }
  }

  async function deleteTaskById(taskId) {
    isLoading.value = true
    error.value = null
    try {
      const result = await lcDeleteTask(taskId)
      if (result.success) {
        await loadTasks()
        return { success: true }
      } else {
        error.value = result.error
        return { success: false, error: result.error }
      }
    } catch (err) {
      error.value = err.message
      return { success: false, error: err.message }
    } finally {
      isLoading.value = false
    }
  }

  async function toggleTaskCompletion(taskId) {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task) return { success: false, error: '任务不存在' }

    return await saveTaskData({
      ...task,
      serverId: task.serverId || taskId,
      isDone: !task.isDone
    })
  }

  function clearError() {
    error.value = null
  }

  return {
    tasks,
    isLoading,
    error,
    pendingTasks,
    completedTasks,
    todayTasks,
    loadTasks,
    saveTask: saveTaskData,
    deleteTaskById,
    toggleTaskCompletion,
    clearError
  }
})
