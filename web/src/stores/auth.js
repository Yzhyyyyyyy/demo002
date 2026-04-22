import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUser } from '../utils/leancloud'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)

  const isLoggedIn = computed(() => !!user.value)

  function setUser(userData) {
    user.value = userData
    if (userData) {
      localStorage.setItem('taskflow_user', JSON.stringify(userData))
    }
  }

  function clearUser() {
    user.value = null
    localStorage.removeItem('taskflow_user')
  }

  function restoreSession() {
    const stored = localStorage.getItem('taskflow_user')
    if (stored) {
      try {
        user.value = JSON.parse(stored)
      } catch {
        localStorage.removeItem('taskflow_user')
      }
    }
  }

  function init() {
    restoreSession()
    if (!user.value) {
      const currentUser = getCurrentUser()
      if (currentUser) {
        user.value = currentUser
      }
    }
  }

  init()

  return { user, isLoggedIn, setUser, clearUser, restoreSession }
})
