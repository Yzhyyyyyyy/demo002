<template>
  <div class="layout">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <h1 class="header-title">Schedule</h1>
          <span v-if="pendingCount > 0" class="header-subtitle">还有 {{ pendingCount }} 件事待完成</span>
          <span v-else class="header-subtitle done">今天全部完成啦</span>
        </div>
        <div class="header-right">
          <button class="icon-btn" @click="refreshTasks" :disabled="taskStore.isLoading" title="刷新同步">
            <svg :class="{ spinning: taskStore.isLoading }" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 11-6.22-8.56"/><path d="M21 3v5h-5"/></svg>
          </button>
          <div class="user-menu">
            <button class="icon-btn avatar-btn" @click="showUserMenu = !showUserMenu">
              <div class="avatar">{{ username.charAt(0) }}</div>
            </button>
            <div v-if="showUserMenu" class="dropdown" @click.self="showUserMenu = false">
              <div class="dropdown-item user-info">
                <span class="user-name">{{ username }}</span>
                <span class="user-email">{{ email }}</span>
              </div>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item" @click="refreshTasks(); showUserMenu = false">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 11-6.22-8.56"/><path d="M21 3v5h-5"/></svg>
                立即同步
              </button>
              <button class="dropdown-item danger" @click="handleLogout">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                退出登录
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main">
      <router-view />
    </main>

    <!-- 浮动添加按钮 -->
    <router-link to="/task/create" class="fab" title="添加任务">
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="white" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
    </router-link>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTaskStore } from '../stores/task'
import { logout as lcLogout } from '../utils/leancloud'

const router = useRouter()
const authStore = useAuthStore()
const taskStore = useTaskStore()

const showUserMenu = ref(false)

const username = computed(() => authStore.user?.username || '用户')
const email = computed(() => authStore.user?.email || '')
const pendingCount = computed(() => taskStore.pendingTasks.length)

function refreshTasks() {
  taskStore.loadTasks()
}

function handleLogout() {
  lcLogout()
  authStore.clearUser()
  showUserMenu.value = false
  router.push('/login')
}

let syncInterval = null
onMounted(() => {
  if (authStore.isLoggedIn) {
    taskStore.loadTasks()
    syncInterval = setInterval(() => {
      taskStore.loadTasks()
    }, 5 * 60 * 1000)
  }
})

onUnmounted(() => {
  if (syncInterval) clearInterval(syncInterval)
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f5;
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 26px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: 0.5px;
}

.header-subtitle {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  margin-top: 2px;
  display: block;
}

.header-subtitle.done {
  color: #7dd3fc;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: none;
  background: #fff;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: background 0.2s;
}

.icon-btn:hover {
  background: #f8fafc;
}

.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.avatar-btn {
  padding: 0;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6C5CE7, #a855f7);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-menu {
  position: relative;
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  min-width: 200px;
  overflow: hidden;
  z-index: 100;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: none;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
  text-align: left;
}

.dropdown-item:hover {
  background: #f8fafc;
}

.dropdown-item.danger {
  color: #ef4444;
}

.dropdown-item.user-info {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  cursor: default;
}

.dropdown-item.user-info:hover {
  background: transparent;
}

.user-name {
  font-weight: 600;
  color: #1a1a2e;
}

.user-email {
  font-size: 12px;
  color: #94a3b8;
}

.dropdown-divider {
  height: 1px;
  background: #f1f5f9;
}

.main {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 24px 100px;
}

.fab {
  position: fixed;
  bottom: 28px;
  right: 28px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6C5CE7, #a855f7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(108, 92, 231, 0.35);
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 40;
}

.fab:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 24px rgba(108, 92, 231, 0.45);
}

.fab:active {
  transform: scale(0.95);
}

@media (max-width: 768px) {
  .header-inner {
    padding: 12px 16px;
  }
  .main {
    padding: 16px 16px 100px;
  }
}
</style>
