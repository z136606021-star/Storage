import { ref } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export interface WorkbenchTab {
  key: string
  title: string
  path: string
  closable: boolean
}

const tabs = ref<WorkbenchTab[]>([])

function toTab(path: string, title: string, closable = true): WorkbenchTab {
  return { key: path, path, title, closable }
}

function addTab(path: string, title: string, closable = true) {
  if (tabs.value.some((tab) => tab.path === path)) {
    return
  }
  tabs.value.push(toTab(path, title, closable))
}

function syncTabFromRoute(route: RouteLocationNormalizedLoaded) {
  if (route.meta.skipTab || !route.meta.title) {
    return
  }
  const requiresAuth = route.matched.some((record) => record.meta.requiresAuth)
  if (!requiresAuth) {
    return
  }
  const title = String(route.meta.title)
  const closable = route.meta.tabClosable !== false
  addTab(route.path, title, closable)
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
  return tabs.value[nextIndex]?.path ?? null
}

function clearTabs() {
  tabs.value = []
}

export function useWorkbenchTabs() {
  return {
    tabs,
    syncTabFromRoute,
    removeTab,
    clearTabs,
  }
}
