import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { AuthSession, LoginRequest } from '@/types/auth'
import { useWorkbenchTabs } from '@/composables/useWorkbenchTabs'
import { useMenuStore } from '@/stores/menu'

const ACCESS_TOKEN_KEY = 'storage.accessToken'

function readStoredToken() {
  return globalThis.localStorage?.getItem(ACCESS_TOKEN_KEY) ?? null
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(null)
  const accessToken = ref<string | null>(readStoredToken())
  const initialized = ref(false)
  const initializing = ref<Promise<void> | null>(null)

  function persistToken(token: string | null | undefined) {
    accessToken.value = token || null
    if (accessToken.value) {
      globalThis.localStorage?.setItem(ACCESS_TOKEN_KEY, accessToken.value)
    } else {
      globalThis.localStorage?.removeItem(ACCESS_TOKEN_KEY)
    }
  }

  function applySession(nextSession: AuthSession) {
    session.value = nextSession
    if (nextSession.accessToken) {
      persistToken(nextSession.accessToken)
    }
  }

  async function initialize() {
    if (initialized.value) {
      return
    }
    if (initializing.value) {
      await initializing.value
      return
    }
    if (!accessToken.value) {
      session.value = null
      initialized.value = true
      return
    }

    initializing.value = (async () => {
      try {
        const { data } = await authApi.fetchMe()
        session.value = data
      } catch {
        clearSession()
      } finally {
        initialized.value = true
        initializing.value = null
      }
    })()

    await initializing.value
  }

  async function login(payload: LoginRequest) {
    const { data } = await authApi.login(payload)
    applySession(data)
    initialized.value = true
    return data
  }

  async function register(payload: import('@/types/system').RegisterRequest) {
    const { data } = await authApi.register(payload)
    applySession(data)
    initialized.value = true
    return data
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    session.value = null
    persistToken(null)
    initialized.value = true
    initializing.value = null
    useMenuStore().clearMenusAndRoutes()
    useWorkbenchTabs().clearTabs()
  }

  function hasPermission(permission: string) {
    return session.value?.permissions.includes(permission) ?? false
  }

  const isAuthenticated = computed(() => session.value !== null)

  return {
    session,
    accessToken,
    initialized,
    initialize,
    login,
    register,
    logout,
    clearSession,
    isAuthenticated,
    hasPermission,
  }
})

export { ACCESS_TOKEN_KEY }
