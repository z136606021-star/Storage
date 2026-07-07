import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { http } from '@/api/http'
import { ACCESS_TOKEN_KEY, useAuthStore } from '@/stores/auth'

const { clearTabsMock } = vi.hoisted(() => ({
  clearTabsMock: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  fetchMe: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/composables/useWorkbenchTabs', () => ({
  useWorkbenchTabs: () => ({
    clearTabs: clearTabsMock,
  }),
}))

vi.mock('@/stores/menu', () => ({
  useMenuStore: () => ({
    clearMenusAndRoutes: vi.fn(),
  }),
}))

const storage = new Map<string, string>()

function stubLocalStorage() {
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => {
      storage.set(key, value)
    },
    removeItem: (key: string) => {
      storage.delete(key)
    },
  })
}

describe('auth store', () => {
  beforeEach(() => {
    storage.clear()
    clearTabsMock.mockClear()
    stubLocalStorage()
    setActivePinia(createPinia())
  })

  it('restores session from stored token', async () => {
    const authApi = await import('@/api/auth')
    vi.mocked(authApi.fetchMe).mockResolvedValue({
      data: {
        user: { id: 1, username: 'admin', displayName: '管理员' },
        roles: ['ADMIN'],
        permissions: ['warehouse:material-ledger:read'],
      },
    } as never)
    storage.set(ACCESS_TOKEN_KEY, 'stored-token')

    const auth = useAuthStore()
    await auth.initialize()

    expect(auth.accessToken).toBe('stored-token')
    expect(auth.session?.user.username).toBe('admin')
  })

  it('login stores access token and permissions can be checked', async () => {
    const authApi = await import('@/api/auth')
    vi.mocked(authApi.login).mockResolvedValue({
      data: {
        user: { id: 1, username: 'admin', displayName: '管理员' },
        roles: ['ADMIN'],
        permissions: ['warehouse:material-ledger:read'],
        accessToken: 'login-token',
      },
    } as never)

    const auth = useAuthStore()
    await auth.login({ username: 'admin', password: 'admin123' })

    expect(storage.get(ACCESS_TOKEN_KEY)).toBe('login-token')
    expect(auth.hasPermission('warehouse:material-ledger:read')).toBe(true)
    expect(auth.hasPermission('system:user:write')).toBe(false)
  })

  it('logout clears token, session and workbench tabs', async () => {
    const authApi = await import('@/api/auth')
    vi.mocked(authApi.logout).mockResolvedValue({} as never)
    const auth = useAuthStore()
    auth.clearSession()
    storage.set(ACCESS_TOKEN_KEY, 'old-token')
    auth.accessToken = 'old-token'
    auth.session = {
      user: { id: 1, username: 'admin', displayName: '管理员' },
      roles: ['ADMIN'],
      permissions: [],
    }

    await auth.logout()

    expect(auth.accessToken).toBeNull()
    expect(auth.session).toBeNull()
    expect(storage.get(ACCESS_TOKEN_KEY)).toBeUndefined()
    expect(clearTabsMock).toHaveBeenCalled()
  })

  it('initialize clears session when stored token is invalid', async () => {
    const authApi = await import('@/api/auth')
    vi.mocked(authApi.fetchMe).mockRejectedValue(new Error('401'))
    storage.set(ACCESS_TOKEN_KEY, 'invalid-token')

    const auth = useAuthStore()
    await auth.initialize()

    expect(auth.accessToken).toBeNull()
    expect(auth.session).toBeNull()
    expect(storage.get(ACCESS_TOKEN_KEY)).toBeUndefined()
  })

  it('http request interceptor injects bearer token', async () => {
    storage.set(ACCESS_TOKEN_KEY, 'request-token')
    const handler = (http.interceptors.request as never as {
      handlers: Array<{ fulfilled: (config: { headers: Record<string, string> }) => { headers: Record<string, string> } }>
    }).handlers[0].fulfilled
    const config = await handler({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer request-token')
  })
})
