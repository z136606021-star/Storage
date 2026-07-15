import axios from 'axios'
import { reportApiServerError } from '@/diagnostics/clientErrorReporting'
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

    if (axios.isAxiosError(error) && error.response && error.response.status >= 500) {
      reportApiServerError(error)
    }

    return Promise.reject(error)
  },
)

export function getErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) {
    if (error instanceof Error && error.message.trim()) {
      return error.message.trim()
    }
    return fallback
  }

  if (error.code === 'ECONNABORTED') {
    return '请求超时，请检查网络或稍后重试'
  }

  if (!error.response) {
    return '网络连接失败，请检查网络或代理配置'
  }

  const status = error.response.status
  if (status === 413) {
    return '文件超过服务器允许大小（HTTP 413）'
  }
  if (status === 502 || status === 504) {
    return `网关错误（HTTP ${status}），可能是上游代理超时或不可用`
  }

  const data = error.response.data
  const requestIdFromHeader = resolveRequestIdHeader(error.response.headers)
  if (data && typeof data === 'object') {
    const payload = data as { message?: string; error?: string; requestId?: string }
    if (payload.message) {
      const requestId = payload.requestId ?? requestIdFromHeader
      const requestIdSuffix = requestId ? `（请求ID: ${requestId}）` : ''
      return `${String(payload.message)}${requestIdSuffix}`
    }
    if (payload.error) {
      return String(payload.error)
    }
  }

  if (typeof data === 'string' && data.trim()) {
    const normalized = data.toLowerCase()
    if (normalized.includes('413') || normalized.includes('too large') || normalized.includes('request entity too large')) {
      return '文件超过代理允许大小（HTTP 413）'
    }
  }

  if (requestIdFromHeader) {
    return `${fallback}（请求ID: ${requestIdFromHeader}）`
  }

  return fallback
}

function resolveRequestIdHeader(headers: Record<string, unknown> | undefined): string | undefined {
  if (!headers) {
    return undefined
  }
  const value = headers['x-request-id'] ?? headers['X-Request-Id']
  if (typeof value === 'string' && value.trim()) {
    return value.trim()
  }
  if (Array.isArray(value) && typeof value[0] === 'string' && value[0].trim()) {
    return value[0].trim()
  }
  return undefined
}
