import axios from 'axios'
import { useAuth } from '@/composables/useAuth'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const { clearSession } = useAuth()
      clearSession()
      const path = window.location.pathname
      if (!path.startsWith('/login')) {
        const redirect = encodeURIComponent(path + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    return Promise.reject(error)
  },
)

export function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data as { message?: string; error?: string }
    if (data.message) {
      return String(data.message)
    }
    if (data.error) {
      return String(data.error)
    }
  }
  return fallback
}
