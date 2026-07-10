import { onActivated, onDeactivated, ref, watch, type Ref } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'

export type CrudRouteDetailClearQueryMode = 'omitKey' | 'clearAll'

export interface UseCrudRouteDetailConfig {
  route: RouteLocationNormalizedLoaded
  router: Router
  openDetail: (record: { id: number }) => void | Promise<void>
  queryKey: string
  parseId: (raw: unknown) => number | null
  rowHighlightClass: string
  clearQueryMode?: CrudRouteDetailClearQueryMode
  onRouteIdChange?: (id: number | null) => void | Promise<void>
}

export function useCrudRouteDetail(config: UseCrudRouteDetailConfig) {
  const highlightRecordId = ref<number | null>(null)
  const clearQueryMode = config.clearQueryMode ?? 'omitKey'
  const ownerPath = config.route.path
  let isActive = true

  onActivated(() => {
    isActive = true
    const id = config.parseId(config.route.query[config.queryKey])
    if (id != null && highlightRecordId.value !== id) {
      void applyRouteDetail(id)
      return
    }
    if (id == null && highlightRecordId.value != null) {
      highlightRecordId.value = null
      void config.onRouteIdChange?.(null)
    }
  })

  onDeactivated(() => {
    isActive = false
  })

  function ownsCurrentRoute() {
    return isActive && config.route.path === ownerPath
  }

  async function applyRouteDetail(id: number) {
    highlightRecordId.value = id
    await config.onRouteIdChange?.(id)
    await config.openDetail({ id })
  }

  async function clearRouteDetailQuery() {
    highlightRecordId.value = null
    if (config.route.query[config.queryKey]) {
      if (clearQueryMode === 'clearAll') {
        await config.router.replace({ path: config.route.path })
      } else {
        const query = { ...config.route.query }
        delete query[config.queryKey]
        await config.router.replace({ path: config.route.path, query })
      }
    }
    await config.onRouteIdChange?.(null)
  }

  async function initFromRoute(): Promise<boolean> {
    const id = config.parseId(config.route.query[config.queryKey])
    if (id == null) {
      return false
    }
    await applyRouteDetail(id)
    return true
  }

  function setupRouteWatch() {
    watch(
      () => config.route.query[config.queryKey],
      async (raw) => {
        if (!ownsCurrentRoute()) {
          return
        }
        const id = config.parseId(raw)
        if (id != null) {
          if (highlightRecordId.value !== id) {
            await applyRouteDetail(id)
          }
          return
        }
        highlightRecordId.value = null
        await config.onRouteIdChange?.(null)
      },
    )
  }

  function customRow(record: { id: number }) {
    return {
      class: highlightRecordId.value === record.id ? config.rowHighlightClass : '',
    }
  }

  return {
    highlightRecordId: highlightRecordId as Readonly<Ref<number | null>>,
    initFromRoute,
    setupRouteWatch,
    clearRouteDetailQuery,
    applyRouteDetail,
    customRow,
  }
}
