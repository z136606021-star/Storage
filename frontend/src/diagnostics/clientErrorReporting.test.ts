import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type InternalAxiosRequestConfig } from 'axios'
import { getErrorMessage } from '@/api/http'
import {
  getRequestIdFromHeaders,
  logClientError,
  reportClientError,
} from '@/diagnostics/clientErrorReporting'

const reportFrontendException = vi.fn()
const useAuthStore = vi.fn()

vi.mock('@/api/system/exceptionLog', () => ({
  reportFrontendException: (...args: unknown[]) => reportFrontendException(...args),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => useAuthStore(),
  ACCESS_TOKEN_KEY: 'storage.accessToken',
}))

function mockResponse(status: number, data: unknown, headers: Record<string, string> = {}) {
  return {
    status,
    data,
    statusText: String(status),
    headers,
    config: { headers: {} } as InternalAxiosRequestConfig,
  }
}

describe('getErrorMessage', () => {
  it('returns timeout message for aborted requests', () => {
    const error = new axios.AxiosError('timeout', 'ECONNABORTED')
    expect(getErrorMessage(error, '请求失败')).toBe('请求超时，请检查网络或稍后重试')
  })

  it('returns network message when response is missing', () => {
    const error = new axios.AxiosError('network')
    expect(getErrorMessage(error, '请求失败')).toBe('网络连接失败，请检查网络或代理配置')
  })

  it('returns explicit 413 message', () => {
    const error = new axios.AxiosError(
      'payload too large',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      mockResponse(413, '<html>Request Entity Too Large</html>'),
    )
    expect(getErrorMessage(error, '请求失败')).toBe('文件超过服务器允许大小（HTTP 413）')
  })

  it('extracts backend JSON message and requestId', () => {
    const error = new axios.AxiosError(
      'server error',
      'ERR_BAD_RESPONSE',
      undefined,
      undefined,
      mockResponse(500, { message: '服务器内部错误，请联系管理员', requestId: 'req-123' }),
    )
    expect(getErrorMessage(error, '请求失败')).toBe('服务器内部错误，请联系管理员（请求ID: req-123）')
  })
})

describe('clientErrorReporting', () => {
  beforeEach(() => {
    reportFrontendException.mockReset()
    useAuthStore.mockReturnValue({
      accessToken: 'token-1',
      session: { permissions: [] },
    })
  })

  it('extracts request id from response headers', () => {
    expect(getRequestIdFromHeaders({ 'x-request-id': 'abc123' })).toBe('abc123')
  })

  it('reports authenticated client errors once within dedup window', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    const payload = {
      errorCode: 'VUE_RUNTIME_ERROR' as const,
      summary: 'render failed',
      frontendRoute: '/warehouse/material-ledger',
      browserInfo: 'vitest',
      reportType: 'FRONTEND' as const,
    }

    await reportClientError(payload)
    await reportClientError(payload)

    expect(reportFrontendException).toHaveBeenCalledTimes(1)
    consoleError.mockRestore()
  })

  it('skips reporting when user is not authenticated', async () => {
    useAuthStore.mockReturnValue({
      accessToken: null,
      session: null,
    })

    await reportClientError({
      errorCode: 'WINDOW_ERROR',
      summary: 'window crash',
    })

    expect(reportFrontendException).not.toHaveBeenCalled()
  })

  it('logs client errors to console', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    logClientError('UNHANDLED_REJECTION', new Error('promise failed'), {
      route: '/login',
    })

    expect(consoleError).toHaveBeenCalled()
    consoleError.mockRestore()
  })
})
