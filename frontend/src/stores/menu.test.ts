import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router/routes'
import { useMenuStore } from '@/stores/menu'
import type { NavMenuNode } from '@/types/system'

vi.mock('@/api/menu', () => ({
  fetchNavTree: vi.fn(),
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes,
  })
}

describe('menu store dynamic routes', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('loads nav tree and registers known component routes', async () => {
    const { fetchNavTree } = await import('@/api/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '111',
          label: '物料台账',
          path: '/warehouse/material-ledger',
          permission: 'warehouse:material-ledger:read',
          componentKey: 'MaterialLedger',
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(menu.navTree).toHaveLength(1)
    expect(router.hasRoute('DynamicMenu111')).toBe(true)
    expect(router.resolve('/warehouse/material-ledger').matched.at(-1)?.meta.permission).toBe(
      'warehouse:material-ledger:read',
    )
  })

  it('skips unknown component keys without blocking other routes', async () => {
    const { fetchNavTree } = await import('@/api/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '111',
          label: '物料台账',
          path: '/warehouse/material-ledger',
          permission: 'warehouse:material-ledger:read',
          componentKey: 'MaterialLedger',
        },
        {
          key: '999',
          label: '未知页面',
          path: '/unknown',
          permission: 'unknown:read',
          componentKey: 'UnknownComponent',
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(router.hasRoute('DynamicMenu111')).toBe(true)
    expect(router.hasRoute('DynamicMenu999')).toBe(false)
  })

  it('clears registered routes on logout cleanup', async () => {
    const { fetchNavTree } = await import('@/api/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '111',
          label: '物料台账',
          path: '/warehouse/material-ledger',
          permission: 'warehouse:material-ledger:read',
          componentKey: 'MaterialLedger',
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()
    await menu.ensureDynamicRoutes(router)

    menu.clearMenusAndRoutes(router)

    expect(menu.navTree).toEqual([])
    expect(router.hasRoute('DynamicMenu111')).toBe(false)
  })
})
