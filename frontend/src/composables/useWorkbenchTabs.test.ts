import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { beforeEach, describe, expect, it } from 'vitest'
import {
  canClearAllTabs,
  canCloseOtherTabs,
  canCloseTabsLeft,
  canCloseTabsRight,
  PERSONAL_CENTER_PERMISSION,
  resolveClearAllTargetPath,
  useWorkbenchTabs,
  type WorkbenchTab,
} from '@/composables/useWorkbenchTabs'

function tabFixture(
  key: string,
  path: string,
  title: string,
  closable: boolean,
): WorkbenchTab {
  return {
    key,
    path,
    fullPath: path,
    title,
    closable,
    refreshRevision: 0,
  }
}

function createRoute(
  path: string,
  title: string,
  query: Record<string, string> = {},
): RouteLocationNormalizedLoaded {
  const search = new URLSearchParams(query).toString()
  const fullPath = search ? `${path}?${search}` : path

  return {
    path,
    fullPath,
    query,
    meta: { title, requiresAuth: true },
    matched: [{ meta: { requiresAuth: true } }],
  } as RouteLocationNormalizedLoaded
}

function seedTabs(workbenchTabs: ReturnType<typeof useWorkbenchTabs>) {
  workbenchTabs.syncTabFromRoute(createRoute('/platform/personal', '个人中心'))
  workbenchTabs.syncTabFromRoute(createRoute('/warehouse/bin', 'Bin位管理'))
  workbenchTabs.syncTabFromRoute(createRoute('/warehouse/bom', '物料清单管理'))
  workbenchTabs.syncTabFromRoute(createRoute('/warehouse/material-ledger', '物料台账'))
}

describe('useWorkbenchTabs helpers', () => {
  it('prefers personal center as clear-all target path', () => {
    const targetPath = resolveClearAllTargetPath(
      (permission) => (permission === PERSONAL_CENTER_PERMISSION ? { path: '/platform/personal' } : null),
      () => ({ path: '/warehouse/inventory-stats' }),
    )

    expect(targetPath).toBe('/platform/personal')
  })

  it('falls back to default route when personal center is unavailable', () => {
    const targetPath = resolveClearAllTargetPath(
      () => null,
      () => ({ path: '/warehouse/material-ledger' }),
    )

    expect(targetPath).toBe('/warehouse/material-ledger')
  })

  it('disables clear-all when there are no tabs', () => {
    expect(canClearAllTabs([], '/platform/personal')).toBe(false)
  })

  it('disables clear-all when only personal center tab remains', () => {
    const tabs: WorkbenchTab[] = [
      tabFixture('/platform/personal', '/platform/personal', '个人中心', true),
    ]

    expect(canClearAllTabs(tabs, '/platform/personal')).toBe(false)
  })

  it('enables clear-all when multiple tabs are open', () => {
    const tabs: WorkbenchTab[] = [
      tabFixture('/platform/personal', '/platform/personal', '个人中心', true),
      tabFixture('/warehouse/material-ledger', '/warehouse/material-ledger', '物料台账', true),
    ]

    expect(canClearAllTabs(tabs, '/platform/personal')).toBe(true)
  })

  it('detects closable tabs on the left and right', () => {
    const tabs: WorkbenchTab[] = [
      tabFixture('/a', '/a', 'A', true),
      tabFixture('/b', '/b', 'B', false),
      tabFixture('/c', '/c', 'C', true),
    ]

    expect(canCloseTabsLeft(tabs, '/c')).toBe(true)
    expect(canCloseTabsLeft(tabs, '/a')).toBe(false)
    expect(canCloseTabsRight(tabs, '/a')).toBe(true)
    expect(canCloseTabsRight(tabs, '/c')).toBe(false)
    expect(canCloseOtherTabs(tabs, '/b')).toBe(true)
    expect(canCloseOtherTabs(tabs, '/c')).toBe(true)
    expect(canCloseOtherTabs([tabs[1]], '/b')).toBe(false)
  })
})

