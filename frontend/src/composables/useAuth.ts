import { ref } from 'vue'
import * as authApi from '@/api/auth'
import type { AuthSession, LoginRequest } from '@/types/auth'
import { useWorkbenchTabs } from '@/composables/useWorkbenchTabs'

const session = ref<AuthSession | null>(null)
const initialized = ref(false)
const initializing = ref<Promise<void> | null>(null)

async function initialize() {
  if (initialized.value) {
    return
  }
  if (initializing.value) {
    await initializing.value
    return
  }

  initializing.value = (async () => {
    try {
      const { data } = await authApi.fetchMe()
      session.value = data
    } catch {
      session.value = null
    } finally {
      initialized.value = true
      initializing.value = null
    }
  })()

  await initializing.value
}

async function login(payload: LoginRequest) {
  const { data } = await authApi.login(payload)
  session.value = data
  initialized.value = true
  return data
}

async function logout() {
  try {
    await authApi.logout()
  } finally {
    session.value = null
    initialized.value = true
    useWorkbenchTabs().clearTabs()
  }
}


function clearSession() {
  session.value = null
  initialized.value = true
  useWorkbenchTabs().clearTabs()
}

async function register(payload: import('@/types/system').RegisterRequest) {
  const { data } = await authApi.register(payload)
  session.value = data
  initialized.value = true
  return data
}

function hasPermission(permission: string) {
  return session.value?.permissions.includes(permission) ?? false
}

function isAuthenticated() {
  return session.value !== null
}

export function useAuth() {
  return {
    session,
    initialized,
    initialize,
    login,
    register,
    logout,
    clearSession,
    isAuthenticated,
    hasPermission,
  }
}
