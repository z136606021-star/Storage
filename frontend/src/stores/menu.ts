import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Router } from 'vue-router'
import { fetchNavTree } from '@/api/system/menu'
import type { NavMenuNode } from '@/types/system'
import { clearDynamicRoutes, registerDynamicRoutes } from '@/router/dynamicRoutes'

let boundRouter: Router | null = null

export interface MenuRouteInfo {
  key: string
  label: string
  path: string
  permission?: string | null
  componentKey?: string | null
  visible?: number
}

export function bindMenuRouter(router: Router) {
  boundRouter = router
}

function joinMenuPath(path: string | undefined, parentPath?: string): string | null {
  if (!path) {
    return null
  }
  if (path.startsWith('/')) {
    return path
  }
  if (!parentPath) {
    return `/${path}`
  }
  return `${parentPath.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
}

export const useMenuStore = defineStore('menu', () => {
  const navTree = ref<NavMenuNode[]>([])
  const loading = ref(false)
  const loaded = ref(false)
  const registered = ref(false)
  const loadingPromise = ref<Promise<void> | null>(null)

  async function loadNavTree(force = false) {
    if (loaded.value && !force) {
      return
    }
    if (loadingPromise.value) {
      await loadingPromise.value
      return
    }

    loading.value = true
    loadingPromise.value = (async () => {
      const { data } = await fetchNavTree()
      navTree.value = data
      loaded.value = true
      registered.value = false
    })()

    try {
      await loadingPromise.value
    } finally {
      loading.value = false
      loadingPromise.value = null
    }
  }

  async function ensureDynamicRoutes(router: Router = boundRouter!) {
    if (!router) {
      return false
    }
    await loadNavTree()
    if (!registered.value) {
      registerDynamicRoutes(router, navTree.value)
      registered.value = true
      return true
    }
    return false
  }

  function clearMenusAndRoutes(router: Router | null = boundRouter) {
    navTree.value = []
    loading.value = false
    loaded.value = false
    loadingPromise.value = null
    registered.value = false
    if (router) {
      clearDynamicRoutes(router)
    }
  }

  function visitMenuRoutes(
    visitor: (node: NavMenuNode, fullPath: string) => boolean | void,
    nodes: NavMenuNode[] = navTree.value,
    parentPath?: string,
  ): MenuRouteInfo | null {
    for (const node of nodes) {
      const fullPath = joinMenuPath(node.path, parentPath)
      if (fullPath) {
        const matched = visitor(node, fullPath)
        if (matched) {
          return {
            key: node.key,
            label: node.label,
            path: fullPath,
            permission: node.permission,
            componentKey: node.componentKey,
            visible: node.visible,
          }
        }
      }
      const childMatched = visitMenuRoutes(visitor, node.children ?? [], fullPath ?? parentPath)
      if (childMatched) {
        return childMatched
      }
    }
    return null
  }

  function collectChildRoutesByPermission(parentPermission: string): MenuRouteInfo[] {
    let children: MenuRouteInfo[] = []
    visitMenuRoutes((node, fullPath) => {
      if (node.permission !== parentPermission) {
        return false
      }
      children = (node.children ?? [])
        .map((child) => {
          const childPath = joinMenuPath(child.path, fullPath)
          if (!childPath || !child.componentKey) {
            return null
          }
          const route: MenuRouteInfo = {
            key: child.key,
            label: child.label,
            path: childPath,
            permission: child.permission,
            componentKey: child.componentKey,
            visible: child.visible,
          }
          return route
        })
        .filter((route): route is MenuRouteInfo => route !== null)
      return true
    })
    return children
  }

  function findRouteByPermission(permission: string): MenuRouteInfo | null {
    return visitMenuRoutes((node) => node.permission === permission)
  }

  function findRouteByPath(path: string): MenuRouteInfo | null {
    return visitMenuRoutes((_node, fullPath) => fullPath === path)
  }

  function getDefaultRoute(): MenuRouteInfo | null {
    return visitMenuRoutes((node) => Boolean(node.componentKey && (node.visible === undefined || node.visible === 1)))
  }

  const hasMenus = computed(() => navTree.value.length > 0)

  return {
    navTree,
    loading,
    loaded,
    registered,
    hasMenus,
    loadNavTree,
    ensureDynamicRoutes,
    clearMenusAndRoutes,
    collectChildRoutesByPermission,
    findRouteByPermission,
    findRouteByPath,
    getDefaultRoute,
  }
})