describe('useWorkbenchTabs store', () => {
  beforeEach(() => {
    useWorkbenchTabs().clearTabs()
  })

  it('clears all tabs and re-syncs personal center from route', () => {
    const workbenchTabs = useWorkbenchTabs()

    seedTabs(workbenchTabs)

    expect(workbenchTabs.tabs.value).toHaveLength(4)

    workbenchTabs.clearAllTabs()
    expect(workbenchTabs.tabs.value).toHaveLength(0)

    workbenchTabs.syncTabFromRoute(createRoute('/platform/personal', '个人中心'))
    expect(workbenchTabs.tabs.value).toEqual([
      expect.objectContaining({
        path: '/platform/personal',
        title: '个人中心',
      }),
    ])
  })

  it('updates fullPath when the same tab is revisited with new query', () => {
    const workbenchTabs = useWorkbenchTabs()

    workbenchTabs.syncTabFromRoute(createRoute('/warehouse/material-ledger', '物料台账'))
    workbenchTabs.syncTabFromRoute(
      createRoute('/warehouse/material-ledger', '物料台账', { stockStatus: 'IN_STOCK' }),
    )

    expect(workbenchTabs.tabs.value).toHaveLength(1)
    expect(workbenchTabs.tabs.value[0].fullPath).toBe('/warehouse/material-ledger?stockStatus=IN_STOCK')
  })

  it('returns saved fullPath as navigation target after query changes', () => {
    const workbenchTabs = useWorkbenchTabs()

    workbenchTabs.syncTabFromRoute(
      createRoute('/warehouse/material-ledger', '物料台账', { page: '2' }),
    )

    expect(workbenchTabs.tabNavigationTarget('/warehouse/material-ledger')).toBe(
      '/warehouse/material-ledger?page=2',
    )
  })

  it('increments only the requested tab refresh revision', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)

    workbenchTabs.requestRefresh('/warehouse/bin')
    workbenchTabs.requestRefresh('/warehouse/bin')
    workbenchTabs.requestRefresh('/warehouse/bom')

    expect(workbenchTabs.findTab('/warehouse/bin')?.refreshRevision).toBe(2)
    expect(workbenchTabs.findTab('/warehouse/bom')?.refreshRevision).toBe(1)
    expect(workbenchTabs.findTab('/warehouse/material-ledger')?.refreshRevision).toBe(0)
  })

  it('replaceTabsWithRoute keeps only the target route tab', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)

    workbenchTabs.replaceTabsWithRoute(
      createRoute('/warehouse/material-ledger', '物料台账', { page: '3' }),
    )

    expect(workbenchTabs.tabs.value).toEqual([
      expect.objectContaining({
        path: '/warehouse/material-ledger',
        fullPath: '/warehouse/material-ledger?page=3',
        title: '物料台账',
      }),
    ])
  })

  it('closes closable tabs to the left and navigates when active tab is removed', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)
    workbenchTabs.tabs.value[0].closable = false

    const nextPath = workbenchTabs.closeTabsLeft('/warehouse/material-ledger', '/warehouse/bin')

    expect(nextPath).toBe('/warehouse/material-ledger')
    expect(workbenchTabs.tabs.value.map((tab) => tab.path)).toEqual([
      '/platform/personal',
      '/warehouse/material-ledger',
    ])
  })

  it('closes closable tabs to the right without navigation when active tab survives', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)

    const nextPath = workbenchTabs.closeTabsRight('/warehouse/bin', '/warehouse/bin')

    expect(nextPath).toBeNull()
    expect(workbenchTabs.tabs.value.map((tab) => tab.path)).toEqual([
      '/platform/personal',
      '/warehouse/bin',
    ])
  })

  it('closes other tabs while keeping target and non-closable tabs', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)
    workbenchTabs.tabs.value[0].closable = false

    const nextPath = workbenchTabs.closeOtherTabs('/warehouse/bin', '/warehouse/material-ledger')

    expect(nextPath).toBe('/warehouse/bin')
    expect(workbenchTabs.tabs.value.map((tab) => tab.path)).toEqual([
      '/platform/personal',
      '/warehouse/bin',
    ])
  })

  it('reorders tabs by drag source and target paths', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)

    expect(workbenchTabs.moveTab('/warehouse/material-ledger', '/platform/personal')).toBe(true)
    expect(workbenchTabs.tabs.value.map((tab) => tab.path)).toEqual([
      '/warehouse/material-ledger',
      '/platform/personal',
      '/warehouse/bin',
      '/warehouse/bom',
    ])

    expect(workbenchTabs.moveTab('/warehouse/bin', '/warehouse/material-ledger')).toBe(true)
    expect(workbenchTabs.tabs.value.map((tab) => tab.path)).toEqual([
      '/warehouse/bin',
      '/warehouse/material-ledger',
      '/platform/personal',
      '/warehouse/bom',
    ])
  })

  it('ignores invalid reorder and bulk-close targets', () => {
    const workbenchTabs = useWorkbenchTabs()
    seedTabs(workbenchTabs)

    expect(workbenchTabs.moveTab('/missing', '/warehouse/bin')).toBe(false)
    expect(workbenchTabs.moveTab('/warehouse/bin', '/missing')).toBe(false)
    expect(workbenchTabs.closeTabsLeft('/missing', '/warehouse/bin')).toBeNull()
    expect(workbenchTabs.closeTabsRight('/warehouse/material-ledger', '/warehouse/material-ledger')).toBeNull()
  })
})
