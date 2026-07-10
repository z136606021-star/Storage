import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router/routes'
import { useMenuStore } from '@/stores/menu'
import type { NavMenuNode } from '@/types/system'

const MATERIAL_LEDGER_COMPONENT = 'views/warehouse/MaterialLedgerView.vue'
const INVENTORY_STATS_COMPONENT = 'views/warehouse/InventoryStatsView.vue'
const USER_MANAGE_COMPONENT = 'views/system/UserManageView.vue'
const ROLE_MANAGE_COMPONENT = 'components/system/RoleManagePanel.vue'
const MENU_MANAGE_COMPONENT = 'components/system/MenuManagePanel.vue'

vi.mock('@/api/system/menu', () => ({
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
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '111',
          label: '物料台账',
          path: '/configured-ledger',
          permission: 'warehouse:material-ledger:read',
          componentKey: MATERIAL_LEDGER_COMPONENT,
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(menu.navTree).toHaveLength(1)
    expect(router.hasRoute('DynamicMenu111')).toBe(true)
    expect(router.resolve('/configured-ledger').matched.at(-1)?.meta.permission).toBe(
      'warehouse:material-ledger:read',
    )
  })

  it('skips unknown component paths without blocking other routes', async () => {
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '111',
          label: '物料台账',
          path: '/configured-ledger',
          permission: 'warehouse:material-ledger:read',
          componentKey: MATERIAL_LEDGER_COMPONENT,
        },
        {
          key: '999',
          label: '未知页面',
          path: '/unknown',
          permission: 'unknown:read',
          componentKey: 'views/warehouse/UnknownView.vue',
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(router.hasRoute('DynamicMenu111')).toBe(true)
    expect(router.hasRoute('DynamicMenu999')).toBe(false)
  })

  it('registers flat system-management routes from menu tree', async () => {
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '201',
          label: '用户管理',
          path: '/system/users',
          permission: 'system:user:read',
          componentKey: USER_MANAGE_COMPONENT,
          visible: 1,
        },
        {
          key: '202',
          label: '角色管理',
          path: '/system/roles',
          permission: 'system:role:read',
          componentKey: ROLE_MANAGE_COMPONENT,
          visible: 1,
        },
        {
          key: '203',
          label: '菜单管理',
          path: '/system/menus',
          permission: 'system:menu:read',
          componentKey: MENU_MANAGE_COMPONENT,
          visible: 1,
        },
        {
          key: '204',
          label: '客户管理',
          path: '/system/customers',
          permission: 'system:customer:read',
          componentKey: 'views/system/CustomerManageView.vue',
          visible: 1,
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(router.hasRoute('UserManage')).toBe(true)
    expect(router.hasRoute('RoleManage')).toBe(true)
    expect(router.hasRoute('MenuManage')).toBe(true)
    expect(router.hasRoute('DynamicMenu204')).toBe(true)
    expect(router.resolve('/system/users').matched.some((record) => record.name === 'UserManage')).toBe(true)
    expect(router.resolve('/system/roles').matched.some((record) => record.name === 'RoleManage')).toBe(true)
    expect(router.resolve('/system/menus').matched.some((record) => record.name === 'MenuManage')).toBe(true)
    expect(router.resolve('/system/customers').matched.some((record) => record.name === 'DynamicMenu204')).toBe(true)
    expect(menu.findRouteByPermission('system:user:read')?.path).toBe('/system/users')
    expect(menu.findRouteByPermission('system:role:read')?.path).toBe('/system/roles')
    expect(menu.findRouteByPermission('system:customer:read')?.path).toBe('/system/customers')
  })

  it('skips menu routes without path instead of throwing during registration', async () => {
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '201',
          label: '无路径菜单',
          permission: 'system:user:read',
          componentKey: USER_MANAGE_COMPONENT,
          visible: 1,
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(router.hasRoute('UserManage')).toBe(false)
    expect(menu.getDefaultRoute()).toBeNull()
  })

  it('prefers inventory stats as default route when authorized', async () => {
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '100',
          label: '资源管理',
          visible: 1,
          children: [
            {
              key: '111',
              label: '物料台账',
              path: '/configured-ledger',
              permission: 'warehouse:material-ledger:read',
              componentKey: MATERIAL_LEDGER_COMPONENT,
              visible: 1,
            },
            {
              key: '117',
              label: '库存统计',
              path: '/configured-stats',
              permission: 'warehouse:stats:read',
              componentKey: INVENTORY_STATS_COMPONENT,
              visible: 1,
            },
          ],
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(menu.getDefaultRoute()).toEqual(expect.objectContaining({
      label: '库存统计',
      path: '/configured-stats',
      componentKey: INVENTORY_STATS_COMPONENT,
    }))
  })

  it('falls back to first visible route when inventory stats is unavailable', async () => {
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '100',
          label: '资源管理',
          visible: 1,
          children: [
            {
              key: '111',
              label: '物料台账',
              path: '/configured-ledger',
              permission: 'warehouse:material-ledger:read',
              componentKey: MATERIAL_LEDGER_COMPONENT,
              visible: 1,
            },
          ],
        },
      ] satisfies NavMenuNode[],
    } as never)
    const router = createTestRouter()
    const menu = useMenuStore()

    await menu.ensureDynamicRoutes(router)

    expect(menu.getDefaultRoute()).toEqual(expect.objectContaining({
      label: '物料台账',
      path: '/configured-ledger',
      componentKey: MATERIAL_LEDGER_COMPONENT,
    }))
  })

  it('clears registered routes on logout cleanup', async () => {
    const { fetchNavTree } = await import('@/api/system/menu')
    vi.mocked(fetchNavTree).mockResolvedValue({
      data: [
        {
          key: '111',
          label: '物料台账',
          path: '/configured-ledger',
          permission: 'warehouse:material-ledger:read',
          componentKey: MATERIAL_LEDGER_COMPONENT,
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

describe('legacy system-management redirects', () => {
  it('redirects nested role and menu paths to flat routes', () => {
    const router = createTestRouter()
    const appRoot = router.getRoutes().find((route) => route.name === 'AppRoot')
    const roleRedirect = appRoot?.children?.find((route) => route.path === 'system/users/roles')
    const menuRedirect = appRoot?.children?.find((route) => route.path === 'system/users/menus')

    expect(roleRedirect?.redirect).toBe('/system/roles')
    expect(menuRedirect?.redirect).toBe('/system/menus')
  })
})
