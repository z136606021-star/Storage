import { ref } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export interface WorkbenchTab {
  key: string
  title: string
  path: string
  fullPath: string
  closable: boolean
  refreshRevision: number
}

export const PERSONAL_CENTER_PERMISSION = 'platform:personal:read'

const tabs = ref<WorkbenchTab[]>([])

function toTab(
  path: string,
  title: string,
  fullPath: string,
  closable = true,
): WorkbenchTab {
  return {
    key: path,
    path,
    fullPath,
    title,
    closable,
    refreshRevision: 0,
  }
}

function findTab(path: string): WorkbenchTab | undefined {
  return tabs.value.find((tab) => tab.path === path)
}

function tabNavigationTarget(path: string | null): string | null {
  if (!path) {
    return null
  }
  return findTab(path)?.fullPath ?? path
}

function upsertTabFromRoute(route: RouteLocationNormalizedLoaded) {
  const existing = findTab(route.path)
  if (existing) {
    existing.fullPath = route.fullPath
    return
  }
  const title = String(route.meta.title)
  const closable = route.meta.tabClosable !== false
  tabs.value.push(toTab(route.path, title, route.fullPath, closable))
}

function syncTabFromRoute(route: RouteLocationNormalizedLoaded) {
  if (route.meta.skipTab || !route.meta.title) {
    return
  }
  const requiresAuth = route.matched.some((record) => record.meta.requiresAuth)
  if (!requiresAuth) {
    return
  }
  upsertTabFromRoute(route)
}

function removeTab(path: string): string | null {
  const index = tabs.value.findIndex((tab) => tab.path === path)
  if (index < 0) {
    return null
  }
  if (tabs.value.length <= 1) {
    return null
  }
  const tab = tabs.value[index]
  if (!tab.closable) {
    return null
  }
  tabs.value.splice(index, 1)
  const nextIndex = index >= tabs.value.length ? tabs.value.length - 1 : index
  return tabNavigationTarget(tabs.value[nextIndex]?.path ?? null)
}

function clearTabs() {
  tabs.value = []
}

function clearAllTabs() {
  clearTabs()
}

function replaceTabsWithRoute(route: RouteLocationNormalizedLoaded) {
  if (route.meta.skipTab || !route.meta.title) {
    clearTabs()
    return
  }
  const requiresAuth = route.matched.some((record) => record.meta.requiresAuth)
  if (!requiresAuth) {
    clearTabs()
    return
  }
  tabs.value = [toTab(route.path, String(route.meta.title), route.fullPath, route.meta.tabClosable !== false)]
}

function requestRefresh(path: string) {
  const tab = findTab(path)
  if (!tab) {
    return
  }
  tab.refreshRevision += 1
}

export function canCloseTabsLeft(tabList: WorkbenchTab[], path: string): boolean {
  const index = tabList.findIndex((tab) => tab.path === path)
  if (index <= 0) {
    return false
  }
  return tabList.slice(0, index).some((tab) => tab.closable)
}

export function canCloseTabsRight(tabList: WorkbenchTab[], path: string): boolean {
  const index = tabList.findIndex((tab) => tab.path === path)
  if (index < 0 || index >= tabList.length - 1) {
    return false
  }
  return tabList.slice(index + 1).some((tab) => tab.closable)
}

export function canCloseOtherTabs(tabList: WorkbenchTab[], path: string): boolean {
  if (!tabList.some((tab) => tab.path === path)) {
    return false
  }
  return tabList.some((tab) => tab.path !== path && tab.closable)
}

function closeTabsLeft(path: string, activePath: string | null): string | null {
  const index = tabs.value.findIndex((tab) => tab.path === path)
  if (index < 0) {
    return null
  }
  const removedPaths = tabs.value
    .slice(0, index)
    .filter((tab) => tab.closable)
    .map((tab) => tab.path)
  if (removedPaths.length === 0) {
    return null
  }
  tabs.value = tabs.value.filter((tab, tabIndex) => tabIndex >= index || !tab.closable)
  if (activePath && removedPaths.includes(activePath)) {
    return tabNavigationTarget(path)
  }
  return null
}

function closeTabsRight(path: string, activePath: string | null): string | null {
  const index = tabs.value.findIndex((tab) => tab.path === path)
  if (index < 0) {
    return null
  }
  const removedPaths = tabs.value
    .slice(index + 1)
    .filter((tab) => tab.closable)
    .map((tab) => tab.path)
  if (removedPaths.length === 0) {
    return null
  }
  tabs.value = tabs.value.filter((tab, tabIndex) => tabIndex <= index || !tab.closable)
  if (activePath && removedPaths.includes(activePath)) {
    return tabNavigationTarget(path)
  }
  return null
}

function closeOtherTabs(path: string, activePath: string | null): string | null {
  if (!tabs.value.some((tab) => tab.path === path)) {
    return null
  }
  const removedPaths = tabs.value
    .filter((tab) => tab.path !== path && tab.closable)
    .map((tab) => tab.path)
  if (removedPaths.length === 0) {
    return null
  }
  tabs.value = tabs.value.filter((tab) => tab.path === path || !tab.closable)
  if (activePath && removedPaths.includes(activePath)) {
    return tabNavigationTarget(path)
  }
  return null
}

function moveTab(sourcePath: string, targetPath: string): boolean {
  if (sourcePath === targetPath) {
    return false
  }
  const fromIndex = tabs.value.findIndex((tab) => tab.path === sourcePath)
  if (fromIndex < 0) {
    return false
  }
  const [moved] = tabs.value.splice(fromIndex, 1)
  const toIndex = tabs.value.findIndex((tab) => tab.path === targetPath)
  if (toIndex < 0) {
    tabs.value.splice(fromIndex, 0, moved)
    return false
  }
  tabs.value.splice(toIndex, 0, moved)
  return true
}

export function resolveClearAllTargetPath(
  findRouteByPermission: (permission: string) => { path: string } | null,
  getDefaultRoute: () => { path: string } | null,
): string | null {
  return findRouteByPermission(PERSONAL_CENTER_PERMISSION)?.path
    ?? getDefaultRoute()?.path
    ?? null
}

export function canClearAllTabs(tabList: WorkbenchTab[], personalCenterPath: string | null): boolean {
  if (tabList.length === 0) {
    return false
  }
  if (tabList.length === 1 && personalCenterPath && tabList[0].path === personalCenterPath) {
    return false
  }
  return true
}

export function useWorkbenchTabs() {
  return {
    tabs,
    syncTabFromRoute,
    removeTab,
    closeTabsLeft,
    closeTabsRight,
    closeOtherTabs,
    moveTab,
    requestRefresh,
    replaceTabsWithRoute,
    clearTabs,
    clearAllTabs,
    findTab,
    tabNavigationTarget,
  }
}
