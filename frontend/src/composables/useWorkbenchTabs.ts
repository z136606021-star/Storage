import { ref } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import {
  DEFAULT_ADMIN_TAB_PRESETS,
  DEFAULT_USER_TAB_PRESET,
} from '@/constants/shellRouteRegistry'

export interface WorkbenchTab {
  key: string
  title: string
  path: string
  closable: boolean
}

const tabs = ref<WorkbenchTab[]>([])
const presetsInitialized = ref(false)

function toTab(path: string, title: string, closable = true): WorkbenchTab {
  return { key: path, path, title, closable }
}

function addTab(path: string, title: string, closable = true) {
  if (tabs.value.some((tab) => tab.path === path)) {
    return
  }
  tabs.value.push(toTab(path, title, closable))
}

function initPresets(hasPermission: (permission: string) => boolean) {
  if (presetsInitialized.value) {
    return
  }
  presetsInitialized.value = true

  const isAdminLike = DEFAULT_ADMIN_TAB_PRESETS.some((preset) => hasPermission(preset.permission))
  if (isAdminLike) {
    for (const preset of DEFAULT_ADMIN_TAB_PRESETS) {
      if (hasPermission(preset.permission)) {
        addTab(preset.path, preset.title)
      }
    }
    return
  }

  if (hasPermission(DEFAULT_USER_TAB_PRESET.permission)) {
    addTab(DEFAULT_USER_TAB_PRESET.path, DEFAULT_USER_TAB_PRESET.title)
  }
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
  if (tabs.value.length === 0) {
    return '/warehouse/material-ledger'
  }
  const nextIndex = index >= tabs.value.length ? tabs.value.length - 1 : index
  return tabs.value[nextIndex]?.path ?? '/warehouse/material-ledger'
}

function clearTabs() {
  tabs.value = []
  presetsInitialized.value = false
}

export function useWorkbenchTabs() {
  return {
    tabs,
    initPresets,
    syncTabFromRoute,
    removeTab,
    clearTabs,
  }
}
