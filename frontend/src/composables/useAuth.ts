import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'

export function useAuth() {
  const auth = useAuthStore()
  const { session, initialized, accessToken } = storeToRefs(auth)
  return {
    session,
    initialized,
    accessToken,
    initialize: auth.initialize,
    login: auth.login,
    register: auth.register,
    logout: auth.logout,
    clearSession: auth.clearSession,
    isAuthenticated: () => auth.isAuthenticated,
    hasPermission: auth.hasPermission,
  }
}
