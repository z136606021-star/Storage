import axios from 'axios'
import { ACCESS_TOKEN_KEY, useAuthStore } from '@/stores/auth'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  const token = auth.accessToken || globalThis.localStorage?.getItem(ACCESS_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const auth = useAuthStore()
      auth.clearSession()
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
