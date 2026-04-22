import AV from 'leancloud-storage'

// LeanCloud 配置
const APP_ID = import.meta.env.VITE_LEANCLOUD_APP_ID || ''
const APP_KEY = import.meta.env.VITE_LEANCLOUD_APP_KEY || ''
const SERVER_URL = import.meta.env.VITE_LEANCLOUD_SERVER_URL || ''

export function initLeanCloud() {
  if (!APP_ID || !APP_KEY || !SERVER_URL) {
    console.warn(
      '[LeanCloud] 未配置 VITE_LEANCLOUD_APP_ID / VITE_LEANCLOUD_APP_KEY / VITE_LEANCLOUD_SERVER_URL，将保持离线（仅本地 UI 可打开）'
    )
    return
  }
  AV.init({
    appId: APP_ID,
    appKey: APP_KEY,
    serverURL: SERVER_URL
  })

  const currentUser = AV.User.current()
  if (currentUser) {
    console.log('已恢复登录状态:', currentUser.get('username'))
  }
}

// ── 用户认证 ──

export async function loginWithEmail(email, password) {
  try {
    const user = await AV.User.loginWithEmail(email, password)
    return {
      success: true,
      user: {
        id: user.id,
        username: user.get('username'),
        email: user.get('email')
      }
    }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

export async function loginWithPhone(phone, code) {
  try {
    const user = await AV.User.loginWithMobilePhone(phone, code)
    return {
      success: true,
      user: {
        id: user.id,
        username: user.get('username'),
        email: user.get('email')
      }
    }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

export async function signUp(username, email, password) {
  try {
    const user = new AV.User()
    user.setUsername(username)
    user.setEmail(email)
    user.setPassword(password)
    await user.signUp()
    return {
      success: true,
      user: {
        id: user.id,
        username: user.get('username'),
        email: user.get('email')
      }
    }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

export async function requestSMSCode(phone) {
  try {
    await AV.User.requestLoginSmsCode(phone)
    return { success: true }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

export function logout() {
  AV.User.logOut()
  return { success: true }
}

export function checkIsLoggedIn() {
  return !!AV.User.current()
}

export function getCurrentUser() {
  const user = AV.User.current()
  if (!user) return null
  return {
    id: user.id,
    username: user.get('username'),
    email: user.get('email')
  }
}

// ── 任务 CRUD ──

export async function saveTask(task) {
  try {
    let taskObj

    if (task.serverId) {
      // 更新已有对象
      taskObj = AV.Object.createWithoutData('Task', task.serverId)
    } else {
      // 创建新对象
      const TaskClass = AV.Object.extend('Task')
      taskObj = new TaskClass()
    }

    taskObj.set('title', task.title)
    taskObj.set('note', task.note || '')
    taskObj.set('dueDate', task.dueDate || null)
    taskObj.set('startTime', task.startTime || null)
    taskObj.set('endTime', task.endTime || null)
    taskObj.set('priority', task.priority || 'MEDIUM')
    taskObj.set('isDone', task.isDone || false)
    taskObj.set('tagLabels', task.tags ? task.tags.join(',') : '')
    taskObj.set('location', task.location || '')
    taskObj.set('reminderOffset', task.reminderOffset != null ? task.reminderOffset : null)

    taskObj.set('localId', task.localId || 0)
    taskObj.set('syncStatus', task.syncStatus || 'synced')
    taskObj.set('deleted', task.deleted || false)
    taskObj.set('clientUpdatedAt', task.updatedAt || Date.now())

    const currentUser = AV.User.current()
    if (currentUser) {
      taskObj.set('userId', currentUser.id)
    }

    await taskObj.save()

    return {
      success: true,
      serverId: taskObj.id,
      task: taskObj.toJSON()
    }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

export async function fetchUserTasks() {
  try {
    const query = new AV.Query('Task')
    const currentUser = AV.User.current()

    if (currentUser) {
      query.equalTo('userId', currentUser.id)
    }

    query.equalTo('deleted', false)
    query.descending('updatedAt')
    query.limit(1000)

    const tasks = await query.find()

    return {
      success: true,
      tasks: tasks.map(task => ({
        serverId: task.id,
        ...task.toJSON()
      }))
    }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

export async function deleteTask(taskId) {
  try {
    const task = AV.Object.createWithoutData('Task', taskId)
    task.set('deleted', true)
    task.set('syncStatus', 'synced')
    await task.save()
    return { success: true }
  } catch (error) {
    return { success: false, error: error.message }
  }
}

// ── 常量（与 Android 端对齐）──

export const PRIORITIES = {
  HIGH: { label: '重要', color: '#FF6B6B', order: 0 },
  MEDIUM: { label: '普通', color: '#FFB347', order: 1 },
  LOW: { label: '次要', color: '#7DD3FC', order: 2 }
}

export const PRESET_TAGS = [
  { label: '学习', color: '#818CF8' },
  { label: '编程', color: '#34D399' },
  { label: '专业', color: '#FB7185' },
  { label: '社团', color: '#F59E0B' },
  { label: '家庭', color: '#60A5FA' },
  { label: '朋友', color: '#A78BFA' },
  { label: '生活', color: '#2DD4BF' },
  { label: '锻炼', color: '#EC4899' },
  { label: '习惯', color: '#6EE7B7' },
  { label: '其他', color: '#94A3B8' }
]
