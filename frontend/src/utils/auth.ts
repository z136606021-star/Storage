import { useAuth } from '@/composables/useAuth'

export function isAuthenticated(): boolean {
  return useAuth().isAuthenticated()
}

export function clearAuthToken(): void {
  useAuth().clearSession()
}
