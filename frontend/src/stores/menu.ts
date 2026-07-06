import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Router } from 'vue-router'
import { fetchNavTree } from '@/api/system/menu'
import type { NavMenuNode } from '@/types/system'
import { clearDynamicRoutes, registerDynamicRoutes } from '@/router/dynamicRoutes'

let boundRouter: Router | null = null

export function bindMenuRouter(router: Router) {
  boundRouter = router
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
  }
})
