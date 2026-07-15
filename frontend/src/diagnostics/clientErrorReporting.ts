import type { App } from 'vue'
import type { Router } from 'vue-router'
import axios from 'axios'
import { reportFrontendException } from '@/api/system/exceptionLog'
import { useAuthStore } from '@/stores/auth'
import type { FrontendErrorCode, FrontendExceptionReportPayload } from '@/types/system/exceptionLog'

const REPORT_ENDPOINT = '/system/exception-logs/report'
const DEDUP_WINDOW_MS = 60_000

const recentFingerprints = new Map<string, number>()

export function getRequestIdFromHeaders(headers: unknown): string | undefined {
  if (!headers || typeof headers !== 'object') {
    return undefined
  }
  const record = headers as Record<string, unknown>
  const value = record['x-request-id'] ?? record['X-Request-Id']
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

function isAuthenticated(): boolean {
  const auth = useAuthStore()
  return Boolean(auth.accessToken || auth.session)
}

function shouldSkipReporting(url?: string): boolean {
  return Boolean(url && url.includes(REPORT_ENDPOINT))
}

function buildFingerprint(code: FrontendErrorCode, summary: string, route?: string): string {
  return [code, summary, route ?? ''].join('|')
}

function shouldDedup(fingerprint: string): boolean {
  const now = Date.now()
  const lastSeen = recentFingerprints.get(fingerprint)
  if (lastSeen != null && now - lastSeen < DEDUP_WINDOW_MS) {
    return true
  }
  recentFingerprints.set(fingerprint, now)
  return false
}

function sanitizePath(path?: string): string | undefined {
  if (!path) {
    return undefined
  }
  const trimmed = path.trim()
  const queryIndex = trimmed.indexOf('?')
  const hashIndex = trimmed.indexOf('#')
  let end = trimmed.length
  if (queryIndex >= 0) {
    end = Math.min(end, queryIndex)
  }
  if (hashIndex >= 0) {
    end = Math.min(end, hashIndex)
  }
  return trimmed.slice(0, end)
}

function serializeError(error: unknown): { name?: string; message: string; stack?: string } {
  if (error instanceof Error) {
    return {
      name: error.name,
      message: error.message || 'Unknown error',
      stack: error.stack,
    }
  }
  if (typeof error === 'string') {
    return { message: error }
  }
  try {
    return { message: JSON.stringify(error) }
  } catch {
    return { message: String(error) }
  }
}

function getBrowserInfo(): string {
  if (typeof navigator === 'undefined') {
    return 'unknown'
  }
  return navigator.userAgent.slice(0, 512)
}

function getCurrentRoute(router?: Router): string | undefined {
  if (!router) {
    return sanitizePath(globalThis.location?.pathname)
  }
  return sanitizePath(router.currentRoute.value.fullPath)
}

export function logClientError(
  code: FrontendErrorCode,
  error: unknown,
  context?: {
    requestId?: string
    httpStatus?: number
    httpMethod?: string
    requestPath?: string
    route?: string
    info?: string
  },
): void {
  const serialized = serializeError(error)
  const summary = context?.info
    ? `${context.info}: ${serialized.message}`
    : serialized.message
  const route = context?.route ?? sanitizePath(globalThis.location?.pathname)

  console.error('[client-error]', {
    code,
    requestId: context?.requestId,
    httpStatus: context?.httpStatus,
    route,
    error: serialized,
  })

  void reportClientError({
    errorCode: code,
    requestId: context?.requestId,
    httpStatus: context?.httpStatus,
    httpMethod: context?.httpMethod,
    requestPath: sanitizePath(context?.requestPath),
    exceptionClass: serialized.name,
    summary: summary.slice(0, 500),
    stackTrace: serialized.stack?.slice(0, 8000),
    frontendRoute: route,
    browserInfo: getBrowserInfo(),
    reportType: code === 'API_SERVER_ERROR' ? 'API' : 'FRONTEND',
  })
}

export async function reportClientError(payload: FrontendExceptionReportPayload): Promise<void> {
  if (!isAuthenticated()) {
    return
  }

  const fingerprint = buildFingerprint(payload.errorCode, payload.summary, payload.frontendRoute)
  if (shouldDedup(fingerprint)) {
    return
  }

  try {
    await reportFrontendException(payload)
  } catch (reportError) {
    console.warn('[client-error] failed to report exception', reportError)
  }
}

export function installClientErrorHandlers(app: App, router: Router): void {
  app.config.errorHandler = (error, _instance, info) => {
    logClientError('VUE_RUNTIME_ERROR', error, {
      route: getCurrentRoute(router),
      info,
    })
  }

  globalThis.addEventListener('error', (event) => {
    logClientError('WINDOW_ERROR', event.error ?? event.message, {
      route: getCurrentRoute(router),
      info: event.filename ? `${event.filename}:${event.lineno}:${event.colno}` : undefined,
    })
  })

  globalThis.addEventListener('unhandledrejection', (event) => {
    logClientError('UNHANDLED_REJECTION', event.reason, {
      route: getCurrentRoute(router),
    })
  })

  router.onError((error) => {
    logClientError('ROUTER_ERROR', error, {
      route: getCurrentRoute(router),
    })
  })
}

export function reportApiServerError(error: unknown): void {
  if (!axios.isAxiosError(error) || !error.response) {
    return
  }
  const status = error.response.status
  if (status < 500) {
    return
  }
  if (shouldSkipReporting(error.config?.url)) {
    return
  }

  const requestId = getRequestIdFromHeaders(error.response.headers)
  const data = error.response.data
  const backendMessage =
    data && typeof data === 'object' && 'message' in data
      ? String((data as { message?: string }).message ?? '')
      : ''

  logClientError('API_SERVER_ERROR', error, {
    requestId,
    httpStatus: status,
    httpMethod: error.config?.method?.toUpperCase(),
    requestPath: error.config?.url,
    route: sanitizePath(globalThis.location?.pathname),
    info: backendMessage || `HTTP ${status}`,
  })
}
