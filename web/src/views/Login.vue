<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Logo 区域 -->
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="#6C5CE7"/>
            <path d="M24 12L30 18H26V28H22V18H18L24 12Z" fill="white"/>
            <path d="M14 30V34H34V30" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="app-title">TaskFlow</h1>
        <p class="app-subtitle">智能任务管理 · 多端同步</p>
      </div>

      <!-- 登录方式切换 -->
      <div class="tab-switch">
        <button
          :class="['tab-btn', { active: loginMode === 'email' }]"
          @click="loginMode = 'email'"
        >邮箱登录</button>
        <button
          :class="['tab-btn', { active: loginMode === 'phone' }]"
          @click="loginMode = 'phone'"
        >手机号登录</button>
      </div>

      <!-- 邮箱登录表单 -->
      <form v-if="loginMode === 'email'" @submit.prevent="handleEmailLogin" class="login-form">
        <div class="input-group">
          <label>邮箱</label>
          <input
            v-model="emailForm.email"
            type="email"
            placeholder="请输入邮箱"
            required
          />
        </div>
        <div class="input-group">
          <label>密码</label>
          <input
            v-model="emailForm.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 手机号登录表单 -->
      <form v-else @submit.prevent="handlePhoneLogin" class="login-form">
        <div class="input-group">
          <label>手机号</label>
          <input
            v-model="phoneForm.phone"
            type="tel"
            placeholder="请输入手机号"
            required
          />
        </div>
        <div class="input-group">
          <label>验证码</label>
          <div class="code-input">
            <input
              v-model="phoneForm.code"
              type="text"
              placeholder="请输入验证码"
              maxlength="6"
              required
            />
            <button
              type="button"
              class="btn-code"
              @click="handleSendCode"
              :disabled="codeCooldown > 0"
            >
              {{ codeCooldown > 0 ? `${codeCooldown}s` : '获取验证码' }}
            </button>
          </div>
        </div>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 注册入口 -->
      <div class="register-entry">
        <span>还没有账号？</span>
        <button class="btn-link" @click="showRegister = true">立即注册</button>
      </div>

      <!-- 错误提示 -->
      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

      <!-- 注册弹窗 -->
      <div v-if="showRegister" class="modal-overlay" @click.self="showRegister = false">
        <div class="modal-content">
          <h2>注册账号</h2>
          <form @submit.prevent="handleRegister" class="login-form">
            <div class="input-group">
              <label>用户名</label>
              <input v-model="registerForm.username" type="text" placeholder="请输入用户名" required />
            </div>
            <div class="input-group">
              <label>邮箱</label>
              <input v-model="registerForm.email" type="email" placeholder="请输入邮箱" required />
            </div>
            <div class="input-group">
              <label>密码</label>
              <input v-model="registerForm.password" type="password" placeholder="请输入密码（至少6位）" required minlength="6" />
            </div>
            <div class="input-group">
              <label>确认密码</label>
              <input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" required />
            </div>
            <div class="modal-actions">
              <button type="button" class="btn-secondary" @click="showRegister = false">取消</button>
              <button type="submit" class="btn-primary" :disabled="loading">
                {{ loading ? '注册中...' : '注册' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { loginWithEmail, loginWithPhone, requestSMSCode, signUp } from '../utils/leancloud'

const router = useRouter()
const authStore = useAuthStore()

const loginMode = ref('email')
const loading = ref(false)
const errorMsg = ref('')
const showRegister = ref(false)
const codeCooldown = ref(0)
let cooldownTimer = null

const emailForm = ref({ email: '', password: '' })
const phoneForm = ref({ phone: '', code: '' })
const registerForm = ref({ username: '', email: '', password: '', confirmPassword: '' })

async function handleEmailLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const result = await loginWithEmail(emailForm.value.email, emailForm.value.password)
    if (result.success) {
      authStore.setUser(result.user)
      router.push('/')
    } else {
      errorMsg.value = result.error || '登录失败'
    }
  } catch (e) {
    errorMsg.value = '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

async function handlePhoneLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const result = await loginWithPhone(phoneForm.value.phone, phoneForm.value.code)
    if (result.success) {
      authStore.setUser(result.user)
      router.push('/')
    } else {
      errorMsg.value = result.error || '登录失败'
    }
  } catch (e) {
    errorMsg.value = '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!phoneForm.value.phone) {
    errorMsg.value = '请先输入手机号'
    return
  }
  const result = await requestSMSCode(phoneForm.value.phone)
  if (result.success) {
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(cooldownTimer)
    }, 1000)
    errorMsg.value = ''
  } else {
    errorMsg.value = result.error || '发送验证码失败'
  }
}

async function handleRegister() {
  errorMsg.value = ''
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    const result = await signUp(
      registerForm.value.username,
      registerForm.value.email,
      registerForm.value.password
    )
    if (result.success) {
      authStore.setUser(result.user)
      showRegister.value = false
      router.push('/')
    } else {
      errorMsg.value = result.error || '注册失败'
    }
  } catch (e) {
    errorMsg.value = '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 20px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.logo-area {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.app-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.app-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin: 6px 0 0;
}

.tab-switch {
  display: flex;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 3px;
  margin-bottom: 24px;
}

.tab-btn {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #fff;
  color: #6C5CE7;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-group label {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.input-group input {
  padding: 12px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  background: #fafbfc;
}

.input-group input:focus {
  border-color: #6C5CE7;
  background: #fff;
}

.code-input {
  display: flex;
  gap: 10px;
}

.code-input input {
  flex: 1;
}

.btn-code {
  padding: 12px 16px;
  background: #6C5CE7;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.btn-code:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  padding: 14px;
  background: linear-gradient(135deg, #6C5CE7, #a855f7);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
  margin-top: 4px;
}

.btn-primary:active {
  transform: scale(0.98);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.register-entry {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #94a3b8;
}

.btn-link {
  background: none;
  border: none;
  color: #6C5CE7;
  font-weight: 600;
  cursor: pointer;
  font-size: 14px;
}

.error-msg {
  margin-top: 12px;
  padding: 10px 14px;
  background: #fef2f2;
  color: #ef4444;
  border-radius: 8px;
  font-size: 13px;
  text-align: center;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 20px;
}

.modal-content {
  background: #fff;
  border-radius: 20px;
  padding: 32px;
  width: 100%;
  max-width: 400px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin: 0 0 20px;
  font-size: 22px;
  color: #1a1a2e;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn-secondary {
  flex: 1;
  padding: 12px;
  background: #f1f5f9;
  color: #475569;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}

.btn-secondary + .btn-primary {
  flex: 2;
}
</style>
